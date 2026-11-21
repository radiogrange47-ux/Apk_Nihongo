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

internal sealed interface QuestionDataUpdateUiState {
    data object Idle : QuestionDataUpdateUiState
    data object Checking : QuestionDataUpdateUiState
    data object NoRelease : QuestionDataUpdateUiState
    data class Current(val version: String) : QuestionDataUpdateUiState
    data class Available(val release: QuestionDataRelease) : QuestionDataUpdateUiState
    data class Downloading(val release: QuestionDataRelease) : QuestionDataUpdateUiState
    data class Updated(val version: String) : QuestionDataUpdateUiState
    data class Error(val message: String) : QuestionDataUpdateUiState
}

class MainActivity : ComponentActivity() {
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private var updateState by mutableStateOf<UpdateUiState>(UpdateUiState.Idle)
    private var questionDataUpdateState by
        mutableStateOf<QuestionDataUpdateUiState>(QuestionDataUpdateUiState.Idle)
    private var questionCatalog by mutableStateOf<QuestionCatalog?>(null)
    private var progressSnapshot by mutableStateOf<ProgressSnapshot?>(null)
    private var extraAttempts by mutableStateOf(1)
    private var questionMasteryThreshold by mutableStateOf(5)
    private var masteredQuestionRepeatPercent by mutableStateOf(5)
    private var sessionQuestionCount by mutableStateOf(10)
    private var selectedLearningTrack by mutableStateOf(LearningTrack.VOCABULARY)
    private var pendingInstallerUri: Uri? = null
    private var pendingInstallRelease: GitHubRelease? = null
    private val releaseClient = GitHubReleaseClient()
    private val progressStore by lazy { ProgressStore(applicationContext) }
    private val questionDataStore by lazy {
        QuestionDataStore(applicationContext, progressStore)
    }
    private val questionReleaseClient = QuestionReleaseClient()
    private val questionSpeech by lazy { JapaneseQuestionSpeech(applicationContext) }
    private val learningSettingsStore by lazy { LearningSettingsStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val catalog = questionDataStore.loadOrSeed()
        questionCatalog = catalog
        progressSnapshot = progressStore.snapshot(catalog)
        extraAttempts = learningSettingsStore.extraAttempts()
        questionMasteryThreshold = learningSettingsStore.questionMasteryThreshold()
        masteredQuestionRepeatPercent = learningSettingsStore.masteredQuestionRepeatPercent()
        sessionQuestionCount = learningSettingsStore.sessionQuestionCount()
        selectedLearningTrack = learningSettingsStore.selectedLearningTrack()
        setContent {
            MainScreen(
                appVersion = BuildConfig.VERSION_NAME,
                updateState = updateState,
                questionCatalog = checkNotNull(questionCatalog),
                questionDataUpdateState = questionDataUpdateState,
                progress = checkNotNull(progressSnapshot),
                extraAttempts = extraAttempts,
                questionMasteryThreshold = questionMasteryThreshold,
                masteredQuestionRepeatPercent = masteredQuestionRepeatPercent,
                sessionQuestionCount = sessionQuestionCount,
                selectedLearningTrack = selectedLearningTrack,
                selectedQuestionBanks = learningSettingsStore.selectedQuestionBanks(),
                questionSpeech = questionSpeech,
                onQuestionBanksChange = ::setSelectedQuestionBanks,
                onLearningTrackChange = ::persistSelectedLearningTrack,
                onCheckUpdates = ::checkForUpdates,
                onDownloadUpdate = ::downloadUpdate,
                onCheckQuestionUpdates = ::checkForQuestionUpdates,
                onDownloadQuestionUpdate = ::downloadQuestionUpdate,
                onRetryInstallPermission = ::retryInstallPermission,
                onAnswer = ::recordAnswer,
                onExtraAttemptsChange = ::setExtraAttempts,
                onQuestionMasteryThresholdChange = ::setQuestionMasteryThreshold,
                onMasteredQuestionRepeatPercentChange = ::setMasteredQuestionRepeatPercent,
                onSessionQuestionCountChange = ::setSessionQuestionCount,
                onResetMasteryCounters = ::resetMasteryCounters
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
        questionSpeech.release()
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

    private fun checkForQuestionUpdates() {
        val installedCatalog = questionCatalog ?: return
        questionDataUpdateState = QuestionDataUpdateUiState.Checking
        worker.execute {
            val result = try {
                Result.success(questionReleaseClient.checkForUpdate(installedCatalog.contentVersion))
            } catch (exception: Exception) {
                Result.failure(exception)
            }
            runOnUiThread {
                questionDataUpdateState = result.fold(
                    onSuccess = { check ->
                        when (check) {
                            QuestionDataReleaseCheck.NoRelease -> QuestionDataUpdateUiState.NoRelease
                            is QuestionDataReleaseCheck.Current ->
                                QuestionDataUpdateUiState.Current(check.version)

                            is QuestionDataReleaseCheck.Available ->
                                QuestionDataUpdateUiState.Available(check.release)
                        }
                    },
                    onFailure = {
                        QuestionDataUpdateUiState.Error(
                            it.message ?: "La recherche de questions a échoué."
                        )
                    }
                )
            }
        }
    }

    private fun downloadQuestionUpdate(release: QuestionDataRelease) {
        questionDataUpdateState = QuestionDataUpdateUiState.Downloading(release)
        worker.execute {
            val result = try {
                val archive = QuestionDataDownloader(applicationContext).download(release.asset)
                val catalog = questionDataStore.installArchive(
                    archive = archive,
                    expectedVersion = release.contentVersion
                )
                Result.success(catalog)
            } catch (exception: Exception) {
                Result.failure(exception)
            }
            runOnUiThread {
                result.fold(
                    onSuccess = { catalog ->
                        questionCatalog = catalog
                        progressSnapshot = progressStore.snapshot(catalog)
                        questionDataUpdateState =
                            QuestionDataUpdateUiState.Updated(catalog.contentVersion)
                    },
                    onFailure = {
                        questionDataUpdateState = QuestionDataUpdateUiState.Error(
                            it.message ?: "La mise à jour des questions a échoué."
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

    private fun recordAnswer(
        question: LearningQuestion,
        game: LearningGame,
        wasCorrect: Boolean
    ): String? =
        try {
            val catalog = questionCatalog
                ?: return "Les questions ne sont pas encore chargées."
            progressSnapshot = progressStore.recordAnswer(question, game, wasCorrect, catalog, questionMasteryThreshold)
            null
        } catch (exception: Exception) {
            exception.message ?: "La réponse n’a pas pu être enregistrée."
        }

    private fun setExtraAttempts(value: Int): String? =
        try {
            learningSettingsStore.setExtraAttempts(value)
            extraAttempts = value
            null
        } catch (exception: Exception) {
            exception.message ?: "Le réglage n’a pas pu être enregistré."
        }

    private fun setQuestionMasteryThreshold(value: Int): String? =
        try {
            learningSettingsStore.setQuestionMasteryThreshold(value)
            questionMasteryThreshold = value
            null
        } catch (exception: Exception) {
            exception.message ?: "Le seuil de maîtrise n’a pas pu être enregistré."
        }

    private fun setMasteredQuestionRepeatPercent(value: Int): String? =
        try {
            learningSettingsStore.setMasteredQuestionRepeatPercent(value)
            masteredQuestionRepeatPercent = value
            null
        } catch (exception: Exception) {
            exception.message ?: "Le taux de réapparition n’a pas pu être enregistré."
        }

    private fun setSessionQuestionCount(value: Int): String? =
        try {
            learningSettingsStore.setSessionQuestionCount(value)
            sessionQuestionCount = value
            null
        } catch (exception: Exception) {
            exception.message ?: "Le nombre de questions n’a pas pu être enregistré."
        }

    private fun setSelectedQuestionBanks(value: Set<QuestionBank>) {
        learningSettingsStore.setSelectedQuestionBanks(value)
    }

    private fun persistSelectedLearningTrack(value: LearningTrack) {
        learningSettingsStore.setSelectedLearningTrack(value)
        selectedLearningTrack = value
    }

    private fun resetMasteryCounters(): String? =
        try {
            val catalog = questionCatalog
                ?: return "Les questions ne sont pas encore chargées."
            progressSnapshot = progressStore.resetMasteryCounters(catalog)
            null
        } catch (exception: Exception) {
            exception.message ?: "Les compteurs des questions n’ont pas pu être réinitialisés."
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
