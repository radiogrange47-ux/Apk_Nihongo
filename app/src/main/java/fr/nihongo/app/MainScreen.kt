package fr.nihongo.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MainScreen(
    appVersion: String,
    updateState: UpdateUiState,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (GitHubRelease) -> Unit,
    onRetryInstallPermission: () -> Unit
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                Text("にほんご", style = MaterialTheme.typography.headlineMedium)
                Text("Apprendre le japonais, à votre rythme.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Votre espace d’apprentissage sera bientôt disponible. " +
                        "Cette première version prépare l’application et ses mises à jour.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Mise à jour de l’application", style = MaterialTheme.typography.titleLarge)
                        Text("Version installée : $appVersion")
                        UpdateStatus(updateState)

                        when (updateState) {
                            UpdateUiState.Checking,
                            is UpdateUiState.Downloading,
                            is UpdateUiState.Installing -> CircularProgressIndicator()

                            is UpdateUiState.AwaitingInstallPermission -> TextButton(
                                onClick = onRetryInstallPermission,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Réessayer l’autorisation Android")
                            }

                            is UpdateUiState.Available -> Button(
                                onClick = { onDownloadUpdate(updateState.release) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Télécharger ${updateState.release.tag}")
                            }

                            else -> Unit
                        }

                        TextButton(
                            onClick = onCheckUpdates,
                            enabled = updateState !is UpdateUiState.Checking &&
                                updateState !is UpdateUiState.Downloading &&
                                updateState !is UpdateUiState.AwaitingInstallPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Rechercher une mise à jour")
                        }
                    }
                }
                Text(
                    "La recherche de mise à jour nécessite une connexion Internet. " +
                        "Les fonctions d’apprentissage seront ajoutées ensuite.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun UpdateStatus(state: UpdateUiState) {
    val message = when (state) {
        UpdateUiState.Idle -> "Les mises à jour sont vérifiées uniquement à votre demande."
        UpdateUiState.Checking -> "Recherche de la dernière version…"
        UpdateUiState.NoRelease ->
            "Aucune Release publique n’est accessible. Publiez une version ou vérifiez l’accès au dépôt."
        is UpdateUiState.Current -> "L’application est à jour (version ${state.version})."
        is UpdateUiState.Available ->
            "Version ${state.release.tag} disponible.\n\n${state.release.notes.ifBlank { "Aucune note de version." }}"
        is UpdateUiState.Downloading -> "Téléchargement de ${state.release.tag}…"
        is UpdateUiState.AwaitingInstallPermission ->
            "Autorisez Nihongo à installer ${state.release.tag} dans les paramètres Android."
        is UpdateUiState.Installing ->
            "Android va vous demander de confirmer l’installation de ${state.release.tag}."
        is UpdateUiState.Error -> "La mise à jour a échoué : ${state.message}"
    }
    Text(message, style = MaterialTheme.typography.bodyMedium)
}
