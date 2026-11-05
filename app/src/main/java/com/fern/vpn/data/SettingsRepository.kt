package com.fern.vpn.data

import android.content.Context
import android.content.SharedPreferences
import com.fern.vpn.vpn.WarpRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsRepository {

    private const val PREFS = "fern_prefs"

    private const val KEY_PET = "pet_enabled"
    private const val KEY_LANG = "language"

    private const val KEY_TOTAL_RX = "total_rx"
    private const val KEY_TOTAL_TX = "total_tx"
    private const val KEY_TOTAL_SECONDS = "total_seconds"

    private const val KEY_WG_PRIVATE = "wg_private"
    private const val KEY_WG_PUBLIC = "wg_public"
    private const val KEY_PEER_PUBLIC = "peer_public"
    private const val KEY_ADDRESS_V4 = "address_v4"
    private const val KEY_ADDRESS_V6 = "address_v6"
    private const val KEY_ENDPOINT = "endpoint"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_TOKEN = "token"

    private lateinit var prefs: SharedPreferences

    private val _petEnabled = MutableStateFlow(true)
    val petEnabled: StateFlow<Boolean> = _petEnabled.asStateFlow()

    private val _language = MutableStateFlow("system")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _totalSeconds = MutableStateFlow(0L)
    val totalSecondsFlow: StateFlow<Long> = _totalSeconds.asStateFlow()

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytesFlow: StateFlow<Long> = _totalBytes.asStateFlow()

    fun init(context: Context) {
        if (this::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _petEnabled.value = prefs.getBoolean(KEY_PET, true)
        _language.value = prefs.getString(KEY_LANG, "system") ?: "system"
        _totalSeconds.value = prefs.getLong(KEY_TOTAL_SECONDS, 0L)
        _totalBytes.value = prefs.getLong(KEY_TOTAL_RX, 0L) + prefs.getLong(KEY_TOTAL_TX, 0L)
    }

    fun setPetEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PET, enabled).apply()
        _petEnabled.value = enabled
    }

    fun setLanguage(code: String) {
        prefs.edit().putString(KEY_LANG, code).apply()
        _language.value = code
    }

    val totalSeconds: Long
        get() = if (this::prefs.isInitialized) prefs.getLong(KEY_TOTAL_SECONDS, 0L) else 0L

    val totalBytes: Long
        get() = if (this::prefs.isInitialized) {
            prefs.getLong(KEY_TOTAL_RX, 0L) + prefs.getLong(KEY_TOTAL_TX, 0L)
        } else {
            0L
        }

    fun addSession(rx: Long, tx: Long, seconds: Long) {
        if (!this::prefs.isInitialized) return
        val rxTotal = prefs.getLong(KEY_TOTAL_RX, 0L) + rx.coerceAtLeast(0L)
        val txTotal = prefs.getLong(KEY_TOTAL_TX, 0L) + tx.coerceAtLeast(0L)
        val secondsTotal = prefs.getLong(KEY_TOTAL_SECONDS, 0L) + seconds.coerceAtLeast(0L)
        prefs.edit()
            .putLong(KEY_TOTAL_RX, rxTotal)
            .putLong(KEY_TOTAL_TX, txTotal)
            .putLong(KEY_TOTAL_SECONDS, secondsTotal)
            .apply()
        _totalSeconds.value = secondsTotal
        _totalBytes.value = rxTotal + txTotal
    }

    fun registration(): WarpRegistration? {
        if (!this::prefs.isInitialized) return null
        val privateKey = prefs.getString(KEY_WG_PRIVATE, null) ?: return null
        val publicKey = prefs.getString(KEY_WG_PUBLIC, null) ?: return null
        val peerPublicKey = prefs.getString(KEY_PEER_PUBLIC, null) ?: return null
        val addressV4 = prefs.getString(KEY_ADDRESS_V4, null) ?: return null
        val endpoint = prefs.getString(KEY_ENDPOINT, null) ?: return null
        return WarpRegistration(
            privateKey = privateKey,
            publicKey = publicKey,
            peerPublicKey = peerPublicKey,
            addressV4 = addressV4,
            addressV6 = prefs.getString(KEY_ADDRESS_V6, null),
            endpoint = endpoint,
            deviceId = prefs.getString(KEY_DEVICE_ID, "").orEmpty(),
            token = prefs.getString(KEY_TOKEN, "").orEmpty()
        )
    }

    /** Forgets the current WARP device so the next connect registers a fresh key. */
    fun clearRegistration() {
        if (!this::prefs.isInitialized) return
        prefs.edit()
            .remove(KEY_WG_PRIVATE)
            .remove(KEY_WG_PUBLIC)
            .remove(KEY_PEER_PUBLIC)
            .remove(KEY_ADDRESS_V4)
            .remove(KEY_ADDRESS_V6)
            .remove(KEY_ENDPOINT)
            .remove(KEY_DEVICE_ID)
            .remove(KEY_TOKEN)
            .apply()
    }

    fun saveRegistration(registration: WarpRegistration) {
        if (!this::prefs.isInitialized) return
        prefs.edit()
            .putString(KEY_WG_PRIVATE, registration.privateKey)
            .putString(KEY_WG_PUBLIC, registration.publicKey)
            .putString(KEY_PEER_PUBLIC, registration.peerPublicKey)
            .putString(KEY_ADDRESS_V4, registration.addressV4)
            .putString(KEY_ADDRESS_V6, registration.addressV6)
            .putString(KEY_ENDPOINT, registration.endpoint)
            .putString(KEY_DEVICE_ID, registration.deviceId)
            .putString(KEY_TOKEN, registration.token)
            .apply()
    }
}
