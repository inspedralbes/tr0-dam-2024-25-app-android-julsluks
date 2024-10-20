package com.example.myquizgame

import EndGameRequest
import EndGameResponse
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myquizgame.ui.theme.MyQuizGameTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : ComponentActivity() {
    lateinit var apiService: ApiService
    lateinit var questionRepository: QuestionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Start Retrofit and API service
        //Gson convert the json's in kotlin objects
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
        questionRepository = QuestionRepository(apiService)

        setContent {
            MyQuizGameTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavigationGraph(navController = navController)
                }
            }
        }
    }
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "StartScreen") {
        composable("StartScreen") {
            StartScreen(navController = navController)
        }
        composable("GameScreen") {
            GameScreen(navController = navController)
        }
        composable("EndGameScreen/{sessionToken}/{questionsJson}/{answersJson}") { backStackEntry ->
            val sessionToken = backStackEntry.arguments?.getString("sessionToken") ?: ""
            val questionsJson = backStackEntry.arguments?.getString("questionsJson") ?: "[]"
            val answersJson = backStackEntry.arguments?.getString("answersJson") ?: "[]"

            val questions: List<Question> = parseQuestionsJson(questionsJson)
            val answers: List<AnswerRequest> = parseAnswersJson(answersJson)

            EndGameScreen(navController, sessionToken, questions, answers)
        }

    }
}

@Composable
fun StartScreen (navController: NavController) {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to My Quiz Game")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("GameScreen") }) {
            Text(text = "Start Game")
        }
    }
}

@Composable
fun GameScreen(navController: NavController) {
    var sessionToken by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var currentQuestion by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var answersRequest = remember { mutableStateListOf<Pair<Int, Int>>() }

    val context = LocalContext.current as MainActivity

    // Cargar las preguntas desde la API
    LaunchedEffect(Unit) {
        context.apiService.getQuestions().enqueue(object : Callback<QuestionsResponse> {
            override fun onResponse(call: Call<QuestionsResponse>, response: Response<QuestionsResponse>) {
                if (response.isSuccessful) {
                    sessionToken = response.body()?.sessionToken ?: ""
                    questions = response.body()?.questions ?: emptyList()
                    errorMessage = null
                } else {
                    errorMessage = "Error loading questions. Code: ${response.code()}"
                }
                isLoading = false
            }

            override fun onFailure(call: Call<QuestionsResponse>, t: Throwable) {
                errorMessage = "Error in request: ${t.message}"
                isLoading = false
            }
        })
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Loading...")
        }
    } else if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorMessage!!)
        }
    } else if (questions.isNotEmpty()) {
        val currentQuestionObj = questions[currentQuestion]

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(text = "Question ${currentQuestion + 1}/${questions.size}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = currentQuestionObj.question)

            currentQuestionObj.answers.forEach { answer ->
                Button(
                    onClick = {
                        answersRequest.add(Pair(currentQuestionObj.id, answer.id))
                        if (currentQuestion < questions.size - 1) {
                            currentQuestion++
                        } else {
                            // Al terminar, navegamos a EndGameScreen con las preguntas y respuestas
                            val answersJson = buildAnswersRequestJson(answersRequest)
                            val questionsJson = buildQuestionsJson(questions)
                            navController.navigate(
                                "EndGameScreen/$sessionToken/$questionsJson/$answersJson"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(text = answer.answer)
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No questions available")
        }
    }
}

@Composable
fun EndGameScreen(navController: NavController, sessionToken: String, questions: List<Question>, answers: List<AnswerRequest>) {
    val context = LocalContext.current as MainActivity
    var correctAnswers by remember { mutableStateOf(0) }
    var incorrectAnswers by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val requestBody = EndGameRequest(sessionToken = sessionToken, questions = questions, answers = answers)
        context.apiService.endGame(requestBody).enqueue(object : Callback<EndGameResponse> {
            override fun onResponse(call: Call<EndGameResponse>, response: Response<EndGameResponse>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    correctAnswers = result?.correctAnswers ?: 0
                    incorrectAnswers = result?.incorrectAnswers ?: 0
                } else {
                    errorMessage = "Error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<EndGameResponse>, t: Throwable) {
                errorMessage = "Request failed: ${t.message}"
            }
        })
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        if (errorMessage != null) {
            Text(text = errorMessage!!)
        } else {
            Text(text = "Game Over")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Correct answers: $correctAnswers")
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Incorrect answers: $incorrectAnswers")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("StartScreen") }) {
            Text(text = "Restart Game")
        }
    }
}

fun buildQuestionsJson(questions: List<Question>): String {
    val questionsList = questions.map { question ->
        """{
            "id": ${question.id},
            "question": "${question.question}",
            "answers": ${buildAnswersResponseJson(question.answers)},
            "image": "${question.image}"
        }"""
    }
    return questionsList.joinToString(",", "[", "]")
}

fun buildAnswersResponseJson(answers: List<AnswerResponse>): String {
    val answersList = answers.map { answer ->
        """{
            "id": ${answer.id},
            "answer": "${answer.answer}"
        }"""
    }
    return answersList.joinToString(",", "[", "]")
}

fun buildAnswersRequestJson(answers: List<Pair<Int, Int>>): String {
    val answersList = answers.map { answer ->
        """{
            "question": ${answer.first},
            "answer": ${answer.second}
        }"""
        }
    return answersList.joinToString(",", "[", "]")
}

fun parseQuestionsJson(json: String): List<Question> {
    val gson = Gson()
    val type = object : TypeToken<List<Question>>() {}.type
    return gson.fromJson(json, type)
}

fun parseAnswersJson(json: String): List<AnswerRequest> {
    val gson = Gson()
    val type = object : TypeToken<List<AnswerRequest>>() {}.type
    return gson.fromJson(json, type)
}
