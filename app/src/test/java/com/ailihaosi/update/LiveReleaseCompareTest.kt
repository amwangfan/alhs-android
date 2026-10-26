package com.ailihaosi.update

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Hits the real GitHub API. Opt-in: ALHS_LIVE_UPDATE_TEST=1 ./gradlew :app:testReleaseUnitTest */
class LiveReleaseCompareTest {
    @Test fun liveLatest() {
        assumeTrue(System.getenv("ALHS_LIVE_UPDATE_TEST") == "1")
        val body = OkHttpClient().newCall(
            Request.Builder().url(UpdateChecker.LATEST_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AlhsReader/test (JVM; com.ailihaosi)")
                .build()
        ).execute().use { it.body!!.string() }
        val r = ReleaseInfo.parse(body)
        assertNotNull(body, r); r!!
        val expect = System.getenv("ALHS_EXPECT_TAG")
        println("LIVE latest tag=${r.tagName} version=${r.version} apk=${r.apkUrl}")
        println("LIVE notes=${r.notesSummary}")
        if (expect != null) assertTrue("tag ${r.tagName} != $expect", r.tagName == expect)
        println("LIVE isNewer(${r.version}, 0.3.0)=${VersionCompare.isNewer(r.version, "0.3.0")}")
        println("LIVE isNewer(${r.version}, 0.3.1)=${VersionCompare.isNewer(r.version, "0.3.1")}")
        if (expect == "v0.3.1") {
            assertTrue(VersionCompare.isNewer(r.version, "0.3.0"))
            assertFalse(VersionCompare.isNewer(r.version, "0.3.1"))
        }
    }
}
