package com.ailihaosi.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {
    private fun newer(remote: String, current: String) = VersionCompare.isNewer(remote, current)

    @Test fun patchBump() {
        assertTrue(newer("0.3.1", "0.3.0"))
        assertTrue(newer("v0.3.1", "0.3.0"))
        assertFalse(newer("0.3.0", "0.3.1"))
    }

    @Test fun vPrefixEqual() {
        assertEquals(0, VersionCompare.compare("0.3.1", "v0.3.1"))
        assertFalse(newer("v0.3.1", "0.3.1"))
        assertFalse(newer("0.3.1", "0.3.1"))
    }

    @Test fun numericNotLexical() {
        assertTrue(newer("0.3.10", "0.3.9"))
        assertFalse(newer("0.3.9", "0.3.10"))
    }

    @Test fun missingPartsAreZero() {
        assertTrue(newer("0.3", "0.2.9"))
        assertEquals(0, VersionCompare.compare("0.3", "0.3.0"))
        assertFalse(newer("0.3", "0.3.0"))
    }

    @Test fun majorBump() {
        assertTrue(newer("1.0.0", "0.9.9"))
        assertFalse(newer("0.9.9", "1.0.0"))
    }

    @Test fun preRelease() {
        // pre-release sorts below the plain release of same core
        assertTrue(newer("0.3.0", "0.3.0-beta"))
        assertFalse(newer("0.3.0-beta", "0.3.0"))
        // but above the previous release
        assertTrue(newer("0.3.0-beta", "0.2.9"))
        // core still dominates
        assertTrue(newer("0.3.1-beta", "0.3.0"))
        assertEquals(0, VersionCompare.compare("0.3.0-beta", "v0.3.0-beta"))
        assertTrue(newer("0.3.0-rc1", "0.3.0-beta"))
        // build metadata ignored
        assertEquals(0, VersionCompare.compare("0.3.1+5", "0.3.1"))
    }

    @Test fun garbageIsSafe() {
        assertFalse(newer("", "0.3.0"))
        assertFalse(newer("latest", "0.3.0"))
        assertEquals(0, VersionCompare.compare(" v0.3.1 ", "0.3.1"))
    }

    @Test fun parseRelease() {
        val json = """
            {"tag_name":"v0.3.1","name":"v0.3.1","draft":false,"prerelease":false,
             "html_url":"https://github.com/amwangfan/alhs-android/releases/tag/v0.3.1",
             "body":"## What's new\r\n- **Auto** update check via [GitHub](https://github.com)\r\n- `fix`\r\n\r\n---\r\nend",
             "assets":[{"name":"other.zip","browser_download_url":"https://github.com/x/other.zip"},
                       {"name":"alhs-slim.apk","browser_download_url":"https://github.com/amwangfan/alhs-android/releases/download/v0.3.1/alhs-slim.apk"}]}
        """.trimIndent()
        val r = ReleaseInfo.parse(json)
        assertNotNull(r); r!!
        assertEquals("v0.3.1", r.tagName)
        assertEquals("0.3.1", r.version)
        assertEquals("https://github.com/amwangfan/alhs-android/releases/download/v0.3.1/alhs-slim.apk", r.downloadUrl)
        assertEquals("What's new\n• Auto update check via GitHub\n• fix\nend", r.notesSummary)
    }

    @Test fun parseFallbacks() {
        val r = ReleaseInfo.parse("""{"tag_name":"v9.9.9","html_url":"https://github.com/amwangfan/alhs-android/releases/tag/v9.9.9","assets":[]}""")!!
        assertNull(r.apkUrl)
        assertEquals(r.htmlUrl, r.downloadUrl)
        assertNull(ReleaseInfo.parse("not json"))
        assertNull(ReleaseInfo.parse("""{"message":"API rate limit exceeded"}"""))
        assertNull(ReleaseInfo.parse("""{"tag_name":"v1.0.0","prerelease":true}"""))
    }

    @Test fun notesTrimmed() {
        val long = (1..50).joinToString("\n") { "- line $it with some text" }
        val s = ReleaseInfo.summarizeNotes(long)
        assertTrue(s.length <= 301)
        assertTrue(s.lines().size <= 12)
    }
}
