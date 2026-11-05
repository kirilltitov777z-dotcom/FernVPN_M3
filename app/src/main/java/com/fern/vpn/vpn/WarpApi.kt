package com.fern.vpn.vpn

import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.HttpsURLConnection

data class WarpRegistration(
    val privateKey: String,
    val publicKey: String,
    val peerPublicKey: String,
    val addressV4: String,
    val addressV6: String?,
    val endpoint: String,
    val deviceId: String,
    val token: String,
    val warpEnabled: Boolean = true
)

/**
 * One way of talking to Cloudflare's registration API. The Android client is the
 * most reliable variant: the API rejects older client versions with 401.
 */
private data class RegVariant(
    val url: String,
    val clientVersion: String,
    val userAgent: String,
    val model: String,
    val osVersion: String,
    val legacyLinux: Boolean
)

/**
 * Registers a WireGuard key pair with Cloudflare WARP and returns the
 * connection parameters (addresses, peer key, endpoint) it hands back.
 */
object WarpApi {

    private val variants = listOf(
        RegVariant(
            url = "https://api.cloudflareclient.com/v0a5641/reg",
            clientVersion = "a-6.38.9-5641",
            userAgent = "1.1.1.1/6.38.9-5641 (Android 16.0.0)",
            model = "Pixel 8",
            osVersion = "16.0.0",
            legacyLinux = false
        ),
        RegVariant(
            url = "https://api.cloudflareclient.com/v0a5641/reg",
            clientVersion = "a-6.38.9-5641",
            userAgent = "1.1.1.1/6.38.9-5641 (Android 16.0.0)",
            model = "PC",
            osVersion = "16.0.0",
            legacyLinux = false
        ),
        RegVariant(
            url = "https://api.cloudflareclient.com/v0a2158/reg",
            clientVersion = "a-6.30-2158",
            userAgent = "okhttp/3.12.1",
            model = "PC",
            osVersion = "",
            legacyLinux = true
        )
    )

    suspend fun register(): WarpRegistration = withContext(Dispatchers.IO) {
        val keyPair = KeyPair()
        val privateKey = keyPair.privateKey.toBase64()
        val publicKey = keyPair.publicKey.toBase64()

        val problems = mutableListOf<String>()

        for (variant in variants) {
            try {
                val body = JSONObject().apply {
                    put("key", publicKey)
                    put("install_id", "")
                    put("fcm_token", "")
                    put("tos", tosTimestamp())
                    put("model", variant.model)
                    put("locale", "en_US")
                    if (variant.legacyLinux) {
                        put("type", "Linux")
                    } else {
                        put("serial_number", "")
                        put("os_version", variant.osVersion)
                        put("key_type", "curve25519")
                        put("tunnel_type", "wireguard")
                    }
                }

                val connection = (URL(variant.url).openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("User-Agent", variant.userAgent)
                    setRequestProperty("CF-Client-Version", variant.clientVersion)
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Connection", "close")
                }

                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) {
                    problems.add("${variant.clientVersion}: HTTP $code")
                    continue
                }

                val json = JSONObject(text)
                val config = json.getJSONObject("config")
                val peer = config.getJSONArray("peers").getJSONObject(0)
                val interfaceObject = config.getJSONObject("interface")
                val addresses = interfaceObject.optJSONObject("addresses")

                val addressV4 = addresses?.optString("v4").orEmpty().ifBlank { "172.16.0.2" }
                val addressV6 = addresses?.optString("v6").orEmpty().ifBlank { null }

                val endpointObject = peer.optJSONObject("endpoint")
                val host = endpointObject?.optString("host").orEmpty().ifBlank {
                    endpointObject?.optString("v4").orEmpty()
                }.ifBlank { "engage.cloudflareclient.com" }
                val port = endpointObject?.optJSONArray("ports")?.optInt(0, 2408) ?: 2408
                val endpoint = if (host.contains(":")) host else "$host:$port"

                return@withContext WarpRegistration(
                    privateKey = privateKey,
                    publicKey = publicKey,
                    peerPublicKey = peer.getString("public_key"),
                    addressV4 = addressV4,
                    addressV6 = addressV6,
                    endpoint = endpoint,
                    deviceId = json.optString("id"),
                    token = json.optString("token"),
                    warpEnabled = json.optBoolean("warp_enabled", true)
                )
            } catch (e: Exception) {
                problems.add("${variant.clientVersion}: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        throw IOException("Сервер конфигурации недоступен: " + problems.joinToString("; "))
    }

    private fun tosTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
