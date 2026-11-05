package com.fern.vpn.vpn

import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

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
 * One way of talking to the registration API. The Android client is the most
 * reliable variant: older clients are rejected with 401.
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
 * Registers a WireGuard key pair and returns the connection parameters
 * (addresses, peer key, endpoint) handed back by the service.
 */
object WarpApi {

    private val jsonMedia = "application/json; charset=UTF-8".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

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
                val request = Request.Builder()
                    .url(variant.url)
                    .post(buildRegistrationBody(variant, publicKey).toString().toRequestBody(jsonMedia))
                    .header("User-Agent", variant.userAgent)
                    .header("CF-Client-Version", variant.clientVersion)
                    .header("Accept", "application/json")
                    .build()

                val (code, text) = execute(request)
                if (code !in 200..299) {
                    problems.add("${variant.clientVersion}: HTTP $code")
                    continue
                }

                var json = JSONObject(text)
                if (!json.optBoolean("warp_enabled", true)) {
                    enableTunnel(variant, json.optString("id"), json.optString("token"))
                        ?.let { json = it }
                }

                val config = json.getJSONObject("config")
                val peer = config.getJSONArray("peers").getJSONObject(0)
                val interfaceObject = config.getJSONObject("interface")
                val addresses = interfaceObject.optJSONObject("addresses")

                val addressV4 = addresses?.optString("v4").orEmpty().ifBlank { "172.16.0.2" }
                val addressV6 = addresses?.optString("v6").orEmpty().ifBlank { null }

                val endpointObject = peer.optJSONObject("endpoint")
                val host = endpointObject?.optString("host").orEmpty().ifBlank {
                    endpointObject?.optString("v4").orEmpty()
                }.ifBlank { "162.159.192.1" }
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

    private fun buildRegistrationBody(variant: RegVariant, publicKey: String): JSONObject =
        JSONObject().apply {
            put("key", publicKey)
            put("install_id", "")
            put("fcm_token", "")
            put("tos", tosTimestamp())
            put("model", variant.model)
            put("locale", "en_US")
            if (variant.legacyLinux) {
                put("type", "Linux")
            } else {
                put("type", "Android")
                put("serial_number", "")
                put("os_version", variant.osVersion)
                put("key_type", "curve25519")
                put("tunnel_type", "wireguard")
            }
        }

    /**
     * Some registrations come back with the tunnel disabled. A PATCH turns it on
     * and returns the full, ready-to-use configuration.
     */
    private fun enableTunnel(variant: RegVariant, deviceId: String, token: String): JSONObject? {
        if (deviceId.isBlank()) return null
        return runCatching {
            val body = JSONObject().apply {
                put("warp_enabled", true)
                put("tos", tosTimestamp())
            }
            val request = Request.Builder()
                .url(variant.url + "/" + deviceId)
                .patch(body.toString().toRequestBody(jsonMedia))
                .header("User-Agent", variant.userAgent)
                .header("CF-Client-Version", variant.clientVersion)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            val (code, text) = execute(request)
            if (code !in 200..299) return@runCatching null
            JSONObject(text).takeIf { it.has("config") }
        }.getOrNull()
    }

    private fun execute(request: Request): Pair<Int, String> =
        client.newCall(request).execute().use { response ->
            response.code to (response.body?.string().orEmpty())
        }

    private fun tosTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
