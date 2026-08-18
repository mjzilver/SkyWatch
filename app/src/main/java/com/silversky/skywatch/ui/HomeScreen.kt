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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.model.SavedServer

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    servers: List<SmbServer>,
    scanning: Boolean,
    error: String?,
    onServerClick: (SmbServer) -> Unit,
    onAddServer: () -> Unit,
    onScanNetwork: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 72.dp,
                vertical = 48.dp
            ),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column {
            Text(
                text = "SKYWATCH",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "SMB MEDIA PLAYER",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onScanNetwork,
                enabled = !scanning
            ) {
                Text(
                    if (scanning) {
                        "Scanning..."
                    } else {
                        "Scan Network"
                    }
                )
            }

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Button(
                onClick = onAddServer,
                enabled = !scanning
            ) {
                Text("Add Server")
            }
        }

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Column {
            Text(
                text = "Network Servers",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (servers.isEmpty()) {
                Text(
                    text = if (scanning) {
                        "Searching for SMB servers..."
                    } else {
                        "No SMB servers found."
                    }
                )
            } else {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    servers.forEach { server ->
                        Button(
                            onClick = {
                                onServerClick(server)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !scanning
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Text(
                                    server.name
                                        ?: server.ipAddress
                                )

                                Text(
                                    server.ipAddress
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}