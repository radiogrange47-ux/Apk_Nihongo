package fr.nihongo.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ProgressScreen(
    progress: ProgressSnapshot,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Menu") }
        Text("Ma progression", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${progress.masteredCount} notion(s) maîtrisée(s)", style = MaterialTheme.typography.titleLarge)
                Text("${progress.totalAttempts} réponse(s) enregistrée(s)")
                if (progress.totalAttempts > 0) {
                    Text("${progress.overallAccuracyPercent} % de bonnes réponses")
                } else {
                    Text("Joue une première séance pour commencer le suivi.")
                }
            }
        }

        Text("Notions à réviser", style = MaterialTheme.typography.titleLarge)
        progress.topics.forEach { topic ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(topic.topic, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            when {
                                topic.isMastered -> "Maîtrisée"
                                topic.attempts == 0 -> "À découvrir"
                                else -> "À réviser"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (topic.attempts > 0) {
                            Text("${topic.accuracyPercent} % · ${topic.attempts} réponse(s)")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Une notion est considérée comme maîtrisée après au moins 3 réponses et avec " +
                "80 % de bonnes réponses ou plus. Les résultats restent enregistrés sur cet appareil.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
