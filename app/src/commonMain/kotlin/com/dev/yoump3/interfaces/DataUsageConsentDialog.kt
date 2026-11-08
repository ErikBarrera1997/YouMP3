package com.dev.yoump3.interfaces

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dev.yoump3.consent.DataUsageConsent

/**
 * Aviso previo a la primera búsqueda. No es un endpoint: el texto y la decisión viven en la app.
 * Si el usuario rechaza, [onDecline] persiste el rechazo y la búsqueda no arranca.
 */
@Composable
fun DataUsageConsentDialog(
    previouslyDeclined: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(onDismissRequest = onDecline) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AppBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = DataUsageConsent.TITLE,
                color = PrimaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = DataUsageConsent.MESSAGE,
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(14.dp))

            DataUsageConsent.DETAILS.forEach { detail ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Text(
                        text = "•",
                        color = PrimaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = detail,
                        color = SecondaryText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (previouslyDeclined) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = DataUsageConsent.DECLINED_HINT,
                    color = DestructiveText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(22.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ConsentButton(
                    text = DataUsageConsent.DECLINE_LABEL,
                    filled = false,
                    onClick = onDecline,
                    modifier = Modifier.weight(1f)
                )
                ConsentButton(
                    text = DataUsageConsent.ACCEPT_LABEL,
                    filled = true,
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConsentButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) PrimaryText else Color.Transparent)
            .border(1.dp, PrimaryText, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp)
    ) {
        Text(
            text = text,
            color = if (filled) AppBackground else PrimaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
