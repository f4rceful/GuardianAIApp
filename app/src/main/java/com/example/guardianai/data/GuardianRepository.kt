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
        return api.predict(PredictionRequest(text, strictMode, context))
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
