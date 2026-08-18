package com.silversky.cli

import com.silversky.cli.logger.ConsoleLogger


fun main() {
    val logger = ConsoleLogger()

    val cli = Cli(logger)

    cli.run()
}