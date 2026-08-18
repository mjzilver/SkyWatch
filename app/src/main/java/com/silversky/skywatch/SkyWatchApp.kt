package com.silversky.skywatch

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.silversky.skywatch.ui.PlayerScreen
import com.silversky.skywatch.ui.ScanDialog
import com.silversky.skywatch.ui.ScanResult
import com.silversky.skywatch.ui.ServerConnectionInput
import com.silversky.skywatch.ui.ServerDialog
import com.silversky.skywatch.ui.ShareScreen
import com.silversky.skywatch.utils.ServerPersistenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Screen {
    HOME, SHARES, BROWSER, PLAYER
}

@Composable
fun SkyWatchApp(
    logger: Logger, context: Context
) {
    val scope = rememberCoroutineScope()
    val persistenceManager = remember {
        ServerPersistenceManager(context)
    }
    val prefs = remember {
        context.getSharedPreferences("skywatch_prefs", Context.MODE_PRIVATE)
    }

    var screen by remember { mutableStateOf(Screen.HOME) }

    var showServerDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }

    var cachedServers by remember {
        mutableStateOf<List<SmbServer>>(emptyList())
    }

    var scanResults by remember {
        mutableStateOf<List<ScanResult>>(emptyList())
    }

    var scanError by remember {
        mutableStateOf<String?>(null)
    }

    var scanning by remember {
        mutableStateOf(false)
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

    /*
     * Load persisted servers once when the composable enters composition.
     */
    LaunchedEffect(Unit) {
        cachedServers = withContext(Dispatchers.IO) {
            persistenceManager.getServers().map { it.server }
        }
    }

    /*
     * Connect to an SMB server.
     */
    fun connect(input: ServerConnectionInput) {
        val server = SmbServer(
            name = input.name, ipAddress = input.address
        )

        logger.info("Connecting to ${server.ipAddress}")

        scope.launch(Dispatchers.IO) {
            val client = SmbClient(logger)

            try {
                client.connect(
                    server = server, username = input.username, password = input.password
                )

                val saved = SavedServer(
                    server = server, username = input.username, password = input.password
                )

                val existingServers = persistenceManager.getServers()

                if (existingServers.none { it.server.ipAddress == server.ipAddress }) {
                    persistenceManager.saveServer(saved)
                }
                val servers = persistenceManager.getServers()

                withContext(Dispatchers.Main) {
                    smbClient?.close()

                    smbClient = client
                    selectedServer = server
                    selectedShare = null
                    selectedFile = null

                    cachedServers = servers.map { it.server }

                    showServerDialog = false
                    screen = Screen.SHARES
                }
            } catch (e: Exception) {
                logger.error(
                    "Failed to connect to ${server.ipAddress}", e
                )

                client.close()

                withContext(Dispatchers.Main) {
                    scanError = e.message ?: "Connection failed"
                }
            }
        }
    }

    /*
     * Connect using a previously saved server.
     */
    fun selectServer(savedServer: SavedServer) {
        connect(
            ServerConnectionInput(
                name = savedServer.server.name,
                address = savedServer.server.ipAddress,
                username = savedServer.username,
                password = savedServer.password
            )
        )
    }

    /*
     * Scan the local network for SMB servers.
     */
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
                val cachedTimestamp = prefs.getLong("cached_servers_timestamp", 0)

                val cacheValid = now - cachedTimestamp < 5 * 60 * 1000

                val discoveredServers = if (cacheValid && cachedServers.isNotEmpty()) {
                    logger.info("Using cached servers")
                    cachedServers
                } else {
                    SmbScanner().scanNetwork(logger).also { servers ->
                        servers.forEach { server ->
                            logger.info(
                                "Found server ${server.name} " + "with ip ${server.ipAddress}"
                            )
                        }

                        logger.info(
                            "Found ${servers.size} SMB servers"
                        )
                    }
                }

                /*
                 * Also include saved servers.
                 */
                val savedServers = persistenceManager.getServers().map { it.server }

                val uniqueServers = (discoveredServers + savedServers).distinctBy { it.ipAddress }

                withContext(Dispatchers.Main) {
                    cachedServers = uniqueServers
                    scanning = false

                    if (!cacheValid) {
                        prefs.edit().putLong(
                            "cached_servers_timestamp", now
                        ).apply()
                    }
                }
            } catch (e: Exception) {
                logger.error(
                    "SMB network scan failed", e
                )

                withContext(Dispatchers.Main) {
                    scanning = false
                    scanError = e.message ?: "Network scan failed"
                }
            }
        }
    }

    /*
     * Add a server without connecting to it.
     */
    fun addServer(input: ServerConnectionInput) {
        val server = SmbServer(
            name = input.name, ipAddress = input.address
        )

        logger.info("Adding server ${server.ipAddress}")

        scope.launch(Dispatchers.IO) {
            try {
                persistenceManager.saveServer(
                    SavedServer(
                        server = server, username = input.username, password = input.password
                    )
                )

                val servers = persistenceManager.getServers()

                withContext(Dispatchers.Main) {
                    cachedServers = servers.map { it.server }
                }
            } catch (e: Exception) {
                logger.error(
                    "Failed to add server", e
                )
            }
        }
    }

    /*
     * Update an existing saved server.
     */
    fun updateServer(
        input: ServerConnectionInput, oldServer: SmbServer
    ) {
        val server = SmbServer(
            name = input.name, ipAddress = input.address
        )

        logger.info("Updating server ${server.ipAddress}")

        scope.launch(Dispatchers.IO) {
            try {
                persistenceManager.updateServer(
                    SavedServer(
                        server = server, username = input.username, password = input.password
                    ), oldServer
                )

                val servers = persistenceManager.getServers()

                withContext(Dispatchers.Main) {
                    cachedServers = servers.map { it.server }
                }
            } catch (e: Exception) {
                logger.error(
                    "Failed to update server", e
                )
            }
        }
    }

    /*
     * Delete a saved server.
     */
    fun deleteServer(server: SmbServer) {
        logger.info("Deleting server ${server.ipAddress}")

        scope.launch(Dispatchers.IO) {
            try {
                persistenceManager.deleteServer(server)

                val servers = persistenceManager.getServers()

                withContext(Dispatchers.Main) {
                    cachedServers = servers.map { it.server }
                }
            } catch (e: Exception) {
                logger.error(
                    "Failed to delete server", e
                )
            }
        }
    }

    /*
     * Handle Android back navigation.
     */
    BackHandler(
        enabled = screen != Screen.HOME
    ) {
        when (screen) {
            Screen.PLAYER -> {
                selectedFile = null
                screen = Screen.BROWSER
            }

            Screen.BROWSER -> {
                selectedShare = null
                screen = Screen.SHARES
            }

            Screen.SHARES -> {
                smbClient?.close()
                smbClient = null
                selectedServer = null
                selectedShare = null
                selectedFile = null
                screen = Screen.HOME
            }

            Screen.HOME -> Unit
        }
    }

    /*
     * Screen navigation.
     */
    when (screen) {

        Screen.HOME -> {
            val savedServers = persistenceManager.getServers()

            HomeScreen(
                savedServers = savedServers, error = scanError,

                onServerClick = { savedServer ->
                    logger.info("Saved server clicked")
                    selectServer(savedServer)
                },

                onEditServer = { savedServer ->
                    logger.info(
                        "Edit server clicked: " + savedServer.server.ipAddress
                    )

                    // TODO: Open edit dialog.
                },

                onAddServer = {
                    showServerDialog = true
                },

                onDeleteServer = { savedServer ->
                    scope.launch(Dispatchers.IO) {
                        persistenceManager.deleteServer(savedServer.server)

                        val servers = persistenceManager.getServers()

                        withContext(Dispatchers.Main) {
                            cachedServers = servers.map { it.server }
                        }
                    }
                })
        }

        Screen.SHARES -> {
            val client = smbClient
            val server = selectedServer

            if (client != null && server != null) {
                ShareScreen(
                    client = client, server = server, logger = logger,

                    onShareSelected = { share ->
                        selectedShare = share
                        screen = Screen.BROWSER
                    },

                    onBack = {
                        scope.launch(Dispatchers.IO) {
                            client.close()
                        }
                        smbClient = null
                        selectedServer = null
                        selectedShare = null
                        selectedFile = null
                        screen = Screen.HOME
                    })
            }
        }

        Screen.BROWSER -> {
            val client = smbClient
            val server = selectedServer
            val share = selectedShare

            if (client != null && server != null && share != null) {
                FileBrowserScreen(
                    client = client, server = server, shareName = share, logger = logger,

                    onFileSelected = { file ->
                        selectedFile = file
                        screen = Screen.PLAYER
                    },

                    onBack = {
                        selectedShare = null
                        screen = Screen.SHARES
                    })
            }
        }

        Screen.PLAYER -> {
            val client = smbClient
            val share = selectedShare
            val file = selectedFile

            if (client != null && share != null && file != null) {
                PlayerScreen(
                    client = client, shareName = share, file = file, logger = logger,

                    onBack = {
                        selectedFile = null
                        screen = Screen.BROWSER
                    })
            }
        }
    }

    /*
     * Add server dialog.
     */
    if (showServerDialog) {
        ServerDialog(
            onDismiss = {
                showServerDialog = false
            }, onConnect = ::connect, onScan = ::scanNetwork
        )
    }

    /*
     * Network scan results.
     */
    if (showScanDialog && scanResults.isNotEmpty()) {
        ScanDialog(
            onDismiss = {
                showScanDialog = false
            },

            onServerSelected = { result ->
                addServer(
                    ServerConnectionInput(
                        name = result.name, address = result.ip, username = "", password = ""
                    )
                )

                showScanDialog = false
            },

            servers = scanResults
        )
    }
}