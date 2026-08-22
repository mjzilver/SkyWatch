package com.silversky.skywatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.silversky.skywatch.viewmodel.HomeViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onServerConnected: () -> Unit,
    onSettingsClick: () -> Unit,
) {
  val savedServers by viewModel.servers.collectAsState()
  val error = viewModel.scanError

  Column(
      modifier =
          Modifier.fillMaxSize()
              .padding(
                  horizontal = 72.dp,
                  vertical = 48.dp,
              ),
      verticalArrangement = Arrangement.spacedBy(32.dp),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
      Column {
        Text(
            text = "SKYWATCH",
            style = MaterialTheme.typography.headlineLarge,
        )

        Text(
            text = "SMB MEDIA PLAYER",
            style = MaterialTheme.typography.bodyLarge,
        )
      }

      Button(onClick = onSettingsClick) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
        )
      }
    }

    Button(onClick = { viewModel.addServer() }) {
      Text("Add Server")
    }

    error?.let {
      Text(
          text = it,
          style = MaterialTheme.typography.bodyLarge,
      )
    }

    Column {
      Text(
          text = "Saved Servers",
          style = MaterialTheme.typography.titleLarge,
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (savedServers.isEmpty()) {
        Text(text = "No saved servers")
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          savedServers.forEach { server ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Button(
                  onClick = {
                    viewModel.selectServer(server, onServerConnected)
                  },
                  modifier = Modifier.weight(1f),
              ) {
                Column {
                  Text(server.server.name ?: server.server.ipAddress)

                  Text(
                      server.server.ipAddress,
                      style = MaterialTheme.typography.bodySmall,
                  )
                }
              }

              Button(
                  onClick = {
                    viewModel.editServer(server)
                  },
                  modifier = Modifier.width(80.dp),
              ) {
                Text("Edit")
              }

              Button(
                  onClick = {
                    viewModel.deleteServer(server)
                  },
                  modifier = Modifier.width(80.dp),
              ) {
                Text("Delete")
              }
            }
          }
        }
      }
    }
  }
}
