package com.dev.yoump3.interfaces

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.dev.yoump3.generated.resources.server_error
import com.dev.yoump3.viewModels.ErrorStatusViewModel
import org.jetbrains.compose.resources.painterResource

private val ChevronLeftIcon = ImageVector.Builder(
    name = "ChevronLeft",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(15.41f, 7.41f)
        lineTo(14f, 6f)
        lineTo(8f, 12f)
        lineTo(14f, 18f)
        lineTo(15.41f, 16.59f)
        lineTo(11.83f, 12f)
        close()
    }
}.build()

private val ArrowForwardIcon = ImageVector.Builder(
    name = "ArrowForward",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 4f)
        lineTo(10.59f, 5.41f)
        lineTo(16.17f, 11f)
        lineTo(4f, 11f)
        lineTo(4f, 13f)
        lineTo(16.17f, 13f)
        lineTo(10.59f, 18.59f)
        lineTo(12f, 20f)
        lineTo(20f, 12f)
        close()
    }
}.build()

@Composable
fun ErrorStatusScreen(
    viewModel: ErrorStatusViewModel,
    onGoBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        ErrorStatusTopBar(onBackClick = onGoBackToHome)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 28.dp)
                .padding(top = 36.dp)
        ) {
            ErrorStatusIcon()

            Spacer(Modifier.height(32.dp))

            Text(
                text = viewModel.state.title,
                color = PrimaryText,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    lineHeight = 32.sp
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = viewModel.state.message,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            )

            Spacer(Modifier.height(32.dp))

            ErrorStatusButton(
                text = viewModel.state.buttonText,
                onClick = onGoBackToHome
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 34.dp)
        ) {
            AppFooter()
        }
    }
}

@Composable
private fun ErrorStatusTopBar(onBackClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(PanelBackground)
            .padding(horizontal = 20.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBackClick)
        ) {
            Icon(
                imageVector = ChevronLeftIcon,
                contentDescription = null,
                tint = PrimaryText,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun ErrorStatusIcon() {
    val transition = rememberInfiniteTransition(label = "error-status-wave")

    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "error-icon-pulse-scale"
    )

    val wave1Scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "error-wave-scale-1"
    )
    val wave1Alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "error-wave-alpha-1"
    )

    val wave2Scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1000)
        ),
        label = "error-wave-scale-2"
    )
    val wave2Alpha by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1000)
        ),
        label = "error-wave-alpha-2"
    )

    val borderGlowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "error-border-glow-alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(190.dp)
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    scaleX = wave2Scale
                    scaleY = wave2Scale
                    alpha = wave2Alpha
                }
                .clip(CircleShape)
                .background(PrimaryText.copy(alpha = 0.08f))
                .border(1.5.dp, PrimaryText.copy(alpha = 0.5f), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    scaleX = wave1Scale
                    scaleY = wave1Scale
                    alpha = wave1Alpha
                }
                .clip(CircleShape)
                .background(PrimaryText.copy(alpha = 0.10f))
                .border(1.5.dp, PrimaryText.copy(alpha = 0.7f), CircleShape)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape)
                .background(PanelBackground)
                .border(
                    2.dp,
                    AccentText.copy(alpha = borderGlowAlpha),
                    CircleShape
                )
        ) {
            Image(
                painter = painterResource(Res.drawable.server_error),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(112.dp)
            )
        }
    }
}

@Composable
private fun ErrorStatusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PrimaryText)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = AppBackground,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp
            )
        )
        Icon(
            imageVector = ArrowForwardIcon,
            contentDescription = null,
            tint = AppBackground,
            modifier = Modifier.size(20.dp)
        )
    }
}