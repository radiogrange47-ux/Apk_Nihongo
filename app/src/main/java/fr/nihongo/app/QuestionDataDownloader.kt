package fr.nihongo.app

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal class QuestionDataDownloader(context: Context) {
    private val cacheDirectory = File(context.cacheDir, "question-content")

    fun download(asset: ReleaseAsset): File {
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
            throw IllegalStateException("Impossible de préparer le téléchargement des questions.")
        }
        val archive = File(cacheDirectory, "questions.zip")
        val partialArchive = File(cacheDirectory, "questions.zip.part")
        val connection = openConnection(asset.downloadUrl)

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Le téléchargement des questions a échoué (HTTP $responseCode).")
            }
            if (connection.contentLengthLong > MAX_ARCHIVE_BYTES) {
                throw IllegalStateException("Le paquet de questions dépasse la taille maximale autorisée.")
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytes = 0L
            connection.inputStream.buffered().use { input ->
                partialArchive.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(totalBytes <= MAX_ARCHIVE_BYTES) {
                            "Le paquet de questions dépasse la taille maximale autorisée."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            val actualSha256 = digest.digest().joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
            require(actualSha256.equals(asset.sha256, ignoreCase = true)) {
                "L’empreinte SHA-256 des questions ne correspond pas à celle publiée par GitHub."
            }
            if (archive.exists() && !archive.delete()) {
                throw IllegalStateException("Impossible de remplacer un ancien téléchargement.")
            }
            if (!partialArchive.renameTo(archive)) {
                throw IllegalStateException("Impossible de finaliser le téléchargement des questions.")
            }
            return archive
        } finally {
            connection.disconnect()
            if (partialArchive.exists()) partialArchive.delete()
        }
    }

    private fun openConnection(downloadUrl: String): HttpURLConnection {
        var url = URL(downloadUrl)
        repeat(MAX_REDIRECTS + 1) { redirect ->
            require(url.protocol == "https" && isTrustedHost(url.host)) {
                "Le téléchargement des questions doit rester sur un domaine GitHub sécurisé."
            }
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 60_000
                setRequestProperty("User-Agent", "Nihongo-Android")
            }
            val responseCode = connection.responseCode
            if (responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                require(!location.isNullOrBlank()) {
                    "GitHub a renvoyé une redirection sans destination."
                }
                require(redirect < MAX_REDIRECTS) {
                    "Le téléchargement dépasse le nombre de redirections autorisé."
                }
                url = URL(url, location)
            } else {
                return connection
            }
        }
        throw IllegalStateException("Impossible de suivre la redirection du téléchargement des questions.")
    }

    private fun isTrustedHost(host: String): Boolean =
        host == GITHUB_HOST || host.endsWith(GITHUB_ASSET_DOMAIN_SUFFIX)

    private companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_ARCHIVE_BYTES = 20L * 1024L * 1024L
        const val GITHUB_HOST = "github.com"
        const val GITHUB_ASSET_DOMAIN_SUFFIX = ".githubusercontent.com"
    }
}
