package com.dev.yoump3.interfaces

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun DownloadLink(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
