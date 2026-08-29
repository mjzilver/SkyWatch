package com.silversky.skywatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.silversky.core.model.MovieInfo

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieVersionDialog(
    title: String,
    versions: List<MovieInfo>,
    onDismiss: () -> Unit,
    onVersionSelected: (MovieInfo) -> Unit,
) {
  SkyWatchDialog(
      title = title,
      onDismiss = onDismiss,
      buttons = {
        Button(
            onClick = onDismiss,
        ) {
          Text("Cancel")
        }
      },
  ) {
    Text(
        text = "Multiple versions found. Select one to play:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(versions) { version ->
        Button(
            onClick = { onVersionSelected(version) },
            modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            val filename = version.entryPath.substringAfterLast('\\').substringAfterLast('/')
            Text(
                text = filename,
                style = MaterialTheme.typography.bodyMedium,
            )

            val edition = version.edition?.let { " [$it]" } ?: ""
            val year = version.year?.let { " ($it)" } ?: ""
            if (edition.isNotEmpty() || year.isNotEmpty()) {
              Text(
                  text = "$year$edition",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}
