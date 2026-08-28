package com.dev.yoump3.interfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val ErrorRed = Color(0xFFFF5252)
private val ButtonOnColor = Color(0xFF111111)

private val ErrorIcon = ImageVector.Builder(
    name = "Error",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(ErrorRed)) {
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

@Composable
fun ExtractionStatusView(
    isFailed: Boolean,
    title: String?,
    message: String?,
    onReturnToInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PanelBackground, RoundedCornerShape(10.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isFailed) {
                Icon(
                    imageVector = ErrorIcon,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                CircularProgressIndicator(
                    color = PrimaryText,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = if (isFailed) "EXTRACTION FAILED" else "EXTRACTING AUDIO...",
                color = PrimaryText,
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

            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = it,
                    color = if (isFailed) ErrorRed else SecondaryText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isFailed) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "TRY AGAIN",
                    color = ButtonOnColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryText)
                        .clickable(onClick = onReturnToInput)
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
    }
}