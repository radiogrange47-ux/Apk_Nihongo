package fr.nihongo.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GitHubReleaseClientTest {
    private val client = GitHubReleaseClient()

    @Test
    fun parsesOneSignedApkAssetFromTheProjectRelease() {
        val release = client.parseRelease(releaseJson(asset()))

        assertEquals("v0.1.1", release.tag)
        assertEquals("Nihongo 0.1.1", release.title)
        assertEquals("Corrections.", release.notes)
        assertEquals("app-release.apk", release.apk.name)
        assertEquals("a".repeat(64), release.apk.sha256)
    }

    @Test
    fun rejectsNonGitHubReleaseDownloadUrls() {
        val json = releaseJson(asset(url = "https://example.com/app.apk"))

        assertThrows(IllegalStateException::class.java) {
            client.parseRelease(json)
        }
    }

    @Test
    fun rejectsMissingOrMalformedReleaseDigests() {
        val missingDigest = releaseJson(asset(digest = ""))
        val malformedDigest = releaseJson(asset(digest = "sha256:1234"))

        assertThrows(IllegalStateException::class.java) {
            client.parseRelease(missingDigest)
        }
        assertThrows(IllegalStateException::class.java) {
            client.parseRelease(malformedDigest)
        }
    }

    @Test
    fun requiresExactlyOneApkPerRelease() {
        val twoApks = releaseJson(
            asset(),
            asset(name = "another.apk", url = "https://github.com/radiogrange47-ux/Apk_Nihongo/releases/download/v0.1.1/another.apk")
        )

        assertThrows(IllegalStateException::class.java) {
            client.parseRelease(twoApks)
        }
    }

    @Test
    fun applicationUpdateSearchIgnoresQuestionDataReleases() {
        val selected = client.latestApplicationRelease(
            JSONArray()
                .put(releaseJson(asset()))
                .put(JSONObject().put("tag_name", "questions-v9.0.0"))
                .put(releaseJson(asset()).put("tag_name", "v0.2.0"))
        )

        assertEquals("v0.2.0", selected?.getString("tag_name"))
    }

    private fun releaseJson(vararg assets: JSONObject) = JSONObject()
        .put("tag_name", "v0.1.1")
        .put("name", "Nihongo 0.1.1")
        .put("body", "  Corrections.  ")
        .put("draft", false)
        .put("prerelease", false)
        .put("assets", JSONArray().apply { assets.forEach { put(it) } })

    private fun asset(
        name: String = "app-release.apk",
        url: String = "https://github.com/radiogrange47-ux/Apk_Nihongo/releases/download/v0.1.1/app-release.apk",
        digest: String = "sha256:${"a".repeat(64)}"
    ) = JSONObject()
        .put("name", name)
        .put("browser_download_url", url)
        .put("digest", digest)
}
