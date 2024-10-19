package com.example.myquizgame

import EndGameResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class Question(
    val id: Int,
    val question: String,
    val answers: List<String>,
    val image: String
)

data class QuestionsResponse(
    val questions: List<Question>
)

interface ApiService {
    @GET("/data.json")
    fun getQuestions(): Call<QuestionsResponse>

    @POST("/preguntesPartida")
    fun questionsGame(@Body gameRequest: Map<String, String>): Call<EndGameResponse>
}