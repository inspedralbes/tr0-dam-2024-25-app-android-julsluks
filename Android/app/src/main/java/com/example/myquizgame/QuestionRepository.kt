package com.example.myquizgame

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QuestionRepository(private val apiService: ApiService) {

    fun getQuestions(onResult: (List<Question>?, String?) -> Unit) {
        apiService.getQuestions().enqueue(object : Callback<QuestionsResponse> {
            override fun onResponse(call: Call<QuestionsResponse>, response: Response<QuestionsResponse>) {
                if (response.isSuccessful) {
                    onResult(response.body()?.questions, null)
                } else {
                    onResult(null, "Error loading all questions. Code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<QuestionsResponse>, t: Throwable) {
                onResult(null, "Error in request: ${t.message}")
            }
        })
    }
}
