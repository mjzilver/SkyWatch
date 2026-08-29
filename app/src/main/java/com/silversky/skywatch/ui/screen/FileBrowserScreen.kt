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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.silversky.core.model.EpisodeInfo
import com.silversky.core.model.MovieInfo
import com.silversky.core.model.SmbEntry
import com.silversky.core.model.SmbEntryType
import com.silversky.skywatch.ui.component.EmptyMessage
import com.silversky.skywatch.ui.component.ErrorMessage
import com.silversky.skywatch.ui.component.LoadingMessage
import com.silversky.skywatch.ui.component.MovieVersionDialog
import com.silversky.skywatch.ui.component.ScreenHeader
import com.silversky.skywatch.ui.viewmodel.BrowserTab
import com.silversky.skywatch.ui.viewmodel.FileBrowserViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onFileSelected: (SmbEntry) -> Unit,
    onSeriesSelected: () -> Unit,
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

    TabRow(
        selectedTabIndex = viewModel.selectedTab.ordinal,
    ) {
      BrowserTab.entries.forEach { tab ->
        Tab(
            selected = viewModel.selectedTab == tab,
            onFocus = { viewModel.selectTab(tab) },
            onClick = { viewModel.selectTab(tab) },
        ) {
          Text(
              text = tab.name,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (viewModel.selectedTab == BrowserTab.Folders) {
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
                    if (entry.type == SmbEntryType.Directory) {
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
    } else {
      MediaList(
          viewModel = viewModel,
          onMovieSelected = onFileSelected,
          onSeriesSelected = onSeriesSelected,
      )
    }

    viewModel.movieVersionsToPick?.let { versions ->
      MovieVersionDialog(
          title = versions.firstOrNull()?.title ?: "Select Version",
          versions = versions,
          onDismiss = { viewModel.dismissMovieVersionPicker() },
          onVersionSelected = { version ->
            viewModel.dismissMovieVersionPicker()
            viewModel.selectFile(
                SmbEntry(
                    name = version.title,
                    path = version.entryPath,
                    type = SmbEntryType.File,
                    shareName = viewModel.shareName ?: "",
                ),
                onFileSelected,
            )
          },
      )
    }
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaList(
    viewModel: FileBrowserViewModel,
    onMovieSelected: (SmbEntry) -> Unit,
    onSeriesSelected: () -> Unit,
) {
  val mediaItems = viewModel.mediaItems
  val isScanning = viewModel.isScanning

  if (mediaItems.isEmpty() && isScanning) {
    LoadingMessage("Scanning media...")
    return
  }

  if (mediaItems.isEmpty()) {
    EmptyMessage("No media found.")
    return
  }

  val grouped = mediaItems.groupBy { it.title to it.year }
  val keys = remember(grouped) { grouped.keys.toList() }

  LazyColumn(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(
        items = keys,
        key = { "${it.first}_${it.second}" },
    ) { (title, year) ->
      val itemsForGroup = grouped[title to year] ?: emptyList()
      val episodes = itemsForGroup.filterIsInstance<EpisodeInfo>()
      val movies = itemsForGroup.filterIsInstance<MovieInfo>()

      Button(
          onClick = {
            if (episodes.isNotEmpty()) {
              viewModel.startSeriesSelection(title, onSeriesSelected)
            } else if (movies.size == 1) {
              val first = movies.first()
              viewModel.selectFile(
                  SmbEntry(
                      name = first.title,
                      path = first.entryPath,
                      type = SmbEntryType.File,
                      shareName = viewModel.shareName ?: "",
                  ),
                  onMovieSelected,
              )
            } else if (movies.size > 1) {
              viewModel.pickMovieVersion(movies)
            }
          },
          modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = if (year != null) "$title ($year)" else title)

          Spacer(modifier = Modifier.weight(1f))

          if (movies.size > 1) {
            Text(
                text = "${movies.size} versions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else if (episodes.isNotEmpty()) {
            val seasonCount = episodes.distinctBy { it.season }.size
            Text(
                text = "$seasonCount seasons",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                entry.type == SmbEntryType.Directory -> Icons.Outlined.Folder
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
