package fr.nihongo.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NihongoBackground = Color(0xFF080808)
private val NihongoPanel = Color(0xFF141414)
private val NihongoAccent = Color(0xFFE05252)
private val NihongoBorder = Color(0xFF292929)

private enum class AppScreen {
    HOME,
    PARTICLE_GAME,
    PROGRESS,
    SETTINGS,
    UPDATES
}

@Composable
internal fun MainScreen(
    appVersion: String,
    updateState: UpdateUiState,
    progress: ProgressSnapshot,
    extraAttempts: Int,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (GitHubRelease) -> Unit,
    onRetryInstallPermission: () -> Unit,
    onAnswer: (ParticleExercise, Boolean) -> String?,
    onExtraAttemptsChange: (Int) -> String?
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    BackHandler(enabled = screen != AppScreen.HOME) {
        screen = AppScreen.HOME
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NihongoAccent,
            onPrimary = Color.White,
            secondary = Color(0xFFFF7777),
            background = NihongoBackground,
            onBackground = Color(0xFFF3F3F3),
            surface = NihongoPanel,
            onSurface = Color(0xFFF3F3F3),
            surfaceVariant = Color(0xFF202020),
            onSurfaceVariant = Color(0xFFB8B8B8),
            outline = NihongoBorder,
            error = Color(0xFFFF7474)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = NihongoBackground) {
            when (screen) {
                AppScreen.HOME -> HomeScreen(
                    progress = progress,
                    onStartGame = { screen = AppScreen.PARTICLE_GAME },
                    onShowProgress = { screen = AppScreen.PROGRESS },
                    onShowSettings = { screen = AppScreen.SETTINGS },
                    onShowUpdates = { screen = AppScreen.UPDATES }
                )

                AppScreen.PARTICLE_GAME -> ParticleGameScreen(
                    onBack = { screen = AppScreen.HOME },
                    onAnswer = onAnswer,
                    extraAttempts = extraAttempts
                )

                AppScreen.PROGRESS -> ProgressScreen(
                    progress = progress,
                    onBack = { screen = AppScreen.HOME }
                )

                AppScreen.SETTINGS -> LearningSettingsScreen(
                    extraAttempts = extraAttempts,
                    onBack = { screen = AppScreen.HOME },
                    onExtraAttemptsChange = onExtraAttemptsChange
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
    onShowSettings: () -> Unit,
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
        Spacer(modifier = Modifier.height(16.dp))
        Text("にほんご", style = MaterialTheme.typography.headlineLarge, color = NihongoAccent)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStartGame),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = NihongoPanel),
            border = androidx.compose.foundation.BorderStroke(1.dp, NihongoBorder)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Jeu des particules", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${ParticleExerciseBank.exercises.size} exercices",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = onStartGame,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = NihongoAccent)
                ) {
                    Text("Commencer")
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onShowProgress),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = NihongoPanel),
            border = androidx.compose.foundation.BorderStroke(1.dp, NihongoBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Ma progression", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${progress.masteredCount} maîtrisée(s) · ${progress.totalAttempts} réponse(s)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TextButton(onClick = onShowSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Paramètres")
        }

        TextButton(onClick = onShowUpdates, modifier = Modifier.fillMaxWidth()) {
            Text("Mise à jour · v${BuildConfig.VERSION_NAME}")
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
        TextButton(onClick = onBack) { Text("‹  Accueil") }
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearningSettingsScreen(
    extraAttempts: Int,
    onBack: () -> Unit,
    onExtraAttemptsChange: (Int) -> String?
) {
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹  Accueil") }
        Text("Paramètres", style = MaterialTheme.typography.headlineSmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = NihongoPanel),
            border = androidx.compose.foundation.BorderStroke(1.dp, NihongoBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Essais supplémentaires", style = MaterialTheme.typography.titleLarge)
                Text(
                    "$extraAttempts après la première réponse",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..5).forEach { attempts ->
                        FilterChip(
                            selected = extraAttempts == attempts,
                            onClick = { saveError = onExtraAttemptsChange(attempts) },
                            label = { Text(attempts.toString()) }
                        )
                    }
                }
                saveError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
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
