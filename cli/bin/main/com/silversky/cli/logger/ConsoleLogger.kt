package com.silversky.cli.logger

import com.silversky.core.logger.Logger

class ConsoleLogger : Logger {
  override fun debug(message: String) {
    println("[DEBUG] $message")
  }

  override fun info(message: String) {
    println("[INFO] $message")
  }

  override fun warn(message: String) {
    println("[WARN] $message")
  }

  override fun error(message: String, throwable: Throwable?) {
    println("[ERROR] $message")
    throwable?.printStackTrace()
  }
}
