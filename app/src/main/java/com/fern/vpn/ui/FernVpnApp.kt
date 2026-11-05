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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fern.vpn.data.SettingsRepository
import com.fern.vpn.vpn.VpnManager

@Composable
fun FernVpnApp(onToggle: () -> Unit) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val status by VpnManager.status.collectAsStateWithLifecycle()
    val stats by VpnManager.stats.collectAsStateWithLifecycle()
    val samples by VpnManager.samples.collectAsStateWithLifecycle()
    val error by VpnManager.error.collectAsStateWithLifecycle()
    val petEnabled by SettingsRepository.petEnabled.collectAsStateWithLifecycle()
    val storedSeconds by SettingsRepository.totalSecondsFlow.collectAsStateWithLifecycle()

    BackHandler(enabled = showSettings) { showSettings = false }

    AnimatedContent(
        targetState = showSettings,
        transitionSpec = {
            (fadeIn(tween(280)) + slideInHorizontally(tween(280)) { if (targetState) it / 5 else -it / 5 })
                .togetherWith(
                    fadeOut(tween(180)) + slideOutHorizontally(tween(280)) { if (targetState) -it / 8 else it / 8 }
                )
        },
        label = "screen"
    ) { settings ->
        if (settings) {
            SettingsScreen(onBack = { showSettings = false })
        } else {
            HomeScreen(
                status = status,
                stats = stats,
                samples = samples,
                error = error,
                petEnabled = petEnabled,
                totalSeconds = storedSeconds + stats.sessionSeconds,
                onToggle = onToggle,
                onOpenSettings = { showSettings = true }
            )
        }
    }
}
