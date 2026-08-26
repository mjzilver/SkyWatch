package com.silversky.skywatch.data.remote

import com.silversky.core.logger.Logger
import com.silversky.skywatch.di.ApplicationScope
import com.silversky.skywatch.utils.getLocalIpAddress
import javax.inject.Inject
import javax.inject.Singleton
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class SubtitleServerDiscovery
@Inject
constructor(
    private val logger: Logger,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
  private var jmdns: JmDNS? = null
  private var listener: ServiceListener? = null

  @Synchronized
  fun start(onServerFound: (ip: String, port: Int) -> Unit) {
    if (jmdns != null) return

    applicationScope.launch(Dispatchers.IO) {
      try {
        val localIp = getLocalIpAddress()
        if (localIp == null) {
          logger.error("Could not determine local IP address for mDNS")
          return@launch
        }

        synchronized(this@SubtitleServerDiscovery) {
          if (jmdns != null) return@synchronized
          jmdns = JmDNS.create(localIp)

          val currentListener =
              object : ServiceListener {
                override fun serviceAdded(event: ServiceEvent) {
                  logger.debug("Subtitle server service added: ${event.info}")
                }

                override fun serviceRemoved(event: ServiceEvent) {
                  logger.debug("Subtitle server service removed: ${event.info}")
                }

                override fun serviceResolved(event: ServiceEvent) {
                  val info = event.info
                  info.hostAddresses.firstOrNull()?.let { ip ->
                    logger.debug("Subtitle server discovered: $ip:${info.port}")
                    onServerFound(ip, info.port)
                  }
                }
              }
          listener = currentListener
          jmdns?.addServiceListener("_skywatch-subtitle._tcp.local.", currentListener)
        }
      } catch (e: Exception) {
        logger.error("Failed to start mDNS discovery", e)
        stop()
      }
    }
  }

  @Synchronized
  fun stop() {
    logger.debug("Stopping subtitle server discovery")
    try {
      listener?.let { jmdns?.removeServiceListener("_skywatch-subtitle._tcp.local.", it) }
      jmdns?.close()
    } catch (e: Exception) {
      logger.error("Error closing mDNS", e)
    } finally {
      jmdns = null
      listener = null
    }
  }
}
