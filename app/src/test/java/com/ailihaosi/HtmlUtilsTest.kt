package com.ailihaosi

import com.ailihaosi.data.model.AlhsPost
import com.ailihaosi.data.model.PostDto
import com.ailihaosi.ui.components.formatWpDate
import com.ailihaosi.ui.components.stripHtml
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ailihaosi.data.model.ApiEnvelope
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
    fun interactive_detectedByCategoryName() {
        val post = AlhsPost(
            id = 1L,
            title = "短占位标题",
            contentHtml = "<p>占位正文</p>",
            categoryNames = listOf("互动小说")
        )
        assertTrue(post.isInteractive)
    }

    @Test
    fun interactive_notForNormalPost() {
        val post = AlhsPost(
            id = 2L,
            title = "普通文章",
            contentHtml = "<p>短占位</p>",
            categoryIds = listOf(78L),
            categoryNames = listOf("原创小说")
        )
        assertFalse(post.isInteractive)
    }

    @Test
    fun postId_parsesAsLong() {
        val json = """{"ok":true,"data":{"id":3134949415941,"title":"t","author":{"id":138689659007634,"display_name":"a"}}}"""
        val type = object : TypeToken<ApiEnvelope<PostDto>>() {}.type
        val env: ApiEnvelope<PostDto> = Gson().fromJson(json, type)
        assertEquals(3134949415941L, env.data!!.id)
        assertEquals(138689659007634L, env.data!!.author!!.id)
    }
}
