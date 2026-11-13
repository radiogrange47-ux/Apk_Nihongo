package fr.nihongo.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object NoRelease : UpdateUiState
    data class Current(val version: String) : UpdateUiState
    data class Available(val release: GitHubRelease) : UpdateUiState
    data class Downloading(val release: GitHubRelease) : UpdateUiState
    data class AwaitingInstallPermission(val release: GitHubRelease) : UpdateUiState
    data class Installing(val release: GitHubRelease) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class MainActivity : ComponentActivity() {
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private var updateState by mutableStateOf<UpdateUiState>(UpdateUiState.Idle)
    private var progressSnapshot by mutableStateOf<ProgressSnapshot?>(null)
    private var pendingInstallerUri: Uri? = null
    private var pendingInstallRelease: GitHubRelease? = null
    private val releaseClient = GitHubReleaseClient()
    private val progressStore by lazy { ProgressStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressSnapshot = progressStore.snapshot()
        setContent {
            MainScreen(
                appVersion = BuildConfig.VERSION_NAME,
                updateState = updateState,
                progress = progressSnapshot ?: progressStore.snapshot(),
                onCheckUpdates = ::checkForUpdates,
                onDownloadUpdate = ::downloadUpdate,
                onRetryInstallPermission = ::retryInstallPermission,
                onAnswer = ::recordAnswer
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val pendingUri = pendingInstallerUri
        if (pendingUri != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallerUri = null
            val release = pendingInstallRelease
            pendingInstallRelease = null
            launchInstaller(pendingUri, release)
        } else if (updateState is UpdateUiState.Installing) {
            updateState = UpdateUiState.Available((updateState as UpdateUiState.Installing).release)
        }
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private fun checkForUpdates() {
        updateState = UpdateUiState.Checking
        worker.execute {
            val result = try {
                Result.success(releaseClient.checkForUpdate(BuildConfig.VERSION_NAME))
            } catch (exception: Exception) {
                Result.failure(exception)
            }
            runOnUiThread {
                updateState = result.fold(
                    onSuccess = { check ->
                        when (check) {
                            ReleaseCheck.NoRelease -> UpdateUiState.NoRelease
                            is ReleaseCheck.Current -> UpdateUiState.Current(check.installedVersion)
                            is ReleaseCheck.Available -> UpdateUiState.Available(check.release)
                        }
                    },
                    onFailure = { UpdateUiState.Error(it.message ?: "La recherche de mise à jour a échoué.") }
                )
            }
        }
    }

    private fun downloadUpdate(release: GitHubRelease) {
        updateState = UpdateUiState.Downloading(release)
        worker.execute {
            val result = try {
                Result.success(UpdateDownloader(applicationContext).download(release.apk))
            } catch (exception: Exception) {
                Result.failure(exception)
            }
            runOnUiThread {
                result.fold(
                    onSuccess = { uri -> requestInstallation(uri, release) },
                    onFailure = {
                        updateState = UpdateUiState.Error(
                            it.message ?: "Le téléchargement de la mise à jour a échoué."
                        )
                    }
                )
            }
        }
    }

    private fun requestInstallation(uri: Uri, release: GitHubRelease) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            pendingInstallerUri = uri
            pendingInstallRelease = release
            updateState = UpdateUiState.AwaitingInstallPermission(release)
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (exception: Exception) {
                updateState = UpdateUiState.Error(
                    exception.message ?: "Android n’a pas pu ouvrir les autorisations d’installation."
                )
            }
            return
        }
        launchInstaller(uri, release)
    }

    private fun retryInstallPermission() {
        val uri = pendingInstallerUri ?: return
        requestInstallation(uri, pendingInstallRelease ?: return)
    }

    private fun recordAnswer(exercise: ParticleExercise, wasCorrect: Boolean): String? =
        try {
            progressSnapshot = progressStore.recordAnswer(exercise, wasCorrect)
            null
        } catch (exception: Exception) {
            exception.message ?: "La réponse n’a pas pu être enregistrée."
        }

    private fun launchInstaller(uri: Uri, release: GitHubRelease? = null) {
        try {
            val installIntent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(installIntent)
            if (release != null) updateState = UpdateUiState.Installing(release)
        } catch (exception: Exception) {
            updateState = UpdateUiState.Error(
                exception.message ?: "Android n’a pas pu ouvrir l’installateur de l’application."
            )
        }
    }
}
