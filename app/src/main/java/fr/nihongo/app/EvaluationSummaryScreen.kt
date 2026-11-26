package fr.nihongo.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal data class EvaluationMistake(val label: String)

@Composable
internal fun EvaluationTimer(elapsedSeconds: Long, modifier: Modifier = Modifier) {
    val duration = "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF302020))) {
        Text(
            "Évaluation · $duration",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = Color(0xFFFFB3B3),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
internal fun EvaluationSummaryScreen(
    track: LearningTrack,
    totalQuestions: Int,
    mistakes: List<EvaluationMistake>,
    errorCount: Int,
    elapsedSeconds: Long,
    onHome: () -> Unit
) {
    val errorPercent = if (totalQuestions == 0) 0 else errorCount * 100 / totalQuestions
    val duration = "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(track.symbol, color = Color(0xFFE05252), style = MaterialTheme.typography.displaySmall)
                Text("Évaluation terminée", style = MaterialTheme.typography.headlineSmall)
                Text("$errorPercent% d’erreurs", color = if (mistakes.isEmpty()) Color(0xFF8EE6B5) else Color(0xFFFF9A9A), style = MaterialTheme.typography.displayMedium)
                Text("$totalQuestions notion(s) évaluée(s) en $duration", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (mistakes.isEmpty()) {
                    Text("Aucune notion à revoir. Bravo !", textAlign = TextAlign.Center, color = Color(0xFF8EE6B5))
                } else {
                    Text("Notions à revoir", style = MaterialTheme.typography.titleMedium)
                    mistakes.forEach { mistake ->
                        Text("• ${mistake.label}", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = onHome, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE05252))) {
                    Text("Retour à l’accueil")
                }
            }
        }
    }
}
