package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import java.util.Locale
import kotlin.math.max

object VoicePhraseMatcher {
    private const val MIN_SIMILARITY = 0.92f
    private val fillerWords = setOf("uh", "um", "er", "ah")

    fun matches(expected: String, recognized: String): Boolean {
        val target = normalize(expected)
        val candidate = normalize(recognized)
        if (target.isBlank() || candidate.isBlank()) return false
        if (target == candidate) return true

        val longest = max(target.length, candidate.length).coerceAtLeast(1)
        val similarity = 1f - (levenshtein(target, candidate).toFloat() / longest)
        return similarity >= MIN_SIMILARITY
    }

    internal fun normalize(value: String): String {
        return value
            .lowercase(Locale.US)
            .map { char -> if (char.isLetterOrDigit()) char else ' ' }
            .joinToString(separator = "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in fillerWords }
            .joinToString(" ")
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (i in left.indices) {
            current[0] = i + 1
            for (j in right.indices) {
                val cost = if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + cost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }
}
