package com.silversky.core.utils

import java.net.InetAddress

class NetworkUtils {
    companion object {
        fun resolveHostName(ip: String): String? =
            try {
                InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip }
            } catch (_: Exception) {
                null
            }
    }
}