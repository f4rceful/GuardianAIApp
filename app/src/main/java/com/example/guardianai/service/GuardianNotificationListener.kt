package com.example.guardianai.service

import android.app.Notification
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.guardianai.network.PredictionRequest
import com.example.guardianai.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GuardianNotificationListener : NotificationListenerService() {

    private val TAG = "GuardianListener"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var windowManager: WindowManager? = null
    // Инициализация репозитория (централизованная логика)
    private val repository = com.example.guardianai.data.GuardianRepository()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Служба Guardian AI ПОДКЛЮЧЕНА! ✅ Готов к перехвату.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Служба Guardian AI ОТКЛЮЧЕНА! ❌")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        
        // Проверка: включена ли защита глобально
        if (!com.example.guardianai.data.SettingsManager.isProtectionEnabled.value) {
             return
        }

        // Проверка настроек
        val trustedApps = com.example.guardianai.data.SettingsManager.trustedApps.value
        if (trustedApps.contains(packageName)) {
            return
        }

        // Фильтр системных пакетов и нашего приложения
        if (packageName == "android" || packageName == "com.android.systemui" || packageName == this.packageName) {
            return
        }

        val extras = sbn.notification.extras
        // Получение заголовка и текста
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        
        // Проверка доверенных контактов
        val trustedContacts = com.example.guardianai.data.SettingsManager.trustedContacts.value
        if (trustedContacts.any { title.contains(it, ignoreCase = true) }) {
             return
        }

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (text.isEmpty()) return

        // Фильтр системных уведомлений о работе в фоне
        if (text.contains("doing work in the background", ignoreCase = true) ||
            title.contains("doing work in the background", ignoreCase = true) ||
            text.contains("is running", ignoreCase = true)) {
            return
        }

        Log.d(TAG, "Получено уведомление от $packageName: $title - $text")

        // Отправка на бэкенд для анализа
        scope.launch {
            try {
                // Объединение заголовка и текста
                val fullText = "$title. $text"
                val isStrict = com.example.guardianai.data.SettingsManager.isStrictMode.value
                
                // Определяем контекст (Имя приложения)
                val appName = getAppName(packageName)
                val context = listOf(appName)

                // ИСПОЛЬЗУЕМ REPOSITORY (включает WhiteList, Protocol checks, и ML)
                val response = repository.predict(fullText, strictMode = isStrict, context = context)
                
                // Логика отображения на основе ответа репозитория
                val isScam = response.is_scam
                val isWarning = response.score > 0.5f && !isScam // Warning если счет выше 0.5 но не скам

                withContext(Dispatchers.Main) {
                    if (isScam) {
                        Log.w(TAG, "SCAM DETECTED! Score: ${response.score}, Verdict: ${response.verdict}")
                        
                        // Формирование сообщения
                        val reasonText = response.reason.firstOrNull() ?: "Мошенничество"
                        
                        showOverlayAlert(reasonText, isWarning = false)
                        com.example.guardianai.data.HistoryManager.saveScan(applicationContext, fullText, response)
                        
                    } else if (isWarning) {
                        Log.w(TAG, "WARNING DETECTED! Score: ${response.score}")
                        
                        val reasonText = response.reason.lastOrNull() ?: "Подозрительно"
                        
                        showOverlayAlert(reasonText, isWarning = true)
                        com.example.guardianai.data.HistoryManager.saveScan(applicationContext, fullText, response, isWarning = true)
                        
                    } else {
                        Log.i(TAG, "Message is SAFE. Score: ${response.score}")
                        // Сохранение SAFE
                        com.example.guardianai.data.HistoryManager.saveScan(applicationContext, fullText, response)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking notification: ${e.message}")
            }
        }
    }

    private fun showOverlayAlert(reason: String, isWarning: Boolean) {
        Log.d(TAG, "Attempting to show overlay. Reason: $reason, Warning: $isWarning")
        
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay PERMISSION DENIED. Showing Toast instead.")
            Toast.makeText(applicationContext, "⚠️ SCAM DETECTED: $reason", Toast.LENGTH_LONG).show()
            return
        }
        Log.d(TAG, "Overlay permission GRANTED. Creating view...")

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                else 
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            params.y = 150

            // Основной контейнер (Пузырь)
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(48, 36, 48, 36)
                
                // Цвета в зависимости от типа
                val alertColor = if (isWarning) Color.parseColor("#FF9800") else Color.RED
                
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 50f
                    setStroke(4, alertColor)
                }
                elevation = 0f
            }

            // Иконка
            val icon = ImageView(this).apply {
                setImageResource(android.R.drawable.stat_notify_error)
                setColorFilter(if (isWarning) Color.parseColor("#FF9800") else Color.RED)
                layoutParams = LinearLayout.LayoutParams(96, 96).apply {
                    marginEnd = 32
                }
            }
            layout.addView(icon)

            // Контейнер текста
            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            val titleView = TextView(this).apply {
                text = if (isWarning) "ПОДОЗРИТЕЛЬНО" else "ОПАСНОСТЬ"
                textSize = 20f
                setTextColor(if (isWarning) Color.parseColor("#FF9800") else Color.RED)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            textLayout.addView(titleView)

            val reasonView = TextView(this).apply {
                text = reason
                textSize = 16f
                setTextColor(Color.DKGRAY)
                maxLines = 3
            }
            textLayout.addView(reasonView)

            layout.addView(textLayout)
            
            // Логика скрытия
            var isDismissing = false
            fun dismiss() {
                if (isDismissing) return
                isDismissing = true
                layout.animate()
                    .translationY(300f)
                    .alpha(0f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        try { 
                            layout.visibility = android.view.View.GONE
                            windowManager?.removeView(layout) 
                        } catch (e: Exception) { }
                    }
                    .start()
            }
            
            // Автозакрытие
            val dismissRunnable = Runnable { dismiss() }
            layout.postDelayed(dismissRunnable, 6000)

            // Логика взаимодействия
            layout.setOnClickListener {
                layout.removeCallbacks(dismissRunnable)
                dismiss()
            }
            
            // Начальное состояние
            layout.alpha = 0f
            layout.translationY = 300f
            
            // Сначала добавляем в Window
            windowManager?.addView(layout, params)

            // Затем анимируем
            layout.post {
                layout.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Do nothing
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName // Fallback
        }
    }
}
