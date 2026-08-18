package com.silversky.core.utils

import com.silversky.core.logger.Logger
import java.net.InetAddress

class NetworkUtils {
  companion object {
    fun resolveHostName(logger: Logger, ip: String): String? =
        try {
          InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
        } catch (_: Exception) {
          logger.warn("Could not resolve name for $ip")
          null
        }
  }
}
