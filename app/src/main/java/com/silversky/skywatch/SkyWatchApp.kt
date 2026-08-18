package com.silversky.skywatch

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.model.SavedServer
import com.silversky.skywatch.ui.FileBrowserScreen
import com.silversky.skywatch.ui.HomeScreen
import com.silversky.skywatch.ui.HomeViewModel
import com.silversky.skywatch.ui.PlayerScreen
import com.silversky.skywatch.ui.ServerConnectionInput
import com.silversky.skywatch.ui.ServerDialog
import com.silversky.skywatch.ui.ShareScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen {
    HOME,
    SHARES,
    BROWSER,
    PLAYER
}

@Composable
fun SkyWatchApp(
    logger: Logger
) {
    val scope = rememberCoroutineScope()

    var screen by remember {
        mutableStateOf(Screen.HOME)
    }

    var showServerDialog by remember {
        mutableStateOf(false)
    }

    var servers by remember {
        mutableStateOf<List<SmbServer>>(emptyList())
    }

    var scanError by remember {
        mutableStateOf<String?>(null)
    }

    var selectedServer by remember {
        mutableStateOf<SmbServer?>(null)
    }

    var selectedShare by remember {
        mutableStateOf<String?>(null)
    }

    var selectedFile by remember {
        mutableStateOf<SmbEntry?>(null)
    }

    var smbClient by remember {
        mutableStateOf<SmbClient?>(null)
    }

    var scanning = false;

    fun connect(
        input: ServerConnectionInput
    ) {
        val server = SmbServer(
            name = input.name,
            ipAddress = input.address
        )

        logger.info(
            "Connecting to ${server.ipAddress}"
        )

        val client = SmbClient(logger)

        try {
            client.connect(
                server = server,
                username = input.username,
                password = input.password
            )

            smbClient?.close()

            smbClient = client
            selectedServer = server

            val saved = SavedServer(
                server = server,
                username = input.username,
                password = input.password
            )


            showServerDialog = false
            screen = Screen.SHARES

        } catch (e: Exception) {
            logger.error(
                "Failed to connect",
                e
            )

            client.close()

            // TODO: surface connection error in dialog.
        }
    }

    fun selectServer(
        savedServer: SavedServer
    ) {
        connect(
            ServerConnectionInput(
                name = savedServer.server.name,
                address = savedServer.server.ipAddress,
                username = savedServer.username,
                password = savedServer.password
            )
        )
    }

    BackHandler(
        enabled = screen != Screen.HOME
    ) {
        when (screen) {
            Screen.PLAYER -> {
                screen = Screen.BROWSER
            }

            Screen.BROWSER -> {
                screen = Screen.SHARES
            }

            Screen.SHARES -> {
                smbClient?.close()
                smbClient = null
                selectedServer = null
                selectedShare = null
                screen = Screen.HOME
            }

            Screen.HOME -> Unit
        }
    }

    fun scanNetwork() {
        if (scanning) {
            return
        }

        scanning = true
        scanError = null

        scope.launch(Dispatchers.IO) {
            try {
                logger.info("Starting SMB network scan")

                val found = SmbScanner().scanNetwork(logger)

                found.forEach { f ->
                    logger.info("Found server ${f.name} with ip ${f.ipAddress}")
                }

                logger.info(
                    "Found ${found.size} SMB servers"
                )

                withContext(Dispatchers.Main) {
                    servers = found
                    scanning = false
                }
            } catch (e: Exception) {
                logger.error(
                    "SMB network scan failed",
                    e
                )

                withContext(Dispatchers.Main) {
                    scanError =
                        e.message ?: "Network scan failed"
                    scanning = false
                }
            }
        }
    }

    when (screen) {

        Screen.HOME -> {
            HomeScreen(
                servers = servers,
                scanning = scanning,
                error = scanError,
                onServerClick = { server ->
                    logger.info("Server clicked")
                },
                onAddServer = {
                    showServerDialog = true
                },
                onScanNetwork = {
                    scanNetwork()
                }
            )
        }

        Screen.SHARES -> {
            val client = smbClient
            val server = selectedServer

            if (client != null && server != null) {
                ShareScreen(
                    client = client,
                    server = server,
                    logger = logger,
                    onShareSelected = { share ->
                        selectedShare = share
                        screen = Screen.BROWSER
                    },
                    onBack = {
                        client.close()
                        smbClient = null
                        selectedServer = null
                        screen = Screen.HOME
                    }
                )
            }
        }

        Screen.BROWSER -> {
            val client = smbClient
            val server = selectedServer
            val share = selectedShare

            if (
                client != null &&
                server != null &&
                share != null
            ) {
                FileBrowserScreen(
                    client = client,
                    server = server,
                    shareName = share,
                    logger = logger,
                    onFileSelected = { file ->
                        selectedFile = file
                        screen = Screen.PLAYER
                    },
                    onBack = {
                        selectedShare = null
                        screen = Screen.SHARES
                    }
                )
            }
        }

        Screen.PLAYER -> {
            val client = smbClient
            val share = selectedShare
            val file = selectedFile

            if (
                client != null &&
                share != null &&
                file != null
            ) {
                PlayerScreen(
                    client = client,
                    shareName = share,
                    file = file,
                    logger = logger,
                    onBack = {
                        selectedFile = null
                        screen = Screen.BROWSER
                    }
                )
            }
        }
    }

    if (showServerDialog) {
        ServerDialog(
            onDismiss = {
                showServerDialog = false
            },
            onConnect = ::connect
        )
    }
}