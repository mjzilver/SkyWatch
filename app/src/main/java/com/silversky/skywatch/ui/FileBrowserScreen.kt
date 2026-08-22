package com.silversky.skywatch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.persistence.PlaybackState
import com.silversky.skywatch.persistence.PlaybackStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    client: SmbClient,
    server: SmbServer,
    shareName: String,
    currentPath: String,
    logger: Logger,
    playbackStateStore: PlaybackStateStore,
    onPathChanged: (String) -> Unit,
    onFileSelected: (SmbEntry) -> Unit,
    onBack: () -> Unit,
) {
  var entries by remember {
    mutableStateOf<List<SmbEntry>>(emptyList())
  }

  var resumeEntries by remember {
    mutableStateOf<Map<String, PlaybackState>>(emptyMap())
  }

  var loading by remember {
    mutableStateOf(true)
  }

  var error by remember {
    mutableStateOf<String?>(null)
  }

  BackHandler {
    onBack()
  }

  LaunchedEffect(
      shareName,
      currentPath,
  ) {
    loading = true
    error = null

    try {
      entries =
          withContext(Dispatchers.IO) {
            client
                .list(
                    shareName = shareName,
                    path = currentPath,
                )
                .filter { !it.isHidden }
          }
    } catch (e: Exception) {
      logger.error(
          "Failed to list //$shareName/$currentPath",
          e,
      )

      entries = emptyList()
      error = e.message ?: "Failed to load directory"
    } finally {
      loading = false
    }
  }

  LaunchedEffect(
      shareName,
      currentPath,
      entries,
  ) {
    resumeEntries =
        withContext(Dispatchers.IO) {
          entries
              .filter { !it.isDirectory }
              .mapNotNull { entry ->
                val progress =
                    playbackStateStore.get(
                        ip = server.ipAddress,
                        share = shareName,
                        path = entry.path,
                    )

                progress?.let {
                  entry.path to it
                }
              }
              .toMap()
        }
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(48.dp),
  ) {
    ScreenHeader(
        title = shareName,
        subtitle =
            if (currentPath.isEmpty()) {
              "/"
            } else {
              "/$currentPath"
            },
        onBack = onBack,
    )

    Spacer(modifier = Modifier.height(32.dp))

    when {
      loading -> {
        LoadingMessage()
      }

      error != null -> {
        ErrorMessage(error!!)
      }

      entries.isEmpty() -> {
        EmptyMessage("This folder is empty.")
      }

      else -> {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(
              items = entries,
              key = { entry -> entry.path },
          ) { entry ->
            val progress = resumeEntries[entry.path]

            val hasFinished =
                progress != null &&
                    progress.duration > 0 &&
                    progress.position >= progress.duration * 0.90

            val hasResumePosition = progress != null && !hasFinished

            FileEntryButton(
                entry = entry,
                hasResumePosition = hasResumePosition,
                hasFinished = hasFinished,
                onClick = {
                  if (entry.isDirectory) {
                    onPathChanged(entry.path)
                  } else {
                    onFileSelected(entry)
                  }
                },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FileEntryButton(
    entry: SmbEntry,
    hasResumePosition: Boolean,
    hasFinished: Boolean,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
          imageVector =
              when {
                entry.isDirectory -> Icons.Outlined.Folder
                hasFinished -> Icons.Filled.Done
                hasResumePosition -> Icons.Filled.PlayCircle
                else -> Icons.Filled.PlayArrow
              },
          contentDescription = null,
      )

      Spacer(modifier = Modifier.width(12.dp))

      Text(text = entry.name)
    }
  }
}
