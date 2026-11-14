package fr.nihongo.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun SessionProgressFooter(
    questionsCompleted: Int,
    target: Int,
    allQuestionsMastered: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "$questionsCompleted / $target questions",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        LinearProgressIndicator(
            progress = { (questionsCompleted.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
        if (allQuestionsMastered) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12352A))
            ) {
                Text(
                    "Toutes les questions sélectionnées ont atteint le seuil de maîtrise.",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF8EE6B5),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
