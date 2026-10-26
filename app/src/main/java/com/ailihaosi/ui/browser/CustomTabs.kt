package com.ailihaosi.ui.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.toColorInt

fun openInCustomTab(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        val dark = CustomTabColorSchemeParams.Builder()
            .setToolbarColor("#121212".toColorInt())
            .build()
        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(dark)
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
    } catch (_: Exception) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
