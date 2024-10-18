import com.example.myquizgame.Question

data class EndGameResponse(
    val sessionToken: String,
    val questions: List<Question>
)