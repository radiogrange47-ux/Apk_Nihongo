package fr.nihongo.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestionCatalogLoaderTest {
    @Test
    fun bundledQuestionFilesLoadAllFourGames() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = QuestionCatalogLoader(context).loadBundled()

        assertEquals("1.2.0", catalog.contentVersion)
        assertEquals(22, catalog.particles.count { it.bank == QuestionBank.JLPT5 })
        assertEquals(67, catalog.vocabulary.count { it.bank == QuestionBank.JLPT5 })
        assertEquals(65, catalog.verbs.count { it.bank == QuestionBank.JLPT5 })
        assertEquals(22, catalog.conjugation.count { it.bank == QuestionBank.JLPT5 })
        assertEquals(14, catalog.particles.count { it.bank == QuestionBank.COURSE })
        assertEquals(81, catalog.vocabulary.count { it.bank == QuestionBank.COURSE })
        assertEquals(24, catalog.verbs.count { it.bank == QuestionBank.COURSE })
        assertEquals(14, catalog.conjugation.count { it.bank == QuestionBank.COURSE })
        assertTrue(catalog.particles.all { it.choices.size == 6 })
        assertTrue(catalog.vocabulary.all { it.choices.size == 6 })
        assertTrue(catalog.verbs.all { it.choices.size == 6 })
        assertTrue(catalog.conjugation.all { it.choices.size == 6 })
        assertEquals(309, catalog.allQuestionIds().values.flatten().size)
        assertTrue(
            LearningGame.values().all { game ->
                catalog.questionsFor(game).all {
                    it.bank == QuestionBank.COURSE ||
                        (it.level == "JLPT N5" && !it.source.isNullOrBlank())
                }
            }
        )
        assertFalse(catalog.allQuestionIds().values.any { it.isEmpty() })
    }

    @Test
    fun archiveLoadsOnlyWhenItContainsFilesListedInTheManifest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = QuestionCatalogLoader(context)
        val archive = File.createTempFile("questions-test", ".zip", context.cacheDir)
        try {
            val manifest = context.assets.open("manifest.json").bufferedReader().use { it.readText() }
            val files = org.json.JSONObject(manifest).getJSONArray("files").let { entries ->
                (0 until entries.length()).map { entries.getString(it) }
            }
            ZipOutputStream(archive.outputStream()).use { zip ->
                (files + "manifest.json").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    context.assets.open(name).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            assertEquals("1.2.0", loader.loadArchive(archive).contentVersion)

            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("../unexpected.json"))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
            assertThrows(Exception::class.java) {
                loader.loadArchive(archive)
            }
        } finally {
            archive.delete()
        }
    }
}
