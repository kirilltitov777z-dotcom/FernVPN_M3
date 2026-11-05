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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fern.vpn.R
import com.fern.vpn.data.SettingsRepository
import com.fern.vpn.vpn.VpnManager
import kotlinx.coroutines.delay

private data class Achievement(
    val title: String,
    val description: String,
    val progress: Float
) {
    val unlocked: Boolean get() = progress >= 1f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val petEnabled by SettingsRepository.petEnabled.collectAsStateWithLifecycle()
    val language by SettingsRepository.language.collectAsStateWithLifecycle()
    val totalSeconds by SettingsRepository.totalSecondsFlow.collectAsStateWithLifecycle()
    val totalBytes by SettingsRepository.totalBytesFlow.collectAsStateWithLifecycle()
    val error by VpnManager.error.collectAsStateWithLifecycle()
    val log by VpnManager.log.collectAsStateWithLifecycle()

    var appeared by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appeared = true
    }

    val achievements = buildAchievements(totalSeconds, totalBytes)
    val unlockedCount = achievements.count { it.unlocked }
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        topBar = {
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
                enter = fadeIn(tween(420)) + slideInVertically(tween(420)) { it / 10 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.pet_enable),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = stringResource(R.string.pet_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = petEnabled,
                                onCheckedChange = { SettingsRepository.setPetEnabled(it) }
                            )
                        }
                    }

                    SettingsCard {
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        LanguageRow(
                            label = stringResource(R.string.language_system),
                            selected = language == "system",
                            onClick = { SettingsRepository.setLanguage("system") }
                        )
                        LanguageRow(
                            label = stringResource(R.string.language_ru),
                            selected = language == "ru",
                            onClick = { SettingsRepository.setLanguage("ru") }
                        )
                        LanguageRow(
                            label = stringResource(R.string.language_en),
                            selected = language == "en",
                            onClick = { SettingsRepository.setLanguage("en") }
                        )
                    }

                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.achievements),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = scheme.primaryContainer,
                                contentColor = scheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "$unlockedCount / ${achievements.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.achievements_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        achievements.forEach { achievement ->
                            AchievementRow(achievement)
                        }
                    }

                    SettingsCard {
                        Text(
                            text = stringResource(R.string.diagnostics),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
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
                            log.takeLast(6).forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                VpnManager.resetKeys()
                                resetDone = true
                            }
                        ) {
                            Text(stringResource(R.string.reset_keys))
                        }
                        if (resetDone) {
                            LaunchedEffect(Unit) {
                                delay(2500)
                                resetDone = false
                            }
                            Text(
                                text = stringResource(R.string.keys_reset),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.primary
                            )
                        }
                    }

                    SettingsCard {
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
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
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
private fun AchievementRow(achievement: Achievement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = if (achievement.unlocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (achievement.unlocked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!achievement.unlocked) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { achievement.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun buildAchievements(totalSeconds: Long, totalBytes: Long): List<Achievement> {
    val minute = 60f
    val bytes100mb = 100f * 1024f * 1024f

    return listOf(
        Achievement(
            title = stringResource(R.string.ach_first_leaf),
            description = stringResource(R.string.ach_first_leaf_desc),
            progress = (totalSeconds / minute).coerceIn(0f, 1f)
        ),
        Achievement(
            title = stringResource(R.string.ach_sprout),
            description = stringResource(R.string.ach_sprout_desc),
            progress = (totalSeconds / 1800f).coerceIn(0f, 1f)
        ),
        Achievement(
            title = stringResource(R.string.ach_young),
            description = stringResource(R.string.ach_young_desc),
            progress = (totalSeconds / 7200f).coerceIn(0f, 1f)
        ),
        Achievement(
            title = stringResource(R.string.ach_strong),
            description = stringResource(R.string.ach_strong_desc),
            progress = (totalSeconds / 36000f).coerceIn(0f, 1f)
        ),
        Achievement(
            title = stringResource(R.string.ach_bloom),
            description = stringResource(R.string.ach_bloom_desc),
            progress = (totalSeconds / 86400f).coerceIn(0f, 1f)
        ),
        Achievement(
            title = stringResource(R.string.ach_traveler),
            description = stringResource(R.string.ach_traveler_desc),
            progress = (totalBytes / bytes100mb).coerceIn(0f, 1f)
        ),
        Achievement(
            title = stringResource(R.string.ach_gigabyte),
            description = stringResource(R.string.ach_gigabyte_desc),
            progress = (totalBytes / (1024f * 1024f * 1024f)).coerceIn(0f, 1f)
        )
    )
}
