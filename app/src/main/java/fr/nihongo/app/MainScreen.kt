package fr.nihongo.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val NihongoBackground = Color(0xFF080808)
private val NihongoPanel = Color(0xFF141414)
private val NihongoAccent = Color(0xFFE05252)
private val NihongoBorder = Color(0xFF292929)

private enum class AppScreen {
    HOME,
    SESSION,
    PROGRESS,
    SETTINGS,
    UPDATES,
    QUESTION_DATA_UPDATES
}

@Composable
internal fun MainScreen(
    appVersion: String,
    updateState: UpdateUiState,
    questionCatalog: QuestionCatalog,
    questionDataUpdateState: QuestionDataUpdateUiState,
    progress: ProgressSnapshot,
    extraAttempts: Int,
    questionMasteryThreshold: Int,
    masteredQuestionRepeatPercent: Int,
    sessionQuestionCount: Int,
    selectedQuestionBanks: Set<QuestionBank>,
    questionSpeech: JapaneseQuestionSpeech,
    onQuestionBanksChange: (Set<QuestionBank>) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (GitHubRelease) -> Unit,
    onCheckQuestionUpdates: () -> Unit,
    onDownloadQuestionUpdate: (QuestionDataRelease) -> Unit,
    onRetryInstallPermission: () -> Unit,
    onAnswer: (LearningQuestion, LearningGame, Boolean) -> String?,
    onExtraAttemptsChange: (Int) -> String?,
    onQuestionMasteryThresholdChange: (Int) -> String?,
    onMasteredQuestionRepeatPercentChange: (Int) -> String?,
    onSessionQuestionCountChange: (Int) -> String?,
    onResetMasteryCounters: () -> String?
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var selectedGameNames by rememberSaveable {
        mutableStateOf(LearningGame.values().joinToString(",") { it.name })
    }
    var selectedBankNames by rememberSaveable {
        mutableStateOf(selectedQuestionBanks.joinToString(",") { it.name })
    }
    val activeBanks = selectedBankNames.split(",").mapNotNull { name ->
        QuestionBank.values().firstOrNull { it.name == name }
    }.toSet()
    val activeCatalog = questionCatalog.forBanks(activeBanks)
    var sessionGameNames by rememberSaveable { mutableStateOf("") }
    var sessionGameIndex by rememberSaveable { mutableIntStateOf(0) }
    var questionsCompleted by rememberSaveable { mutableIntStateOf(0) }
    var sessionRound by rememberSaveable { mutableIntStateOf(0) }
    var sessionTargetCounts by rememberSaveable { mutableStateOf("") }
    val selectedGames = LearningGame.values()
        .filter { it.name in selectedGameNames.split(",") && activeCatalog.questionsFor(it).isNotEmpty() }
    val eligibleQuestionCounts = LearningGame.values().associateWith { game ->
        val masteryCounters = progress.questionsFor(game).associate { it.questionId to it.masteryCounter }
        QuestionSelector.countEligibleQuestions(
            questions = activeCatalog.questionsFor(game),
            masteryCounters = masteryCounters,
            masteryThreshold = questionMasteryThreshold,
            masteredQuestionRepeatPercent = masteredQuestionRepeatPercent
        )
    }
    val playableGames = selectedGames.filter { (eligibleQuestionCounts[it] ?: 0) > 0 }
    val sessionGames = sessionGameNames.split(",")
        .mapNotNull { name -> LearningGame.values().firstOrNull { it.name == name } }
    val storedSessionTargets = sessionTargetCounts.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        val game = parts.getOrNull(0)?.let { name -> LearningGame.values().firstOrNull { it.name == name } }
        val target = parts.getOrNull(1)?.toIntOrNull()
        if (game != null && target != null) game to target else null
    }.toMap()

    fun updateGameSelection(game: LearningGame, checked: Boolean) {
        val selected = selectedGameNames.split(",").filter(String::isNotBlank).toMutableSet()
        if (checked) selected.add(game.name) else selected.remove(game.name)
        selectedGameNames = LearningGame.values()
            .filter { it.name in selected }
            .joinToString(",") { it.name }
    }

    fun updateBankSelection(bank: QuestionBank, checked: Boolean) {
        val updated = activeBanks.toMutableSet().apply {
            if (checked) add(bank) else remove(bank)
        }
        selectedBankNames = QuestionBank.values().filter { it in updated }.joinToString(",") { it.name }
        onQuestionBanksChange(updated)
    }

    fun startSession() {
        if (playableGames.isEmpty()) return
        sessionGameNames = playableGames.joinToString(",") { it.name }
        sessionTargetCounts = playableGames.joinToString(",") { game ->
            "${game.name}:${minOf(sessionQuestionCount, eligibleQuestionCounts.getValue(game))}"
        }
        sessionGameIndex = 0
        questionsCompleted = 0
        sessionRound++
        screen = AppScreen.SESSION
    }

    fun selectAllGames() {
        selectedGameNames = LearningGame.values().joinToString(",") { it.name }
    }

    fun selectNoGames() {
        selectedGameNames = ""
    }

    val sessionAnswer: (LearningQuestion, LearningGame, Boolean) -> String? =
        { question, game, wasCorrect -> onAnswer(question, game, wasCorrect) }

    val completeSessionQuestion: (LearningGame) -> Unit = { game ->
        if (screen == AppScreen.SESSION &&
            sessionGames.isNotEmpty() &&
            sessionGames.getOrNull(sessionGameIndex) == game
        ) {
            val target = storedSessionTargets[game]
                ?: minOf(sessionQuestionCount, activeCatalog.questionsFor(game).size)
            val update = LearningSessionRules.afterQuestion(
                questionsCompleted = questionsCompleted,
                gameIndex = sessionGameIndex,
                gameCount = sessionGames.size,
                target = target
            )
            questionsCompleted = update.questionsCompleted
            if (update.advanceToNextGame) {
                sessionGameIndex = update.gameIndex
                sessionRound++
            }
        }
    }

    val allSelectedQuestionsMastered = sessionGames.isNotEmpty() &&
        sessionGames.all { game ->
            val selectedIds = activeCatalog.questionsFor(game).mapTo(hashSetOf()) { it.id }
            val gameQuestions = progress.questionsFor(game).filter { it.questionId in selectedIds }
            gameQuestions.isNotEmpty() &&
                gameQuestions.all { it.masteryCounter >= questionMasteryThreshold }
        }
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
                    questionCatalog = questionCatalog,
                    masteryThreshold = questionMasteryThreshold,
                    selectedGames = selectedGames,
                    playableGameCount = playableGames.size,
                    selectedBanks = activeBanks,
                    onBankSelectionChange = ::updateBankSelection,
                    onGameSelectionChange = ::updateGameSelection,
                    onSelectAll = ::selectAllGames,
                    onSelectNone = ::selectNoGames,
                    onStart = ::startSession,
                    onShowProgress = { screen = AppScreen.PROGRESS },
                    onShowSettings = { screen = AppScreen.SETTINGS },
                )

                AppScreen.SESSION -> {
                    val game = sessionGames.getOrNull(sessionGameIndex)
                    if (game == null) {
                        SideEffect { screen = AppScreen.HOME }
                    } else {
                        key(sessionRound, game) {
                            when (game) {
                                LearningGame.PARTICLES -> ParticleGameScreen(
                                    questions = activeCatalog.particles,
                                    questionSpeech = questionSpeech,
                                    onBack = { screen = AppScreen.HOME },
                                    onAnswer = sessionAnswer,
                                    onQuestionCompleted = completeSessionQuestion,
                                    extraAttempts = extraAttempts,
                                    progress = progress,
                                    masteryThreshold = questionMasteryThreshold,
                                    masteredQuestionRepeatPercent = masteredQuestionRepeatPercent,
                                    completedQuestions = questionsCompleted,
                                    seriesTarget = storedSessionTargets[game]
                                        ?: minOf(sessionQuestionCount, activeCatalog.particles.size),
                                    allQuestionsMastered = allSelectedQuestionsMastered
                                )

                                LearningGame.VOCABULARY -> VocabularyGameScreen(
                                    game = game,
                                    questions = activeCatalog.vocabulary,
                                    questionSpeech = questionSpeech,
                                    onBack = { screen = AppScreen.HOME },
                                    onAnswer = sessionAnswer,
                                    onQuestionCompleted = completeSessionQuestion,
                                    extraAttempts = extraAttempts,
                                    progress = progress,
                                    masteryThreshold = questionMasteryThreshold,
                                    masteredQuestionRepeatPercent = masteredQuestionRepeatPercent,
                                    completedQuestions = questionsCompleted,
                                    seriesTarget = storedSessionTargets[game]
                                        ?: minOf(sessionQuestionCount, activeCatalog.vocabulary.size),
                                    allQuestionsMastered = allSelectedQuestionsMastered
                                )

                                LearningGame.VERBS -> VocabularyGameScreen(
                                    game = game,
                                    questions = activeCatalog.verbs,
                                    questionSpeech = questionSpeech,
                                    onBack = { screen = AppScreen.HOME },
                                    onAnswer = sessionAnswer,
                                    onQuestionCompleted = completeSessionQuestion,
                                    extraAttempts = extraAttempts,
                                    progress = progress,
                                    masteryThreshold = questionMasteryThreshold,
                                    masteredQuestionRepeatPercent = masteredQuestionRepeatPercent,
                                    completedQuestions = questionsCompleted,
                                    seriesTarget = storedSessionTargets[game]
                                        ?: minOf(sessionQuestionCount, activeCatalog.verbs.size),
                                    allQuestionsMastered = allSelectedQuestionsMastered
                                )

                                LearningGame.CONJUGATION -> ConjugationGameScreen(
                                    questions = activeCatalog.conjugation,
                                    questionSpeech = questionSpeech,
                                    onBack = { screen = AppScreen.HOME },
                                    onAnswer = sessionAnswer,
                                    onQuestionCompleted = completeSessionQuestion,
                                    extraAttempts = extraAttempts,
                                    progress = progress,
                                    masteryThreshold = questionMasteryThreshold,
                                    masteredQuestionRepeatPercent = masteredQuestionRepeatPercent,
                                    completedQuestions = questionsCompleted,
                                    seriesTarget = storedSessionTargets[game]
                                        ?: minOf(sessionQuestionCount, activeCatalog.conjugation.size),
                                    allQuestionsMastered = allSelectedQuestionsMastered
                                )
                            }
                        }
                    }
                }

                AppScreen.PROGRESS -> ProgressScreen(
                    progress = progress,
                    masteryThreshold = questionMasteryThreshold,
                    onBack = { screen = AppScreen.HOME }
                )

                AppScreen.SETTINGS -> LearningSettingsScreen(
                    extraAttempts = extraAttempts,
                    questionMasteryThreshold = questionMasteryThreshold,
                    masteredQuestionRepeatPercent = masteredQuestionRepeatPercent,
                    sessionQuestionCount = sessionQuestionCount,
                    appVersion = appVersion,
                    contentVersion = questionCatalog.contentVersion,
                    onBack = { screen = AppScreen.HOME },
                    onExtraAttemptsChange = onExtraAttemptsChange,
                    onQuestionMasteryThresholdChange = onQuestionMasteryThresholdChange,
                    onMasteredQuestionRepeatPercentChange = onMasteredQuestionRepeatPercentChange,
                    onSessionQuestionCountChange = onSessionQuestionCountChange,
                    onResetMasteryCounters = onResetMasteryCounters,
                    onShowUpdates = { screen = AppScreen.UPDATES },
                    onShowQuestionUpdates = { screen = AppScreen.QUESTION_DATA_UPDATES }
                )

                AppScreen.UPDATES -> UpdatesScreen(
                    appVersion = appVersion,
                    updateState = updateState,
                    onBack = { screen = AppScreen.HOME },
                    onCheckUpdates = onCheckUpdates,
                    onDownloadUpdate = onDownloadUpdate,
                    onRetryInstallPermission = onRetryInstallPermission
                )

                AppScreen.QUESTION_DATA_UPDATES -> QuestionDataUpdatesScreen(
                    contentVersion = questionCatalog.contentVersion,
                    updateState = questionDataUpdateState,
                    onBack = { screen = AppScreen.HOME },
                    onCheckUpdates = onCheckQuestionUpdates,
                    onDownloadUpdate = onDownloadQuestionUpdate
                )
            }
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
    questionMasteryThreshold: Int,
    masteredQuestionRepeatPercent: Int,
    sessionQuestionCount: Int,
    appVersion: String,
    contentVersion: String,
    onBack: () -> Unit,
    onExtraAttemptsChange: (Int) -> String?,
    onQuestionMasteryThresholdChange: (Int) -> String?,
    onMasteredQuestionRepeatPercentChange: (Int) -> String?,
    onSessionQuestionCountChange: (Int) -> String?,
    onResetMasteryCounters: () -> String?,
    onShowUpdates: () -> Unit,
    onShowQuestionUpdates: () -> Unit
) {
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    var thresholdInput by rememberSaveable(questionMasteryThreshold) {
        mutableStateOf(questionMasteryThreshold.toString())
    }
    var thresholdError by rememberSaveable { mutableStateOf<String?>(null) }
    var repeatPercentValue by rememberSaveable(masteredQuestionRepeatPercent) {
        mutableFloatStateOf(masteredQuestionRepeatPercent.toFloat())
    }
    var repeatPercentError by rememberSaveable { mutableStateOf<String?>(null) }
    var sessionQuestionCountInput by rememberSaveable(sessionQuestionCount) {
        mutableStateOf(sessionQuestionCount.toString())
    }
    var sessionQuestionCountError by rememberSaveable { mutableStateOf<String?>(null) }
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }
    var resetError by rememberSaveable { mutableStateOf<String?>(null) }
    var resetMessage by rememberSaveable { mutableStateOf<String?>(null) }

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
                Text("Enchaînement des mini-jeux", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Chaque mini-jeu propose des questions différentes, sans répétition dans la série. La séance passe ensuite au jeu suivant.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = sessionQuestionCountInput,
                    onValueChange = {
                        sessionQuestionCountInput = it
                        sessionQuestionCountError = null
                    },
                    label = { Text("Questions par mini-jeu") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val target = sessionQuestionCountInput.toIntOrNull()
                        sessionQuestionCountError = if (target == null || target < 1) {
                            "Saisis un nombre entier supérieur à zéro."
                        } else {
                            onSessionQuestionCountChange(target)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer")
                }
                sessionQuestionCountError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        }

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
                Text("Révision des questions", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Après ${questionMasteryThreshold} bonnes réponses, une question est considérée comme maîtrisée. Une erreur fait baisser son compteur.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = {
                        thresholdInput = it
                        thresholdError = null
                    },
                    label = { Text("Bonnes réponses avant réduction") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val threshold = thresholdInput.toIntOrNull()
                        thresholdError = if (threshold == null || threshold < 1) {
                            "Saisis un nombre entier supérieur à zéro."
                        } else {
                            onQuestionMasteryThresholdChange(threshold)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enregistrer le seuil")
                }
                thresholdError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Réapparition des questions maîtrisées : ${repeatPercentValue.roundToInt()} %",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "0 % : jamais sélectionnées. 100 % : même fréquence que les autres questions.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = repeatPercentValue,
                    onValueChange = {
                        repeatPercentValue = it
                        repeatPercentError = null
                    },
                    valueRange = 0f..100f,
                    steps = 99,
                    onValueChangeFinished = {
                        repeatPercentError = onMasteredQuestionRepeatPercentChange(repeatPercentValue.roundToInt())
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0 %", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("100 %", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                repeatPercentError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        resetError = null
                        showResetConfirmation = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF393939))
                ) {
                    Text("Réinitialiser les compteurs")
                }
                resetMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = NihongoPanel),
            border = androidx.compose.foundation.BorderStroke(1.dp, NihongoBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Mises à jour", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onShowUpdates, modifier = Modifier.fillMaxWidth()) {
                    Text("Application · v$appVersion", modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = onShowQuestionUpdates, modifier = Modifier.fillMaxWidth()) {
                    Text("Questions · v$contentVersion", modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Réinitialiser les compteurs ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Les compteurs de maîtrise de toutes les questions seront remis à zéro.")
                    Text(
                        "L’historique des réponses et les statistiques seront conservés.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    resetError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetError = onResetMasteryCounters()
                        if (resetError == null) {
                            resetMessage = "Les compteurs ont été réinitialisés."
                            showResetConfirmation = false
                        }
                    }
                ) {
                    Text("Réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun QuestionDataUpdatesScreen(
    contentVersion: String,
    updateState: QuestionDataUpdateUiState,
    onBack: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (QuestionDataRelease) -> Unit
) {
    val isBusy = updateState is QuestionDataUpdateUiState.Checking ||
        updateState is QuestionDataUpdateUiState.Downloading
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹  Accueil") }
        Text("Mise à jour des questions", style = MaterialTheme.typography.headlineSmall)
        Text("Version installée : $contentVersion")
        Text(
            when (val state = updateState) {
                QuestionDataUpdateUiState.Idle ->
                    "Les exercices installés restent disponibles hors ligne."

                QuestionDataUpdateUiState.Checking ->
                    "Recherche d’une nouvelle version des exercices…"

                QuestionDataUpdateUiState.NoRelease ->
                    "Aucune version de questions n’est publiée."

                is QuestionDataUpdateUiState.Current ->
                    "Les questions sont à jour (version ${state.version})."

                is QuestionDataUpdateUiState.Available ->
                    "Version ${state.release.contentVersion} disponible : ${state.release.title}"

                is QuestionDataUpdateUiState.Downloading ->
                    "Téléchargement de la version ${state.release.contentVersion}…"

                is QuestionDataUpdateUiState.Updated ->
                    "Les questions ont été mises à jour (version ${state.version})."

                is QuestionDataUpdateUiState.Error ->
                    "La mise à jour des questions a échoué : ${state.message}"
            }
        )

        when (updateState) {
            QuestionDataUpdateUiState.Checking,
            is QuestionDataUpdateUiState.Downloading -> CircularProgressIndicator()

            is QuestionDataUpdateUiState.Available -> Button(
                onClick = { onDownloadUpdate(updateState.release) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Télécharger les questions")
            }

            else -> Unit
        }

        TextButton(
            onClick = onCheckUpdates,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rechercher des questions")
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
