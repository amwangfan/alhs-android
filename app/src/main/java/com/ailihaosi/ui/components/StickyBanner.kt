package com.ailihaosi.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ailihaosi.data.model.AlhsPost

@Composable
fun StickyBanner(
    posts: List<AlhsPost>,
    onClick: (AlhsPost) -> Unit,
    modifier: Modifier = Modifier
) {
    if (posts.isEmpty()) return
    val first = posts.first()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(first) },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "精选 · ${stripHtml(first.title)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
