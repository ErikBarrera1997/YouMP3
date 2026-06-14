package com.dev.yoump3.init

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.yoump3.interfaces.AppBackground
import com.dev.yoump3.interfaces.PrimaryText
import com.dev.yoump3.interfaces.SecondaryText
import kotlinx.coroutines.delay

@Composable
fun InitScreen(onConnected: () -> Unit, onError: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Text(
            text = "YOUMP3",
            color = PrimaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(48.dp))

        CircularProgressIndicator(
            color = PrimaryText,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "CARGANDO...",
            color = SecondaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
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
fun ErrorScreen(onExit: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Text(
            text = "YOUMP3",
            color = PrimaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(48.dp))

        Text(
            text = "NO SE PUDO CONECTAR",
            color = Color(0xFFFF5252),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "AL SERVIDOR",
            color = Color(0xFFFF5252),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )

        LaunchedEffect(Unit) {
            delay(2000)
            onExit()
        }
    }
}
