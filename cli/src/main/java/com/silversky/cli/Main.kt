package com.silversky.cli

import com.silversky.cli.logger.ConsoleLogger
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

fun main() {
  Security.addProvider(BouncyCastleProvider())
  val logger = ConsoleLogger()

  val cli = Cli(logger)

  cli.run()
}
