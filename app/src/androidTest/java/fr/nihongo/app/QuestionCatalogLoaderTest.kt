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

        assertEquals("1.5.3", catalog.contentVersion)
        assertEquals(22, catalog.particles.count { it.bank == QuestionBank.JLPT5 })
        assertEquals(108, catalog.vocabulary.count { it.bank == QuestionBank.JLPT5 })
        assertEquals(65, catalog.verbs.count { it.bank == QuestionBank.JLPT5 })
        assertEquals(22, catalog.conjugation.count { it.bank == QuestionBank.JLPT5 })
        assertEquals(28, catalog.particles.count { it.bank == QuestionBank.JLPT4 })
        assertEquals(112, catalog.vocabulary.count { it.bank == QuestionBank.JLPT4 })
        assertEquals(56, catalog.verbs.count { it.bank == QuestionBank.JLPT4 })
        assertEquals(50, catalog.conjugation.count { it.bank == QuestionBank.JLPT4 })
        assertEquals(125, catalog.vocabulary.count { it.bank == QuestionBank.FOOD })
        assertEquals(36, catalog.verbs.count { it.bank == QuestionBank.FOOD })
        assertEquals(122, catalog.vocabulary.count { it.bank == QuestionBank.COUNTERS })
        assertEquals(24, catalog.vocabulary.count { it.bank == QuestionBank.CONNECTORS })
        assertEquals(24, catalog.vocabulary.count { it.bank == QuestionBank.DEMONSTRATIVES })
        assertEquals(25, catalog.vocabulary.count { it.bank == QuestionBank.INTERROGATIVES })
        assertEquals(25, catalog.vocabulary.count { it.bank == QuestionBank.DETERMINERS })
        assertEquals(24, catalog.particles.count { it.bank == QuestionBank.COURSE })
        assertEquals(148, catalog.vocabulary.count { it.bank == QuestionBank.COURSE })
        assertEquals(41, catalog.verbs.count { it.bank == QuestionBank.COURSE })
        assertEquals(25, catalog.conjugation.count { it.bank == QuestionBank.COURSE })
        assertTrue(catalog.particles.all { it.choices.size == 6 })
        assertTrue(catalog.vocabulary.all { it.choices.size == 6 })
        assertTrue(catalog.verbs.all { it.choices.size == 6 })
        assertTrue(catalog.conjugation.all { it.choices.size == 6 })
        assertEquals(1082, catalog.allQuestionIds().values.flatten().size)
        assertTrue(
            LearningGame.values().all { game ->
                catalog.questionsFor(game).all {
                    it.bank == QuestionBank.COURSE ||
                        (it.level == "JLPT N5" && !it.source.isNullOrBlank()) ||
                        (it.level == "JLPT N4" && !it.source.isNullOrBlank()) ||
                        (it.bank in setOf(
                            QuestionBank.FOOD,
                            QuestionBank.COUNTERS,
                            QuestionBank.CONNECTORS,
                            QuestionBank.DEMONSTRATIVES,
                            QuestionBank.INTERROGATIVES,
                            QuestionBank.DETERMINERS
                        ) &&
                            !it.source.isNullOrBlank())
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
            assertEquals("1.5.3", loader.loadArchive(archive).contentVersion)

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
