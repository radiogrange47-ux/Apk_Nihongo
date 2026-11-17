package fr.nihongo.app

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

internal data class QuestionCatalog(
    val contentVersion: String,
    val particles: List<ParticleExercise>,
    val vocabulary: List<VocabularyQuestion>,
    val verbs: List<VocabularyQuestion>,
    val conjugation: List<ConjugationQuestion>
) {
    fun forBanks(banks: Set<QuestionBank>): QuestionCatalog = copy(
        particles = particles.filter { it.bank in banks },
        vocabulary = vocabulary.filter { it.bank in banks },
        verbs = verbs.filter { it.bank in banks },
        conjugation = conjugation.filter { it.bank in banks }
    )
    fun questionsFor(game: LearningGame): List<LearningQuestion> = when (game) {
        LearningGame.PARTICLES -> particles
        LearningGame.VOCABULARY -> vocabulary
        LearningGame.VERBS -> verbs
        LearningGame.CONJUGATION -> conjugation
        LearningGame.PAIRS -> pairingQuestions()
        LearningGame.COUNTER_CHALLENGE -> counterChallengeQuestions()
    }

    fun pairingQuestions(): List<VocabularyQuestion> = (vocabulary + verbs)
        .distinctBy { Triple(it.kanji, it.hiragana, it.answer) }
        .map { it.copy(id = stablePairId(it)) }

    internal fun legacyPairingIds(): Map<String, String> = (vocabulary + verbs)
        .distinctBy { Triple(it.kanji, it.hiragana, it.answer) }
        .associate { stablePairId(it) to "pairs-${it.id}" }

    fun counterChallengeQuestions(): List<VocabularyQuestion> {
        val counterQuestions = vocabulary.filter { it.bank == QuestionBank.COUNTERS }
        return counterQuestions.map { question ->
            val random = kotlin.random.Random(question.id.hashCode())
            val distractors = counterQuestions
                .filter { it.topic == question.topic && it.id != question.id }
                .shuffled(random)
                .take(5)
            val choices = (distractors.map { it.kanji } + question.kanji).shuffled(random)
            question.copy(
                id = "counter-${question.id}",
                kanji = question.answer,
                hiragana = question.hiragana,
                answer = question.kanji,
                choices = choices
            )
        }
    }

    fun allQuestionIds(): Map<LearningGame, Set<String>> =
        LearningGame.values().associateWith { game ->
            questionsFor(game).mapTo(linkedSetOf()) { it.id }
        }

    private fun stablePairId(question: VocabularyQuestion): String {
        val identity = "${question.kanji}\u0000${question.hiragana}\u0000${question.answer}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
        return "pairs-${digest.take(12).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }}"
    }
}

internal object QuestionCatalogCodec {
    const val SCHEMA_VERSION = 1
    fun parse(manifestText: String, dataFiles: Map<String, String>): QuestionCatalog {
        val manifest = JSONObject(manifestText)
        require(manifest.getInt("schemaVersion") == SCHEMA_VERSION) {
            "La version du format des questions n’est pas prise en charge."
        }
        val contentVersion = manifest.getString("contentVersion")
        require(ReleaseVersion.parse(contentVersion) != null) {
            "La version du contenu n’est pas valide."
        }
        val manifestFiles = manifest.getJSONArray("files").stringValues().map(::normalizedFileName)
        require(manifestFiles.isNotEmpty() && manifestFiles.distinct().size == manifestFiles.size &&
            manifestFiles.all(::isValidFileName)) {
            "Le manifeste contient un chemin invalide ou un doublon."
        }
        val normalizedDataFiles = dataFiles.entries.associate { (name, content) ->
            normalizedFileName(name) to content
        }
        require(normalizedDataFiles.keys == manifestFiles.toSet()) {
            "Le paquet ne contient pas exactement les fichiers annoncés."
        }

        val particles = mutableListOf<ParticleExercise>()
        val vocabularyQuestions = mutableListOf<VocabularyQuestion>()
        val verbQuestions = mutableListOf<VocabularyQuestion>()
        val conjugationQuestions = mutableListOf<ConjugationQuestion>()
        manifestFiles.forEach { path ->
            val bank = bankForPath(path)
            val content = normalizedDataFiles.getValue(path)
            when (path.substringAfterLast('/')) {
                "particles.json" -> particles += content.questionArray { particle(it, bank) }
                "vocabulary.json" -> vocabularyQuestions += content.questionArray { vocabulary(it, bank) }
                "verbs.json" -> verbQuestions += content.questionArray { vocabulary(it, bank) }
                "conjugation.json" -> conjugationQuestions += content.questionArray { conjugation(it, bank) }
            }
        }
        return QuestionCatalog(contentVersion, particles, vocabularyQuestions, verbQuestions, conjugationQuestions)
            .also(::validateCatalog)
    }

    fun isValidFileName(name: String): Boolean {
        val path = normalizedFileName(name)
        return path.count { it == '/' } == 1 &&
            path.substringBefore('/') in setOf(
                "JLPT5", "Mes cours", "JLPT4", "Nourriture", "Compteurs", "Connecteurs",
                "Démonstratifs", "Interrogatifs", "Quantifieurs"
            ) &&
            path.substringAfter('/') in setOf("particles.json", "vocabulary.json", "verbs.json", "conjugation.json")
    }

    private fun bankForPath(path: String): QuestionBank = when (path.substringBefore('/')) {
        "JLPT5" -> QuestionBank.JLPT5
        "Mes cours" -> QuestionBank.COURSE
        "JLPT4" -> QuestionBank.JLPT4
        "Nourriture" -> QuestionBank.FOOD
        "Compteurs" -> QuestionBank.COUNTERS
        "Connecteurs" -> QuestionBank.CONNECTORS
        "Démonstratifs" -> QuestionBank.DEMONSTRATIVES
        "Interrogatifs" -> QuestionBank.INTERROGATIVES
        "Quantifieurs" -> QuestionBank.DETERMINERS
        else -> error("Banque de questions inconnue.")
    }

    private fun normalizedFileName(name: String): String =
        if ('/' in name) name else "JLPT5/$name"

    private fun validateCatalog(catalog: QuestionCatalog) {
        val allQuestions = LearningGame.values().flatMap(catalog::questionsFor)
        require(allQuestions.map { it.id }.distinct().size == allQuestions.size) {
            "Les identifiants des questions doivent être uniques dans tout le contenu."
        }
        LearningGame.values().forEach { game ->
            val questions = catalog.questionsFor(game)
            require(questions.isNotEmpty()) { "Le jeu « ${game.title} » ne contient aucune question." }
            require(questions.map { it.id }.distinct().size == questions.size) {
                "Les identifiants des questions du jeu « ${game.title} » doivent être uniques."
            }
        }
    }

    private fun particle(json: JSONObject, pathBank: QuestionBank) = ParticleExercise(
        id = json.getString("id"),
        topic = json.getString("topic"),
        tokens = json.getJSONArray("tokens").objectValues(::token),
        missingTokenIndex = json.getInt("missingTokenIndex"),
        choices = json.getJSONArray("choices").stringValues(),
        answer = json.getString("answer"),
        explanation = json.getString("explanation"),
        translation = json.getString("translation"),
        level = json.optionalText("level"),
        source = json.optionalText("source"),
        bank = json.optionalText("bank")?.let(QuestionBank::valueOf)?.also { require(it == pathBank) } ?: pathBank
    )

    private fun vocabulary(json: JSONObject, pathBank: QuestionBank) = VocabularyQuestion(
        id = json.getString("id"),
        topic = json.getString("topic"),
        kanji = json.getString("kanji"),
        hiragana = json.getString("hiragana"),
        answer = json.getString("answer"),
        choices = json.getJSONArray("choices").stringValues(),
        level = json.optionalText("level"),
        source = json.optionalText("source"),
        bank = json.optionalText("bank")?.let(QuestionBank::valueOf)?.also { require(it == pathBank) } ?: pathBank
    )

    private fun conjugation(json: JSONObject, pathBank: QuestionBank) = ConjugationQuestion(
        id = json.getString("id"),
        topic = json.getString("topic"),
        contextFrench = json.getString("contextFrench"),
        tokens = json.getJSONArray("tokens").objectValues(::token),
        verbTokenIndex = json.getInt("verbTokenIndex"),
        punctuation = json.getString("punctuation"),
        answerEnding = json.getString("answerEnding"),
        choices = json.getJSONArray("choices").stringValues(),
        translation = json.getString("translation"),
        explanation = json.getString("explanation"),
        level = json.optionalText("level"),
        source = json.optionalText("source"),
        bank = json.optionalText("bank")?.let(QuestionBank::valueOf)?.also { require(it == pathBank) } ?: pathBank
    )

    private fun token(json: JSONObject) = PhraseToken(
        japanese = json.getString("japanese"),
        hiragana = json.getString("hiragana"),
        french = json.getString("french"),
        category = WordCategory.valueOf(json.getString("category"))
    )

    private fun JSONObject.optionalText(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return getString(key).also { require(it.isNotBlank()) { "$key ne peut pas être vide." } }
    }

    private fun <T> String.questionArray(factory: (JSONObject) -> T): List<T> =
        JSONArray(this).objectValues(factory)

    private fun JSONArray.stringValues(): List<String> =
        (0 until length()).map { index -> getString(index) }

    private fun <T> JSONArray.objectValues(factory: (JSONObject) -> T): List<T> =
        (0 until length()).map { index -> factory(getJSONObject(index)) }
}
