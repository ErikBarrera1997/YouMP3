package com.dev.yoump3.init

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dev.yoump3.interfaces.AppBackground
import com.dev.yoump3.interfaces.PrimaryText
import com.dev.yoump3.interfaces.SecondaryText

private val SpinnerSize = 88.dp
private val WaveRingSize = 120.dp
private val WaveMaxSize = WaveRingSize * 1.75f

@Composable
fun InitScreen(onConnected: () -> Unit, onError: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Text(
            text = "YOUMP3",
            color = PrimaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 64.sp,
                lineHeight = 72.sp,
                fontWeight = FontWeight.Black
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        )

        LoadingSpinner(
            modifier = Modifier.align(Alignment.Center)
        )

        Text(
            text = "CARGANDO...",
            color = SecondaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = WaveMaxSize / 2 + 16.dp)
        )

        LaunchedEffect(Unit) {
            val result = YouMp3Api.checkConnection()
            if (result.isSuccess) {
                onConnected()
            } else {
                onError()
            }
        }
    }
}

@Composable
private fun LoadingSpinner(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading-wave")

    val innerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "inner-wave-scale"
    )
    val innerAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(0)
        ),
        label = "inner-wave-alpha"
    )
    val outerScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(900)
        ),
        label = "outer-wave-scale"
    )
    val outerAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(900)
        ),
        label = "outer-wave-alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(WaveMaxSize)
    ) {
        Box(
            modifier = Modifier
                .size(WaveRingSize)
                .graphicsLayer {
                    scaleX = outerScale
                    scaleY = outerScale
                    alpha = outerAlpha
                }
                .border(1.5.dp, PrimaryText, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(WaveRingSize)
                .graphicsLayer {
                    scaleX = innerScale
                    scaleY = innerScale
                    alpha = innerAlpha
                }
                .border(1.5.dp, PrimaryText, CircleShape)
        )
        CircularProgressIndicator(
            color = PrimaryText,
            strokeWidth = 5.dp,
            modifier = Modifier.size(SpinnerSize)
        )
    }
}
