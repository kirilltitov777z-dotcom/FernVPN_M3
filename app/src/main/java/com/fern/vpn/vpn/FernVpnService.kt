package com.fern.vpn.vpn

import com.wireguard.android.backend.GoBackend

/**
 * The app's VPN service. It extends the WireGuard GoBackend service, which owns the
 * TUN interface and the wireguard-go userspace implementation.
 */
class FernVpnService : GoBackend.VpnService() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: FernVpnService? = null
    }
}
