package com.dev.yoump3.interfaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.yoump3.viewModels.SongInputViewModel

@Composable
fun SongInputScreenContent(
    viewModel: SongInputViewModel,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 28.dp, vertical = 34.dp)
    ) {
        Text(
            text = state.appTitle,
            color = PrimaryText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelBackground)
                .border(1.dp, BorderColor)
                .padding(18.dp)
        ) {
            CloseButton(
                onClick = onCloseClick,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, bottom = 18.dp)
            ) {
                SongInputBox(
                    value = state.songQuery,
                    placeholder = state.placeholder,
                    onValueChange = viewModel::onSongQueryChange,
                    modifier = Modifier.weight(1f)
                )
                SearchActionButton(onClick = viewModel::onSearchClick)
            }
        }

        Text(
            text = state.footer,
            color = SecondaryText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .background(AppBackground)
            .border(2.dp, PrimaryText)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "X",
            color = PrimaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun SongInputBox(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.merge(
            TextStyle(color = PrimaryText)
        ),
        modifier = modifier
            .height(70.dp)
            .clip(CircleShape)
            .background(AppBackground)
            .border(4.dp, PrimaryText, CircleShape)
            .padding(horizontal = 26.dp),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = SecondaryText,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun SearchActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(70.dp)
            .background(AppBackground)
            .border(4.dp, PrimaryText)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = "GO",
            color = PrimaryText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
