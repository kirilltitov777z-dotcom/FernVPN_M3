package com.fern.vpn.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.fern.vpn.data.SettingsRepository
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
        val sessionSeconds: Long = 0L,
        val latencyMs: Int = 0
    )

    private const val SAMPLE_COUNT = 48
    private const val PROBE_URL = "https://1.1.1.1/cdn-cgi/trace"
    private const val CHANNEL_ID = "fern_connection"

    /** Cloudflare listens on several UDP ports; carriers often block the default one. */
    private val primaryEndpoints = listOf(
        "162.159.192.1:2408",
        "162.159.192.7:2408",
        "162.159.193.1:2408",
        "162.159.193.10:2408",
        "188.114.96.1:2408",
        "188.114.97.1:2408"
    )

    private val fallbackEndpoints = listOf(
        "162.159.192.1:500",
        "162.159.192.1:1701",
        "162.159.192.1:4500",
        "188.114.96.1:500",
        "188.114.96.1:4500"
    )

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

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _generatingSeconds = MutableStateFlow(0)
    val generatingSeconds: StateFlow<Int> = _generatingSeconds.asStateFlow()

    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    private val _checkResult = MutableStateFlow<String?>(null)
    val checkResult: StateFlow<String?> = _checkResult.asStateFlow()

    private var backend: GoBackend? = null
    private var tunnel: WarpTunnel? = null
    private var statsJob: Job? = null
    private var connectJob: Job? = null
    private var generateJob: Job? = null

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
                val imported = SettingsRepository.importedConfig()
                val tunnelImpl = tunnel ?: WarpTunnel().also { tunnel = it }
                val wireGuard = backend ?: GoBackend(app).also { backend = it }

                if (imported != null) {
                    log("Профиль из файла: " + imported.first)
                    val config = WarpConfig.parse(imported.second)
                    withTimeout(15_000) {
                        startTunnel(app, wireGuard, tunnelImpl, config)
                    }
                    if (!probeTunnel()) {
                        throw IOException("Сервер из файла не ответил. Проверьте конфигурацию.")
                    }
                    _attemptTotal.value = 1
                    _attempt.value = 1
                    _endpoint.value = imported.first
                } else {
                    val registration = SettingsRepository.registration()
                        ?: throw IOException("Сначала создайте конфигурацию или загрузите файл .conf")

                    val endpoints = candidateEndpoints(registration)
                    _attemptTotal.value = endpoints.size
                    log("Вариантов подключения: " + endpoints.size)

                    var working: String? = null
                    for ((index, candidate) in endpoints.withIndex()) {
                        if (!isActive) return@launch
                        _attempt.value = index + 1
                        log("Сервер " + sanitize(candidate))
                        val config = runCatching { WarpConfig.build(registration, candidate) }.getOrNull()
                        if (config == null) {
                            log("Конфигурация отклонена")
                            continue
                        }
                        try {
                            withTimeout(15_000) {
                                startTunnel(app, wireGuard, tunnelImpl, config)
                            }
                        } catch (throwable: Throwable) {
                            log("Не поднялся: " + describe(throwable))
                            runCatching { wireGuard.setState(tunnelImpl, Tunnel.State.DOWN, null) }
                            delay(350)
                            continue
                        }

                        if (probeTunnel()) {
                            working = candidate
                            break
                        }

                        log("Нет трафика через " + sanitize(candidate))
                        runCatching { wireGuard.setState(tunnelImpl, Tunnel.State.DOWN, null) }
                        delay(450)
                    }

                    if (working == null) {
                        throw IOException(
                            "Сервер не отвечает. Возможно, сеть блокирует соединение — попробуйте Wi-Fi или загрузите файл .conf."
                        )
                    }
                    _endpoint.value = sanitize(working)
                    SettingsRepository.setLastEndpoint(working)
                }

                _attempt.value = 0
                _samples.value = List(SAMPLE_COUNT) { 0f }
                _stats.value = Stats()
                _checkResult.value = null
                sessionStartElapsed = SystemClock.elapsedRealtime()
                sessionSaved = false
                _status.value = Status.CONNECTED
                log("Туннель поднят")
                startStatsLoop(wireGuard, tunnelImpl)
                notifyConnected(app)
            } catch (throwable: Throwable) {
                val detail = describe(throwable)
                _status.value = Status.ERROR
                _attempt.value = 0
                _error.value = friendlyMessage(throwable)
                log("Ошибка: " + detail)
                runCatching {
                    val currentTunnel = tunnel
                    if (currentTunnel != null) {
                        backend?.setState(currentTunnel, Tunnel.State.DOWN, null)
                    }
                }
            }
        }
    }

    fun generateConfig() {
        if (_generating.value) return
        generateJob = scope.launch {
            _generating.value = true
            _generatingSeconds.value = 0
            _error.value = null
            log("Создание конфигурации")
            val ticker = launch {
                while (isActive) {
                    delay(1000)
                    _generatingSeconds.value += 1
                }
            }
            try {
                val registration = withTimeout(60_000) { WarpApi.register() }
                SettingsRepository.saveRegistration(registration)
                SettingsRepository.clearImportedConfig()
                _error.value = null
                if (_status.value == Status.ERROR) _status.value = Status.DISCONNECTED
                log("Конфигурация создана")
            } catch (timeout: TimeoutCancellationException) {
                _error.value = "Сервер не ответил за 60 секунд. Сеть может блокировать создание профиля — попробуйте Wi-Fi или загрузите файл .conf."
                _status.value = Status.ERROR
                log("Создание остановлено: сервер не ответил")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                _error.value = "Сервер не ответил. Сеть может блокировать создание профиля — попробуйте Wi-Fi или загрузите файл .conf."
                _status.value = Status.ERROR
                log("Создание не удалось: сервер не ответил")
            } finally {
                ticker.cancel()
                _generating.value = false
                _generatingSeconds.value = 0
            }
        }
    }

    fun cancelGenerate() {
        generateJob?.cancel()
        generateJob = null
        _generating.value = false
        _generatingSeconds.value = 0
        log("Создание отменено")
    }

    fun importConfig(name: String, text: String): Boolean {
        return try {
            WarpConfig.parse(text)
            SettingsRepository.saveImportedConfig(name, text)
            _error.value = null
            if (_status.value == Status.ERROR) _status.value = Status.DISCONNECTED
            log("Загружен профиль: " + name)
            true
        } catch (throwable: Throwable) {
            _error.value = "Файл не подходит: нужен конфигурационный файл WireGuard (.conf)"
            _status.value = Status.ERROR
            log("Импорт не удался: " + describe(throwable))
            false
        }
    }

    fun checkConnection() {
        if (_checking.value) return
        scope.launch {
            _checking.value = true
            try {
                val text = fetchTrace()
                val ip = text.lineSequence()
                    .firstOrNull { it.startsWith("ip=") }
                    ?.removePrefix("ip=")
                    ?.trim()
                _checkResult.value = if (ip.isNullOrBlank()) {
                    "Соединение работает"
                } else {
                    "Ваш IP: $ip"
                }
            } catch (throwable: Throwable) {
                _checkResult.value = "Нет ответа. Проверьте соединение."
                log("Проверка не удалась: " + describe(throwable))
            } finally {
                _checking.value = false
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
            _attempt.value = 0
            _stats.value = Stats()
            _samples.value = List(SAMPLE_COUNT) { 0f }
        }
    }

    fun resetKeys() {
        scope.launch {
            log("Сброс ключей")
            statsJob?.cancel()
            connectJob?.cancel()
            val wireGuard = backend
            val warpTunnel = tunnel
            if (wireGuard != null && warpTunnel != null) {
                runCatching { wireGuard.setState(warpTunnel, Tunnel.State.DOWN, null) }
            }
            persistSession()
            SettingsRepository.clearRegistration()
            SettingsRepository.clearImportedConfig()
            _status.value = Status.DISCONNECTED
            _endpoint.value = ""
            _attempt.value = 0
            _stats.value = Stats()
            _samples.value = List(SAMPLE_COUNT) { 0f }
            _error.value = null
            log("Ключи и профиль удалены")
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun candidateEndpoints(registration: WarpRegistration): List<String> {
        val list = mutableListOf<String>()
        SettingsRepository.lastEndpoint()?.let { list.add(it) }
        list.addAll(primaryEndpoints)
        list.addAll(resolveEndpoint(registration.endpoint))
        list.addAll(fallbackEndpoints)
        return list
            .filter { candidate -> candidate.substringBefore(':').any { it.isDigit() } }
            .distinct()
            .take(10)
    }

    /**
     * The registration API may answer with a hostname. Resolving it here keeps
     * the diagnostics free of provider names and works even when DNS is slow.
     */
    private fun resolveEndpoint(endpoint: String): List<String> {
        val host = endpoint.substringBefore(':')
        val port = endpoint.substringAfter(':', "2408").substringBefore(' ').ifBlank { "2408" }
        if (!host.any { it.isLetter() }) return listOf(endpoint)
        return runCatching {
            java.net.InetAddress.getAllByName(host)
                .filterIsInstance<java.net.Inet4Address>()
                .map { "${it.hostAddress}:$port" }
        }.getOrDefault(emptyList())
    }

    /** Never leak the infrastructure provider name into the visible log. */
    private fun sanitize(endpoint: String): String {
        val host = endpoint.substringBefore(':')
        return if (host.any { it.isLetter() }) "сервер" else endpoint
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
                setRequestProperty("User-Agent", "AsterVPN")
                setRequestProperty("Connection", "close")
            }
            val code = connection.responseCode
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            code in 200..299 && text.isNotEmpty()
        } catch (throwable: Throwable) {
            false
        }
    }

    private suspend fun fetchTrace(): String = withContext(Dispatchers.IO) {
        val connection = (URL(PROBE_URL).openConnection() as HttpsURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("User-Agent", "AsterVPN")
            setRequestProperty("Connection", "close")
        }
        connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun friendlyMessage(throwable: Throwable): String {
        val text = (throwable.message ?: "").lowercase()
        return when {
            text.contains("timeout") || text.contains("timed out") -> "Сервер не отвечает. Проверьте интернет и попробуйте ещё раз."
            text.contains("unknownhost") || text.contains("unable to resolve") -> "Нет доступа к интернету. Проверьте подключение."
            text.contains("сначала создайте") -> "Сначала создайте конфигурацию или загрузите файл .conf."
            else -> "Не удалось подключиться. Откройте «Диагностику» в настройках."
        }
    }

    private fun describe(throwable: Throwable): String = buildString {
        append(throwable.javaClass.simpleName)
        throwable.message?.let { append(": ").append(it) }
    }

    private fun notifyConnected(context: Context) {
        if (!SettingsRepository.notifyConnect.value) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Подключение",
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Aster VPN")
                .setContentText("Защищённое соединение включено")
                .setAutoCancel(true)
                .build()
            manager.notify(1, notification)
        } catch (throwable: Throwable) {
            log("Уведомление не показано: " + throwable.javaClass.simpleName)
        }
    }

    private fun startStatsLoop(wireGuard: GoBackend, warpTunnel: WarpTunnel) {
        statsJob?.cancel()
        statsJob = scope.launch {
            var lastRx = 0L
            var lastTx = 0L
            var lastAt = SystemClock.elapsedRealtime()
            var latency = 0
            var tick = 0
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
                tick += 1
                if (tick % 5 == 0) latency = measureLatency()
                val seconds = (now - sessionStartElapsed) / 1000L
                _stats.value = Stats(rx, tx, down, up, seconds, latency)
                _samples.value = (_samples.value.drop(1) + down).takeLast(SAMPLE_COUNT)
            }
        }
    }

    /** Round-trip time of a tiny TCP handshake, in milliseconds. Zero when unknown. */
    private fun measureLatency(): Int {
        val started = SystemClock.elapsedRealtime()
        val ok = runCatching {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("1.1.1.1", 443), 2000)
            }
        }.isSuccess
        if (!ok) return 0
        return (SystemClock.elapsedRealtime() - started).toInt().coerceIn(1, 1999)
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
        override fun getName(): String = "fern"
        override fun onStateChange(newState: Tunnel.State) = Unit
    }
}
