package com.silversky.skywatch.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.silversky.core.model.EpisodeInfo
import com.silversky.core.model.MovieInfo
import com.silversky.core.model.SmbEntry
import com.silversky.core.model.SmbEntryType
import com.silversky.skywatch.data.local.PlaybackState
import com.silversky.skywatch.model.BrowserTab
import com.silversky.skywatch.ui.component.AppErrorOverlay
import com.silversky.skywatch.ui.component.EmptyMessage
import com.silversky.skywatch.ui.component.LoadingMessage
import com.silversky.skywatch.ui.component.MovieVersionDialog
import com.silversky.skywatch.ui.component.PlaybackStatus
import com.silversky.skywatch.ui.component.ScreenHeader
import com.silversky.skywatch.ui.component.StatusIcon
import com.silversky.skywatch.ui.component.getPlaybackStatus
import com.silversky.skywatch.ui.viewmodel.FileBrowserViewModel
import com.silversky.skywatch.ui.viewmodel.MediaGroup

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onFileSelected: () -> Unit,
    onSeriesSelected: () -> Unit,
    onBack: () -> Unit,
) {
  val entries = viewModel.entries
  val resumeEntries = viewModel.resumeEntries
  val loading = viewModel.loading
  val currentPath = viewModel.currentPath
  val shareName = viewModel.shareName ?: ""

  val tabRowFocusRequester = remember { FocusRequester() }
  val upButtonFocusRequester = remember { FocusRequester() }
  val listFocusRequester = remember { FocusRequester() }
  var isTabRowFocused by remember { mutableStateOf(false) }

  val canNavigateUp = currentPath.isNotEmpty()

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
        onBack = { viewModel.goBack(onBack) },
        downFocusRequester = tabRowFocusRequester,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .onFocusChanged { isTabRowFocused = it.isFocused }
                .focusRequester(tabRowFocusRequester)
                .focusProperties {
                  down =
                      if (viewModel.selectedTab == BrowserTab.Folders && canNavigateUp) {
                        upButtonFocusRequester
                      } else {
                        listFocusRequester
                      }
                }
                .focusable()
                .onKeyEvent { event ->
                  if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                      Key.DirectionLeft -> {
                        val prevIndex = (viewModel.selectedTab.ordinal - 1).coerceAtLeast(0)
                        viewModel.selectTab(BrowserTab.entries[prevIndex])
                        true
                      }

                      Key.DirectionRight -> {
                        val nextIndex =
                            (viewModel.selectedTab.ordinal + 1).coerceAtMost(
                                BrowserTab.entries.size - 1
                            )
                        viewModel.selectTab(BrowserTab.entries[nextIndex])
                        true
                      }

                      else -> false
                    }
                  } else false
                },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      BrowserTab.entries.forEach { tab ->
        val isSelected = viewModel.selectedTab == tab

        Button(
            onClick = { viewModel.selectTab(tab) },
            modifier = Modifier.weight(1f).focusProperties { canFocus = false },
            scale = ButtonDefaults.scale(focusedScale = 1f),
            colors =
                ButtonDefaults.colors(
                    containerColor =
                        when {
                          isSelected && isTabRowFocused -> Color.White
                          isSelected -> Color.DarkGray
                          else -> Color.Transparent
                        },
                    contentColor =
                        if (isSelected && isTabRowFocused) {
                          Color.Black
                        } else {
                          Color.White
                        },
                ),
        ) {
          Text(
              text = tab.name,
              style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }

    if (viewModel.selectedTab == BrowserTab.Folders) {
      Spacer(modifier = Modifier.height(16.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
            onClick = { viewModel.up() },
            enabled = canNavigateUp,
            modifier =
                Modifier.focusRequester(upButtonFocusRequester).focusProperties {
                  up = tabRowFocusRequester
                  down = listFocusRequester
                },
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Up",
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Up")
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "/$currentPath",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    when (viewModel.selectedTab) {
      BrowserTab.Folders -> {
        when {
          loading -> {
            LoadingMessage()
          }

          entries.isEmpty() -> {
            EmptyMessage("This folder is empty.")
          }

          else -> {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().focusRequester(listFocusRequester),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(
                  items = entries,
                  key = { entry -> entry.path },
              ) { entry ->
                val isFirst = entry == entries.first()

                FileEntryButton(
                    entry = entry,
                    status = getPlaybackStatus(resumeEntries[entry.path]),
                    upFocusRequester =
                        if (isFirst && canNavigateUp) {
                          upButtonFocusRequester
                        } else {
                          null
                        },
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
      }

      BrowserTab.Movies -> {
        MediaList(
            groups = viewModel.movieGroups,
            resumeStates = viewModel.mediaResumeStates,
            isScanning = viewModel.isScanning,
            tabRowFocusRequester = tabRowFocusRequester,
            listFocusRequester = listFocusRequester,
            onClick = { group ->
              val versions = group.items.filterIsInstance<MovieInfo>()
              if (versions.size == 1) {
                viewModel.selectFile(
                    SmbEntry(
                        name = group.title,
                        path = versions.first().entryPath,
                        type = SmbEntryType.File,
                        shareName = viewModel.shareName ?: "",
                    ),
                    onFileSelected,
                )
              } else {
                viewModel.pickMovieVersion(versions)
              }
            },
            trailingContent = { group ->
              if (group.items.size > 1) {
                Text(
                    text = "${group.items.size} versions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            },
        )
      }

      BrowserTab.Series -> {
        MediaList(
            groups = viewModel.seriesGroups,
            resumeStates = viewModel.mediaResumeStates,
            isScanning = viewModel.isScanning,
            tabRowFocusRequester = tabRowFocusRequester,
            listFocusRequester = listFocusRequester,
            onClick = { group ->
              viewModel.startSeriesSelection(group.title, onSeriesSelected)
            },
            trailingContent = { group ->
              val seasonCount =
                  group.items.filterIsInstance<EpisodeInfo>().distinctBy { it.season }.size
              Text(
                  text = "$seasonCount seasons",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
        )
      }
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

    viewModel.error?.let { error ->
      AppErrorOverlay(
          error = error,
          onClose = { onBack() },
      )
    }
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaList(
    groups: List<MediaGroup>,
    resumeStates: Map<String, PlaybackState>,
    isScanning: Boolean,
    tabRowFocusRequester: FocusRequester,
    listFocusRequester: FocusRequester,
    onClick: (MediaGroup) -> Unit,
    trailingContent: @Composable (MediaGroup) -> Unit = {},
) {
  if (groups.isEmpty() && isScanning) {
    LoadingMessage("Scanning media...")
    return
  }

  if (groups.isEmpty()) {
    EmptyMessage("No media found.")
    return
  }

  LazyColumn(
      modifier =
          Modifier.fillMaxWidth().focusRequester(listFocusRequester).focusProperties {
            up = tabRowFocusRequester
          },
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(
        items = groups,
        key = { "${it.title}_${it.year}" },
    ) { group ->
      val statuses = group.items.map { getPlaybackStatus(resumeStates[it.entryPath]) }
      val aggregateStatus =
          when {
            statuses.all { it == PlaybackStatus.Finished } -> PlaybackStatus.Finished
            statuses.any { it == PlaybackStatus.InProgress || it == PlaybackStatus.Finished } ->
                PlaybackStatus.InProgress
            else -> PlaybackStatus.NotStarted
          }

      Button(
          onClick = { onClick(group) },
          modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          StatusIcon(status = aggregateStatus)

          Spacer(modifier = Modifier.width(12.dp))

          Text(
              text =
                  if (group.year != null) {
                    "${group.title} (${group.year})"
                  } else {
                    group.title
                  },
          )

          Spacer(modifier = Modifier.weight(1f))

          trailingContent(group)
        }
      }
    }
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FileEntryButton(
    entry: SmbEntry,
    status: PlaybackStatus,
    upFocusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      modifier =
          Modifier.fillMaxWidth().focusProperties {
            upFocusRequester?.let { up = it }
          },
  ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      if (entry.type == SmbEntryType.Directory) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
        )
      } else {
        StatusIcon(status = status)
      }

      Spacer(modifier = Modifier.width(12.dp))

      Text(text = entry.name)
    }
  }
}
