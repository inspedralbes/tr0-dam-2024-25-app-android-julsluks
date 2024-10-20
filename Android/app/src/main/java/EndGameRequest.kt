import com.example.myquizgame.Question
import com.example.myquizgame.AnswerRequest

data class EndGameRequest(
    val sessionToken: String,
    val questions: List<Question>,
    val answers: List<AnswerRequest>
)