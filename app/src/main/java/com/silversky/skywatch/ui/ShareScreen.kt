package com.silversky.skywatch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ShareScreen(
    client: SmbClient,
    server: SmbServer,
    logger: Logger,
    onShareSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
  var shares by remember {
    mutableStateOf<List<String>>(emptyList())
  }

  var loading by remember {
    mutableStateOf(true)
  }

  var error by remember {
    mutableStateOf<String?>(null)
  }

  LaunchedEffect(Unit) {
    try {
      logger.debug("Loading shares from ${server.ipAddress}")

      shares =
          withContext(Dispatchers.IO) {
            client.listShares()
          }

      logger.info("Found ${shares.size} shares")
    } catch (e: Exception) {
      logger.error(
          "Failed to list SMB shares",
          e,
      )

      error = e.message ?: "Failed to load shares"
    } finally {
      loading = false
    }
  }

  Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
    ScreenHeader(
        title = server.name ?: server.ipAddress,
        subtitle = "Select a share",
        onBack = onBack,
    )

    Spacer(modifier = Modifier.height(32.dp))

    when {
      loading -> {
        LoadingMessage("Loading shares...")
      }

      error != null -> {
        ErrorMessage(error!!)
      }

      shares.isEmpty() -> {
        EmptyMessage("No SMB shares found.")
      }

      else -> {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          shares.forEach { share ->
            Button(
                onClick = {
                  logger.info("Selected share: $share")

                  onShareSelected(share)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
              Text("📁  $share")
            }
          }
        }
      }
    }
  }
}
