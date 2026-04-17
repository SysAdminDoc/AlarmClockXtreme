package com.sysadmindoc.alarmclock.domain

import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.Challenge
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChallengeGenerator
import com.sysadmindoc.alarmclock.ui.alarmfiring.challenges.ChallengeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke tests for the maze challenge generator. The previous `while (true)`
 * regenerate loop could in theory deadlock the alarm-firing flow on a
 * pathological RNG outcome — these tests assert that the bounded retry
 * always produces a usable maze.
 */
class ChallengeMazeTest {

    @Test
    fun `generated maze always has consistent grid bounds`() {
        // 200 iterations is enough to surface any off-by-one / wall-set
        // corruption from the bounded-retry path.
        repeat(200) {
            val maze = ChallengeGenerator.generate(ChallengeType.MAZE) as Challenge.MazeChallenge
            val cells = maze.gridSize * maze.gridSize
            assertTrue("startPos in bounds", maze.startPos in 0 until cells)
            assertTrue("endPos in bounds", maze.endPos in 0 until cells)
            assertTrue("start is not a wall", maze.startPos !in maze.walls)
            assertTrue("end is not a wall", maze.endPos !in maze.walls)
        }
    }

    @Test
    fun `generated maze is solvable from start to end`() {
        repeat(50) {
            val maze = ChallengeGenerator.generate(ChallengeType.MAZE) as Challenge.MazeChallenge
            val size = maze.gridSize
            val visited = mutableSetOf(maze.startPos)
            val queue = ArrayDeque<Int>().apply { add(maze.startPos) }
            var reached = false
            while (queue.isNotEmpty()) {
                val cell = queue.removeFirst()
                if (cell == maze.endPos) { reached = true; break }
                val row = cell / size
                val col = cell % size
                val neighbors = listOfNotNull(
                    if (row > 0) cell - size else null,
                    if (row < size - 1) cell + size else null,
                    if (col > 0) cell - 1 else null,
                    if (col < size - 1) cell + 1 else null
                )
                for (n in neighbors) {
                    if (n !in maze.walls && n !in visited) {
                        visited.add(n)
                        queue.add(n)
                    }
                }
            }
            assertTrue("maze must be solvable", reached)
        }
    }

    @Test
    fun `walk challenge requires at least one step`() {
        val walk = ChallengeGenerator.generate(ChallengeType.WALK_STEPS) as Challenge.WalkChallenge
        assertTrue(walk.requiredSteps > 0)
    }

    @Test
    fun `math choices include the correct answer and three distractors`() {
        repeat(50) {
            val math = ChallengeGenerator.generate(ChallengeType.MATH_EASY) as Challenge.MathChallenge
            assertEquals(4, math.choices.toSet().size)
            assertTrue("correct answer is among choices", math.answer in math.choices)
            assertTrue("no negative distractors", math.choices.all { it >= 0 })
        }
    }
}
