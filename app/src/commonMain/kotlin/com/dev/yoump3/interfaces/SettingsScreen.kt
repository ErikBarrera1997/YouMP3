package com.dev.yoump3.interfaces

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.yoump3.appVersion
import com.dev.yoump3.generated.resources.Res
import com.dev.yoump3.generated.resources.dark_theme
import com.dev.yoump3.generated.resources.light_theme
import org.jetbrains.compose.resources.painterResource

enum class ThemeModeOption(
    val title: String,
    val description: String
) {
    DARK("Modo Oscuro", "Tema oscuro predeterminado"),
    LIGHT("Modo Claro", "Tema claro de alto contraste")
}

@Composable
fun SettingsScreenContent(
    onCloseClick: () -> Unit,
    footer: String = "BY CLEVER CLOUD · v$appVersion",
    modifier: Modifier = Modifier
) {
    var selectedTheme by remember { mutableStateOf(ThemeModeOption.DARK) }

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
                BackButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "AJUSTES",
                        color = PrimaryText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "CONFIGURACIÓN DE APARIENCIA",
                        color = SecondaryText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 36.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "TEMA DE LA APLICACIÓN",
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
                )

                ThemeModeOption.entries.forEach { option ->
                    val isSelected = selectedTheme == option
                    ThemeOptionCard(
                        option = option,
                        isSelected = isSelected,
                        onClick = { selectedTheme = option },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PanelBackground)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "El modo seleccionado actualmente es: ${selectedTheme.title}. (La aplicación completa del tema se activará próximamente).",
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Text(
                text = footer,
                color = SecondaryText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    option: ThemeModeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) PrimaryText else BorderColor
    val imageRes = if (option == ThemeModeOption.DARK) Res.drawable.dark_theme else Res.drawable.light_theme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PanelBackground)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AppBackground)
                    .border(1.dp, if (isSelected) PrimaryText else BorderColor, CircleShape)
                    .padding(8.dp)
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = option.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = option.title,
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = option.description,
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        RadioButtonIndicator(isSelected = isSelected)
    }
}

@Composable
private fun RadioButtonIndicator(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(2.dp, if (isSelected) PrimaryText else SecondaryText, CircleShape)
            .padding(3.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(PrimaryText)
            )
        }
    }
}

@Composable
private fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "<",
            color = PrimaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
