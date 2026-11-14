package fr.nihongo.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestionReleaseClientTest {
    private val client = QuestionReleaseClient()

    @Test
    fun parsesAndSelectsTheNewestQuestionReleaseSeparatelyFromApkReleases() {
        val selected = client.latestQuestionRelease(
            JSONArray()
                .put(questionRelease("questions-v1.2.0"))
                .put(questionRelease("questions-v1.1.0"))
                .put(JSONObject().put("tag_name", "v4.0.0"))
        )
        val release = client.parseRelease(checkNotNull(selected))

        assertEquals("questions-v1.2.0", release.tag)
        assertEquals("1.2.0", release.contentVersion)
        assertEquals("questions.zip", release.asset.name)
    }

    @Test
    fun rejectsUntrustedUrlsAndMissingDigests() {
        assertThrows(IllegalArgumentException::class.java) {
            client.parseRelease(questionRelease(url = "https://example.com/questions.zip"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            client.parseRelease(questionRelease(digest = ""))
        }
    }

    private fun questionRelease(
        tag: String = "questions-v1.2.0",
        url: String = "https://github.com/radiogrange47-ux/Apk_Nihongo/releases/download/$tag/questions.zip",
        digest: String = "sha256:${"b".repeat(64)}"
    ) = JSONObject()
        .put("tag_name", tag)
        .put("name", "Questions v1.2.0")
        .put("draft", false)
        .put("prerelease", false)
        .put(
            "assets",
            JSONArray().put(
                JSONObject()
                    .put("name", "questions.zip")
                    .put("browser_download_url", url)
                    .put("digest", digest)
            )
        )
}
