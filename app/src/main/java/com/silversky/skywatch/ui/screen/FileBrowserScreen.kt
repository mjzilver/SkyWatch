package com.silversky.skywatch.ui.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.silversky.core.smb.SmbEntry
import com.silversky.skywatch.ui.component.EmptyMessage
import com.silversky.skywatch.ui.component.ErrorMessage
import com.silversky.skywatch.ui.component.LoadingMessage
import com.silversky.skywatch.ui.component.ScreenHeader
import com.silversky.skywatch.ui.viewmodel.FileBrowserViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onFileSelected: (SmbEntry) -> Unit,
    onBack: () -> Unit,
) {
  val entries = viewModel.entries
  val resumeEntries = viewModel.resumeEntries
  val loading = viewModel.loading
  val error = viewModel.error
  val currentPath = viewModel.currentPath
  val shareName = viewModel.shareName ?: ""

  BackHandler {
    viewModel.goBack(onBack)
  }

  LaunchedEffect(Unit) {
    viewModel.loadEntries()
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
        onBack = { viewModel.goBack(onBack) },
    )

    Spacer(modifier = Modifier.height(32.dp))

    when {
      loading -> {
        LoadingMessage()
      }

      error != null -> {
        ErrorMessage(error)
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
                    (progress.completed ||
                        (progress.duration > 0 && progress.position >= progress.duration * 0.90))

            val hasResumePosition = progress != null && !hasFinished

            FileEntryButton(
                entry = entry,
                hasResumePosition = hasResumePosition,
                hasFinished = hasFinished,
                onClick = {
                  if (entry.isDirectory) {
                    viewModel.navigateTo(entry.path)
                  } else {
                    viewModel.selectFile(entry, onFileSelected)
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
