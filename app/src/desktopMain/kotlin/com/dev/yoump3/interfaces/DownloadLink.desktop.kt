package com.dev.yoump3.interfaces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
actual fun DownloadLink(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = text,
        color = if (enabled) Color(0xFF4FC3F7) else SecondaryText,
        textAlign = TextAlign.Center,
        textDecoration = if (enabled) TextDecoration.Underline else null,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        style = MaterialTheme.typography.titleLarge
    )
}
