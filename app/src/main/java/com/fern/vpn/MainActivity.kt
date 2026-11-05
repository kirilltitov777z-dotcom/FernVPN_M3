package com.fern.vpn

import android.content.Context
import android.content.res.Configuration
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fern.vpn.data.SettingsRepository
import com.fern.vpn.ui.FernVpnApp
import com.fern.vpn.ui.theme.FernTheme
import com.fern.vpn.vpn.VpnManager
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                VpnManager.connect(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val language by SettingsRepository.language.collectAsStateWithLifecycle()
            val baseContext = LocalContext.current
            val localizedContext = remember(language) { baseContext.withLanguage(language) }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                FernTheme {
                    FernVpnApp(onToggle = ::toggleVpn)
                }
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
