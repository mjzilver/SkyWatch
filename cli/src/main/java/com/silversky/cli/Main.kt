package com.silversky.cli

import com.silversky.cli.logger.ConsoleLogger
import com.silversky.core.smb.SmbClient

fun main() {
    val logger = ConsoleLogger()
    val smb = SmbClient(logger)

    print("Enter SMB server: ")
    val server = readln()

    smb.connect(server)
}