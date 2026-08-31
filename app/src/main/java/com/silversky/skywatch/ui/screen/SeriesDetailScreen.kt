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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.silversky.skywatch.ui.component.PlaybackStatus
import com.silversky.skywatch.ui.component.ScreenHeader
import com.silversky.skywatch.ui.component.StatusIcon
import com.silversky.skywatch.ui.component.getPlaybackStatus
import com.silversky.skywatch.ui.viewmodel.SeriesDetailViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    viewModel: SeriesDetailViewModel,
    onEpisodeSelected: () -> Unit,
    onBack: () -> Unit,
) {
  val episodes = viewModel.episodes
  val title = viewModel.title

  var expandedSeason by remember { mutableStateOf<Int?>(null) }

  BackHandler {
    onBack()
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(48.dp),
  ) {
    ScreenHeader(
        title = title,
        subtitle = "Select an episode",
        onBack = onBack,
    )

    Spacer(modifier = Modifier.height(32.dp))

    val seasons = episodes.groupBy { it.season }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      seasons.forEach { (seasonNumber, seasonEpisodes) ->
        val seasonStatuses = seasonEpisodes.map {
          getPlaybackStatus(viewModel.episodeStates[it.entryPath])
        }
        val seasonAggregateStatus =
            when {
              seasonStatuses.all { it == PlaybackStatus.Finished } -> PlaybackStatus.Finished
              seasonStatuses.any {
                it == PlaybackStatus.InProgress || it == PlaybackStatus.Finished
              } -> PlaybackStatus.InProgress
              else -> PlaybackStatus.NotStarted
            }

        item(key = "season_$seasonNumber") {
          Button(
              onClick = {
                expandedSeason = if (expandedSeason == seasonNumber) null else seasonNumber
              },
              modifier = Modifier.fillMaxWidth(),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              StatusIcon(status = seasonAggregateStatus)
              Spacer(modifier = Modifier.width(12.dp))
              Text(text = "Season $seasonNumber")
            }
          }
        }

        if (expandedSeason == seasonNumber) {
          items(
              items = seasonEpisodes,
              key = { "ep_${it.season}_${it.episode}_${it.entryPath}" },
          ) { ep ->
            val status = getPlaybackStatus(viewModel.episodeStates[ep.entryPath])

            Button(
                onClick = { viewModel.selectEpisode(ep, onEpisodeSelected) },
                modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                StatusIcon(status = status)
                Spacer(modifier = Modifier.width(12.dp))
                val episodeText = buildString {
                  append("Episode ${ep.episode}")
                  ep.episodeName?.let { append(": $it") }
                  ep.edition?.let { append(" [${it.uppercase()}]") }
                }
                Text(text = episodeText)
              }
            }
          }
        }
      }
    }
  }
}
