package com.dev.yoump3.interfaces

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.dev.yoump3.generated.resources.Res
import com.dev.yoump3.generated.resources.retry
import com.dev.yoump3.viewModels.SearchResultUi
import com.dev.yoump3.viewModels.SongInputViewModel
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.painterResource

@Composable
fun SongInputScreenContent(
    viewModel: SongInputViewModel,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state
    val scrollState = rememberScrollState()
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

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    hideKeyboard()
                }
            }
            .padding(horizontal = 28.dp, vertical = 34.dp)
    ) {
        BackButton(
            onClick = {
                when {
                    state.isExtracting -> {
                        viewModel.onCancelExtraction()
                        if (state.searchResults.isNotEmpty()) {
                            viewModel.onReturnToResults()
                        } else {
                            viewModel.onReturnToInput()
                        }
                    }
                    state.resultTitle != null && state.searchResults.isNotEmpty() -> {
                        viewModel.onReturnToResults()
                    }
                    else -> onCloseClick()
                }
            },
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
                    .verticalScroll(scrollState)
                    .padding(top = 54.dp)
            ) {
                val isExtractionView = state.isExtracting || state.isExtractionFailed
                val resultsVisible = !isExtractionView && !state.isLoading

                LaunchedEffect(scrollState.value) {
                    if (resultsVisible) {
                        viewModel.resultsScrollOffset = scrollState.value
                    }
                }

                LaunchedEffect(resultsVisible) {
                    if (resultsVisible) {
                        scrollState.scrollTo(viewModel.resultsScrollOffset)
                    }
                }

                if (!isExtractionView) {
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
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(40.dp))
                        SearchActionButton(
                            onClick = submitSearch,
                            enabled = !state.isLoading && state.songQuery.isNotBlank()
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !isExtractionView && state.isLoading,
                    enter = fadeIn(tween(240)),
                    exit = fadeOut(tween(180)),
                    label = "search-loading-status"
                ) {
                    SearchStatusView(
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                AnimatedVisibility(
                    visible = !isExtractionView && !state.isLoading,
                    enter = fadeIn(tween(260)) + expandVertically(animationSpec = tween(260)),
                    exit = fadeOut(tween(220)) + shrinkVertically(animationSpec = tween(220)),
                    label = "results-and-extra-result-area"
                ) {
                    Column {
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
                            if (state.resultTitle == null) {
                                ResponseText(
                                    text = message,
                                    color = DestructiveText
                                )
                                Spacer(Modifier.height(18.dp))
                                StatusActionButton(
                                    text = "REINTENTAR BÚSQUEDA",
                                    filled = true,
                                    iconPainter = painterResource(Res.drawable.retry),
                                    onClick = viewModel::onRetrySearch,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        state.resultTitle?.let { title ->
                            Spacer(Modifier.height(16.dp))
                            ExtractionResultCard(
                                title = title,
                                format = state.resultFormat ?: "MP3",
                                sizeBytes = state.resultSizeBytes,
                                isDownloading = state.isDownloading,
                                downloadStatus = state.downloadStatus,
                                isDownloadFailed = state.isDownloadFailed,
                                errorMessage = if (state.isDownloadFailed) state.errorMessage else null,
                                onDownloadClick = viewModel::onDownloadClick
                            )
                            if (state.resultAudioBase64 != null) {
                                Spacer(Modifier.height(12.dp))
                                MiniPlayer(
                                    audioPlayer = viewModel.audioPlayer,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (isExtractionView) {
                    ExtractionStatusView(
                        isFailed = state.isExtractionFailed,
                        title = state.selectedTitle ?: state.resultTitle,
                        message = state.errorMessage,
                        onReturnToInput = {
                            if (state.searchResults.isNotEmpty()) {
                                viewModel.onReturnToResults()
                            } else {
                                viewModel.onReturnToInput()
                            }
                        },
                        onRetryDownload = null,
                        onRetrySearch = viewModel::onRetrySearch
                    )
                }
        }

            AppFooter(footerText = state.footer)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) lerp(PanelBackground, PrimaryText, 0.08f) else PanelBackground,
        animationSpec = tween(durationMillis = 120),
        label = "result-item-background"
    )
    val itemBorderColor by animateColorAsState(
        targetValue = if (isPressed) lerp(BorderColor, PrimaryText, 0.3f) else BorderColor,
        animationSpec = tween(durationMillis = 120),
        label = "result-item-border"
    )

    val meta = listOfNotNull(
        result.author,
        result.durationSeconds?.let { formatDuration(it) }
    ).joinToString("  ·  ")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .border(1.dp, itemBorderColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
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
private fun ExtractionResultCard(
    title: String,
    format: String,
    sizeBytes: Long? = null,
    isDownloading: Boolean,
    downloadStatus: String?,
    isDownloadFailed: Boolean,
    errorMessage: String?,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PanelBackground, RoundedCornerShape(10.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 20.dp, vertical = 26.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = PrimaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Formato: $format",
                color = SecondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium
            )

            if (sizeBytes != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Tamaño: ${formatFileSize(sizeBytes)}",
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))

            DownloadLink(
                text = if (isDownloading) "Descargando..." else "Descargar",
                enabled = !isDownloading,
                onClick = onDownloadClick
            )

            AnimatedVisibility(
                visible = isDownloadFailed && errorMessage != null,
                enter = fadeIn(tween(260)) + expandVertically(animationSpec = tween(260)),
                exit = fadeOut(tween(220)) + shrinkVertically(animationSpec = tween(220)),
                label = "download-error-retry"
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = DestructiveText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    StatusActionButton(
                        text = "REINTENTAR DESCARGA",
                        filled = true,
                        iconPainter = painterResource(Res.drawable.retry),
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            downloadStatus?.let { status ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = status,
                    color = PrimaryText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes * 100.0 / (1024.0 * 1024.0)
    val hundredths = (mb * 100.0).roundToLong()
    val whole = hundredths / 100
    val frac = hundredths % 100
    return "$whole,${frac.toString().padStart(2, '0')} MB"
}

@Composable
private fun SongInputBox(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    searchEnabled: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = { newText ->
            val sanitized = newText.filter { char ->
                char.isLetter() || char.isDigit() || char == ' ' || char == '-' || char == ','
            }
            onValueChange(sanitized)
        },
        singleLine = true,
        enabled = enabled,
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
                    if (value.isNotEmpty()) {
                        Icon(
                            imageVector = ClearIcon,
                            contentDescription = "Borrar",
                            tint = PrimaryText,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(enabled = enabled) {
                                    onValueChange("")
                                }
                        )
                    } else {
                        Icon(
                            imageVector = SearchIcon,
                            contentDescription = "Search",
                            tint = if (searchEnabled) PrimaryText else SecondaryText,
                            modifier = Modifier
                                .size(26.dp)
                                .clickable(enabled = enabled && searchEnabled, onClick = onSearchClick)
                        )
                    }
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed && enabled) lerp(AppBackground, PrimaryText, 0.25f) else AppBackground,
        animationSpec = tween(durationMillis = 120),
        label = "search-button-background"
    )
    val textColor = if (enabled) PrimaryText else SecondaryText
    val borderColor = if (enabled) PrimaryText else BorderColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
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

private val ClearIcon = ImageVector.Builder(
    name = "Clear",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        close()
    }
}.build()

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
