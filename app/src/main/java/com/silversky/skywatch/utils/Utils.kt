package com.silversky.skywatch.utils

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

internal fun formatTime(milliseconds: Long): String {
  if (milliseconds <= 0L) {
    return "00:00"
  }

  val totalSeconds = milliseconds / 1_000

  val hours = totalSeconds / 3_600
  val minutes = (totalSeconds % 3_600) / 60
  val seconds = totalSeconds % 60

  return if (hours > 0) {
    "%d:%02d:%02d"
        .format(
            hours,
            minutes,
            seconds,
        )
  } else {
    "%02d:%02d"
        .format(
            minutes,
            seconds,
        )
  }
}

fun buildSmbUri(
    shareName: String,
    path: String,
): String {
  return "smb://$shareName/${path.replace('\\', '/').trimStart('/')}"
}

fun getLocalIpAddress(): InetAddress? {
  return NetworkInterface.getNetworkInterfaces()
      .toList()
      .flatMap { it.inetAddresses.toList() }
      .firstOrNull { address ->
        !address.isLoopbackAddress && address is Inet4Address
      }
}
