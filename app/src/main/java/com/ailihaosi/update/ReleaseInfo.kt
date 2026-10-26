package com.ailihaosi.update

import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class ReleaseInfo(
    val tagName: String,
    val version: String,
    val name: String,
    val notesSummary: String,
    val htmlUrl: String,
    /** browser_download_url of alhs-slim.apk (or first .apk), null if none. */
    val apkUrl: String?
) {
    /** Where 「下载更新」 sends the user. */
    val downloadUrl: String get() = apkUrl ?: htmlUrl

    companion object {
        const val APK_ASSET_NAME = "alhs-slim.apk"
        const val FALLBACK_HTML = "https://github.com/amwangfan/alhs-android/releases/latest"

        /** Parses a GitHub `releases/latest` JSON body. Tree parsing → R8-safe, no reflection. */
        fun parse(json: String): ReleaseInfo? {
            val root = runCatching { JsonParser.parseString(json) }.getOrNull()
                ?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            val tag = root.str("tag_name")?.trim().orEmpty()
            if (tag.isEmpty()) return null
            if (root.bool("draft") == true || root.bool("prerelease") == true) return null
            val assets = root.get("assets")?.takeIf { it.isJsonArray }?.asJsonArray
                ?.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
                .orEmpty()
            val apk = assets.firstOrNull { it.str("name") == APK_ASSET_NAME }
                ?: assets.firstOrNull { it.str("name")?.endsWith(".apk", true) == true }
            val html = root.str("html_url")
                ?.takeIf { it.startsWith("https://github.com/") } ?: FALLBACK_HTML
            val apkUrl = apk?.str("browser_download_url")
                ?.takeIf { it.startsWith("https://github.com/") }
            return ReleaseInfo(
                tagName = tag,
                version = VersionCompare.normalize(tag),
                name = root.str("name").orEmpty(),
                notesSummary = summarizeNotes(root.str("body").orEmpty()),
                htmlUrl = html,
                apkUrl = apkUrl
            )
        }

        private fun JsonObject.str(key: String): String? =
            get(key)?.takeIf { it.isJsonPrimitive }?.asString

        private fun JsonObject.bool(key: String): Boolean? =
            get(key)?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asBoolean }.getOrNull() }

        /** Light markdown cleanup, first ~12 non-empty lines, max ~300 chars. */
        fun summarizeNotes(body: String, maxChars: Int = 300, maxLines: Int = 12): String {
            val lines = body.replace("\r\n", "\n").lines()
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("```") && !it.matches(Regex("^[-*_=]{3,}$")) }
                .map { line ->
                    line
                        .replace(Regex("^#{1,6}\\s*"), "")              // headings
                        .replace(Regex("^[-*+]\\s+"), "• ")              // bullets
                        .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")   // images
                        .replace(Regex("\\[([^\\]]+)]\\([^)]*\\)"), "$1") // links → text
                        .replace(Regex("(\\*\\*|__|`)"), "")             // bold / code ticks
                        .replace(Regex("^>\\s*"), "")                    // quotes
                        .trim()
                }
                .filter { it.isNotEmpty() }
                .take(maxLines)
                .toList()
            val joined = lines.joinToString("\n")
            return if (joined.length <= maxChars) joined
            else joined.take(maxChars).trimEnd() + "…"
        }
    }
}
