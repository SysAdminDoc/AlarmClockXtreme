package com.sysadmindoc.alarmclock.ui.bedtime

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sysadmindoc.alarmclock.domain.ChronotypeEstimator
import com.sysadmindoc.alarmclock.ui.components.AppFilterChip
import com.sysadmindoc.alarmclock.ui.components.AppSectionTitle
import com.sysadmindoc.alarmclock.ui.components.AppStatusChip
import com.sysadmindoc.alarmclock.ui.components.AppSurfaceCard
import com.sysadmindoc.alarmclock.ui.theme.DismissGreen
import com.sysadmindoc.alarmclock.ui.theme.SnoozeYellow
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary

@Composable
internal fun ChronotypeSection(
    state: BedtimeUiState,
    onAnswer: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier,
        highlighted = state.chronotypeComplete
    ) {
        AppSectionTitle(
            title = "Chronotype estimate",
            description = if (state.chronotypeComplete) {
                "Your natural timing preference is folded into the sleep target."
            } else {
                "Estimate your morning/evening tilt for a calmer target window."
            }
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = state.chronotypeCategoryLabel,
                icon = Icons.Default.Lightbulb,
                color = if (state.chronotypeComplete) MaterialTheme.colorScheme.primary else TextMuted
            )
            AppStatusChip(
                label = state.chronotypeTimingLabel,
                icon = Icons.Default.Schedule,
                color = if (state.chronotypeComplete) DismissGreen else SnoozeYellow
            )
            AppStatusChip(
                label = "${state.chronotypeAnsweredCount}/${ChronotypeEstimator.QUESTION_COUNT}",
                icon = Icons.Default.CheckCircle,
                color = if (state.chronotypeComplete) DismissGreen else TextMuted
            )
        }

        Text(
            text = state.chronotypeHelper,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        ChronotypeEstimator.questions.forEachIndexed { questionIndex, question ->
            if (questionIndex > 0) {
                HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = question.prompt,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.options.forEachIndexed { answerIndex, label ->
                        AppFilterChip(
                            label = label,
                            selected = state.chronotypeAnswers.getOrNull(questionIndex) == answerIndex,
                            onClick = { onAnswer(questionIndex, answerIndex) },
                            selectionSemantics = true
                        )
                    }
                }
            }
        }
    }
}
