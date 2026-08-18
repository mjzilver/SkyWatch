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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.tv.material3.Text
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    client: SmbClient,
    server: SmbServer,
    shareName: String,
    logger: Logger,
    onFileSelected: (SmbEntry) -> Unit,
    onBack: () -> Unit
) {
    var currentPath by remember {
        mutableStateOf("")
    }

    var entries by remember {
        mutableStateOf<List<SmbEntry>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    fun goBackDirectory() {
        if (currentPath.isEmpty()) {
            onBack()
            return
        }

        currentPath = parentPath(currentPath)
    }

    BackHandler {
        goBackDirectory()
    }

    LaunchedEffect(
        shareName,
        currentPath
    ) {
        loading = true
        error = null

        try {
            logger.debug(
                "Listing //$shareName/$currentPath"
            )

            withContext(Dispatchers.IO) {
                entries = client.list(
                    shareName = shareName,
                    path = currentPath
                )
            }

            logger.debug(
                "Found ${entries.size} entries"
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to list //$shareName/$currentPath",
                e
            )

            entries = emptyList()

            error = e.message
                ?: "Failed to load directory"
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        ScreenHeader(
            title = shareName,
            subtitle = if (currentPath.isEmpty()) {
                "/"
            } else {
                "/$currentPath"
            },
            onBack = ::goBackDirectory
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        when {
            loading -> {
                LoadingMessage()
            }

            error != null -> {
                ErrorMessage(error!!)
            }

            entries.isEmpty() -> {
                EmptyMessage(
                    "This folder is empty."
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = entries,
                        key = { entry -> entry.path }
                    ) { entry ->
                        FileEntryButton(
                            entry = entry,
                            onClick = {
                                if (entry.isDirectory) {
                                    currentPath = entry.path
                                } else {
                                    onFileSelected(entry)
                                }
                            }
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
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (entry.isDirectory) {
                    "📁  ${entry.name}"
                } else {
                    "▶  ${entry.name}"
                }
            )
        }
    }
}

private fun parentPath(
    path: String
): String {
    val normalized = path.trimEnd('\\')

    val index = normalized.lastIndexOf('\\')

    return if (index < 0) {
        ""
    } else {
        normalized.substring(
            0,
            index
        )
    }
}