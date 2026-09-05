package com.silversky.skywatch.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.silversky.skywatch.ui.component.EmptyMessage
import com.silversky.skywatch.ui.component.LoadingMessage
import com.silversky.skywatch.ui.component.ScreenHeader
import com.silversky.skywatch.ui.viewmodel.SharesViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ShareScreen(
    viewModel: SharesViewModel,
    onShareSelected: () -> Unit,
    onBack: () -> Unit,
) {
  val shares = viewModel.shares
  val loading = viewModel.loading
  val server = viewModel.server

  BackHandler {
    onBack()
  }

  LaunchedEffect(Unit) {
    viewModel.loadShares()
  }

  Column(modifier = Modifier.fillMaxSize().padding(48.dp)) {
    ScreenHeader(
        title = server?.name ?: server?.ipAddress ?: "Disconnected",
        subtitle = "Select a share",
        onBack = onBack,
    )

    Spacer(modifier = Modifier.height(32.dp))

    when {
      server == null -> {
        EmptyMessage("Disconnected from server.")
      }

      loading -> {
        LoadingMessage("Loading shares...")
      }

      shares.isEmpty() -> {
        EmptyMessage("No SMB shares found.")
      }

      else -> {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          shares.forEach { share ->
            Button(
                onClick = {
                  viewModel.selectShare(share) { onShareSelected() }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(
                  imageVector = Icons.Outlined.Folder,
                  contentDescription = null,
              )

              Spacer(modifier = Modifier.width(12.dp))

              Text(text = share.name)
            }
          }
        }
      }
    }
  }
}
