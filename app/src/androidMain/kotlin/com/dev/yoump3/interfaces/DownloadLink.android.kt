package com.dev.yoump3.interfaces

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val permissionLauncher = rememberLauncherForActivityResult(
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

    Spacer(Modifier.height(12.dp))
    Text(
        text = text,
        color = if (enabled) Color(0xFF4FC3F7) else SecondaryText,
        textAlign = TextAlign.Center,
        textDecoration = if (enabled) TextDecoration.Underline else null,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED

                if (needsPermission) {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    onClick()
                }
            }
            .padding(8.dp),
        style = MaterialTheme.typography.titleLarge
    )
}
