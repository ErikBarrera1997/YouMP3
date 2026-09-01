package com.dev.yoump3.interfaces

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dev.yoump3.generated.resources.Res
import com.dev.yoump3.generated.resources.icon
import com.dev.yoump3.init.InitScreen
import com.dev.yoump3.viewModels.ErrorStatusViewModel
import com.dev.yoump3.viewModels.SongInputViewModel
import com.dev.yoump3.viewModels.YouMp3Screen
import com.dev.yoump3.viewModels.YouMp3ViewModel
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

@Composable
fun YouMp3App(viewModel: YouMp3ViewModel = remember { YouMp3ViewModel() }, onExit: () -> Unit = {}) {
    val songInputViewModel = remember { SongInputViewModel() }
    val errorStatusViewModel = remember { ErrorStatusViewModel() }
    var appReady by remember { mutableStateOf(false) }
    var connectionFailed by remember { mutableStateOf(false) }
    val themeColors = viewModel.settingsViewModel.currentColors

    CompositionLocalProvider(LocalAppColors provides themeColors) {
        YouMp3Theme {
            when {
                !appReady && !connectionFailed -> InitScreen(
                    onConnected = { appReady = true },
                    onError = { connectionFailed = true }
                )
                connectionFailed -> ErrorStatusScreen(
                    viewModel = errorStatusViewModel,
                    onGoBackToHome = { connectionFailed = false }
                )
                else -> YouMp3Screen(
                    viewModel = viewModel,
                    songInputViewModel = songInputViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun YouMp3Theme(content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    MaterialTheme(
        colorScheme = if (colors.isDark) {
            androidx.compose.material3.darkColorScheme(
                primary = colors.primaryText,
                onPrimary = colors.appBackground,
                background = colors.appBackground,
                onBackground = colors.primaryText,
                surface = colors.panelBackground,
                onSurface = colors.primaryText,
                outline = colors.borderColor
            )
        } else {
            androidx.compose.material3.lightColorScheme(
                primary = colors.primaryText,
                onPrimary = colors.appBackground,
                background = colors.appBackground,
                onBackground = colors.primaryText,
                surface = colors.panelBackground,
                onSurface = colors.primaryText,
                outline = colors.borderColor
            )
        },
        typography = MaterialTheme.typography.copy(
            displayLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                lineHeight = 48.sp,
                letterSpacing = 0.sp
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                letterSpacing = 0.sp
            ),
            labelMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp
            )
        ),
        content = content
    )
}

@Composable
fun YouMp3Screen(
    viewModel: YouMp3ViewModel,
    songInputViewModel: SongInputViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state

    Surface(
        color = AppBackground,
        contentColor = PrimaryText,
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = state.currentScreen,
            transitionSpec = {
                val forward = targetState != YouMp3Screen.Home
                val enterOffset = if (forward) 0.25f else -0.25f
                val exitOffset = if (forward) -0.25f else 0.25f
                (fadeIn(tween(260)) + slideInHorizontally(tween(260)) { (it * enterOffset).roundToInt() }) togetherWith
                    (fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { (it * exitOffset).roundToInt() })
            },
            label = "main-screens"
        ) { screen ->
            when (screen) {
                YouMp3Screen.Home -> FindItScreenContent(
                    appTitle = state.appTitle,
                    title = state.title,
                    footer = state.footer,
                    onFindClick = viewModel::onFindButtonClick,
                    onSettingsClick = viewModel::onSettingsClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground)
                )

                YouMp3Screen.SongInput -> SongInputScreenContent(
                    viewModel = songInputViewModel,
                    onCloseClick = {
                        viewModel.onCloseSongInputClick()
                        songInputViewModel.onCancelExtraction()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground)
                )

                YouMp3Screen.Settings -> SettingsScreenContent(
                    settingsViewModel = viewModel.settingsViewModel,
                    onCloseClick = viewModel::onCloseSettingsClick,
                    footer = state.footer,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground)
                )
            }
        }
    }
}

@Composable
private fun FindItScreenContent(
    appTitle: String,
    title: String,
    footer: String,
    onFindClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(horizontal = 28.dp, vertical = 34.dp)) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PanelBackground)
                    .border(1.dp, BorderColor)
                    .padding(vertical = 18.dp)
            ) {
                SettingsButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = appTitle,
                        color = PrimaryText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = title,
                        color = SecondaryText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 36.dp)
            ) {
                MusicNoteCircleButton(onClick = onFindClick)
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Toca el botón para empezar",
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AppFooter(footerText = footer)
        }
    }
}

@Composable
private fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = SettingsIcon,
            contentDescription = "Ajustes",
            tint = PrimaryText,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun MusicNoteCircleButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val pressScale = animateFloatAsState(
        targetValue = if (isPressed) 1.14f else 1f,
        animationSpec = spring(),
        label = "music-note-button-scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "music-note-pulse-glow")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-scale"
    )

    val glowRingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glow-ring-scale"
    )

    val glowRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glow-ring-alpha"
    )

    val glowBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-border-alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(230.dp)
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer {
                    val scale = glowRingScale * pressScale.value
                    scaleX = scale
                    scaleY = scale
                    alpha = glowRingAlpha
                }
                .clip(CircleShape)
                .background(PrimaryText.copy(alpha = 0.12f))
                .border(2.dp, PrimaryText.copy(alpha = glowBorderAlpha), CircleShape)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer {
                    val totalScale = pressScale.value * pulseScale
                    scaleX = totalScale
                    scaleY = totalScale
                }
                .clip(CircleShape)
                .background(NoteButtonBackground)
                .border(5.dp, PrimaryText, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(42.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.icon),
                contentDescription = "Music note",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private val SettingsIcon = ImageVector.Builder(
    name = "Settings",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(19.14f, 12.94f)
        curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
        curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
        lineToRelative(2.03f, -1.58f)
        curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
        lineToRelative(-1.92f, -3.32f)
        curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
        lineToRelative(-2.39f, 0.96f)
        curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
        lineToRelative(-0.36f, -2.54f)
        curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
        horizontalLineToRelative(-3.84f)
        curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
        lineToRelative(-0.36f, 2.54f)
        curveToRelative(-0.59f, 0.24f, -1.13f, 0.57f, -1.62f, 0.94f)
        lineToRelative(-2.39f, -0.96f)
        curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
        lineTo(2.74f, 8.87f)
        curveToRelative(-0.12f, 0.21f, -0.08f, 0.47f, 0.12f, 0.61f)
        lineToRelative(2.03f, 1.58f)
        curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
        reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
        lineToRelative(-2.03f, 1.58f)
        curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
        lineToRelative(1.92f, 3.32f)
        curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
        lineToRelative(2.39f, -0.96f)
        curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
        lineToRelative(0.36f, 2.54f)
        curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
        horizontalLineToRelative(3.84f)
        curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
        lineToRelative(0.36f, -2.54f)
        curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
        lineToRelative(2.39f, 0.96f)
        curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
        lineToRelative(1.92f, -3.32f)
        curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
        lineToRelative(-2.01f, -1.58f)
        close()
        moveTo(12f, 15.5f)
        curveToRelative(-1.93f, 0f, -3.5f, -1.57f, -3.5f, -3.5f)
        reflectiveCurveToRelative(1.57f, -3.5f, 3.5f, -3.5f)
        reflectiveCurveToRelative(3.5f, 1.57f, 3.5f, 3.5f)
        reflectiveCurveToRelative(-1.57f, 3.5f, -3.5f, 3.5f)
        close()
    }
}.build()
