package com.silversky.skywatch.data.remote

import com.silversky.core.logger.Logger
import com.silversky.skywatch.utils.getLocalIpAddress
import javax.inject.Inject
import javax.inject.Singleton
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

@Singleton
class SubtitleServerDiscovery
@Inject
constructor(
    private val logger: Logger,
) {
  private var jmdns: JmDNS? = null

  fun start(onServerFound: (ip: String, port: Int) -> Unit) {
    jmdns = JmDNS.create(getLocalIpAddress())

    logger.debug("Starting subtitle server discovery")

    jmdns?.addServiceListener(
        "_skywatch-subtitle._tcp.local.",
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
        },
    )
  }

  fun stop() {
    logger.debug("Stopping subtitle server discovery")

    jmdns?.close()
    jmdns = null
  }
}
