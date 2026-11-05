package com.fern.vpn.data

import android.content.Context
import android.content.SharedPreferences
import com.fern.vpn.vpn.WarpRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsRepository {

    private const val PREFS = "fern_prefs"

    private const val KEY_LANG = "language"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_PALETTE = "palette"
    private const val KEY_AUTO_CONNECT = "auto_connect"
    private const val KEY_NOTIFY = "notify_connect"

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

    private const val KEY_IMPORTED_NAME = "imported_name"
    private const val KEY_IMPORTED_TEXT = "imported_text"

    private const val KEY_ONBOARDING = "onboarding_done_v2"
    private const val KEY_LAST_ENDPOINT = "last_endpoint"

    const val CONFIG_AUTO = "auto"
    const val CONFIG_FILE = "file"

    private lateinit var prefs: SharedPreferences

    private val _language = MutableStateFlow("system")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _palette = MutableStateFlow("cosmos")
    val palette: StateFlow<String> = _palette.asStateFlow()

    private val _autoConnect = MutableStateFlow(false)
    val autoConnect: StateFlow<Boolean> = _autoConnect.asStateFlow()

    private val _notifyConnect = MutableStateFlow(true)
    val notifyConnect: StateFlow<Boolean> = _notifyConnect.asStateFlow()

    private val _totalSeconds = MutableStateFlow(0L)
    val totalSecondsFlow: StateFlow<Long> = _totalSeconds.asStateFlow()

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytesFlow: StateFlow<Long> = _totalBytes.asStateFlow()

    private val _onboardingDone = MutableStateFlow(false)
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    /** "auto", "file" or null when nothing is configured yet. */
    private val _configKind = MutableStateFlow<String?>(null)
    val configKind: StateFlow<String?> = _configKind.asStateFlow()

    private val _configName = MutableStateFlow<String?>(null)
    val configName: StateFlow<String?> = _configName.asStateFlow()

    fun init(context: Context) {
        if (this::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _language.value = prefs.getString(KEY_LANG, "system") ?: "system"
        _themeMode.value = prefs.getString(KEY_THEME, "system") ?: "system"
        _palette.value = prefs.getString(KEY_PALETTE, "cosmos") ?: "cosmos"
        _autoConnect.value = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        _notifyConnect.value = prefs.getBoolean(KEY_NOTIFY, true)
        _totalSeconds.value = prefs.getLong(KEY_TOTAL_SECONDS, 0L)
        _totalBytes.value = prefs.getLong(KEY_TOTAL_RX, 0L) + prefs.getLong(KEY_TOTAL_TX, 0L)
        _onboardingDone.value = prefs.getBoolean(KEY_ONBOARDING, false)
        refreshConfigState()
    }

    private fun refreshConfigState() {
        if (!this::prefs.isInitialized) return
        val imported = prefs.getString(KEY_IMPORTED_TEXT, null)
        when {
            imported != null -> {
                _configKind.value = CONFIG_FILE
                _configName.value = prefs.getString(KEY_IMPORTED_NAME, null)
            }

            registration() != null -> {
                _configKind.value = CONFIG_AUTO
                _configName.value = null
            }

            else -> {
                _configKind.value = null
                _configName.value = null
            }
        }
    }

    fun setOnboardingDone() {
        if (!this::prefs.isInitialized) return
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
        _onboardingDone.value = true
    }

    fun setLanguage(code: String) {
        prefs.edit().putString(KEY_LANG, code).apply()
        _language.value = code
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
        _themeMode.value = mode
    }

    fun setPalette(key: String) {
        prefs.edit().putString(KEY_PALETTE, key).apply()
        _palette.value = key
    }

    fun setAutoConnect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
        _autoConnect.value = enabled
    }

    fun setNotifyConnect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY, enabled).apply()
        _notifyConnect.value = enabled
    }

    fun lastEndpoint(): String? =
        if (this::prefs.isInitialized) prefs.getString(KEY_LAST_ENDPOINT, null) else null

    fun setLastEndpoint(endpoint: String) {
        if (this::prefs.isInitialized) prefs.edit().putString(KEY_LAST_ENDPOINT, endpoint).apply()
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
            .remove(KEY_IMPORTED_NAME)
            .remove(KEY_IMPORTED_TEXT)
            .apply()
        refreshConfigState()
    }

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
        refreshConfigState()
    }

    fun saveImportedConfig(name: String, text: String) {
        if (!this::prefs.isInitialized) return
        prefs.edit()
            .putString(KEY_IMPORTED_NAME, name)
            .putString(KEY_IMPORTED_TEXT, text)
            .apply()
        refreshConfigState()
    }

    fun importedConfig(): Pair<String, String>? {
        if (!this::prefs.isInitialized) return null
        val text = prefs.getString(KEY_IMPORTED_TEXT, null) ?: return null
        val name = prefs.getString(KEY_IMPORTED_NAME, "config.conf") ?: "config.conf"
        return name to text
    }

    fun clearImportedConfig() {
        if (!this::prefs.isInitialized) return
        prefs.edit().remove(KEY_IMPORTED_NAME).remove(KEY_IMPORTED_TEXT).apply()
        refreshConfigState()
    }
}
