package com.example.guardianai.network

import retrofit2.http.Body
import retrofit2.http.POST

// Классы данных для Запроса/Ответа
data class PredictionRequest(
    val text: String,
    val strict_mode: Boolean = false,
    val context: List<String> = emptyList()
)

data class PredictionResponse(
    val is_scam: Boolean,
    val score: Float,
    val reason: List<String>,
    val verdict: String
)

data class FeedbackRequest(
    val text: String,
    val is_scam_report: Boolean,
    val original_score: Float = 0.0f
)

data class FeedbackResponse(
    val status: String,
    val message: String
)

interface GuardianApiService {
    @POST("predict")
    suspend fun predict(@Body request: PredictionRequest): PredictionResponse

    @POST("feedback")
    suspend fun sendFeedback(@Body request: FeedbackRequest): FeedbackResponse
}
