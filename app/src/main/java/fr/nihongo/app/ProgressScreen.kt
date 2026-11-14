package fr.nihongo.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        TextButton(onClick = onBack) { Text("‹  Accueil") }
        Text("Ma progression", style = MaterialTheme.typography.headlineSmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
            border = BorderStroke(1.dp, Color(0xFF292929))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${progress.masteredCount} notion(s) maîtrisée(s)", style = MaterialTheme.typography.titleLarge)
                Text("${progress.totalAttempts} réponse(s) enregistrée(s)")
                Text("${progress.overallAccuracyPercent} % de réussite")
            }
        }

        Text("Notions à réviser", style = MaterialTheme.typography.titleLarge)
        progress.topics.forEach { topic ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                border = BorderStroke(1.dp, Color(0xFF292929))
            ) {
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

    }
}
