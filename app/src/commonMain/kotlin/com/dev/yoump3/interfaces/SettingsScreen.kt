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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.yoump3.appVersion
import com.dev.yoump3.generated.resources.Res
import com.dev.yoump3.generated.resources.dark_theme
import com.dev.yoump3.generated.resources.light_theme
import com.dev.yoump3.viewModels.SettingsViewModel
import com.dev.yoump3.viewModels.ThemeMode
import org.jetbrains.compose.resources.painterResource

@Composable
fun SettingsScreenContent(
    settingsViewModel: SettingsViewModel,
    onCloseClick: () -> Unit,
    footer: String = "BY CLEVER CLOUD · v$appVersion",
    modifier: Modifier = Modifier
) {
    val selectedTheme = settingsViewModel.state.themeMode

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
                        text = "CONFIGURACIÓN DE LA APP",
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
                    text = "TEMA",
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
                )

                ThemeMode.entries.forEach { option ->
                    val isSelected = selectedTheme == option
                    ThemeOptionCard(
                        option = option,
                        isSelected = isSelected,
                        onClick = { settingsViewModel.onThemeModeChange(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "SERVIDOR",
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
                )

                val settings = settingsViewModel.state
                val hasError = settings.serverUrlError != null
                val showServerActions = settings.isServerUrlDirty || settings.hasServerUrlOverride

                ServerUrlField(
                    value = settings.serverUrl,
                    onValueChange = settingsViewModel::onServerUrlChange,
                    onDone = settingsViewModel::onServerUrlSave,
                    isError = hasError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = settings.serverUrlError
                        ?: if (settings.hasServerUrlOverride) {
                            "USANDO URL PERSONALIZADA"
                        } else {
                            "USANDO URL PREDETERMINADA"
                        },
                    color = if (hasError) DestructiveText else SecondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )

                if (showServerActions) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsActionButton(
                            text = "GUARDAR",
                            onClick = settingsViewModel::onServerUrlSave,
                            isPrimary = true
                        )
                        SettingsActionButton(
                            text = "RESTABLECER",
                            onClick = settingsViewModel::onServerUrlReset,
                            isPrimary = false
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            AppFooter(footerText = footer)
        }
    }
}

@Composable
private fun ThemeOptionCard(
    option: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) PrimaryText else BorderColor
    val imageRes = if (option == ThemeMode.DARK) Res.drawable.dark_theme else Res.drawable.light_theme

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
                    .background(NoteButtonBackground)
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
private fun ServerUrlField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        cursorBrush = SolidColor(PrimaryText),
        textStyle = MaterialTheme.typography.bodyMedium.merge(
            TextStyle(color = PrimaryText)
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PanelBackground)
            .border(
                1.dp,
                if (isError) DestructiveText else BorderColor,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = "https://servidor.com",
                        color = SecondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun SettingsActionButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPrimary) PrimaryText else Color.Transparent)
            .border(1.dp, PrimaryText, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            color = if (isPrimary) AppBackground else PrimaryText,
            style = MaterialTheme.typography.labelMedium
        )
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