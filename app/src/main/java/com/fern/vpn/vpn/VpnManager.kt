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

    private var backend: GoBackend? = null
    private var tunnel: WarpTunnel? = null
    private var statsJob: Job? = null
    private var connectJob: Job? = null

    private var sessionStartElapsed = 0L
    private var sessionSaved = true

    fun connect(context: Context) {
        if (_status.value == Status.CONNECTED || _status.value == Status.CONNECTING) return
        val app = context.applicationContext
        _error.value = null
        _status.value = Status.CONNECTING
        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                val registration = SettingsRepository.registration()
                    ?: WarpApi.register().also { SettingsRepository.saveRegistration(it) }
                val config = WarpConfig.build(registration)

                val wireGuard = backend ?: GoBackend(app).also { backend = it }
                val warpTunnel = tunnel ?: WarpTunnel().also { tunnel = it }

                app.startService(Intent(app, FernVpnService::class.java))
                delay(400)

                wireGuard.setState(warpTunnel, Tunnel.State.UP, config)

                _samples.value = List(SAMPLE_COUNT) { 0f }
                _stats.value = Stats()
                sessionStartElapsed = SystemClock.elapsedRealtime()
                sessionSaved = false
                _status.value = Status.CONNECTED
                startStatsLoop(wireGuard, warpTunnel)
            } catch (throwable: Throwable) {
                _status.value = Status.ERROR
                _error.value = throwable.message ?: throwable.javaClass.simpleName
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
            statsJob?.cancel()
            runCatching { wireGuard.setState(warpTunnel, Tunnel.State.DOWN, null) }
            persistSession()
            _status.value = Status.DISCONNECTED
            _stats.value = Stats()
            _samples.value = List(SAMPLE_COUNT) { 0f }
        }
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
