package fr.nihongo.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class AppScreen {
    HOME,
    PARTICLE_GAME,
    PROGRESS,
    UPDATES
}

@Composable
internal fun MainScreen(
    appVersion: String,
    updateState: UpdateUiState,
    progress: ProgressSnapshot,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (GitHubRelease) -> Unit,
    onRetryInstallPermission: () -> Unit,
    onAnswer: (ParticleExercise, Boolean) -> String?
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    BackHandler(enabled = screen != AppScreen.HOME) {
        screen = AppScreen.HOME
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                AppScreen.HOME -> HomeScreen(
                    progress = progress,
                    onStartGame = { screen = AppScreen.PARTICLE_GAME },
                    onShowProgress = { screen = AppScreen.PROGRESS },
                    onShowUpdates = { screen = AppScreen.UPDATES }
                )

                AppScreen.PARTICLE_GAME -> ParticleGameScreen(
                    onBack = { screen = AppScreen.HOME },
                    onAnswer = onAnswer
                )

                AppScreen.PROGRESS -> ProgressScreen(
                    progress = progress,
                    onBack = { screen = AppScreen.HOME }
                )

                AppScreen.UPDATES -> UpdatesScreen(
                    appVersion = appVersion,
                    updateState = updateState,
                    onBack = { screen = AppScreen.HOME },
                    onCheckUpdates = onCheckUpdates,
                    onDownloadUpdate = onDownloadUpdate,
                    onRetryInstallPermission = onRetryInstallPermission
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    progress: ProgressSnapshot,
    onStartGame: () -> Unit,
    onShowProgress: () -> Unit,
    onShowUpdates: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("にほんご", style = MaterialTheme.typography.headlineLarge)
        Text("Apprendre le japonais, à votre rythme.", style = MaterialTheme.typography.titleMedium)
        Text("Choisis un mode pour commencer.")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStartGame)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Jeu des particules", style = MaterialTheme.typography.titleLarge)
                Text("Complète des phrases japonaises et découvre le sens de chaque mot.")
                Text(
                    "${ParticleExerciseBank.exercises.size} exercices disponibles hors ligne",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onShowProgress)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Ma progression", style = MaterialTheme.typography.titleLarge)
                Text("${progress.masteredCount} notion(s) maîtrisée(s) · ${progress.totalAttempts} réponse(s)")
                TextButton(onClick = onShowProgress) { Text("Voir mes statistiques") }
            }
        }

        TextButton(onClick = onShowUpdates, modifier = Modifier.fillMaxWidth()) {
            Text("Mise à jour de l’application ${BuildConfig.VERSION_NAME}")
        }
    }
}

@Composable
private fun UpdatesScreen(
    appVersion: String,
    updateState: UpdateUiState,
    onBack: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (GitHubRelease) -> Unit,
    onRetryInstallPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Retour") }
        Text("Mise à jour de l’application", style = MaterialTheme.typography.headlineSmall)
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
        Text(
            "La recherche nécessite une connexion Internet.",
            style = MaterialTheme.typography.bodySmall
        )
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
