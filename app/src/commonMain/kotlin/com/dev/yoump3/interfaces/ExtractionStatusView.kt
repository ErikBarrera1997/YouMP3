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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WarningColor = Color(0xFFFFB4AB)
private val ButtonOnColor = Color(0xFF111111)
private val SpinnerRingSize = 72.dp
private val SpinnerMaxSize = 96.dp

private val ErrorIcon = ImageVector.Builder(
    name = "Error",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(WarningColor)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(13f, 17f)
        horizontalLineTo(11f)
        verticalLineTo(15f)
        horizontalLineTo(13f)
        verticalLineTo(17f)
        close()
        moveTo(13f, 13f)
        horizontalLineTo(11f)
        verticalLineTo(7f)
        horizontalLineTo(13f)
        verticalLineTo(13f)
        close()
    }
}.build()

private val RedownloadIcon = ImageVector.Builder(
    name = "Redownload",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(17.65f, 6.35f)
        curveTo(16.2f, 4.9f, 14.21f, 4f, 12f, 4f)
        curveTo(7.58f, 4f, 4.01f, 7.58f, 4.01f, 12f)
        curveTo(4.01f, 16.42f, 7.58f, 20f, 12f, 20f)
        curveTo(15.73f, 20f, 18.84f, 17.45f, 19.73f, 14f)
        horizontalLineTo(17.65f)
        curveTo(16.83f, 16.33f, 14.61f, 18f, 12f, 18f)
        curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
        curveTo(6f, 8.69f, 8.69f, 6f, 12f, 6f)
        curveTo(13.66f, 6f, 15.14f, 6.69f, 16.22f, 7.78f)
        lineTo(13f, 11f)
        horizontalLineTo(21f)
        verticalLineTo(3f)
        lineTo(17.65f, 6.35f)
        close()
    }
}.build()

private val BackIcon = ImageVector.Builder(
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

@Composable
fun ExtractionStatusView(
    isFailed: Boolean,
    title: String?,
    message: String?,
    onReturnToInput: () -> Unit,
    onRetryDownload: (() -> Unit)? = null,
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
            if (isFailed) {
                ErrorGlyph()
            } else {
                ExtractionSpinner()
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "EXTRAYENDO",
                color = if (isFailed) WarningColor else PrimaryText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )

            title?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isFailed) {
                message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = it,
                        color = WarningColor,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(26.dp))

                if (onRetryDownload != null) {
                    StatusActionButton(
                        text = "REINTENTAR DESCARGA",
                        filled = true,
                        icon = RedownloadIcon,
                        onClick = onRetryDownload,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))
                }
            } else {
                Spacer(Modifier.height(26.dp))
            }

            StatusActionButton(
                text = "VOLVER A LA BÚSQUEDA",
                filled = false,
                icon = BackIcon,
                onClick = onReturnToInput,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ExtractionSpinner() {
    val transition = rememberInfiniteTransition(label = "extracting-wave")

    val innerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "extracting-inner-scale"
    )
    val innerAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "extracting-inner-alpha"
    )
    val outerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(900)
        ),
        label = "extracting-outer-scale"
    )
    val outerAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(900)
        ),
        label = "extracting-outer-alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(SpinnerMaxSize)
    ) {
        Box(
            modifier = Modifier
                .size(SpinnerRingSize)
                .graphicsLayer {
                    scaleX = outerScale
                    scaleY = outerScale
                    alpha = outerAlpha
                }
                .border(1.5.dp, PrimaryText, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(SpinnerRingSize)
                .graphicsLayer {
                    scaleX = innerScale
                    scaleY = innerScale
                    alpha = innerAlpha
                }
                .border(1.5.dp, PrimaryText, CircleShape)
        )
        CircularProgressIndicator(
            color = PrimaryText,
            strokeWidth = 4.dp,
            modifier = Modifier.size(54.dp)
        )
    }
}

@Composable
private fun ErrorGlyph() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(SpinnerMaxSize)
    ) {
        Box(
            modifier = Modifier
                .size(SpinnerRingSize)
                .border(1.5.dp, WarningColor.copy(alpha = 0.35f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(SpinnerRingSize)
                .border(1.5.dp, WarningColor.copy(alpha = 0.2f), CircleShape)
        )
        Icon(
            imageVector = ErrorIcon,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(54.dp)
        )
    }
}

@Composable
private fun StatusActionButton(
    text: String,
    filled: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (filled) ButtonOnColor else PrimaryText
    val background = if (filled) PrimaryText else AppBackground

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(CircleShape)
            .then(if (filled) Modifier else Modifier.border(2.dp, PrimaryText, CircleShape))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = textColor,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 0.6.sp
            )
        )
    }
}