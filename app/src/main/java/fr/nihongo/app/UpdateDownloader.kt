package fr.nihongo.app

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal class UpdateDownloader(private val context: Context) {
    fun download(asset: ReleaseAsset): Uri {
        val directory = File(context.cacheDir, "updates")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IllegalStateException("Impossible de préparer le dossier temporaire de mise à jour.")
        }
        val apkFile = File(directory, "update.apk")
        val partialFile = File(directory, "update.apk.part")
        val connection = openDownloadConnection(asset.downloadUrl)

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Le téléchargement a échoué (HTTP $responseCode).")
            }
            if (connection.contentLengthLong > MAX_APK_SIZE_BYTES) {
                throw IllegalStateException("Le fichier APK dépasse la taille maximale autorisée.")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var totalBytes = 0L
            connection.inputStream.buffered().use { input ->
                partialFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > MAX_APK_SIZE_BYTES) {
                            throw IllegalStateException("Le fichier APK dépasse la taille maximale autorisée.")
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }

            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            if (!actualSha256.equals(asset.sha256, ignoreCase = true)) {
                throw IllegalStateException("L’empreinte SHA-256 de l’APK ne correspond pas à celle de GitHub.")
            }
            if (apkFile.exists() && !apkFile.delete()) {
                throw IllegalStateException("Impossible de remplacer un ancien téléchargement.")
            }
            if (!partialFile.renameTo(apkFile)) {
                throw IllegalStateException("Impossible de finaliser le téléchargement de l’APK.")
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } finally {
            connection.disconnect()
            if (partialFile.exists()) partialFile.delete()
        }
    }

    private fun openDownloadConnection(downloadUrl: String): HttpURLConnection {
        var url = URL(downloadUrl)
        repeat(MAX_REDIRECTS + 1) { redirect ->
            if (url.protocol != "https") {
                throw IllegalStateException("Le téléchargement doit rester en HTTPS.")
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
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("GitHub a renvoyé une redirection sans destination.")
                }
                if (redirect == MAX_REDIRECTS) {
                    throw IllegalStateException("Le téléchargement dépasse le nombre de redirections autorisé.")
                }
                url = URL(url, location)
            } else {
                return connection
            }
        }
        throw IllegalStateException("Impossible de suivre la redirection du téléchargement.")
    }

    private companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_APK_SIZE_BYTES = 250L * 1024L * 1024L
    }
}
