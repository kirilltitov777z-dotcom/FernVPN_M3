package com.fern.vpn.vpn

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.fern.vpn.data.SettingsRepository
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
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
        _status.value = Status.CONNECTING
        connectJob?.cancel()
        log("Старт подключения")
        connectJob = scope.launch {
            try {
                val registration = SettingsRepository.registration()
                    ?: WarpApi.register().also {
                        SettingsRepository.saveRegistration(it)
                        log("WARP: новый ключ зарегистрирован")
                    }
                val config = WarpConfig.build(registration)
                log("Адрес " + registration.addressV4 + " · " + registration.endpoint)

                val wireGuard = backend ?: GoBackend(app).also { backend = it }
                val warpTunnel = tunnel ?: WarpTunnel().also { tunnel = it }

                var lastError: Throwable? = null
                for (attemptIndex in 1..2) {
                    try {
                        startTunnel(app, wireGuard, warpTunnel, config)
                        lastError = null
                        break
                    } catch (throwable: Throwable) {
                        lastError = throwable
                        log("Попытка $attemptIndex не удалась: " + describe(throwable))
                        runCatching { wireGuard.setState(warpTunnel, Tunnel.State.DOWN, null) }
                        runCatching { app.stopService(Intent(app, FernVpnService::class.java)) }
                        delay(500)
                    }
                }
                lastError?.let { throw it }

                _samples.value = List(SAMPLE_COUNT) { 0f }
                _stats.value = Stats()
                sessionStartElapsed = SystemClock.elapsedRealtime()
                sessionSaved = false
                _status.value = Status.CONNECTED
                log("Туннель поднят")
                startStatsLoop(wireGuard, warpTunnel)
            } catch (throwable: Throwable) {
                val message = describe(throwable)
                _error.value = message
                _status.value = Status.ERROR
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
            _stats.value = Stats()
            _samples.value = List(SAMPLE_COUNT) { 0f }
            _error.value = null
            log("Ключи WARP сброшены")
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun startTunnel(
        app: Context,
        wireGuard: GoBackend,
        warpTunnel: WarpTunnel,
        config: com.wireguard.config.Config
    ) {
        app.startService(Intent(app, FernVpnService::class.java))
        var waited = 0
        while (FernVpnService.instance == null && waited < 2500) {
            delay(100)
            waited += 100
        }
        log(if (FernVpnService.instance != null) "VPN-сервис готов" else "Сервис не ответил, стартую напрямую")
        wireGuard.setState(warpTunnel, Tunnel.State.UP, config)
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
