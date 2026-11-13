package fr.nihongo.app

internal enum class WordCategory {
    VOCABULARY,
    PARTICLE,
    VERB,
    GRAMMAR_MARKER
}

internal data class PhraseToken(
    val japanese: String,
    val romaji: String,
    val french: String,
    val category: WordCategory
)

internal data class ParticleExercise(
    val id: String,
    val topic: String,
    val tokens: List<PhraseToken>,
    val missingTokenIndex: Int,
    val choices: List<String>,
    val answer: String,
    val explanation: String,
    val translation: String
) {
    init {
        require(missingTokenIndex in tokens.indices)
        require(tokens[missingTokenIndex].category == WordCategory.PARTICLE ||
            tokens[missingTokenIndex].category == WordCategory.GRAMMAR_MARKER)
        require(tokens[missingTokenIndex].japanese == answer)
        require(answer in choices)
    }
}

internal object ParticleExerciseBank {
    val exercises = listOf(
        ParticleExercise(
            id = "mizu-o-nomimasu",
            topic = "を — complément d’objet",
            tokens = listOf(
                PhraseToken("私", "watashi", "moi / je", WordCategory.VOCABULARY),
                PhraseToken("は", "wa", "marque le thème : « quant à moi »", WordCategory.PARTICLE),
                PhraseToken("水", "mizu", "eau", WordCategory.VOCABULARY),
                PhraseToken("を", "o", "marque l’objet que l’on boit", WordCategory.PARTICLE),
                PhraseToken("飲みます", "nomimasu", "boire (forme polie)", WordCategory.VERB)
            ),
            missingTokenIndex = 3,
            choices = listOf("は", "を", "に"),
            answer = "を",
            explanation = "を marque l’objet direct du verbe. Ici, c’est l’eau que l’on boit.",
            translation = "Je bois de l’eau."
        ),
        ParticleExercise(
            id = "neko-ga-imasu",
            topic = "が — sujet / existence",
            tokens = listOf(
                PhraseToken("猫", "neko", "chat", WordCategory.VOCABULARY),
                PhraseToken("が", "ga", "marque le sujet qui existe", WordCategory.PARTICLE),
                PhraseToken("います", "imasu", "être / se trouver (être vivant)", WordCategory.VERB)
            ),
            missingTokenIndex = 1,
            choices = listOf("が", "を", "で"),
            answer = "が",
            explanation = "が marque ici l’être vivant dont on indique l’existence : le chat.",
            translation = "Il y a un chat."
        ),
        ParticleExercise(
            id = "pan-o-tabemasu",
            topic = "を — complément d’objet",
            tokens = listOf(
                PhraseToken("毎朝", "maiasa", "chaque matin", WordCategory.VOCABULARY),
                PhraseToken("パン", "pan", "pain", WordCategory.VOCABULARY),
                PhraseToken("を", "o", "marque l’objet que l’on mange", WordCategory.PARTICLE),
                PhraseToken("食べます", "tabemasu", "manger (forme polie)", WordCategory.VERB)
            ),
            missingTokenIndex = 2,
            choices = listOf("を", "に", "も"),
            answer = "を",
            explanation = "を indique ce que l’on mange : le pain.",
            translation = "Je mange du pain chaque matin."
        ),
        ParticleExercise(
            id = "toshokan-de-hon-o-yomimasu",
            topic = "で — lieu de l’action",
            tokens = listOf(
                PhraseToken("図書館", "toshokan", "bibliothèque", WordCategory.VOCABULARY),
                PhraseToken("で", "de", "indique le lieu où se déroule l’action", WordCategory.PARTICLE),
                PhraseToken("本", "hon", "livre", WordCategory.VOCABULARY),
                PhraseToken("を", "o", "marque l’objet que l’on lit", WordCategory.PARTICLE),
                PhraseToken("読みます", "yomimasu", "lire (forme polie)", WordCategory.VERB)
            ),
            missingTokenIndex = 1,
            choices = listOf("で", "に", "へ"),
            answer = "で",
            explanation = "で indique le lieu où une action se déroule : lire à la bibliothèque.",
            translation = "Je lis un livre à la bibliothèque."
        ),
        ParticleExercise(
            id = "tomodachi-ni-tegami-o-kakimasu",
            topic = "に — destinataire",
            tokens = listOf(
                PhraseToken("友達", "tomodachi", "ami(e)", WordCategory.VOCABULARY),
                PhraseToken("に", "ni", "indique le destinataire", WordCategory.PARTICLE),
                PhraseToken("手紙", "tegami", "lettre", WordCategory.VOCABULARY),
                PhraseToken("を", "o", "marque ce que l’on écrit", WordCategory.PARTICLE),
                PhraseToken("書きます", "kakimasu", "écrire (forme polie)", WordCategory.VERB)
            ),
            missingTokenIndex = 1,
            choices = listOf("に", "で", "が"),
            answer = "に",
            explanation = "に indique le destinataire de la lettre : un ami ou une amie.",
            translation = "J’écris une lettre à un(e) ami(e)."
        ),
        ParticleExercise(
            id = "densha-de-gakkou-e-ikimasu",
            topic = "で — moyen de transport",
            tokens = listOf(
                PhraseToken("電車", "densha", "train", WordCategory.VOCABULARY),
                PhraseToken("で", "de", "indique le moyen de transport", WordCategory.PARTICLE),
                PhraseToken("学校", "gakkou", "école", WordCategory.VOCABULARY),
                PhraseToken("へ", "e", "indique la direction vers laquelle on va", WordCategory.PARTICLE),
                PhraseToken("行きます", "ikimasu", "aller (forme polie)", WordCategory.VERB)
            ),
            missingTokenIndex = 1,
            choices = listOf("で", "を", "の"),
            answer = "で",
            explanation = "で indique le moyen utilisé pour se déplacer : le train.",
            translation = "Je vais à l’école en train."
        ),
        ParticleExercise(
            id = "tsukue-no-ue-ni-hon-ga-arimasu",
            topic = "の — relation entre deux noms",
            tokens = listOf(
                PhraseToken("机", "tsukue", "bureau / table", WordCategory.VOCABULARY),
                PhraseToken("の", "no", "relie deux noms : « de »", WordCategory.GRAMMAR_MARKER),
                PhraseToken("上", "ue", "dessus / haut", WordCategory.VOCABULARY),
                PhraseToken("に", "ni", "indique l’emplacement", WordCategory.PARTICLE),
                PhraseToken("本", "hon", "livre", WordCategory.VOCABULARY),
                PhraseToken("が", "ga", "marque ce qui se trouve à cet endroit", WordCategory.PARTICLE),
                PhraseToken("あります", "arimasu", "être / se trouver (objet)", WordCategory.VERB)
            ),
            missingTokenIndex = 1,
            choices = listOf("の", "を", "で"),
            answer = "の",
            explanation = "の relie 机 (bureau) et 上 (dessus) : « le dessus du bureau ».",
            translation = "Il y a un livre sur le bureau."
        ),
        ParticleExercise(
            id = "nihongo-mo-benkyou-shimasu",
            topic = "も — ajouter une idée (« aussi »)",
            tokens = listOf(
                PhraseToken("日本語", "nihongo", "langue japonaise", WordCategory.VOCABULARY),
                PhraseToken("も", "mo", "marque l’ajout : « aussi »", WordCategory.GRAMMAR_MARKER),
                PhraseToken("勉強します", "benkyou shimasu", "étudier (forme polie)", WordCategory.VERB)
            ),
            missingTokenIndex = 1,
            choices = listOf("も", "を", "が"),
            answer = "も",
            explanation = "も remplace souvent は ou が pour ajouter un élément : le japonais aussi.",
            translation = "J’étudie aussi le japonais."
        ),
        ParticleExercise(
            id = "ringo-to-banana-o-kaimasu",
            topic = "と — relier des noms (« et »)",
            tokens = listOf(
                PhraseToken("りんご", "ringo", "pomme", WordCategory.VOCABULARY),
                PhraseToken("と", "to", "relie deux noms : « et »", WordCategory.PARTICLE),
                PhraseToken("バナナ", "banana", "banane", WordCategory.VOCABULARY),
                PhraseToken("を", "o", "marque ce que l’on achète", WordCategory.PARTICLE),
                PhraseToken("買います", "kaimasu", "acheter (forme polie)", WordCategory.VERB)
            ),
            missingTokenIndex = 1,
            choices = listOf("と", "に", "は"),
            answer = "と",
            explanation = "と relie ici deux noms : une pomme et une banane.",
            translation = "J’achète une pomme et une banane."
        ),
        ParticleExercise(
            id = "kouen-de-hashirimashita",
            topic = "で — lieu de l’action",
            tokens = listOf(
                PhraseToken("昨日", "kinou", "hier", WordCategory.VOCABULARY),
                PhraseToken("公園", "kouen", "parc", WordCategory.VOCABULARY),
                PhraseToken("で", "de", "indique le lieu où se déroule l’action", WordCategory.PARTICLE),
                PhraseToken("走りました", "hashirimashita", "courir (passé poli)", WordCategory.VERB)
            ),
            missingTokenIndex = 2,
            choices = listOf("で", "に", "を"),
            answer = "で",
            explanation = "で indique le lieu de l’action : courir dans le parc.",
            translation = "J’ai couru dans le parc hier."
        )
    )
}
