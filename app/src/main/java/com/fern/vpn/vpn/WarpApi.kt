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
    val token: String
)

/**
 * Registers a WireGuard key pair with Cloudflare WARP and returns the
 * connection parameters (addresses, peer key, endpoint) it hands back.
 */
object WarpApi {

    private const val REG_URL = "https://api.cloudflareclient.com/v0a2158/reg"
    private const val USER_AGENT = "okhttp/3.12.1"
    private const val CF_CLIENT_VERSION = "a-6.30-2158"

    suspend fun register(): WarpRegistration = withContext(Dispatchers.IO) {
        val keyPair = KeyPair()
        val privateKey = keyPair.privateKey.toBase64()
        val publicKey = keyPair.publicKey.toBase64()

        val body = JSONObject().apply {
            put("key", publicKey)
            put("install_id", "")
            put("fcm_token", "")
            put("tos", tosTimestamp())
            put("model", "PC")
            put("type", "Linux")
            put("locale", "en_US")
        }

        val connection = (URL(REG_URL).openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("CF-Client-Version", CF_CLIENT_VERSION)
            setRequestProperty("Accept", "application/json")
        }

        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw IOException("WARP registration failed ($code): ${text.take(180)}")
        }

        val json = JSONObject(text)
        val config = json.getJSONObject("config")
        val peer = config.getJSONArray("peers").getJSONObject(0)
        val interfaceObject = config.getJSONObject("interface")
        val addresses = interfaceObject.optJSONObject("addresses")

        val addressV4 = addresses?.optString("v4").orEmpty().ifBlank { "172.16.0.2" }
        val addressV6 = addresses?.optString("v6").orEmpty().ifBlank { null }

        var endpoint = peer.optJSONObject("endpoint")?.optString("host").orEmpty()
        if (endpoint.isBlank()) {
            endpoint = peer.optJSONObject("endpoint")?.optString("v4").orEmpty()
        }
        if (endpoint.isBlank()) {
            endpoint = "162.159.192.1:2408"
        } else if (endpoint.endsWith(":0")) {
            endpoint = endpoint.substringBeforeLast(":") + ":2408"
        }

        WarpRegistration(
            privateKey = privateKey,
            publicKey = publicKey,
            peerPublicKey = peer.getString("public_key"),
            addressV4 = addressV4,
            addressV6 = addressV6,
            endpoint = endpoint,
            deviceId = json.optString("id"),
            token = json.optString("token")
        )
    }

    private fun tosTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
