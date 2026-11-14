package fr.nihongo.app

import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

internal data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sha256: String
)

internal data class GitHubRelease(
    val tag: String,
    val title: String,
    val notes: String,
    val version: ReleaseVersion,
    val apk: ReleaseAsset
)

internal sealed interface ReleaseCheck {
    data object NoRelease : ReleaseCheck
    data class Current(val installedVersion: String) : ReleaseCheck
    data class Available(val release: GitHubRelease) : ReleaseCheck
}

internal class GitHubReleaseClient {
    fun checkForUpdate(installedVersion: String): ReleaseCheck {
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
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) return ReleaseCheck.NoRelease
            if (responseCode !in 200..299) {
                throw IllegalStateException("GitHub a répondu avec le code HTTP $responseCode.")
            }

            val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val releaseJson = latestApplicationRelease(JSONArray(json)) ?: return ReleaseCheck.NoRelease
            val release = parseRelease(releaseJson)
            val installed = ReleaseVersion.parse(installedVersion)
                ?: throw IllegalStateException("La version installée « $installedVersion » n’est pas valide.")
            return if (release.version > installed) {
                ReleaseCheck.Available(release)
            } else {
                ReleaseCheck.Current(installedVersion)
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun latestApplicationRelease(releases: JSONArray): JSONObject? =
        (0 until releases.length())
            .map { releases.getJSONObject(it) }
            .filterNot { it.optBoolean("draft") || it.optBoolean("prerelease") }
            .mapNotNull { json ->
                val tag = json.optString("tag_name")
                if (!tag.startsWith("v")) return@mapNotNull null
                val version = ReleaseVersion.parse(tag)
                    ?: throw IllegalStateException("Le tag GitHub « $tag » ne suit pas le format v1.2.3.")
                version to json
            }
            .maxByOrNull { it.first }
            ?.second

    internal fun parseRelease(json: JSONObject): GitHubRelease {
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) {
            throw IllegalStateException("La dernière publication GitHub est une préversion non prise en charge.")
        }
        val tag = json.optString("tag_name")
        val version = ReleaseVersion.parse(tag)
            ?: throw IllegalStateException("Le tag GitHub « $tag » doit suivre le format v1.2.3.")
        val assets = json.optJSONArray("assets")
            ?: throw IllegalStateException("La publication GitHub ne contient aucun fichier APK.")
        val apks = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".apk", ignoreCase = true) }
        val asset = apks.singleOrNull()
            ?: throw IllegalStateException(
                if (apks.isEmpty()) "La publication GitHub ne contient aucun fichier APK."
                else "La publication GitHub contient plusieurs APK ; un seul fichier APK est attendu."
            )
        val digest = asset.optString("digest")
        val sha256 = digest.removePrefix("sha256:")
        if (!digest.startsWith("sha256:") || !SHA256_PATTERN.matches(sha256)) {
            throw IllegalStateException("GitHub ne fournit pas d’empreinte SHA-256 pour cet APK.")
        }
        val url = asset.optString("browser_download_url")
        val uri = try {
            URI(url)
        } catch (_: URISyntaxException) {
            null
        }
        if (uri?.scheme != "https" ||
            uri.host != GITHUB_HOST ||
            uri.path?.startsWith(RELEASES_DOWNLOAD_PATH) != true
        ) {
            throw IllegalStateException("L’adresse de téléchargement ne correspond pas à une Release GitHub du projet.")
        }

        return GitHubRelease(
            tag = tag,
            title = json.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: tag,
            notes = json.optString("body").trim(),
            version = version,
            apk = ReleaseAsset(
                name = asset.optString("name"),
                downloadUrl = url,
                sha256 = sha256.lowercase()
            )
        )
    }

    private companion object {
        const val GITHUB_HOST = "github.com"
        const val RELEASES_DOWNLOAD_PATH = "/radiogrange47-ux/Apk_Nihongo/releases/download/"
        val SHA256_PATTERN = Regex("^[a-fA-F0-9]{64}$")
        const val RELEASES_URL =
            "https://api.github.com/repos/radiogrange47-ux/Apk_Nihongo/releases?per_page=100"
    }
}
