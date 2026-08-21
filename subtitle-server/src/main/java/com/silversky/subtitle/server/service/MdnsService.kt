package com.silversky.subtitle.server.service

import com.silversky.subtitle.server.model.MdnsConfig
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

class MdnsService(
    private val port: Int,
    private val config: MdnsConfig,
) {
  private var jmdns: JmDNS? = null

  fun start() {
    val address = InetAddress.getLocalHost()
    val dns = JmDNS.create(address)

    val service =
        ServiceInfo.create(
            config.service,
            config.name,
            port,
            "path=/",
        )

    dns.registerService(service)

    jmdns = dns

    println(
        "mDNS: advertising ${config.name} " +
            "at ${address.hostAddress}:$port " +
            "as ${config.service}"
    )
  }

  fun stop() {
    jmdns?.let {
      it.unregisterAllServices()
      it.close()
    }

    jmdns = null
  }
}
