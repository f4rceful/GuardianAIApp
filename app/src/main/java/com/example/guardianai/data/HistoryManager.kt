package com.example.guardianai.data

import android.content.Context
import com.example.guardianai.network.ExplanationItem
import com.example.guardianai.network.PredictionResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Модель для хранения истории (добавляет метку времени к ответу)
data class HistoryItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isScam: Boolean,
    val isWarning: Boolean = false,
    val reason: List<String>,
    val entities: Map<String, List<String>>? = null,
    val explanation: List<ExplanationItem>? = null,
    val timestamp: String = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date())
)

object HistoryManager {
    private const val PREFS_NAME = "guardian_history"
    private const val KEY_HISTORY = "history_list"
    private val gson = Gson()

    fun saveScan(context: Context, text: String, response: PredictionResponse, isWarning: Boolean = false) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentList = getHistory(context).toMutableList()
        
        // Создание элемента
        val newItem = HistoryItem(
            text = text,
            isScam = response.is_scam,
            isWarning = isWarning,
            reason = response.reason,
            entities = response.entities,
            explanation = response.explanation
        )
        
        // Добавление в начало списка
        currentList.add(0, newItem)
        
        // Лимит 50 записей
        if (currentList.size > 50) {
            currentList.removeAt(currentList.lastIndex)
        }

        // Сохранение
        val json = gson.toJson(currentList)
        sharedPreferences.edit().putString(KEY_HISTORY, json).apply()
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(KEY_HISTORY, null) ?: return emptyList()
        
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun deleteItems(context: Context, ids: List<Long>) {
        val currentList = getHistory(context).toMutableList()
        currentList.removeAll { it.id in ids }
        
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(currentList)
        sharedPreferences.edit().putString(KEY_HISTORY, json).apply()
    }

    fun clearHistory(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().remove(KEY_HISTORY).apply()
    }
    
    // Statistics
    data class Stats(
        val totalScans: Int = 0,
        val scamsBlocked: Int = 0,
        val warningsShown: Int = 0,
        val safeMessages: Int = 0
    )
    
    fun getStats(context: Context): Stats {
        val history = getHistory(context)
        return Stats(
            totalScans = history.size,
            scamsBlocked = history.count { it.isScam },
            warningsShown = history.count { it.isWarning && !it.isScam },
            safeMessages = history.count { !it.isScam && !it.isWarning }
        )
    }
}
