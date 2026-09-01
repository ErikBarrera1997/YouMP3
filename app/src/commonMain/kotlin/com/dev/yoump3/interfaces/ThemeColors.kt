package com.dev.yoump3.interfaces

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.yoump3.appVersion

data class YouMp3Colors(
    val appBackground: Color,
    val panelBackground: Color,
    val borderColor: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accentText: Color,
    val destructiveText: Color,
    val isDark: Boolean
)

val DarkThemeColors = YouMp3Colors(
    appBackground = Color(0xFF111111),
    panelBackground = Color(0xFF181818),
    borderColor = Color(0xFF2B2B2B),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFAEBBD2),
    accentText = Color(0xFF7B9FFF),
    destructiveText = Color(0xFFFF5252),
    isDark = true
)

val LightThemeColors = YouMp3Colors(
    appBackground = Color(0xFFF5F5F7),
    panelBackground = Color(0xFFFFFFFF),
    borderColor = Color(0xFFDCDCE0),
    primaryText = Color(0xFF111111),
    secondaryText = Color(0xFF6E6E73),
    accentText = Color(0xFF3478F6),
    destructiveText = Color(0xFFE53935),
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkThemeColors }

val NoteButtonBackground = Color(0xFF000000)

object AppTheme {
    val colors: YouMp3Colors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

val AppBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.appBackground

val PanelBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.panelBackground

val BorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.borderColor

val PrimaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.primaryText

val SecondaryText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.secondaryText

val AccentText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.accentText

val DestructiveText: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.destructiveText

@Composable
fun AppFooter(
    modifier: Modifier = Modifier,
    footerText: String = "BY CLEVER CLOUD · v$appVersion"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomEnd
    ) {
        Text(
            text = footerText,
            color = SecondaryText,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}
