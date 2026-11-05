package com.fern.vpn.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fern.vpn.R
import com.fern.vpn.data.SettingsRepository
import com.fern.vpn.ui.theme.Palettes
import com.fern.vpn.vpn.VpnManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onShowGuide: () -> Unit) {
    val language by SettingsRepository.language.collectAsStateWithLifecycle()
    val themeMode by SettingsRepository.themeMode.collectAsStateWithLifecycle()
    val palette by SettingsRepository.palette.collectAsStateWithLifecycle()
    val autoConnect by SettingsRepository.autoConnect.collectAsStateWithLifecycle()
    val notifyConnect by SettingsRepository.notifyConnect.collectAsStateWithLifecycle()
    val configKind by SettingsRepository.configKind.collectAsStateWithLifecycle()
    val configName by SettingsRepository.configName.collectAsStateWithLifecycle()
    val error by VpnManager.error.collectAsStateWithLifecycle()
    val log by VpnManager.log.collectAsStateWithLifecycle()
    val endpoint by VpnManager.endpoint.collectAsStateWithLifecycle()
    val checking by VpnManager.checking.collectAsStateWithLifecycle()
    val checkResult by VpnManager.checkResult.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var appeared by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    LaunchedEffect(resetDone) {
        if (resetDone) {
            delay(2600)
            resetDone = false
        }
    }

    val scheme = MaterialTheme.colorScheme
    val tabs = listOf(
        R.string.tab_general,
        R.string.tab_connection,
        R.string.tab_appearance
    )

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicSky(starCount = 70)

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.settings_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    ScrollableTabRow(
                        selectedTabIndex = tab,
                        containerColor = Color.Transparent,
                        contentColor = scheme.primary,
                        edgePadding = 16.dp,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, titleRes ->
                            Tab(
                                selected = tab == index,
                                onClick = { tab = index },
                                text = {
                                    Text(
                                        text = stringResource(titleRes),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(2.dp))

                AnimatedVisibility(
                    visible = appeared,
                    enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 12 }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        when (tab) {
                            0 -> GeneralTab(
                                configKind = configKind,
                                configName = configName,
                                resetDone = resetDone,
                                onReset = {
                                    VpnManager.resetKeys()
                                    resetDone = true
                                },
                                onShowGuide = onShowGuide
                            )

                            1 -> ConnectionTab(
                                autoConnect = autoConnect,
                                notifyConnect = notifyConnect,
                                checking = checking,
                                checkResult = checkResult,
                                error = error,
                                endpoint = endpoint,
                                log = log
                            )

                            else -> AppearanceTab(
                                themeMode = themeMode,
                                palette = palette,
                                language = language
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun GeneralTab(
    configKind: String?,
    configName: String?,
    resetDone: Boolean,
    onReset: () -> Unit,
    onShowGuide: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    GlassCard {
        Text(
            text = stringResource(R.string.configuration),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (configKind) {
                SettingsRepository.CONFIG_AUTO -> stringResource(R.string.config_auto)
                SettingsRepository.CONFIG_FILE -> configName ?: stringResource(R.string.config_file)
                else -> stringResource(R.string.config_none)
            },
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = onReset,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text(stringResource(R.string.reset_keys))
        }
        if (resetDone) {
            Text(
                text = stringResource(R.string.keys_reset),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary
            )
        }
    }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.guide),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.guide_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onShowGuide,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                )
            ) {
                Text(stringResource(R.string.open))
            }
        }
    }

    GlassCard {
        Text(
            text = stringResource(R.string.about),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.about_text),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectionTab(
    autoConnect: Boolean,
    notifyConnect: Boolean,
    checking: Boolean,
    checkResult: String?,
    error: String?,
    endpoint: String,
    log: List<String>
) {
    val scheme = MaterialTheme.colorScheme

    GlassCard {
        SwitchRow(
            title = stringResource(R.string.auto_connect),
            subtitle = stringResource(R.string.auto_connect_hint),
            checked = autoConnect,
            onCheckedChange = { SettingsRepository.setAutoConnect(it) }
        )
    }

    GlassCard {
        SwitchRow(
            title = stringResource(R.string.notifications),
            subtitle = stringResource(R.string.notifications_hint),
            checked = notifyConnect,
            onCheckedChange = { SettingsRepository.setNotifyConnect(it) }
        )
    }

    GlassCard {
        Text(
            text = stringResource(R.string.check_connection),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = checkResult ?: stringResource(R.string.check_connection_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { VpnManager.checkConnection() },
            enabled = !checking,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer
            )
        ) {
            if (checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = scheme.onSecondaryContainer
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.check))
        }
    }

    GlassCard {
        Text(
            text = stringResource(R.string.diagnostics),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.server) + ": " +
                (if (endpoint.isBlank()) "—" else endpoint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Text(
            text = if (error.isNullOrBlank()) {
                stringResource(R.string.no_errors)
            } else {
                stringResource(R.string.error_label) + ": " + error
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (error.isNullOrBlank()) scheme.onSurfaceVariant else scheme.error
        )

        if (log.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                color = scheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            log.takeLast(6).forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppearanceTab(
    themeMode: String,
    palette: String,
    language: String
) {
    val scheme = MaterialTheme.colorScheme

    GlassCard {
        Text(
            text = stringResource(R.string.theme),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        ChoiceRow(
            label = stringResource(R.string.theme_system),
            selected = themeMode == "system",
            onClick = { SettingsRepository.setThemeMode("system") }
        )
        ChoiceRow(
            label = stringResource(R.string.theme_dark),
            selected = themeMode == "dark",
            onClick = { SettingsRepository.setThemeMode("dark") }
        )
        ChoiceRow(
            label = stringResource(R.string.theme_light),
            selected = themeMode == "light",
            onClick = { SettingsRepository.setThemeMode("light") }
        )
    }

    GlassCard {
        Text(
            text = stringResource(R.string.palette),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.palette_hint),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PaletteChip(
                    label = stringResource(R.string.palette_aurora),
                    colors = listOf(Color(0xFFA8E6CF), Color(0xFFC9C4FF)),
                    selected = palette == Palettes.AURORA,
                    onClick = { SettingsRepository.setPalette(Palettes.AURORA) }
                )
                PaletteChip(
                    label = stringResource(R.string.palette_cosmos),
                    colors = listOf(Color(0xFFA9C7FF), Color(0xFFC9BFFF)),
                    selected = palette == Palettes.COSMOS,
                    onClick = { SettingsRepository.setPalette(Palettes.COSMOS) }
                )
                PaletteChip(
                    label = stringResource(R.string.palette_emerald),
                    colors = listOf(Color(0xFF7BE3AE), Color(0xFFDCE98F)),
                    selected = palette == Palettes.EMERALD,
                    onClick = { SettingsRepository.setPalette(Palettes.EMERALD) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)
            ) {
                PaletteChip(
                    label = stringResource(R.string.palette_ocean),
                    colors = listOf(Color(0xFF79C8FF), Color(0xFF8FB8FF)),
                    selected = palette == Palettes.OCEAN,
                    onClick = { SettingsRepository.setPalette(Palettes.OCEAN) }
                )
                PaletteChip(
                    label = stringResource(R.string.palette_sunset),
                    colors = listOf(Color(0xFFFFB178), Color(0xFFFF9AA8)),
                    selected = palette == Palettes.SUNSET,
                    onClick = { SettingsRepository.setPalette(Palettes.SUNSET) }
                )
            }
        }
    }

    GlassCard {
        Text(
            text = stringResource(R.string.language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        ChoiceRow(
            label = stringResource(R.string.language_system),
            selected = language == "system",
            onClick = { SettingsRepository.setLanguage("system") }
        )
        ChoiceRow(
            label = stringResource(R.string.language_ru),
            selected = language == "ru",
            onClick = { SettingsRepository.setLanguage("ru") }
        )
        ChoiceRow(
            label = stringResource(R.string.language_en),
            selected = language == "en",
            onClick = { SettingsRepository.setLanguage("en") }
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PaletteChip(
    label: String,
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = if (selected) {
                androidx.compose.foundation.BorderStroke(2.dp, scheme.primary)
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant)
            }
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(38.dp)
                    .background(
                        brush = Brush.linearGradient(colors),
                        shape = CircleShape
                    )
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant
        )
    }
}
