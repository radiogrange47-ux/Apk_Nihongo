package fr.nihongo.app

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import java.util.Locale

internal class JapaneseQuestionSpeech(context: Context) : TextToSpeech.OnInitListener {
    private data class SpeechRequest(val beforeGap: String, val afterGap: String?)

    private val engine = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var pendingQuestion: SpeechRequest? = null

    var isEnabled by mutableStateOf(true)
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageStatus = runCatching { engine.setLanguage(Locale.JAPAN) }
                .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            ready = languageStatus >= TextToSpeech.LANG_AVAILABLE
            if (ready && isEnabled) {
                pendingQuestion?.let(::speak)
            }
        }
    }

    fun toggle(): Boolean {
        isEnabled = !isEnabled
        if (!isEnabled) {
            pendingQuestion = null
            engine.stop()
        }
        return isEnabled
    }

    fun speakQuestion(text: String) {
        speak(SpeechRequest(text, null))
    }

    fun speakQuestionWithGap(beforeGap: String, afterGap: String) {
        speak(SpeechRequest(beforeGap, afterGap))
    }

    private fun speak(request: SpeechRequest) {
        if (!isEnabled || (request.beforeGap.isBlank() && request.afterGap.isNullOrBlank())) return
        if (!ready) {
            pendingQuestion = request
            return
        }
        pendingQuestion = null
        runCatching {
            val utteranceId = System.nanoTime().toString()
            if (request.afterGap == null) {
                engine.speak(request.beforeGap, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                if (request.beforeGap.isNotBlank()) {
                    engine.speak(
                        request.beforeGap,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "$utteranceId-before"
                    )
                    engine.playSilentUtterance(420, TextToSpeech.QUEUE_ADD, "$utteranceId-gap")
                } else {
                    engine.playSilentUtterance(420, TextToSpeech.QUEUE_FLUSH, "$utteranceId-gap")
                }
                if (request.afterGap.isNotBlank()) {
                    engine.speak(
                        request.afterGap,
                        TextToSpeech.QUEUE_ADD,
                        null,
                        "$utteranceId-after"
                    )
                }
            }
        }
    }

    fun release() {
        engine.stop()
        engine.shutdown()
    }
}

@Composable
internal fun QuestionSpeechButton(
    speech: JapaneseQuestionSpeech,
    questionText: String,
    textAfterGap: String? = null,
    speakOnEnable: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = {
            if (speech.toggle() && speakOnEnable) {
                if (textAfterGap == null) speech.speakQuestion(questionText)
                else speech.speakQuestionWithGap(questionText, textAfterGap)
            }
        },
        modifier = modifier.semantics {
            contentDescription = if (speech.isEnabled) "Désactiver la lecture vocale" else "Activer la lecture vocale"
        }
    ) {
        Text(
            text = if (speech.isEnabled) "🔊" else "🔇",
            color = if (speech.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 21.sp
        )
    }
}
