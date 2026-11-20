package fr.nihongo.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun SessionBlockSummaryScreen(
    track: LearningTrack,
    correctAnswers: Int,
    totalQuestions: Int,
    nextBlockQuestionCount: Int,
    onContinue: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(track.symbol, color = Color(0xFFE05252), style = MaterialTheme.typography.displaySmall)
                Text("Bloc terminé", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$correctAnswers / $totalQuestions",
                    color = Color(0xFF8EE6B5),
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    "Bonnes réponses dans le parcours ${track.title.lowercase()}. Les notions à revoir reviendront au fil des prochaines séances.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE05252))
                ) {
                    Text("Continuer · $nextBlockQuestionCount questions par jeu")
                }
                OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                    Text("Changer de parcours")
                }
            }
        }
    }
}
