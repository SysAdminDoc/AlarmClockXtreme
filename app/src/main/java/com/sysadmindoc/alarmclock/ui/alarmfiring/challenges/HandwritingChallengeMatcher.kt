package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

object HandwritingChallengeMatcher {
    fun matches(expected: String, candidates: List<String>): Boolean {
        val target = VoicePhraseMatcher.normalize(expected)
        val compactTarget = target.replace(" ", "")
        if (target.isBlank()) return false
        return candidates.any { candidate ->
            val normalized = VoicePhraseMatcher.normalize(candidate)
            normalized == target || normalized.replace(" ", "") == compactTarget
        }
    }
}
