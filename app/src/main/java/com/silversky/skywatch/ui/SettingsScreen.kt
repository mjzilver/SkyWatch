package com.silversky.skywatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentAddress: String?,
    onAddressSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var addressInput by remember(currentAddress) { mutableStateOf(currentAddress ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 72.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onBack) {
                Text("Back")
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Subtitle Server",
                style = MaterialTheme.typography.titleLarge
            )
            
            TvTextField(
                value = addressInput,
                onValueChange = { addressInput = it },
                label = "SkyWatch Subtitle Server Address",
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { onAddressSave(addressInput) }) {
                Text("Save Address")
            }
        }
    }
}

