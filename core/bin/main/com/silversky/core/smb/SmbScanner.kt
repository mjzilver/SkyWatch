package com.silversky.core.smb

import com.silversky.core.logger.Logger
import com.silversky.core.utils.NetworkUtils.Companion.resolveHostName
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SmbScanner(
    private val port: Int = 445,
    private val timeoutMillis: Int = 200,
    private val concurrency: Int = 64,
) {
    fun scanNetwork(logger: Logger): List<SmbServer> {
        val candidateIps =
            localIpv4Addresses()
                .flatMap { baseAddress ->
                    val prefix = baseAddress.substringBeforeLast('.')
                    (1..254).map { "$prefix.$it" }.filter { it != baseAddress }
                }
                .distinct()

        val executor = Executors.newFixedThreadPool(concurrency)
        try {
            val futures =
                candidateIps.map { ip ->
                    executor.submit<SmbServer?> {
                        if (isPortOpen(ip, port))
                            SmbServer(ipAddress = ip, port = port, name = resolveHostName(logger, ip))
                        else null
                    }
                }
            return futures.mapNotNull { it.get() }
        } finally {
            executor.shutdown()
            executor.awaitTermination(timeoutMillis.toLong() * 2, TimeUnit.MILLISECONDS)
        }
    }

    private fun isPortOpen(ipAddress: String, port: Int): Boolean =
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), timeoutMillis)
                true
            }
        } catch (_: Exception) {
            false
        }

    private fun localIpv4Addresses(): List<String> =
        try {
            NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .map { it.hostAddress }
                .filter { it != null && it.count { c -> c == '.' } == 3 }
                .toList()
        } catch (_: Exception) {
            emptyList()
        }
}
