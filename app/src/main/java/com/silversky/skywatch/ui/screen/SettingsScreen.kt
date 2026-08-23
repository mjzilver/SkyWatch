package com.silversky.skywatch.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import com.silversky.skywatch.model.SortBy
import com.silversky.skywatch.model.SortOrder
import com.silversky.skywatch.ui.component.TvTextField
import com.silversky.skywatch.ui.viewmodel.SettingsViewModel
import com.silversky.skywatch.ui.viewmodel.SubtitleServerConnectionStatus

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
  val settings by viewModel.settings.collectAsState()
  val currentAddress by viewModel.subtitleServerAddress.collectAsState()
  var addressInput by remember(currentAddress) { mutableStateOf(currentAddress ?: "") }
  val connectionStatus by viewModel.connectionStatus.collectAsState()

  LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 48.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    item {
      Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Button(onClick = onBack) { Text("Back") }
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
        )
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "File Browser Sorting",
            style = MaterialTheme.typography.titleLarge,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Sort by", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SortByOption(
                text = "Name",
                selected = settings.sortBy == SortBy.Name,
                onClick = { viewModel.updateSortBy(SortBy.Name) },
            )
            SortByOption(
                text = "Date modified",
                selected = settings.sortBy == SortBy.DateModified,
                onClick = { viewModel.updateSortBy(SortBy.DateModified) },
            )
            SortByOption(
                text = "Size",
                selected = settings.sortBy == SortBy.Size,
                onClick = { viewModel.updateSortBy(SortBy.Size) },
            )
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Order", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SortOrderOption(
                text = "Ascending",
                selected = settings.sortOrder == SortOrder.Ascending,
                onClick = { viewModel.updateSortOrder(SortOrder.Ascending) },
            )
            SortOrderOption(
                text = "Descending",
                selected = settings.sortOrder == SortOrder.Descending,
                onClick = { viewModel.updateSortOrder(SortOrder.Descending) },
            )
          }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Checkbox(
              checked = settings.foldersFirst,
              onCheckedChange = { viewModel.updateFoldersFirst(it) },
          )
          Text(text = "Folders first", style = MaterialTheme.typography.bodyLarge)
        }
      }
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Subtitle Appearance",
            style = MaterialTheme.typography.titleLarge,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Font size", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(18, 24, 32, 48).forEach { size ->
              Button(
                  onClick = { viewModel.updateSubtitleFontSize(size) },
                  modifier = Modifier.width(100.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  RadioButton(selected = settings.subtitleFontSize == size, onClick = null)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = "$size")
                }
              }
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Font family", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Sans Serif", "Serif", "Monospace").forEach { family ->
              Button(
                  onClick = { viewModel.updateSubtitleFontFamily(family) },
                  modifier = Modifier.width(160.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  RadioButton(selected = settings.subtitleFontFamily == family, onClick = null)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = family)
                }
              }
            }
          }
        }
      }
    }

    // Subtitle server override - This should always be the last setting
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Subtitle server override",
            style = MaterialTheme.typography.titleLarge,
        )

        TvTextField(
            value = addressInput,
            onValueChange = {
              addressInput = it
              viewModel.resetConnectionStatus()
            },
            label = "Subtitle server address (leave empty to automatically discover)",
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(onClick = { viewModel.updateSubtitleServerAddress(addressInput) }) { Text("Save") }

          Button(onClick = { viewModel.checkSubtitleServerAddress(addressInput) }) {
            Text("Test connection")
          }

          Button(onClick = { viewModel.findSubtitleServer() }) { Text("Automatically find server") }
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
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SortByOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      modifier = Modifier.width(160.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      RadioButton(selected = selected, onClick = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = text)
    }
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SortOrderOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      modifier = Modifier.width(160.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      RadioButton(selected = selected, onClick = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = text)
    }
  }
}
