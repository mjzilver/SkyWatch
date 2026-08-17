package com.silversky.core.smb

import com.silversky.core.logger.Logger

class SmbClient(
    private val logger: Logger
) {
    fun connect(server: String) {
        logger.info("Connecting to $server")
    }
}