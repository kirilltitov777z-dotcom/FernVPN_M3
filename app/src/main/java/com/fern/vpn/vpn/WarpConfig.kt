package com.fern.vpn.vpn

import com.wireguard.config.Config
import java.io.ByteArrayInputStream

object WarpConfig {

    fun build(registration: WarpRegistration): Config {
        val addresses = buildList {
            add(if (registration.addressV4.contains("/")) registration.addressV4 else "${registration.addressV4}/32")
            registration.addressV6?.let {
                add(if (it.contains("/")) it else "$it/128")
            }
        }.joinToString(", ")

        val text = buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = ${registration.privateKey}")
            appendLine("Address = $addresses")
            appendLine("DNS = 1.1.1.1")
            appendLine("MTU = 1280")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${registration.peerPublicKey}")
            appendLine("AllowedIPs = 0.0.0.0/0, ::/0")
            appendLine("Endpoint = ${registration.endpoint}")
            appendLine("PersistentKeepalive = 25")
        }

        return Config.parse(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))
    }
}
