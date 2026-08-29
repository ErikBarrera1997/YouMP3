package com.dev.yoump3.interfaces

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

internal val AppBackground = Color(0xFF111111)
internal val PanelBackground = Color(0xFF181818)
internal val BorderColor = Color(0xFF2B2B2B)
internal val PrimaryText = Color(0xFFFFFFFF)
internal val SecondaryText = Color(0xFFAEBBD2)

@Composable
fun YouMp3App(viewModel: YouMp3ViewModel = remember { YouMp3ViewModel() }, onExit: () -> Unit = {}) {
    val songInputViewModel = remember { SongInputViewModel() }
    val errorStatusViewModel = remember { ErrorStatusViewModel() }
    var appReady by remember { mutableStateOf(false) }
    var connectionFailed by remember { mutableStateOf(false) }

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

@Composable
private fun YouMp3Theme(content: @Composable () -> Unit) {
    MaterialTheme(
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
                val forward = targetState == YouMp3Screen.SongInput
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground)
                )

                YouMp3Screen.SongInput -> SongInputScreenContent(
                    viewModel = songInputViewModel,
                    onCloseClick = viewModel::onCloseSongInputClick,
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
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 28.dp, vertical = 34.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelBackground)
                .border(1.dp, BorderColor)
                .padding(vertical = 18.dp)
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

        MusicNoteCircleButton(onClick = onFindClick)

        Text(
            text = footer,
            color = SecondaryText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun MusicNoteCircleButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 1.14f else 1f,
        animationSpec = spring(),
        label = "music-note-button-scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(190.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(CircleShape)
            .background(AppBackground)
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
