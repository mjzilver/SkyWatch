package com.silversky.skywatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.silversky.skywatch.viewmodel.SettingsViewModel
import com.silversky.skywatch.viewmodel.SubtitleServerConnectionStatus

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
  val currentAddress by viewModel.subtitleServerAddress.collectAsState()
  var addressInput by remember(currentAddress) { mutableStateOf(currentAddress ?: "") }
  val connectionStatus by viewModel.connectionStatus.collectAsState()

  Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 48.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Button(onClick = onBack) {
        Text("Back")
      }
      Text(
          text = "Settings",
          style = MaterialTheme.typography.headlineLarge,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
          text = "Subtitle server",
          style = MaterialTheme.typography.titleLarge,
      )

      TvTextField(
          value = addressInput,
          onValueChange = {
            addressInput = it
            viewModel.resetConnectionStatus()
          },
          label = "Subtitle server address",
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(onClick = { viewModel.updateSubtitleServerAddress(addressInput) }) {
          Text("Save address")
        }

        Button(onClick = { viewModel.checkSubtitleServerAddress(addressInput) }) {
          Text("Test connection")
        }

        Button(onClick = { viewModel.findSubtitleServer() }) {
          Text("Resolve server")
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Box(modifier = Modifier.height(32.dp)) {
        when (val status = connectionStatus) {
          SubtitleServerConnectionStatus.Searching -> {
            Text(
                text = "Searching for subtitle server...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          SubtitleServerConnectionStatus.Testing -> {
            Text(
                text = "Testing connection...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          SubtitleServerConnectionStatus.Connected -> {
            Text(
                text = "Server connected successfully",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
          }
          SubtitleServerConnectionStatus.NotConnected -> {
            Text(
                text = "Could not connect to server",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
          }
          is SubtitleServerConnectionStatus.Error -> {
            Text(
                text = status.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
          }
          SubtitleServerConnectionStatus.Idle -> {}
        }
      }
    }
  }
}
