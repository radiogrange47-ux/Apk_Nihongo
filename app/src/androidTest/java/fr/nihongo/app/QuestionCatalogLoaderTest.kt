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

        assertEquals("1.1.0", catalog.contentVersion)
        assertEquals(22, catalog.particles.size)
        assertEquals(67, catalog.vocabulary.size)
        assertEquals(65, catalog.verbs.size)
        assertEquals(22, catalog.conjugation.size)
        assertTrue(catalog.particles.all { it.choices.size == 6 })
        assertTrue(catalog.vocabulary.all { it.choices.size == 6 })
        assertTrue(catalog.verbs.all { it.choices.size == 6 })
        assertTrue(catalog.conjugation.all { it.choices.size == 6 })
        assertEquals(176, catalog.allQuestionIds().values.flatten().size)
        assertTrue(
            LearningGame.values().all { game ->
                catalog.questionsFor(game).all {
                    it.level == "JLPT N5" && !it.source.isNullOrBlank()
                }
            }
        )
        assertFalse(catalog.allQuestionIds().values.any { it.isEmpty() })
    }

    @Test
    fun archiveLoadsOnlyWhenItContainsTheExpectedFlatFileSet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = QuestionCatalogLoader(context)
        val archive = File.createTempFile("questions-test", ".zip", context.cacheDir)
        try {
            ZipOutputStream(archive.outputStream()).use { zip ->
                (QuestionCatalogCodec.fileNames + "manifest.json").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    context.assets.open(name).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            assertEquals("1.1.0", loader.loadArchive(archive).contentVersion)

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
