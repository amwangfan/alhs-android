package com.ailihaosi.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.ailihaosi.ui.browser.openInCustomTab

/**
 * Hands the APK URL to the system browser (its own download manager + installer prompt).
 * No REQUEST_INSTALL_PACKAGES / FileProvider needed. Falls back to Custom Tabs, then the
 * release page.
 */
fun openDownload(context: Context, release: ReleaseInfo) {
    val url = release.downloadUrl
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        Toast.makeText(context, "已在浏览器中开始下载 ${release.version}", Toast.LENGTH_SHORT).show()
    } catch (_: ActivityNotFoundException) {
        runCatching { openInCustomTab(context, url) }
            .onFailure { runCatching { openInCustomTab(context, release.htmlUrl) } }
    }
}
