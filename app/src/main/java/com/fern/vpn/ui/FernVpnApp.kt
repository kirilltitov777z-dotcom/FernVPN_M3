package com.fern.vpn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fern.vpn.Ads
import com.fern.vpn.data.SettingsRepository
import com.fern.vpn.findActivity
import com.fern.vpn.vpn.VpnManager

@Composable
fun FernVpnApp(onToggle: () -> Unit) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showGuide by rememberSaveable { mutableStateOf(false) }

    val onboardingDone by SettingsRepository.onboardingDone.collectAsStateWithLifecycle()

    if (!onboardingDone || showGuide) {
        OnboardingScreen(
            onFinish = {
                SettingsRepository.setOnboardingDone()
                showGuide = false
            }
        )
        return
    }

    val context = LocalContext.current
    val status by VpnManager.status.collectAsStateWithLifecycle()
    val stats by VpnManager.stats.collectAsStateWithLifecycle()
    val samples by VpnManager.samples.collectAsStateWithLifecycle()
    val error by VpnManager.error.collectAsStateWithLifecycle()
    val endpoint by VpnManager.endpoint.collectAsStateWithLifecycle()
    val attempt by VpnManager.attempt.collectAsStateWithLifecycle()
    val attemptTotal by VpnManager.attemptTotal.collectAsStateWithLifecycle()
    val generating by VpnManager.generating.collectAsStateWithLifecycle()
    val configKind by SettingsRepository.configKind.collectAsStateWithLifecycle()
    val configName by SettingsRepository.configName.collectAsStateWithLifecycle()

    var wasConnected by remember { mutableStateOf(false) }
    LaunchedEffect(status) {
        if (status == VpnManager.Status.CONNECTED && !wasConnected) {
            context.findActivity()?.let { Ads.maybeShow(it) }
        }
        wasConnected = status == VpnManager.Status.CONNECTED
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    AnimatedContent(
        targetState = showSettings,
        transitionSpec = {
            (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { if (targetState) it / 5 else -it / 5 })
                .togetherWith(
                    fadeOut(tween(180)) + slideOutHorizontally(tween(300)) { if (targetState) -it / 8 else it / 8 }
                )
        },
        label = "screen"
    ) { settings ->
        if (settings) {
            SettingsScreen(
                onBack = { showSettings = false },
                onShowGuide = { showGuide = true }
            )
        } else {
            HomeScreen(
                status = status,
                stats = stats,
                samples = samples,
                error = error,
                configKind = configKind,
                configName = configName,
                generating = generating,
                endpoint = endpoint,
                attempt = attempt,
                attemptTotal = attemptTotal,
                onToggle = onToggle,
                onGenerate = { VpnManager.generateConfig() },
                onImport = { name, text -> VpnManager.importConfig(name, text) },
                onOpenSettings = { showSettings = true }
            )
        }
    }
}
