package com.dev.yoump3.interfaces

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
actual fun DownloadLink(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onClick()
        } else {
            Toast.makeText(
                context,
                "Permiso de almacenamiento denegado. Habilitalo en los ajustes de la app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        onClick()
    }

    val onActionClick = {
        val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

        when {
            needsStoragePermission -> storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            needsNotificationPermission -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> onClick()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed && enabled) lerp(AppBackground, PrimaryText, 0.25f) else AppBackground,
        animationSpec = tween(durationMillis = 120),
        label = "download-button-background"
    )
    val textColor = if (enabled) PrimaryText else SecondaryText
    val borderColor = if (enabled) PrimaryText else BorderColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(56.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, borderColor, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onActionClick
            )
            .padding(horizontal = 28.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            textAlign = TextAlign.Center,
            textDecoration = if (enabled) TextDecoration.Underline else null,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
