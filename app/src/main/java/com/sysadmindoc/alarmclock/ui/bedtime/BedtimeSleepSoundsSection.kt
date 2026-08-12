package com.sysadmindoc.alarmclock.ui.bedtime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.sysadmindoc.alarmclock.domain.SleepNoisePreset
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary

private data class SleepSound(
    val label: String,
    val icon: ImageVector,
    val preset: SleepNoisePreset
)

private val SLEEP_SOUNDS = listOf(
    SleepSound("White Noise", Icons.Default.Waves, SleepNoisePreset.WHITE),
    SleepSound("Rain", Icons.Default.WaterDrop, SleepNoisePreset.RAIN),
    SleepSound("Brown Noise", Icons.Default.GraphicEq, SleepNoisePreset.BROWN),
    SleepSound("Ocean", Icons.Default.Sailing, SleepNoisePreset.OCEAN),
    SleepSound("Fan", Icons.Default.Air, SleepNoisePreset.FAN),
    SleepSound("Pink Noise", Icons.Default.GraphicEq, SleepNoisePreset.PINK),
    SleepSound("Violet Noise", Icons.Default.Waves, SleepNoisePreset.VIOLET),
)

@Composable
internal fun SleepSoundsSection(
    state: BedtimeUiState,
    viewModel: BedtimeViewModel,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(modifier = modifier) {
        AppSectionTitle(
            title = "Sleep sounds",
            description = "Continuous procedural soundscapes with no looping artifacts."
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(SLEEP_SOUNDS) { _, sound ->
                val isActive = state.activeSoundKey == sound.preset.key

                Card(
                    modifier = Modifier
                        .size(width = 112.dp, height = 108.dp)
                        .clickable(role = Role.Button) {
                            if (isActive) viewModel.stopSound()
                            else viewModel.playSound(sound.preset)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            SurfaceCard
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = sound.icon,
                            contentDescription = sound.label,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = sound.label,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (isActive) "Playing" else "Tap to preview",
                                color = if (isActive) MaterialTheme.colorScheme.primary else TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))

        Text(
            text = "Fade out after",
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(0, 15, 30, 45, 60).forEach { minutes ->
                AppFilterChip(
                    label = if (minutes == 0) "Never" else "$minutes min",
                    selected = state.sleepSoundFadeMinutes == minutes,
                    onClick = { viewModel.setSleepSoundFade(minutes) },
                    selectionSemantics = true,
                )
            }
        }

        // v1.5.0: Final-taper duration. Until this pass the fade was hard-coded
        // to 60s; users with deeper-sleep routines asked for a longer slide.
        Text(
            text = "Final taper length",
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val tapers = listOf(15, 30, 60, 120, 300, 600)
            tapers.forEach { seconds ->
                AppFilterChip(
                    label = when {
                        seconds < 60 -> "${seconds}s"
                        seconds % 60 == 0 -> "${seconds / 60} min"
                        else -> "${seconds}s"
                    },
                    selected = state.sleepSoundFadeSeconds == seconds,
                    onClick = { viewModel.setSleepSoundFadeSeconds(seconds) },
                    selectionSemantics = true,
                )
            }
        }

        if (state.activeSoundKey.isNotBlank()) {
            TextButton(
                onClick = viewModel::stopSound,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Stop sound", color = TextSecondary)
            }
        }
    }
}
