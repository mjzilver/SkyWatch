package com.silversky.skywatch.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text

data class ScanResult(
    val ip: String,
    val name: String,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScanDialog(
    onDismiss: () -> Unit,
    onServerSelected: (ScanResult) -> Unit,
    servers: List<ScanResult>,
) {
  var selectedServer by
      remember(servers) {
        mutableStateOf(servers.firstOrNull())
      }

  val selectFocus = remember {
    FocusRequester()
  }

  val cancelFocus = remember {
    FocusRequester()
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = "Scan Results",
          style = MaterialTheme.typography.headlineSmall,
      )

      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(modifier = Modifier.fillMaxWidth().selectableGroup()) {
        items(servers) { server ->
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .clickable {
                        selectedServer = server
                      }
                      .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            RadioButton(
                selected = selectedServer?.ip == server.ip,
                onClick = {
                  selectedServer = server
                },
            )

            Column {
              Text(
                  text = server.name,
                  style = MaterialTheme.typography.bodyLarge,
              )

              Text(
                  text = server.ip,
                  style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
            onClick = onDismiss,
            modifier =
                Modifier.weight(1f).focusRequester(cancelFocus).onPreviewKeyEvent { event ->
                  if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                  }

                  when (event.key) {
                    Key.DirectionRight -> {
                      selectFocus.requestFocus()
                      true
                    }

                    else -> false
                  }
                },
        ) {
          Text("Cancel")
        }

        Button(
            onClick = {
              selectedServer?.let {
                onServerSelected(it)
              }
            },
            modifier =
                Modifier.weight(1f).focusRequester(selectFocus).onPreviewKeyEvent { event ->
                  if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                  }

                  when (event.key) {
                    Key.DirectionLeft -> {
                      cancelFocus.requestFocus()
                      true
                    }

                    else -> false
                  }
                },
        ) {
          Text("Select")
        }
      }
    }
  }
}
