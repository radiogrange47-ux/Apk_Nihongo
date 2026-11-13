package fr.nihongo.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class WordColorLegend(
    val category: WordCategory,
    val label: String,
    val color: Color
)

private val wordColorLegend = listOf(
    WordColorLegend(WordCategory.VOCABULARY, "Vocabulaire", Color(0xFF1565C0)),
    WordColorLegend(WordCategory.PARTICLE, "Particules", Color(0xFF2E7D32)),
    WordColorLegend(WordCategory.VERB, "Verbes", Color(0xFFEF6C00)),
    WordColorLegend(WordCategory.GRAMMAR_MARKER, "Marqueurs grammaticaux", Color(0xFF7B1FA2))
)

@Composable
internal fun ParticleGameScreen(
    onBack: () -> Unit,
    onAnswer: (ParticleExercise, Boolean) -> String?
) {
    var questionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var selectedToken by remember { mutableStateOf<PhraseToken?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var correctCount by remember { mutableIntStateOf(0) }
    var completed by remember { mutableStateOf(false) }

    val exercises = ParticleExerciseBank.exercises

    if (completed) {
        GameSummary(
            correctCount = correctCount,
            questionCount = exercises.size,
            onBack = onBack,
            onPlayAgain = {
                questionIndex = 0
                selectedAnswer = null
                selectedToken = null
                saveError = null
                correctCount = 0
                completed = false
            }
        )
        return
    }

    val exercise = exercises[questionIndex]
    val wasAnswered = selectedAnswer != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Menu") }
        Text("Choisis la bonne particule", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Question ${questionIndex + 1} sur ${exercises.size}",
            style = MaterialTheme.typography.titleMedium
        )
        Text("Appuie sur les mots pour afficher leur sens en français.")

        WordColorLegendRow()

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JapanesePhrase(
                    exercise = exercise,
                    revealAnswer = wasAnswered,
                    onSelectToken = { selectedToken = it }
                )
                Text(
                    if (wasAnswered) "Traduction complète : ${exercise.translation}"
                    else "Touchez chaque élément pour explorer la phrase.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text("Quelle particule complète la phrase ?", style = MaterialTheme.typography.titleMedium)
        exercise.choices.forEach { choice ->
            val isSelected = selectedAnswer == choice
            val choiceModifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Choisir la particule $choice" }

            if (isSelected || (!wasAnswered && selectedAnswer == null)) {
                OutlinedButton(
                    onClick = {
                        if (!wasAnswered) {
                            selectedAnswer = choice
                            val correct = choice == exercise.answer
                            if (correct) correctCount++
                            saveError = onAnswer(exercise, correct)
                        }
                    },
                    enabled = !wasAnswered,
                    modifier = choiceModifier
                ) {
                    Text(
                        text = choice,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (wasAnswered && choice == exercise.answer) {
                            wordColorLegend.first { it.category == WordCategory.PARTICLE }.color
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }

        if (wasAnswered) {
            val isCorrect = selectedAnswer == exercise.answer
            saveError?.let { message ->
                Text(
                    "Attention : $message",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (isCorrect) "Bonne réponse !" else "La bonne réponse est ${exercise.answer}.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(exercise.explanation)
                }
            }
            Button(
                onClick = {
                    selectedToken = null
                    if (questionIndex == exercises.lastIndex) {
                        completed = true
                    } else {
                        questionIndex++
                        selectedAnswer = null
                        saveError = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (questionIndex == exercises.lastIndex) "Voir le résultat" else "Question suivante")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    selectedToken?.let { token ->
        AlertDialog(
            onDismissRequest = { selectedToken = null },
            title = {
                Text(
                    text = token.japanese,
                    color = wordColorLegend.first { it.category == token.category }.color,
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rōmaji : ${token.romaji}", style = MaterialTheme.typography.titleMedium)
                    Text("Français : ${token.french}", style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedToken = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
private fun WordColorLegendRow() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Code couleur", style = MaterialTheme.typography.titleSmall)
        wordColorLegend.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { item ->
                    Text(
                        "● ${item.label}",
                        color = item.color,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JapanesePhrase(
    exercise: ParticleExercise,
    revealAnswer: Boolean,
    onSelectToken: (PhraseToken) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        exercise.tokens.forEachIndexed { index, token ->
            if (index == exercise.missingTokenIndex && !revealAnswer) {
                Text(
                    "【 ？ 】",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Particule manquante"
                    }
                )
            } else {
                val color = wordColorLegend.first { it.category == token.category }.color
                Text(
                    text = token.japanese,
                    color = color,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onSelectToken(token) }
                        .semantics {
                            contentDescription = "${token.japanese}, ${token.french}"
                        }
                )
            }
        }
    }
}

@Composable
private fun GameSummary(
    correctCount: Int,
    questionCount: Int,
    onBack: () -> Unit,
    onPlayAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Séance terminée !", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "$correctCount bonne(s) réponse(s) sur $questionCount.",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Rejouer")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour au menu")
        }
    }
}
