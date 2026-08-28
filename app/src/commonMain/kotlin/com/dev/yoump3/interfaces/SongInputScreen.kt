package com.dev.yoump3.interfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.yoump3.viewModels.SearchResultUi
import com.dev.yoump3.viewModels.SongInputViewModel

@Composable
fun SongInputScreenContent(
    viewModel: SongInputViewModel,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hideKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    val submitSearch = {
        hideKeyboard()
        if (!state.isLoading && state.songQuery.isNotBlank()) {
            viewModel.onSearchClick()
        }
    }

    Box(modifier = modifier.padding(horizontal = 28.dp, vertical = 34.dp)) {
        BackButton(
            onClick = onCloseClick,
            modifier = Modifier.align(Alignment.TopStart)
        )

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = state.appTitle,
                color = PrimaryText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 54.dp)
            ) {
                when {
                    state.isExtracting || state.isDownloading ||
                        state.isDownloadFailed || state.isExtractionFailed -> {
                        ExtractionStatusView(
                            isFailed = state.isDownloadFailed || state.isExtractionFailed,
                            title = state.selectedTitle ?: state.resultTitle,
                            message = state.errorMessage,
                            onReturnToInput = viewModel::onReturnToInput,
                            onRetryDownload = if (state.isDownloadFailed) {
                                viewModel::onDownloadClick
                            } else {
                                null
                            }
                        )
                    }

                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp)
                        ) {
                            SongInputBox(
                                value = state.songQuery,
                                placeholder = state.placeholder,
                                onValueChange = viewModel::onSongQueryChange,
                                onSearchClick = submitSearch,
                                searchEnabled = !state.isLoading && state.songQuery.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(40.dp))
                            SearchActionButton(
                                onClick = submitSearch,
                                enabled = !state.isLoading && state.songQuery.isNotBlank()
                            )
                        }

                        if (state.isLoading) {
                            SearchStatusView(
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                        if (state.searchResults.isNotEmpty() && state.resultTitle == null) {
                            ResponseText(
                                text = "MEJORES RESULTADOS",
                                color = PrimaryText
                            )
                            Spacer(Modifier.height(8.dp))
                            state.searchResults.forEach { result ->
                                SearchResultItem(
                                    result = result,
                                    onClick = { viewModel.onSelectResult(result.videoId, result.title) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }

                        state.errorMessage?.let { message ->
                            ResponseText(
                                text = message,
                                color = Color(0xFFFF5252)
                            )
                        }

                        state.resultTitle?.let { title ->
                            ResponseText(text = title)
                            ResponseText(text = "Formato: ${state.resultFormat ?: "MP3"}")

                            DownloadLink(
                                text = if (state.isDownloading) "Descargando..." else "Descargar",
                                enabled = !state.isDownloading,
                                onClick = viewModel::onDownloadClick
                            )

                            state.downloadStatus?.let { status ->
                                ResponseText(
                                    text = status,
                                    color = PrimaryText
                                )
                            }
                        }
                    }
                }
            }
        }

            Text(
                text = state.footer,
                color = SecondaryText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ResponseText(
    text: String,
    color: Color = SecondaryText
) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = text,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun SearchResultItem(
    result: SearchResultUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = listOfNotNull(
        result.author,
        result.durationSeconds?.let { formatDuration(it) }
    ).joinToString("  ·  ")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PanelBackground)
            .border(1.dp, BorderColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = result.title,
            color = PrimaryText,
            style = MaterialTheme.typography.bodyMedium
        )
        if (meta.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = meta,
                color = SecondaryText,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val secondsText = if (seconds < 10) "0$seconds" else "$seconds"
    return "$minutes:$secondsText"
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "<",
            color = PrimaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun SongInputBox(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    searchEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(PrimaryText),
        textStyle = MaterialTheme.typography.titleLarge.merge(
            TextStyle(color = PrimaryText)
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSearchClick() }
        ),
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(PanelBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 22.dp),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = SecondaryText,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        innerTextField()
                    }
                    Spacer(Modifier.size(14.dp))
                    Icon(
                        imageVector = SearchIcon,
                        contentDescription = "Search",
                        tint = if (searchEnabled) PrimaryText else SecondaryText,
                        modifier = Modifier
                            .size(26.dp)
                            .clickable(onClick = onSearchClick)
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchActionButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val textColor = if (enabled) PrimaryText else SecondaryText
    val borderColor = if (enabled) PrimaryText else BorderColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(AppBackground)
            .border(2.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "search",
            color = textColor,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

private val SearchIcon = ImageVector.Builder(
    name = "Search",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(9.5f, 3f)
        curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
        reflectiveCurveTo(5.91f, 16f, 9.5f, 16f)
        curveToRelative(1.61f, 0f, 3.09f, -0.59f, 4.22f, -1.56f)
        lineToRelative(4.39f, 4.39f)
        lineToRelative(1.41f, -1.41f)
        lineToRelative(-4.39f, -4.39f)
        curveTo(16.1f, 11.9f, 16.5f, 10.75f, 16.5f, 9.5f)
        curveTo(16.5f, 5.91f, 13.59f, 3f, 9.5f, 3f)
        close()
        moveTo(9.5f, 5f)
        curveTo(11.98f, 5f, 14f, 7.02f, 14f, 9.5f)
        reflectiveCurveTo(11.98f, 14f, 9.5f, 14f)
        reflectiveCurveTo(5f, 11.98f, 5f, 9.5f)
        reflectiveCurveTo(7.02f, 5f, 9.5f, 5f)
        close()
    }
}.build()
