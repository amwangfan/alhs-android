package com.ailihaosi

import com.ailihaosi.data.SiteUrls
import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.ui.components.formatWpDate
import com.ailihaosi.ui.components.stripHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlUtilsTest {
    @Test
    fun stripHtml_removesTags() {
        assertEquals("Hello world", stripHtml("<p>Hello <b>world</b></p>"))
    }

    @Test
    fun formatWpDate_takesDatePart() {
        assertEquals("2026-10-12", formatWpDate("2026-10-12T14:23:09"))
    }

    @Test
    fun interactive_detectedByCategory() {
        val post = AlhsPost(
            id = 1,
            title = "短占位标题",
            contentHtml = "<p>占位正文</p>",
            categoryIds = listOf(SiteUrls.INTERACTIVE_CATEGORY_ID)
        )
        assertTrue(post.isInteractive)
    }

    @Test
    fun interactive_notForNormalPost() {
        val post = AlhsPost(
            id = 2,
            title = "普通文章",
            contentHtml = "<p>短占位</p>",
            categoryIds = listOf(78)
        )
        assertFalse(post.isInteractive)
    }
}
