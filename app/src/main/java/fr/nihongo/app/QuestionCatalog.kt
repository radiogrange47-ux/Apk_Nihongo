package fr.nihongo.app

import org.json.JSONArray
import org.json.JSONObject

internal data class QuestionCatalog(
    val contentVersion: String,
    val particles: List<ParticleExercise>,
    val vocabulary: List<VocabularyQuestion>,
    val verbs: List<VocabularyQuestion>,
    val conjugation: List<ConjugationQuestion>
) {
    fun questionsFor(game: LearningGame): List<LearningQuestion> = when (game) {
        LearningGame.PARTICLES -> particles
        LearningGame.VOCABULARY -> vocabulary
        LearningGame.VERBS -> verbs
        LearningGame.CONJUGATION -> conjugation
    }

    fun allQuestionIds(): Map<LearningGame, Set<String>> =
        LearningGame.values().associateWith { game ->
            questionsFor(game).mapTo(linkedSetOf()) { it.id }
        }
}

internal object QuestionCatalogCodec {
    const val SCHEMA_VERSION = 1
    val fileNames = listOf(
        "particles.json",
        "vocabulary.json",
        "verbs.json",
        "conjugation.json"
    )

    fun parse(manifestText: String, dataFiles: Map<String, String>): QuestionCatalog {
        val manifest = JSONObject(manifestText)
        require(manifest.getInt("schemaVersion") == SCHEMA_VERSION) {
            "La version du format des questions n’est pas prise en charge."
        }
        val contentVersion = manifest.getString("contentVersion")
        require(ReleaseVersion.parse(contentVersion) != null) {
            "La version du contenu n’est pas valide."
        }
        val manifestFiles = manifest.getJSONArray("files").stringValues()
        require(manifestFiles.size == fileNames.size && manifestFiles.toSet() == fileNames.toSet()) {
            "Le manifeste doit référencer exactement les quatre fichiers de questions."
        }
        require(dataFiles.keys == fileNames.toSet()) {
            "Le paquet ne contient pas exactement les fichiers annoncés."
        }

        return QuestionCatalog(
            contentVersion = contentVersion,
            particles = dataFiles.getValue("particles.json").questionArray(::particle),
            vocabulary = dataFiles.getValue("vocabulary.json").questionArray(::vocabulary),
            verbs = dataFiles.getValue("verbs.json").questionArray(::vocabulary),
            conjugation = dataFiles.getValue("conjugation.json").questionArray(::conjugation)
        ).also(::validateCatalog)
    }

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

    private fun particle(json: JSONObject) = ParticleExercise(
        id = json.getString("id"),
        topic = json.getString("topic"),
        tokens = json.getJSONArray("tokens").objectValues(::token),
        missingTokenIndex = json.getInt("missingTokenIndex"),
        choices = json.getJSONArray("choices").stringValues(),
        answer = json.getString("answer"),
        explanation = json.getString("explanation"),
        translation = json.getString("translation"),
        level = json.optionalText("level"),
        source = json.optionalText("source")
    )

    private fun vocabulary(json: JSONObject) = VocabularyQuestion(
        id = json.getString("id"),
        topic = json.getString("topic"),
        kanji = json.getString("kanji"),
        hiragana = json.getString("hiragana"),
        answer = json.getString("answer"),
        choices = json.getJSONArray("choices").stringValues(),
        level = json.optionalText("level"),
        source = json.optionalText("source")
    )

    private fun conjugation(json: JSONObject) = ConjugationQuestion(
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
        source = json.optionalText("source")
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
