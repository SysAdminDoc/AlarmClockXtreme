package com.sysadmindoc.alarmclock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.ui.theme.SurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
import androidx.compose.ui.res.stringResource
import com.sysadmindoc.alarmclock.R

/**
 * v1.5.0: Shown once per app version via [WhatsNewTracker]. A concise, not
 * exhaustive, list of changes — the README / CHANGELOG still carries the
 * full diff. Purpose is to surface new features users can tap into.
 */
@Composable
fun WhatsNewDialog(
    version: String,
    highlights: List<String>,
    onOpenRoadmap: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.whats_new_continue))
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppStatusChip(
                    label = stringResource(R.string.whats_new_updated_to, version),
                    icon = Icons.Default.AutoAwesome,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.whats_new_what_s_new),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.whats_new_quick_tour_biggest_additions_release),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenRoadmap) {
                Text(stringResource(R.string.whats_new_what_s_next), color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                highlights.forEachIndexed { index, line ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceCard.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.14f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = line,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        containerColor = SurfaceMedium,
        shape = RoundedCornerShape(12.dp)
    )
}
