package com.dev.yoump3.interfaces

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SonicCascadeBackground(
    isDark: Boolean = AppTheme.colors.isDark,
    modifier: Modifier = Modifier
) {
    // Monotonic continuous time in seconds (like WebGL requestAnimationFrame) - eliminates any loop seams/cuts
    val timeState = produceState(0f) {
        val startTime = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { frameTimeMs ->
                value = ((frameTimeMs - startTime) / 1000f)
            }
        }
    }
    val time = timeState.value

    // Palette matching Sonic Depth GLSL shader
    val colDeep = if (isDark) Color(0xFF040405) else Color(0xFFF0F4FA)
    val colSurface = if (isDark) Color(0xFF0B0E14) else Color(0xFFE4ECF7)
    val colBlueDark = if (isDark) Color(0xFF0D3870) else Color(0xFFBED6F6)
    val colBlueBright = if (isDark) Color(0xFF007BFF) else Color(0xFF3388FF)
    val colGlow = if (isDark) Color(0xFF73C7FF) else Color(0xFF8DC9FF)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(colDeep, colSurface, colDeep)
                )
            )
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 1. Cascading flowing ribbons of sonic energy (continuous, smooth infinite waves)
        drawSonicRibbons(
            w = w,
            h = h,
            time = time,
            colBlueDark = colBlueDark,
            colBlueBright = colBlueBright,
            colGlow = colGlow,
            isDark = isDark
        )

        // 2. Cascading vertical stream lines / streaks (seamless lifecycle fading at boundaries)
        drawSonicStreaks(
            w = w,
            h = h,
            time = time,
            colBlueBright = colBlueBright,
            colGlow = colGlow,
            isDark = isDark
        )
    }
}

private fun DrawScope.drawSonicRibbons(
    w: Float,
    h: Float,
    time: Float,
    colBlueDark: Color,
    colBlueBright: Color,
    colGlow: Color,
    isDark: Boolean
) {
    val layers = 5
    for (i in 0 until layers) {
        val path = Path()
        val phaseOffset = i * 1.35f
        val speed = 0.65f + i * 0.15f
        val currentPhase = time * speed + phaseOffset

        // Base vertical distribution from top to bottom
        val baseY = (h / (layers + 1)) * (i + 1)
        val amplitude = (h * 0.075f) + (i * 10f)

        path.moveTo(0f, h)
        val startY = baseY + sin(currentPhase) * amplitude
        path.lineTo(0f, startY)

        val steps = 36
        for (step in 0..steps) {
            val x = (w / steps) * step
            val xNorm = step.toFloat() / steps
            val waveFreq = xNorm * 2.8f * PI.toFloat()
            val yOffset = sin(waveFreq + currentPhase) * amplitude +
                    cos(waveFreq * 0.55f - currentPhase * 0.75f) * (amplitude * 0.45f)
            val y = baseY + yOffset
            path.lineTo(x, y)
        }

        path.lineTo(w, h)
        path.close()

        val ribbonAlpha = if (isDark) {
            (0.14f + 0.05f * sin(currentPhase * 0.7f + i)).coerceIn(0.06f, 0.22f)
        } else {
            (0.18f + 0.06f * sin(currentPhase * 0.7f + i)).coerceIn(0.08f, 0.26f)
        }

        val glowAlpha = if (isDark) {
            (0.10f + 0.04f * cos(currentPhase * 0.8f)).coerceIn(0.04f, 0.16f)
        } else {
            (0.14f + 0.05f * cos(currentPhase * 0.8f)).coerceIn(0.06f, 0.22f)
        }

        val ribbonBrush = Brush.verticalGradient(
            colors = listOf(
                colGlow.copy(alpha = glowAlpha),
                colBlueBright.copy(alpha = ribbonAlpha),
                colBlueDark.copy(alpha = ribbonAlpha * 0.55f),
                Color.Transparent
            ),
            startY = (baseY - amplitude).coerceAtLeast(0f),
            endY = h
        )

        drawPath(path = path, brush = ribbonBrush)

        // Draw glowing crest highlight stroke along wave ridge
        val crestPath = Path()
        crestPath.moveTo(0f, startY)
        for (step in 0..steps) {
            val x = (w / steps) * step
            val xNorm = step.toFloat() / steps
            val waveFreq = xNorm * 2.8f * PI.toFloat()
            val yOffset = sin(waveFreq + currentPhase) * amplitude +
                    cos(waveFreq * 0.55f - currentPhase * 0.75f) * (amplitude * 0.45f)
            val y = baseY + yOffset
            crestPath.lineTo(x, y)
        }

        val strokeAlpha = if (isDark) 0.38f else 0.46f
        drawPath(
            path = crestPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    colGlow.copy(alpha = strokeAlpha * 0.7f),
                    colBlueBright.copy(alpha = strokeAlpha),
                    colGlow.copy(alpha = strokeAlpha * 0.7f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = if (i % 2 == 0) 2.2f else 1.4f)
        )
    }
}

private fun DrawScope.drawSonicStreaks(
    w: Float,
    h: Float,
    time: Float,
    colBlueBright: Color,
    colGlow: Color,
    isDark: Boolean
) {
    val streakCount = 20
    for (s in 0 until streakCount) {
        val relX = ((s * 137.5f) % 100f) / 100f
        val x = relX * w

        val streakSpeed = 0.22f + (s % 6) * 0.06f
        val offset = s * 0.173f
        val progress = ((time * streakSpeed + offset) % 1.0f).let { if (it < 0f) it + 1f else it } // Seamless 0f to 1f

        val streakLength = h * (0.16f + (s % 4) * 0.07f)
        val yStart = (progress * (h + streakLength)) - streakLength
        val yEnd = yStart + streakLength

        // Sine envelope guarantees alpha is 0 at progress 0 and 1, completely eliminating any wrap seam
        val alphaPulse = sin(progress * PI.toFloat()).coerceIn(0f, 1f)
        val baseAlpha = if (isDark) 0.20f else 0.24f
        val currentAlpha = baseAlpha * alphaPulse

        if (currentAlpha > 0.005f) {
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        colGlow.copy(alpha = currentAlpha),
                        colBlueBright.copy(alpha = currentAlpha * 0.85f),
                        Color.Transparent
                    ),
                    startY = yStart,
                    endY = yEnd
                ),
                start = Offset(x, yStart),
                end = Offset(x, yEnd),
                strokeWidth = if (s % 3 == 0) 2.2f else 1.2f
            )
        }
    }
}
