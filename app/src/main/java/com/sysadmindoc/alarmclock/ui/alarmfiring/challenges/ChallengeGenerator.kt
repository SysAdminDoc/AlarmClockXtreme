package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import kotlin.random.Random

enum class ChallengeType {
    NONE,
    MATH_EASY,       // Single operation: 12 + 7 = ?
    MATH_MEDIUM,     // Two operations: 12 + 7 x 3 = ?
    MATH_HARD,       // Three operations or larger numbers
    SHAKE,           // Shake phone X times
    SEQUENCE,        // Tap numbers in ascending order
    MEMORY_PATTERN   // Remember and tap a pattern of tiles
}

sealed class Challenge {
    abstract val type: ChallengeType

    data class MathChallenge(
        override val type: ChallengeType,
        val expression: String,
        val answer: Int,
        val choices: List<Int>  // 4 multiple choice options
    ) : Challenge()

    data class ShakeChallenge(
        override val type: ChallengeType = ChallengeType.SHAKE,
        val requiredShakes: Int = 30,
        var currentShakes: Int = 0
    ) : Challenge()

    data class SequenceChallenge(
        override val type: ChallengeType = ChallengeType.SEQUENCE,
        val numbers: List<Int>,         // Numbers displayed in random positions
        val correctOrder: List<Int>,    // The ascending order to tap
        var tappedSoFar: List<Int> = emptyList()
    ) : Challenge()

    data class MemoryPatternChallenge(
        override val type: ChallengeType = ChallengeType.MEMORY_PATTERN,
        val gridSize: Int = 3,          // 3x3 grid
        val pattern: List<Int>,         // Indices to remember (0-8)
        val showDurationMs: Long = 2000,
        var tappedSoFar: List<Int> = emptyList()
    ) : Challenge()
}

object ChallengeGenerator {

    fun generate(type: ChallengeType): Challenge = when (type) {
        ChallengeType.NONE -> Challenge.MathChallenge(ChallengeType.NONE, "0", 0, listOf(0))
        ChallengeType.MATH_EASY -> generateMathEasy()
        ChallengeType.MATH_MEDIUM -> generateMathMedium()
        ChallengeType.MATH_HARD -> generateMathHard()
        ChallengeType.SHAKE -> Challenge.ShakeChallenge(requiredShakes = 30)
        ChallengeType.SEQUENCE -> generateSequence()
        ChallengeType.MEMORY_PATTERN -> generateMemoryPattern()
    }

    private fun generateMathEasy(): Challenge.MathChallenge {
        val a = Random.nextInt(2, 20)
        val b = Random.nextInt(2, 20)
        val ops = listOf("+", "-", "x")
        val op = ops.random()
        val (expression, answer) = when (op) {
            "+" -> "$a + $b" to (a + b)
            "-" -> "${maxOf(a, b)} - ${minOf(a, b)}" to (maxOf(a, b) - minOf(a, b))
            "x" -> "$a x $b" to (a * b)
            else -> "$a + $b" to (a + b)
        }
        return Challenge.MathChallenge(
            type = ChallengeType.MATH_EASY,
            expression = "$expression = ?",
            answer = answer,
            choices = generateChoices(answer)
        )
    }

    private fun generateMathMedium(): Challenge.MathChallenge {
        val a = Random.nextInt(10, 50)
        val b = Random.nextInt(2, 15)
        val c = Random.nextInt(2, 10)
        val answer = a + b * c
        return Challenge.MathChallenge(
            type = ChallengeType.MATH_MEDIUM,
            expression = "$a + $b x $c = ?",
            answer = answer,
            choices = generateChoices(answer)
        )
    }

    private fun generateMathHard(): Challenge.MathChallenge {
        val a = Random.nextInt(50, 200)
        val b = Random.nextInt(10, 100)
        val c = Random.nextInt(2, 20)
        val answer = a + b - c
        return Challenge.MathChallenge(
            type = ChallengeType.MATH_HARD,
            expression = "$a + $b - $c = ?",
            answer = answer,
            choices = generateChoices(answer)
        )
    }

    private fun generateSequence(): Challenge.SequenceChallenge {
        val count = 6
        val numbers = (1..99).shuffled().take(count)
        return Challenge.SequenceChallenge(
            numbers = numbers,
            correctOrder = numbers.sorted()
        )
    }

    private fun generateMemoryPattern(): Challenge.MemoryPatternChallenge {
        val patternLength = 4
        val indices = (0 until 9).shuffled().take(patternLength)
        return Challenge.MemoryPatternChallenge(
            pattern = indices,
            showDurationMs = 2500
        )
    }

    private fun generateChoices(answer: Int): List<Int> {
        val choices = mutableSetOf(answer)
        while (choices.size < 4) {
            val offset = Random.nextInt(-10, 11)
            if (offset != 0) choices.add(answer + offset)
        }
        return choices.shuffled()
    }
}
