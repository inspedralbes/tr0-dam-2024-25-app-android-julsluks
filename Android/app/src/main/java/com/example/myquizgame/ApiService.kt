package com.example.myquizgame

import EndGameRequest
import EndGameResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class AnswerResponse(
    val id: Int,
    val answer: String
)

data class Question(
    val id: Int,
    val question: String,
    val answers: List<AnswerResponse>,
    val image: String
)

data class QuestionsResponse(
    val sessionToken: String,
    val questions: List<Question>
)

data class AnswerRequest(
    val questionId: Int,
    val answer: Int
)

interface ApiService {
    @GET("/game")
    fun getQuestions(): Call<QuestionsResponse>

    @POST("/game")
    fun endGame(@Body gameRequest: EndGameRequest): Call<EndGameResponse>
}