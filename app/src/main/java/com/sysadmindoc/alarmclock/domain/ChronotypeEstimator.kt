package com.sysadmindoc.alarmclock.domain

import androidx.annotation.StringRes
import com.sysadmindoc.alarmclock.R

data class ChronotypeQuestion(
    val prompt: String,
    val options: List<String>
)

enum class ChronotypeCategory {
    EARLY,
    BALANCED,
    LATE
}

data class ChronotypeEstimate(
    val answers: List<Int?>,
    val answeredCount: Int,
    val score: Int?,
    val category: ChronotypeCategory?,
    val idealBedtimeMinutes: Int?,
    val idealWakeMinutes: Int?
) {
    val isComplete: Boolean = answeredCount == ChronotypeEstimator.QUESTION_COUNT
}

object ChronotypeEstimator {
    const val QUESTION_COUNT = 5
    private const val OPTION_COUNT = 5
    private const val MINUTE_OF_DAY = 24 * 60
    private const val EARLIEST_WAKE_MINUTES = 6 * 60
    private const val SCORE_TO_WAKE_MINUTES = 12

    val questions: List<ChronotypeQuestion> = listOf(
        ChronotypeQuestion(
            prompt = "On a free day, when do you naturally wake after enough sleep?",
            options = listOf("Before 6:30", "6:30-7:30", "7:30-8:30", "8:30-10:00", "After 10:00")
        ),
        ChronotypeQuestion(
            prompt = "When does focused work feel easiest?",
            options = listOf("Early morning", "Mid-morning", "Noon", "Late afternoon", "Evening")
        ),
        ChronotypeQuestion(
            prompt = "If tomorrow has no obligations, when would you choose bedtime?",
            options = listOf("Before 9:30", "9:30-10:30", "10:30-11:30", "11:30-12:30", "After 12:30")
        ),
        ChronotypeQuestion(
            prompt = "How do early alarms usually feel?",
            options = listOf("Easy", "Slightly slow", "Manageable", "Hard", "Very hard")
        ),
        ChronotypeQuestion(
            prompt = "When does your energy usually peak?",
            options = listOf("Sunrise", "Morning", "Midday", "Afternoon", "Late evening")
        )
    )

    fun decodeAnswers(raw: String): List<Int?> {
        if (raw.isBlank()) return List(QUESTION_COUNT) { null }
        val tokens = raw.split(",")
        return List(QUESTION_COUNT) { index ->
            tokens.getOrNull(index)
                ?.trim()
                ?.toIntOrNull()
                ?.takeIf { it in 0 until OPTION_COUNT }
        }
    }

    fun encodeAnswers(answers: List<Int?>): String {
        return List(QUESTION_COUNT) { index ->
            answers.getOrNull(index)
                ?.takeIf { it in 0 until OPTION_COUNT }
                ?.toString()
                .orEmpty()
        }.joinToString(",")
    }

    fun sanitizeAnswers(raw: String): String = encodeAnswers(decodeAnswers(raw))

    fun withAnswer(raw: String, questionIndex: Int, answerIndex: Int): String {
        if (questionIndex !in 0 until QUESTION_COUNT) return sanitizeAnswers(raw)
        val answers = decodeAnswers(raw).toMutableList()
        answers[questionIndex] = answerIndex.coerceIn(0, OPTION_COUNT - 1)
        return encodeAnswers(answers)
    }

    fun estimate(rawAnswers: String, sleepGoalMinutes: Int): ChronotypeEstimate {
        val answers = decodeAnswers(rawAnswers)
        val answeredCount = answers.count { it != null }
        if (answeredCount < QUESTION_COUNT) {
            return ChronotypeEstimate(
                answers = answers,
                answeredCount = answeredCount,
                score = null,
                category = null,
                idealBedtimeMinutes = null,
                idealWakeMinutes = null
            )
        }

        val score = answers.filterNotNull().sum()
        val wakeMinutes = normalizeMinutes(
            roundToQuarter(EARLIEST_WAKE_MINUTES + score * SCORE_TO_WAKE_MINUTES)
        )
        val bedMinutes = normalizeMinutes(wakeMinutes - sleepGoalMinutes.coerceIn(300, 960))
        return ChronotypeEstimate(
            answers = answers,
            answeredCount = answeredCount,
            score = score,
            category = categoryForScore(score),
            idealBedtimeMinutes = bedMinutes,
            idealWakeMinutes = wakeMinutes
        )
    }

    @StringRes
    fun categoryLabelRes(category: ChronotypeCategory): Int {
        return when (category) {
            ChronotypeCategory.EARLY -> R.string.chronotype_early_type
            ChronotypeCategory.BALANCED -> R.string.chronotype_balanced
            ChronotypeCategory.LATE -> R.string.chronotype_late_type
        }
    }

    private fun categoryForScore(score: Int): ChronotypeCategory {
        return when {
            score <= 6 -> ChronotypeCategory.EARLY
            score <= 13 -> ChronotypeCategory.BALANCED
            else -> ChronotypeCategory.LATE
        }
    }

    private fun roundToQuarter(minutes: Int): Int {
        return ((minutes + 7) / 15) * 15
    }

    private fun normalizeMinutes(minutes: Int): Int {
        return ((minutes % MINUTE_OF_DAY) + MINUTE_OF_DAY) % MINUTE_OF_DAY
    }
}
