package com.ailihaosi.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ailihaosi.BuildConfig

@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onIgnore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("发现新版本 ${release.version}") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "当前版本 ${BuildConfig.VERSION_NAME} → ${release.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (release.notesSummary.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(release.notesSummary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) { Text("下载更新") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onIgnore) { Text("忽略此版本") }
                TextButton(onClick = onLater) { Text("稍后") }
            }
        }
    )
}
