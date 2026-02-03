package com.example.guardianai.data

import android.content.Context
import com.example.guardianai.network.*

/**
 * Repository to handle data operations.
 * It acts as a single source of truth for data access (Network and Local).
 */
class GuardianRepository(private val api: GuardianApiService = RetrofitClient.instance) {

    // --- Network Operations ---

    suspend fun predict(text: String, strictMode: Boolean, context: List<String> = emptyList()): PredictionResponse {
        val lowerText = text.lowercase()

        // 1. Проверка протокола: HTTP (Незашифрованное) -> Всегда СКАМ
        if (text.contains("http://", ignoreCase = true)) {
            return PredictionResponse(
                is_scam = true,
                score = 0.95f,
                reason = listOf("⛔ Незащищенное соединение (HTTP)", "⚠️ Данные могут быть перехвачены"),
                verdict = "DANGEROUS",
                entities = emptyMap(),
                explanation = emptyList()
            )
        }

        // 2. Проверка Белого списка (Официальные домены) -> Всегда БЕЗОПАСНО
        val safeDomains = listOf(
            "telegram.org", "t.me", "google.com", "android.com", "whatsapp.com", "vk.com"
        )
        val isSafeDomain = safeDomains.any { lowerText.contains(it) }

        if (isSafeDomain) {
            return PredictionResponse(
                is_scam = false,
                score = 0.01f,
                reason = listOf("✅ Официальный домен", "ℹ️ Безопасное соединение"),
                verdict = "SAFE",
                entities = emptyMap(),
                explanation = emptyList()
            )
        }

        // 3. Запрос к API (ML)
        val response = api.predict(PredictionRequest(text, strictMode, context))

        // 4. Пост-обработка: 
        // А) Проверка СПАМ-контекста (keywords)
        // Если есть ссылка И спам-слова -> СКАМ (даже если ML сомневается)
        val spamKeywords = listOf(
            "казино", "casino", "выигрыш", "prize", "подарок", "gift", 
            "bitcoin", "крипта", "инвестиции", "заработок", "счет", "акция"
        )
        val hasSpamKeyword = spamKeywords.any { lowerText.contains(it) }
        val hasLink = text.contains("http", ignoreCase = true) // http or https

        if (hasLink && hasSpamKeyword) {
            return response.copy(
                is_scam = true,
                score = 0.99f,
                reason = response.reason.toMutableList().apply {
                    add(0, "⛔ Обнаружен СПАМ-контекст")
                    add("⚠️ Подозрительные слова + Ссылка")
                },
                verdict = "DANGEROUS"
            )
        }

        // Б) Неизвестный HTTPS -> Предупреждение
        if (text.contains("https://", ignoreCase = true) && !isSafeDomain) {
            val hasHighConfidence = response.score > 0.8f
            if (!hasHighConfidence && !response.is_scam) {
                // Если ML не уверен, что это скам, но ссылка есть -> добавляем Warning
                return response.copy(
                    reason = response.reason + listOf(
                        "⚡ Подозрительно: Неизвестный источник",
                        "ℹ️ Не переходите по ссылке, если не ждали её"
                    ),
                    // Не меняем is_scam, но score можно чуть поднять для UI Warning
                    score = if(response.score < 0.55f) 0.55f else response.score 
                )
            }
        }

        return response
    }

    suspend fun checkServerConnection(): Boolean {
        return try {
            api.predict(PredictionRequest("ping", strict_mode = false))
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendFeedback(text: String, isScamReport: Boolean, originalScore: Float = 0.0f): FeedbackResponse {
        return api.sendFeedback(FeedbackRequest(text, isScamReport, originalScore))
    }

    // --- Local Data Operations (History) ---

    fun saveHistoryItem(context: Context, text: String, response: PredictionResponse) {
        HistoryManager.saveScan(context, text, response)
    }

    fun getHistory(context: Context): List<HistoryItem> {
        return HistoryManager.getHistory(context)
    }

    fun deleteHistoryItems(context: Context, ids: List<Long>) {
        HistoryManager.deleteItems(context, ids)
    }

    // --- Local Data Operations (Settings / Whitelist) ---

    fun isStrictMode(context: Context): Boolean = SettingsManager.isStrictMode.value

    fun getTrustedApps(): Set<String> = SettingsManager.trustedApps.value
    fun getTrustedContacts(): Set<String> = SettingsManager.trustedContacts.value

    fun addTrustedApp(context: Context, packageName: String) = SettingsManager.addTrustedApp(context, packageName)
    fun removeTrustedApp(context: Context, packageName: String) = SettingsManager.removeTrustedApp(context, packageName)
    
    fun addTrustedContact(context: Context, name: String) = SettingsManager.addTrustedContact(context, name)
    fun removeTrustedContact(context: Context, name: String) = SettingsManager.removeTrustedContact(context, name)
}
