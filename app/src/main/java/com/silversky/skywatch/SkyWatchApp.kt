package com.silversky.skywatch

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.silversky.core.client.SmbClient
import com.silversky.core.logger.Logger
import com.silversky.core.smb.SmbEntry
import com.silversky.core.smb.SmbScanner
import com.silversky.core.smb.SmbServer
import com.silversky.skywatch.model.SavedServer
import com.silversky.skywatch.ui.FileBrowserScreen
import com.silversky.skywatch.ui.HomeScreen
import com.silversky.skywatch.ui.PlayerScreen
import com.silversky.skywatch.ui.ServerConnectionInput
import com.silversky.skywatch.ui.ServerDialog
import com.silversky.skywatch.ui.ShareScreen
import com.silversky.skywatch.utils.ServerPersistenceManager
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
    logger: Logger,
    context: Context
) {
    val scope = rememberCoroutineScope()
    val persistenceManager = ServerPersistenceManager(context)
    val prefs = context.getSharedPreferences("skywatch_prefs", Context.MODE_PRIVATE)

    var screen by remember {
        mutableStateOf(Screen.HOME)
    }

    var showServerDialog by remember {
        mutableStateOf(false)
    }

    var servers by remember {
        mutableStateOf<List<SmbServer>>(emptyList())
    }

    // Cache for scanned servers
    var cachedServers by remember {
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

    // Add scanning state variable
    var scanning by remember {
        mutableStateOf(false)
    }

    // Load saved servers on startup
    val loadedServers = persistenceManager.loadServers()
    if (loadedServers.isNotEmpty() && cachedServers.isEmpty()) {
        cachedServers = loadedServers.map { it.server }
    }

    fun loadSavedServers() {
        val loadedServers = persistenceManager.loadServers()
        // We could update the UI here to show saved servers, but for now we'll just keep them in memory
    }
    fun getServerList(): List<SmbServer> {
        return cachedServers
    }
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

        scope.launch(Dispatchers.IO) {
            val client = SmbClient(logger)

            try {
                client.connect(
                    server = server,
                    username = input.username,
                    password = input.password
                )

                withContext(Dispatchers.Main) {
                    smbClient?.close()
                    smbClient = client
                    selectedServer = server

                    val saved = SavedServer(
                        server = server,
                        username = input.username,
                        password = input.password
                    )

                    // Save the server to persistent storage
                    val existingServers = persistenceManager.loadServers()
                    val updatedServers = existingServers + saved
                    persistenceManager.saveServers(updatedServers)

                    showServerDialog = false
                    screen = Screen.SHARES
                }
            } catch (e: Exception) {
                logger.error(
                    "Failed to connect",
                    e
                )

                withContext(Dispatchers.Main) {
                    client.close()

                    // TODO: surface connection error in dialog.
                }
            }
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

                val now = System.currentTimeMillis()
                val cachedServersTimestamp = prefs.getLong("cached_servers_timestamp", 0)
                val isCacheValid = now - cachedServersTimestamp < 5 * 60 * 1000 // 5 minutes

                val found = if (isCacheValid) {
                    logger.info("Using cached servers")
                    cachedServers
                } else {
                    val scanned = SmbScanner().scanNetwork(logger)
                    scanned.forEach { f ->
                        logger.info("Found server ${f.name} with ip ${f.ipAddress}")
                    }
                    logger.info(
                        "Found ${scanned.size} SMB servers"
                    )
                    scanned
                }

                // Merge cached servers with saved servers to show all available servers
                val savedServers = persistenceManager.loadServers()
                val allServers = found + savedServers.map { it.server }
                
                // Remove duplicates by IP address
                val uniqueServers = allServers.distinctBy { it.ipAddress }

                withContext(Dispatchers.Main) {
                    servers = uniqueServers
                    scanning = false
                    
                    // Cache the servers for future use
                    if (!isCacheValid) {
                        cachedServers = uniqueServers
                        prefs.edit().putLong("cached_servers_timestamp", now).apply()
                    }
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
                savedServers = loadedServers,
                error = scanError,
                onServerClick = { savedServer ->
                    logger.info("Saved server clicked")
                    selectServer(savedServer)
                },
                onEditServer = { savedServer ->
                    // For now, we'll just show the dialog again with existing data
                    // In a real implementation, this would open an edit dialog
                    logger.info("Edit server clicked")
                },
                onAddServer = {
                    showServerDialog = true
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