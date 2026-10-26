package com.ailihaosi.ui.components

import android.text.Html
import org.jsoup.Jsoup

fun stripHtml(html: String?): String {
    if (html.isNullOrBlank()) return ""
    return try {
        Jsoup.parse(html).text().trim()
    } catch (_: Exception) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }
}

fun formatWpDate(raw: String): String {
    // "2026-10-12T14:23:09" -> "2026-10-12"
    return raw.take(10).ifBlank { raw }
}

/** Dark-theme CSS wrapper for article HTML shown in WebView. */
fun wrapArticleHtml(bodyHtml: String, title: String): String {
    val safeTitle = title
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=3"/>
          <style>
            :root { color-scheme: dark; }
            body {
              margin: 0;
              padding: 16px;
              background: #121212;
              color: #E0E0E0;
              font-size: 17px;
              line-height: 1.75;
              font-family: sans-serif;
              word-wrap: break-word;
            }
            h1 { font-size: 1.35em; line-height: 1.35; color: #F5F5F5; margin: 0 0 12px; }
            a { color: #BB86FC; }
            img, video { max-width: 100%; height: auto; }
            pre, code { white-space: pre-wrap; background: #1E1E1E; padding: 4px; border-radius: 4px; }
            blockquote { border-left: 3px solid #BB86FC; margin: 12px 0; padding-left: 12px; color: #B0B0B0; }
            p { margin: 0.6em 0; }
          </style>
        </head>
        <body>
          <h1>$safeTitle</h1>
          $bodyHtml
        </body>
        </html>
    """.trimIndent()
}
