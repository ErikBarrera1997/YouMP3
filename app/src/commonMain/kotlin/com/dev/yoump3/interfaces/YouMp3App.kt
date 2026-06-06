package com.dev.yoump3.interfaces

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dev.yoump3.viewModels.YouMp3ViewModel

private val AppBackground = Color(0xFF111111)
private val PanelBackground = Color(0xFF181818)
private val BorderColor = Color(0xFF2B2B2B)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFFAEBBD2)

@Composable
fun YouMp3App(viewModel: YouMp3ViewModel = remember { YouMp3ViewModel() }) {
    YouMp3Theme {
        YouMp3Screen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )
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
    modifier: Modifier = Modifier
) {
    val state = viewModel.state

    Surface(
        color = AppBackground,
        contentColor = PrimaryText,
        modifier = modifier
    ) {
        FindItScreenContent(
            appTitle = state.appTitle,
            title = state.title,
            footer = state.footer,
            onFindClick = viewModel::onFindButtonClick,
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        )
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
        MusicNoteIcon(Modifier.fillMaxSize())
    }
}

@Composable
private fun MusicNoteIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val notePath = Path().apply {
            moveTo(size.width * 0.39f, size.height * 0.20f)
            quadraticTo(size.width * 0.55f, size.height * 0.26f, size.width * 0.70f, size.height * 0.20f)
            quadraticTo(size.width * 0.76f, size.height * 0.34f, size.width * 0.84f, size.height * 0.45f)
            lineTo(size.width * 0.84f, size.height * 0.70f)
            moveTo(size.width * 0.39f, size.height * 0.20f)
            quadraticTo(size.width * 0.45f, size.height * 0.45f, size.width * 0.36f, size.height * 0.66f)
            lineTo(size.width * 0.36f, size.height * 0.78f)
        }
        drawPath(notePath, PrimaryText, style = stroke)
        drawOval(
            color = PrimaryText,
            topLeft = Offset(size.width * 0.22f, size.height * 0.70f),
            size = Size(size.width * 0.18f, size.height * 0.13f),
            style = stroke
        )
        drawOval(
            color = PrimaryText,
            topLeft = Offset(size.width * 0.70f, size.height * 0.68f),
            size = Size(size.width * 0.18f, size.height * 0.13f),
            style = stroke
        )
    }
}
