package com.sysadmindoc.alarmclock.ui.alarmlist

import com.sysadmindoc.alarmclock.data.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the alarm card lists as the dismiss challenges. A long chain used to
 * collapse into a bare count, and a repeated chain showed no sign of its
 * extra rounds.
 */
class AlarmChallengeChainSummaryTest {

    @Test
    fun longChainKeepsEveryStepInOrder() {
        val alarm = Alarm(challengeChain = "MATH_EASY, SHAKE,TYPING,MAZE,,WORDLE")

        assertEquals(listOf("MATH_EASY", "SHAKE", "TYPING", "MAZE", "WORDLE"), alarm.challengeChainSteps())
    }

    @Test
    fun repeatedChainReportsItsRounds() {
        val alarm = Alarm(challengeChain = "MATH_EASY,SHAKE", challengeRepeatCount = 3)

        assertEquals(3, alarm.challengeChainRounds())
    }

    @Test
    fun singleChallengeCanRepeatToo() {
        val alarm = Alarm(challengeType = "MATH_HARD", challengeRepeatCount = 2)

        assertEquals(listOf("MATH_HARD"), alarm.challengeChainSteps())
        assertEquals(2, alarm.challengeChainRounds())
    }

    @Test
    fun repeatCountWithoutAnyChallengeIsOneRound() {
        val alarm = Alarm(challengeType = "NONE", challengeRepeatCount = 4)

        assertEquals(emptyList<String>(), alarm.challengeChainSteps())
        assertEquals(1, alarm.challengeChainRounds())
    }

    @Test
    fun outOfRangeRepeatCountIsClampedLikeTheFiringScreen() {
        assertEquals(
            Alarm.MAX_CHALLENGE_REPEAT_COUNT,
            Alarm(challengeType = "SHAKE", challengeRepeatCount = 99).challengeChainRounds()
        )
        assertEquals(1, Alarm(challengeType = "SHAKE", challengeRepeatCount = 0).challengeChainRounds())
    }
}
