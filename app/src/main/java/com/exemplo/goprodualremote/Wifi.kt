package com.exemplo.goprodualremote

import android.content.Context
import android.net.*
import android.net.wifi.WifiNetworkSpecifier

class Wifi(ctx: Context) {
    private val cm = ctx.getSystemService(ConnectivityManager::class.java)
    private var cb: ConnectivityManager.NetworkCallback? = null

    fun join(ssid: String, pass: String, ok: () -> Unit, fail: (String) -> Unit) {
        leave()
        val spec = WifiNetworkSpecifier.Builder().setSsid(ssid).setWpa2Passphrase(pass).build()
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(spec).build()
        cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: Network) { cm.bindProcessToNetwork(n); ok() }
            override fun onUnavailable() = fail("Não consegui ligar. A câmara tem o Wi-Fi ligado e a palavra-passe está certa?")
            override fun onLost(n: Network) { cm.bindProcessToNetwork(null); fail("Ligação à câmara perdida") }
        }
        cm.requestNetwork(req, cb!!, 30000)
    }

    fun leave() {
        cb?.let { try { cm.unregisterNetworkCallback(it) } catch (_: Exception) {} }
        cb = null
        cm.bindProcessToNetwork(null)
    }
}
