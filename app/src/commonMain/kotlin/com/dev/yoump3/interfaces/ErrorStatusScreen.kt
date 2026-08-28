package com.dev.yoump3.interfaces

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dev.yoump3.viewModels.ErrorStatusViewModel

private val ErrorBackground = Color(0xFF0A0A0A)
private val ErrorSurface = Color(0xFF131313)
private val ErrorOnSurface = Color(0xFFE5E2E1)
private val ErrorOutline = Color(0xFF8B90A0)
private val ErrorPrimary = Color(0xFFADC7FF)
private val ErrorOnPrimary = Color(0xFF002E68)
private val ErrorIconColor = Color(0xFFFFB4AB)
private val RingInner = Color(0xFF353534)
private val RingOuter = Color(0xFF201F1F)
private val IconCircleBorder = Color(0xFF414754)

private val CloudOffIcon = ImageVector.Builder(
    name = "CloudOff",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(ErrorIconColor)) {
        moveTo(19.35f, 10.04f)
        curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
        curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
        curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
        curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
        horizontalLineTo(19f)
        curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
        curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
        close()
    }
    path(
        fill = null,
        stroke = SolidColor(ErrorBackground),
        strokeLineWidth = 4.4f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(4f, 4.2f)
        lineTo(19.8f, 20f)
    }
    path(
        fill = null,
        stroke = SolidColor(ErrorIconColor),
        strokeLineWidth = 2.4f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(4f, 4.2f)
        lineTo(19.8f, 20f)
    }
}.build()

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
            .background(ErrorBackground)
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
                color = ErrorOnSurface,
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
                color = ErrorOutline,
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
    }
}

@Composable
private fun ErrorStatusTopBar(onBackClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(ErrorSurface)
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
                tint = ErrorPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun ErrorStatusIcon() {
    val transition = rememberInfiniteTransition(label = "error-status-rings")

    val innerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "inner-ring-scale"
    )
    val innerAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "inner-ring-alpha"
    )
    val outerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(750)
        ),
        label = "outer-ring-scale"
    )
    val outerAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(750)
        ),
        label = "outer-ring-alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = outerScale
                    scaleY = outerScale
                    alpha = outerAlpha
                }
                .border(1.dp, RingOuter, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    scaleX = innerScale
                    scaleY = innerScale
                    alpha = innerAlpha
                }
                .border(1.dp, RingInner, CircleShape)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(128.dp)
                .border(1.dp, IconCircleBorder, CircleShape)
        ) {
            Icon(
                imageVector = CloudOffIcon,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(60.dp)
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
            .background(ErrorPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = ErrorOnPrimary,
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
            tint = ErrorOnPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}