package com.fern.vpn

import android.content.Context
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fern.vpn.data.SettingsRepository
import com.fern.vpn.ui.FernVpnApp
import com.fern.vpn.ui.theme.FernTheme
import com.fern.vpn.vpn.VpnManager
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                VpnManager.connect(this)
            }
        }

    /**
     * The language is applied here, before any screen is built. Doing it in the
     * composition (as an overridden LocalContext) used to crash on some devices.
     */
    override fun attachBaseContext(newBase: Context) {
        val language = runCatching { SettingsRepository.storedLanguage(newBase) }
            .getOrDefault("system")
        val localized = if (language == "system") {
            newBase
        } else {
            runCatching { newBase.withLanguage(language) }.getOrDefault(newBase)
        }
        super.attachBaseContext(localized)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by SettingsRepository.themeMode.collectAsStateWithLifecycle()
            val palette by SettingsRepository.palette.collectAsStateWithLifecycle()
            val autoConnect by SettingsRepository.autoConnect.collectAsStateWithLifecycle()
            val onboardingDone by SettingsRepository.onboardingDone.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            var autoTried by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(autoConnect, onboardingDone) {
                if (!autoTried && autoConnect && onboardingDone &&
                    VpnManager.status.value == VpnManager.Status.DISCONNECTED
                ) {
                    autoTried = true
                    delay(700)
                    toggleVpn()
                }
            }

            FernTheme(darkTheme = darkTheme, paletteKey = palette) {
                FernVpnApp(onToggle = ::toggleVpn)
            }
        }
    }

    private fun toggleVpn() {
        val current = VpnManager.status.value
        if (current == VpnManager.Status.CONNECTED || current == VpnManager.Status.CONNECTING) {
            VpnManager.disconnect()
            return
        }
        val permissionIntent = VpnService.prepare(this)
        if (permissionIntent != null) {
            vpnPermissionLauncher.launch(permissionIntent)
        } else {
            VpnManager.connect(this)
        }
    }
}

private fun Context.withLanguage(language: String): Context {
    if (language == "system") return this
    val locale = Locale(language)
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    return createConfigurationContext(configuration)
}
