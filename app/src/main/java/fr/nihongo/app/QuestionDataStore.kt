package fr.nihongo.app

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

internal class QuestionCatalogLoader(private val context: Context) {
    fun loadBundled(): QuestionCatalog {
        val manifest = context.assets.open(MANIFEST_FILE).use {
            it.readBounded(MAX_JSON_BYTES).toString(Charsets.UTF_8)
        }
        val names = org.json.JSONObject(manifest).getJSONArray("files").let { files ->
            (0 until files.length()).map { files.getString(it) }
        }
        val dataFiles = names.associateWith { name ->
            context.assets.open(name).use { it.readBounded(MAX_JSON_BYTES).toString(Charsets.UTF_8) }
        }
        return QuestionCatalogCodec.parse(manifest, dataFiles)
    }

    fun loadArchive(archive: File): QuestionCatalog {
        require(archive.isFile && archive.length() <= MAX_ARCHIVE_BYTES) {
            "Le paquet de questions est absent ou dépasse la taille maximale."
        }
        val entries = linkedMapOf<String, String>()
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory &&
                    (entry.name == MANIFEST_FILE || QuestionCatalogCodec.isValidFileName(entry.name))) {
                    "Le paquet contient un fichier inattendu."
                }
                require(entry.name !in entries) { "Le paquet contient un fichier en double." }
                entries[entry.name] = zip.readBounded(MAX_JSON_BYTES).toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        require(MANIFEST_FILE in entries) {
            "Le paquet est incomplet ou contient des fichiers inconnus."
        }
        return QuestionCatalogCodec.parse(
            manifestText = entries.getValue(MANIFEST_FILE),
            dataFiles = entries.filterKeys { it != MANIFEST_FILE }
        )
    }

    private companion object {
        const val MANIFEST_FILE = "manifest.json"
        const val MAX_JSON_BYTES = 2L * 1024L * 1024L
        const val MAX_ARCHIVE_BYTES = 20L * 1024L * 1024L
    }
}

internal class QuestionDataStore(
    context: Context,
    private val progressStore: ProgressStore
) {
    private val loader = QuestionCatalogLoader(context)
    private val directory = File(context.filesDir, "question-content")
    private val currentArchive = File(directory, "current.zip")
    private val backupArchive = File(directory, "previous.zip")

    fun loadOrSeed(): QuestionCatalog {
        if (currentArchive.exists()) {
            return try {
                preferBundledWhenNewer(loader.loadArchive(currentArchive))
                    .also(progressStore::synchronizeQuestions)
            } catch (currentFailure: Exception) {
                if (!backupArchive.isFile) {
                    val bundledCatalog = try {
                        loader.loadBundled()
                    } catch (bundledFailure: Exception) {
                        bundledFailure.addSuppressed(currentFailure)
                        throw bundledFailure
                    }
                    currentArchive.delete()
                    return bundledCatalog.also(progressStore::synchronizeQuestions)
                }
                val backup = loader.loadArchive(backupArchive)
                if (!replaceFile(backupArchive, currentArchive)) {
                    throw IllegalStateException(
                        "Les données installées sont endommagées et la sauvegarde n’a pas pu être restaurée.",
                        currentFailure
                    )
                }
                preferBundledWhenNewer(backup).also(progressStore::synchronizeQuestions)
            }
        }

        if (backupArchive.isFile) {
            val backup = loader.loadArchive(backupArchive)
            if (!replaceFile(backupArchive, currentArchive)) {
                throw IllegalStateException("Impossible de restaurer la sauvegarde des questions.")
            }
            return preferBundledWhenNewer(backup).also(progressStore::synchronizeQuestions)
        }

        return loader.loadBundled().also(progressStore::synchronizeQuestions)
    }

    private fun preferBundledWhenNewer(installedCatalog: QuestionCatalog): QuestionCatalog {
        val bundledCatalog = loader.loadBundled()
        val installedVersion = requireNotNull(ReleaseVersion.parse(installedCatalog.contentVersion))
        val bundledVersion = requireNotNull(ReleaseVersion.parse(bundledCatalog.contentVersion))
        return if (bundledVersion > installedVersion) bundledCatalog else installedCatalog
    }

    fun installArchive(archive: File, expectedVersion: String): QuestionCatalog {
        val catalog = loader.loadArchive(archive)
        require(catalog.contentVersion == expectedVersion) {
            "La version du paquet ne correspond pas à celle annoncée par GitHub."
        }
        val installedCatalog = if (currentArchive.isFile) {
            loader.loadArchive(currentArchive)
        } else {
            loader.loadBundled()
        }
        val downloadedVersion = requireNotNull(ReleaseVersion.parse(catalog.contentVersion))
        val installedVersion = requireNotNull(ReleaseVersion.parse(installedCatalog.contentVersion))
        require(
            downloadedVersion > installedVersion
        ) {
            "Le paquet téléchargé n’est pas plus récent que les questions installées."
        }
        directory.mkdirs().also { created ->
            check(created || directory.isDirectory) {
                "Impossible de préparer le stockage des questions."
            }
        }
        val stagedArchive = File(directory, "installing.zip")
        archive.copyTo(stagedArchive, overwrite = true)
        loader.loadArchive(stagedArchive)

        val hadCurrentArchive = currentArchive.isFile
        if (backupArchive.exists() && !backupArchive.delete()) {
            stagedArchive.delete()
            throw IllegalStateException("Impossible de remplacer l’ancienne sauvegarde des questions.")
        }
        if (hadCurrentArchive && !currentArchive.renameTo(backupArchive)) {
            stagedArchive.delete()
            throw IllegalStateException("Impossible de préparer le remplacement des questions.")
        }
        if (!stagedArchive.renameTo(currentArchive)) {
            if (hadCurrentArchive && !backupArchive.renameTo(currentArchive)) {
                throw IllegalStateException(
                    "Impossible d’activer les nouvelles questions ou de restaurer la version précédente."
                )
            }
            stagedArchive.delete()
            throw IllegalStateException("Impossible d’activer les nouvelles questions.")
        }

        try {
            progressStore.synchronizeQuestions(catalog)
        } catch (exception: Exception) {
            if (currentArchive.exists() && !currentArchive.delete()) {
                exception.addSuppressed(
                    IllegalStateException("Impossible de retirer le paquet de questions non activé.")
                )
            } else if (hadCurrentArchive && !backupArchive.renameTo(currentArchive)) {
                exception.addSuppressed(
                    IllegalStateException("Impossible de restaurer la version précédente des questions.")
                )
            }
            throw exception
        }
        if (backupArchive.exists()) backupArchive.delete()
        return catalog
    }

    private fun replaceFile(source: File, destination: File): Boolean {
        if (destination.exists() && !destination.delete()) return false
        return source.renameTo(destination)
    }

}

private fun InputStream.readBounded(limit: Long): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= limit) { "Un fichier du paquet dépasse la taille maximale autorisée." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
