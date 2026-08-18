package com.silversky.core.utils

import com.silversky.core.logger.Logger
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext

class NetworkUtils {
    companion object {
        fun resolveHostName(logger: Logger, ip: String): String? {
            return try {
                val context =
                    BaseContext(
                        PropertyConfiguration(System.getProperties())
                    ).withGuestCrendentials()

                val addresses =
                    context.nameServiceClient.getNbtAllByAddress(ip)

                addresses
                    .firstOrNull { address ->
                        address.nameType == 0x00 &&
                                !address.isGroupAddress(context)
                    }
                    ?.name
                    ?.name
            } catch (e: Exception) {
                logger.debug(
                    "Failed to resolve NetBIOS name for $ip: ${e.message}"
                )
                null
            }
        }
    }
}