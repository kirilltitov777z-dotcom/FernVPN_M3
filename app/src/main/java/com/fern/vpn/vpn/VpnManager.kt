package com.fern.vpn.vpn

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.fern.vpn.data.SettingsRepository
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object VpnManager {

    enum class Status { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    data class Stats(
        val rx: Long = 0L,
        val tx: Long = 0L,
        val downSpeed: Float = 0f,
        val upSpeed: Float = 0f,
        val sessionSeconds: Long = 0L
    )

    private const val SAMPLE_COUNT = 48

    /**
     * Cloudflare listens for WARP/WireGuard on several UDP ports. Mobile carriers
     * frequently block 2408, and DNS may hand back an unreachable IPv6 address,
     * so IPv4 literals are tried explicitly.
     */
    private val primaryEndpoints = listOf(
        "162.159.192.1:2408",
        "162.159.193.1:2408",
        "188.114.96.1:2408"
    )

    private val fallbackEndpoints = listOf(
        "162.159.192.1:500",
        "162.159.192.1:1701",
        "162.159.192.1:4500",
        "188.114.96.1:500"
    )

    private const val PROBE_URL = "https://1.1.1.1/cdn-cgi/trace"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(Status.DISCONNECTED)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    private val _samples = MutableStateFlow(List(SAMPLE_COUNT) { 0f })
    val samples: StateFlow<List<Float>> = _samples.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _endpoint = MutableStateFlow("")
    val endpoint: StateFlow<String> = _endpoint.asStateFlow()

    private val _attempt = MutableStateFlow(0)
    val attempt: StateFlow<Int> = _attempt.asStateFlow()

    private val _attemptTotal = MutableStateFlow(0)
    val attemptTotal: StateFlow<Int> = _attemptTotal.asStateFlow()

    private var backend: GoBackend? = null
    private var tunnel: WarpTunnel? = null
    private var statsJob: Job? = null
    private var connectJob: Job? = null

    private var sessionStartElapsed = 0L
    private var sessionSaved = true

    private fun log(line: String) {
        val stamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _log.value = (_log.value + "$stamp  $line").takeLast(40)
    }

    fun connect(context: Context) {
        if (_status.value == Status.CONNECTED || _status.value == Status.CONNECTING) return
        val app = context.applicationContext
        _error.value = null
        _attempt.value = 0
        _endpoint.value = ""
        _status.value = Status.CONNECTING
        connectJob?.cancel()
        log("Старт подключения")
        connectJob = scope.launch {
            try {
                val registration = SettingsRepository.registration()
                    ?: WarpApi.register().also {
                        SettingsRepository.saveRegistration(it)
                        log("WARP: новый ключ зарегистрирован (warp=" + it.warpEnabled + ")")
                    }
                log("Адрес " + registration.addressV4 + " · ключ WARP " + if (registration.warpEnabled) "активен" else "без WARP")

                val wireGuard = backend ?: GoBackend(app).also { backend = it }
                val warpTunnel = tunnel ?: WarpTunnel().also { tunnel = it }

                val endpoints = candidateEndpoints(registration)
                _attemptTotal.value = endpoints.size
                log("Кандидатов: " + endpoints.size)

                var working: String? = null
                for ((index, candidate) in endpoints.withIndex()) {
                    if (!isActive) return@launch
                    _attempt.value = index + 1
                    log("Сервер " + candidate)
                    val config = runCatching { WarpConfig.build(registration, candidate) }.getOrNull()
                    if (config == null) {
                        log("Конфиг отклонён: " + candidate)
                        continue
                    }
                    try {
                        startTunnel(app, wireGuard, warpTunnel, config)
                    } catch (throwable: Throwable) {
                        log("Не поднялся " + candidate + ": " + describe(throwable))
                        runCatching { wireGuard.setState(warpTunnel, Tunnel.State.DOWN, null) }
                        delay(350)
                        continue
                    }

                    if (probeTunnel()) {
                        working = candidate
                        break
                    }

                    log("Нет трафика через " + candidate)
                    runCatching { wireGuard.setState(warpTunnel, Tunnel.State.DOWN, null) }
                    delay(450)
                }

                if (working == null) {
                    throw IOException(
                        "WARP не отвечает ни на одном сервере. Провайдер может блокировать UDP — попробуйте другую сеть или Wi-Fi."
                    )
                }

                _endpoint.value = working
                SettingsRepository.setLastEndpoint(working)
                _attempt.value = 0
                _samples.value = List(SAMPLE_COUNT) { 0f }
                _stats.value = Stats()
                sessionStartElapsed = SystemClock.elapsedRealtime()
                sessionSaved = false
                _status.value = Status.CONNECTED
                log("Туннель поднят: " + working)
                startStatsLoop(wireGuard, warpTunnel)
            } catch (throwable: Throwable) {
                val message = describe(throwable)
                _error.value = message
                _status.value = Status.ERROR
                _attempt.value = 0
                log("Ошибка: " + message)
                runCatching {
                    val currentTunnel = tunnel
                    if (currentTunnel != null) {
                        backend?.setState(currentTunnel, Tunnel.State.DOWN, null)
                    }
                }
            }
        }
    }

    fun disconnect() {
        val wireGuard = backend ?: run {
            _status.value = Status.DISCONNECTED
            return
        }
        val warpTunnel = tunnel ?: run {
            _status.value = Status.DISCONNECTED
            return
        }
        scope.launch {
            log("Отключение")
            statsJob?.cancel()
            runCatching { wireGuard.setState(warpTunnel, Tunnel.State.DOWN, null) }
            persistSession()
            _status.value = Status.DISCONNECTED
            _endpoint.value = ""
            _stats.value = Stats()
            _samples.value = List(SAMPLE_COUNT) { 0f }
        }
    }

    fun resetKeys() {
        scope.launch {
            log("Сброс ключей WARP")
            statsJob?.cancel()
            connectJob?.cancel()
            val wireGuard = backend
            val warpTunnel = tunnel
            if (wireGuard != null && warpTunnel != null) {
                runCatching { wireGuard.setState(warpTunnel, Tunnel.State.DOWN, null) }
            }
            persistSession()
            SettingsRepository.clearRegistration()
            _status.value = Status.DISCONNECTED
            _endpoint.value = ""
            _stats.value = Stats()
            _samples.value = List(SAMPLE_COUNT) { 0f }
            _error.value = null
            log("Ключи WARP сброшены")
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun candidateEndpoints(registration: WarpRegistration): List<String> {
        val list = mutableListOf<String>()
        SettingsRepository.lastEndpoint()?.let { list.add(it) }
        list.addAll(primaryEndpoints)
        list.add(registration.endpoint)
        list.addAll(fallbackEndpoints)
        return list.distinct().take(6)
    }

    private suspend fun startTunnel(
        app: Context,
        wireGuard: GoBackend,
        warpTunnel: WarpTunnel,
        config: Config
    ) {
        app.startService(Intent(app, FernVpnService::class.java))
        var waited = 0
        while (FernVpnService.instance == null && waited < 2000) {
            delay(100)
            waited += 100
        }
        wireGuard.setState(warpTunnel, Tunnel.State.UP, config)
    }

    /** Sends one real request through the tunnel; a dead handshake never answers. */
    private suspend fun probeTunnel(): Boolean = withContext(Dispatchers.IO) {
        delay(250)
        try {
            val connection = (URL(PROBE_URL).openConnection() as HttpsURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                setRequestProperty("User-Agent", "FernVPN")
                setRequestProperty("Connection", "close")
            }
            val code = connection.responseCode
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            code in 200..299 && text.isNotEmpty()
        } catch (throwable: Throwable) {
            false
        }
    }

    private fun describe(throwable: Throwable): String = buildString {
        append(throwable.javaClass.simpleName)
        throwable.message?.let { append(": ").append(it) }
    }

    private fun startStatsLoop(wireGuard: GoBackend, warpTunnel: WarpTunnel) {
        statsJob?.cancel()
        statsJob = scope.launch {
            var lastRx = 0L
            var lastTx = 0L
            var lastAt = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(1000)
                val statistics = runCatching { wireGuard.getStatistics(warpTunnel) }.getOrNull() ?: continue
                val rx = statistics.totalRx()
                val tx = statistics.totalTx()
                val now = SystemClock.elapsedRealtime()
                val deltaSeconds = ((now - lastAt).coerceAtLeast(200L)) / 1000f
                val down = ((rx - lastRx).coerceAtLeast(0L)) / deltaSeconds
                val up = ((tx - lastTx).coerceAtLeast(0L)) / deltaSeconds
                lastRx = rx
                lastTx = tx
                lastAt = now
                val seconds = (now - sessionStartElapsed) / 1000L
                _stats.value = Stats(rx, tx, down, up, seconds)
                _samples.value = (_samples.value.drop(1) + down).takeLast(SAMPLE_COUNT)
            }
        }
    }

    private fun persistSession() {
        if (sessionSaved || sessionStartElapsed == 0L) return
        sessionSaved = true
        val seconds = ((SystemClock.elapsedRealtime() - sessionStartElapsed) / 1000L).coerceAtLeast(0L)
        val current = _stats.value
        SettingsRepository.addSession(current.rx, current.tx, seconds)
        sessionStartElapsed = 0L
    }

    private class WarpTunnel : Tunnel {
        override fun getName(): String = "warp"
        override fun onStateChange(newState: Tunnel.State) = Unit
    }
}
