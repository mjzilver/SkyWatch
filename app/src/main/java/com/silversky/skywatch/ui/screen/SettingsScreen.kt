package com.silversky.skywatch.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.silversky.skywatch.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
  val settings by viewModel.settings.collectAsState()

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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "Media Library Priority", style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val priorities = com.silversky.skywatch.model.MediaPriority.entries
            priorities.forEach { priority ->
              Button(
                  onClick = { viewModel.updateMediaPriority(priority) },
                  modifier = Modifier.width(160.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  RadioButton(selected = settings.mediaPriority == priority, onClick = null)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                      text =
                          when (priority) {
                            com.silversky.skywatch.model.MediaPriority.MoviesFirst -> "Movies first"
                            com.silversky.skywatch.model.MediaPriority.SeriesFirst -> "Series first"
                            com.silversky.skywatch.model.MediaPriority.None -> "None"
                          }
                  )
                }
              }
            }
          }
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
