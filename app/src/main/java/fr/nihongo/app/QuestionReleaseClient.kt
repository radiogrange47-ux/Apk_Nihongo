package fr.nihongo.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

internal data class QuestionDataRelease(
    val tag: String,
    val title: String,
    val contentVersion: String,
    val version: ReleaseVersion,
    val asset: ReleaseAsset
)

internal sealed interface QuestionDataReleaseCheck {
    data object NoRelease : QuestionDataReleaseCheck
    data class Current(val version: String) : QuestionDataReleaseCheck
    data class Available(val release: QuestionDataRelease) : QuestionDataReleaseCheck
}

internal class QuestionReleaseClient {
    fun checkForUpdate(installedVersion: String): QuestionDataReleaseCheck {
        val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Nihongo-Android")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("GitHub a répondu avec le code HTTP $responseCode.")
            }
            val releases = JSONArray(
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            )
            val releaseJson = latestQuestionRelease(releases)
                ?: return QuestionDataReleaseCheck.NoRelease
            val release = parseRelease(releaseJson)
            val installed = ReleaseVersion.parse(installedVersion)
                ?: throw IllegalStateException("La version du contenu installé n’est pas valide.")
            return if (release.version > installed) {
                QuestionDataReleaseCheck.Available(release)
            } else {
                QuestionDataReleaseCheck.Current(installedVersion)
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun latestQuestionRelease(releases: JSONArray): JSONObject? =
        (0 until releases.length())
            .map { releases.getJSONObject(it) }
            .filterNot { it.optBoolean("draft") || it.optBoolean("prerelease") }
            .mapNotNull { json ->
                val tag = json.optString("tag_name")
                if (!tag.startsWith(QUESTION_TAG_PREFIX)) return@mapNotNull null
                val versionText = tag.removePrefix(QUESTION_TAG_PREFIX)
                val version = ReleaseVersion.parse(versionText)
                    ?: throw IllegalStateException("Le tag de contenu « $tag » n’est pas valide.")
                version to json
            }
            .maxByOrNull { it.first }
            ?.second

    internal fun parseRelease(json: JSONObject): QuestionDataRelease {
        val tag = json.getString("tag_name")
        require(tag.startsWith(QUESTION_TAG_PREFIX)) {
            "La publication GitHub n’est pas une mise à jour des questions."
        }
        val versionText = tag.removePrefix(QUESTION_TAG_PREFIX)
        val version = ReleaseVersion.parse(versionText)
            ?: throw IllegalStateException("Le tag de contenu « $tag » n’est pas valide.")
        val assets = json.optJSONArray("assets") ?: JSONArray()
        val matchingAssets = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .filter { it.optString("name") == QUESTION_ARCHIVE_NAME }
        val asset = matchingAssets.singleOrNull()
            ?: throw IllegalStateException(
                "La publication « $tag » doit contenir exactement un fichier $QUESTION_ARCHIVE_NAME."
            )
        val digest = asset.optString("digest")
        val sha256 = digest.removePrefix("sha256:")
        require(digest.startsWith("sha256:") && SHA256_PATTERN.matches(sha256)) {
            "GitHub ne fournit pas d’empreinte SHA-256 valide pour les questions."
        }
        val downloadUrl = asset.optString("browser_download_url")
        val uri = try {
            URI(downloadUrl)
        } catch (_: URISyntaxException) {
            null
        }
        require(
            uri?.scheme == "https" &&
                uri.host == GITHUB_HOST &&
                uri.path?.startsWith(QUESTION_RELEASE_DOWNLOAD_PATH + tag + "/") == true
        ) {
            "L’adresse de téléchargement ne correspond pas à une publication de questions du projet."
        }
        return QuestionDataRelease(
            tag = tag,
            title = json.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: tag,
            contentVersion = versionText.removePrefix("v"),
            version = version,
            asset = ReleaseAsset(
                name = QUESTION_ARCHIVE_NAME,
                downloadUrl = downloadUrl,
                sha256 = sha256.lowercase()
            )
        )
    }

    private companion object {
        const val QUESTION_TAG_PREFIX = "questions-v"
        const val QUESTION_ARCHIVE_NAME = "questions.zip"
        const val GITHUB_HOST = "github.com"
        const val QUESTION_RELEASE_DOWNLOAD_PATH = "/radiogrange47-ux/Apk_Nihongo/releases/download/"
        const val RELEASES_URL =
            "https://api.github.com/repos/radiogrange47-ux/Apk_Nihongo/releases?per_page=100"
        val SHA256_PATTERN = Regex("^[a-fA-F0-9]{64}$")
    }
}
