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

private data class RegVariant(
    val url: String,
    val clientVersion: String,
    val type: String,
    val model: String
)

/**
 * Registers a WireGuard key pair with Cloudflare WARP and returns the
 * connection parameters (addresses, peer key, endpoint) it hands back.
 */
object WarpApi {

    private val variants = listOf(
        RegVariant("https://api.cloudflareclient.com/v0a2158/reg", "a-6.30-2158", "Linux", "PC"),
        RegVariant("https://api.cloudflareclient.com/v0a2158/reg", "a-6.30-2158", "Android", "Pixel 7"),
        RegVariant("https://api.cloudflareclient.com/v0a1922/reg", "a-6.10-1668", "Linux", "PC"),
        RegVariant("https://api.cloudflareclient.com/v0i1909051800/reg", "a-6.10-1668", "Android", "Pixel 7")
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
                    put("type", variant.type)
                    put("locale", "en_US")
                }

                val connection = (URL(variant.url).openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 20_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("User-Agent", "okhttp/3.12.1")
                    setRequestProperty("CF-Client-Version", variant.clientVersion)
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Connection", "close")
                }

                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) {
                    problems.add("${variant.type}/${variant.clientVersion}: HTTP $code")
                    continue
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

                return@withContext WarpRegistration(
                    privateKey = privateKey,
                    publicKey = publicKey,
                    peerPublicKey = peer.getString("public_key"),
                    addressV4 = addressV4,
                    addressV6 = addressV6,
                    endpoint = endpoint,
                    deviceId = json.optString("id"),
                    token = json.optString("token")
                )
            } catch (e: Exception) {
                problems.add("${variant.type}/${variant.clientVersion}: ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        throw IOException("WARP недоступен: " + problems.joinToString("; "))
    }

    private fun tosTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
