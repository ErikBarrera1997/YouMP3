package com.dev.yoump3.interfaces

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.yoump3.appVersion
import com.dev.yoump3.consent.DataUsageConsent
import com.dev.yoump3.generated.resources.Res
import com.dev.yoump3.generated.resources.dark_theme
import com.dev.yoump3.generated.resources.light_theme
import com.dev.yoump3.viewModels.ServerTarget
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

                ServerTargetDropdown(
                    selected = settings.serverTarget,
                    onSelect = settingsViewModel::onServerTargetChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = settings.serverTarget.description,
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = settings.serverUrl,
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "USO DE DATOS",
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 14.dp, start = 4.dp)
                )

                ConsentOptionCard(
                    enabled = settings.dataUsageConsent,
                    configured = settings.dataUsageConsentConfigured,
                    onClick = {
                        settingsViewModel.onDataUsageConsentChange(!settings.dataUsageConsent)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = when {
                        settings.dataUsageConsent -> DataUsageConsent.SETTINGS_ENABLED_STATUS
                        settings.dataUsageConsentConfigured ->
                            DataUsageConsent.SETTINGS_DISABLED_STATUS
                        else -> DataUsageConsent.SETTINGS_NOT_CONFIGURED_STATUS
                    },
                    color = if (settings.dataUsageConsentConfigured && !settings.dataUsageConsent) {
                        DestructiveText
                    } else {
                        SecondaryText
                    },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = DataUsageConsent.SETTINGS_HELP,
                    color = SecondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                )

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
private fun ConsentOptionCard(
    enabled: Boolean,
    configured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PanelBackground)
            .border(1.dp, if (enabled) PrimaryText else BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = DataUsageConsent.SETTINGS_TITLE,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    enabled -> DataUsageConsent.SETTINGS_ENABLED_DESCRIPTION
                    configured -> DataUsageConsent.SETTINGS_DISABLED_DESCRIPTION
                    else -> DataUsageConsent.SETTINGS_NOT_CONFIGURED_DESCRIPTION
                },
                color = SecondaryText,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.width(16.dp))

        ConsentToggleIndicator(enabled = enabled)
    }
}

@Composable
private fun ConsentToggleIndicator(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(if (enabled) PrimaryText else Color.Transparent)
            .border(2.dp, if (enabled) PrimaryText else SecondaryText, CircleShape)
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (enabled) AppBackground else SecondaryText)
        )
    }
}

@Composable
private fun ServerTargetDropdown(
    selected: ServerTarget,
    onSelect: (ServerTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBackground)
                .border(1.dp, if (expanded) PrimaryText else BorderColor, RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = selected.title,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (expanded) "▴" else "▾",
                color = SecondaryText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
            containerColor = PanelBackground,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, BorderColor)
        ) {
            ServerTarget.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = if (option == selected) PrimaryText else SecondaryText
                    )
                )
            }
        }
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