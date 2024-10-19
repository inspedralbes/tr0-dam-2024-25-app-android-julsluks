package com.example.myquizgame

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
        composable("EndGameScreen/{score}") { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toInt() ?: 0
            EndGameScreen(navController = navController, score = score)
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
    var clickCount by remember { mutableStateOf(0) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList())}
    var currentQuestion by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null)}
    var isLoading by remember { mutableStateOf(true)}
    val score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(30) }
    var isTimeRunning by remember { mutableStateOf(true) }

    val context = LocalContext.current as MainActivity
    val sessionToken = "some-session-token"
    val gameRequest = mapOf("sessionToken" to sessionToken)

    LaunchedEffect (Unit) {
        context.apiService.questionsGame(gameRequest).enqueue(object : Callback<EndGameResponse> {
            override fun onResponse(call: Call<EndGameResponse>, response: Response<EndGameResponse>) {
                if (response.isSuccessful) {
                    questions = response.body()?.questions ?: emptyList()
                    currentQuestion = 0
                    errorMessage = null
                    isLoading = false
                } else {
                    Log.e("GameScreen", "Error loading questions. Code: ${response.code()}")
                    errorMessage = "Error loading questions. Code: ${response.code()}"
                    isLoading = false
                }
            }

            override fun onFailure(call: Call<EndGameResponse>, t: Throwable) {
                Log.e("GameScreen", "Error in request: ${t.message}")
                errorMessage = "Error in request: ${t.message}"
                isLoading = false
            }
        })
    }

    LaunchedEffect (currentQuestion, isTimeRunning) {
        timeLeft = 30
        isTimeRunning = true

        while (isTimeRunning && timeLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            timeLeft--
        }

        if (timeLeft == 0 && isTimeRunning) {
            if (currentQuestion < questions.size - 1) {
                currentQuestion++
            } else {
                navController.navigate("EndGameScreen/$score")
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Loading...")
        }
    } else if (errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = errorMessage.toString())
        }
    } else if (questions.isNotEmpty()) {
        val currentQuestionObj = questions[currentQuestion]

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Question ${currentQuestion + 1}/${questions.size}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Time left: $timeLeft seconds")
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = currentQuestionObj.question, modifier = Modifier.padding(bottom = 16.dp))
                AsyncImage(
                    model = currentQuestionObj.image,
                    contentDescription = "Image of the question",
                    modifier = Modifier.height(200.dp).fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                currentQuestionObj.answers.forEachIndexed { index, answer ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            isTimeRunning = false
                            clickCount++

                            if (clickCount >= 10 || currentQuestion >= questions.size - 1) {
                                navController.navigate("EndGameScreen/$score")
                            } else {
                                currentQuestion++
                            }
                        }
                    ) {
                        Text(text = answer)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No questions available")
        }
    }
}

@Composable
fun EndGameScreen (navController: NavController, score: Int) {
    Column (verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Text(text = "Game Over")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Correct answers: $score")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("StartScreen") }) {
            Text(text = "Restart Game")
        }
    }
}