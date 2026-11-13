package fr.nihongo.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {
    @Test
    fun parsesTagsWithOrWithoutPrefix() {
        assertEquals(ReleaseVersion(1, 2, 3, null), ReleaseVersion.parse("v1.2.3"))
        assertEquals(ReleaseVersion(1, 2, 3, null), ReleaseVersion.parse("1.2.3"))
    }

    @Test
    fun rejectsNonSemanticTags() {
        assertNull(ReleaseVersion.parse("latest"))
        assertNull(ReleaseVersion.parse("v1.2"))
    }

    @Test
    fun comparesStableAndPrereleaseVersions() {
        assertTrue(ReleaseVersion.parse("v1.2.0")!! > ReleaseVersion.parse("v1.1.9")!!)
        assertTrue(ReleaseVersion.parse("v1.2.0")!! > ReleaseVersion.parse("v1.2.0-rc.1")!!)
        assertTrue(ReleaseVersion.parse("v1.2.0-rc.2")!! > ReleaseVersion.parse("v1.2.0-rc.1")!!)
    }
}
