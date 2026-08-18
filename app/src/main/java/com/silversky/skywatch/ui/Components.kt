package com.silversky.skywatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(onClick = onClick, modifier = modifier) {
        Text("Back")
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingMessage(
    message: String = "Loading..."
) {
    Text(
        text = message, style = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ErrorMessage(
    message: String
) {
    Text(
        text = message, style = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreenHeader(
    title: String, subtitle: String? = null, onBack: (() -> Unit)? = null
) {
    val backFocus = remember {
        FocusRequester()
    }

    LaunchedEffect(Unit) {
        backFocus.requestFocus()
    }

    Row(
        modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            BackButton(onClick = onBack, Modifier.focusRequester(backFocus))

            Spacer(
                modifier = Modifier.width(24.dp)
            )
        }

        Column {
            Text(
                text = title, style = MaterialTheme.typography.headlineMedium
            )

            if (subtitle != null) {
                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = subtitle, style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyMessage(
    message: String
) {
    Column(
        modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message, style = MaterialTheme.typography.bodyLarge
        )
    }
}