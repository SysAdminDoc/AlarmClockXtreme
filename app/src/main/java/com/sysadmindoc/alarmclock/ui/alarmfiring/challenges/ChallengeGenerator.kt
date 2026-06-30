package com.sysadmindoc.alarmclock.ui.alarmfiring.challenges

import kotlin.random.Random

enum class ChallengeType {
    NONE,
    MATH_EASY,       // Single operation: 12 + 7 = ?
    MATH_MEDIUM,     // Two operations: 12 + 7 x 3 = ?
    MATH_HARD,       // Three operations or larger numbers
    SHAKE,           // Shake phone X times
    SEQUENCE,        // Tap numbers in ascending order
    MEMORY_PATTERN,  // Remember and tap a pattern of tiles
    TYPING,          // Type a displayed phrase exactly
    VOICE_PHRASE,    // Speak a displayed phrase with offline SpeechRecognizer
    HANDWRITING,     // Draw a displayed wake word with Digital Ink recognition
    WALK_STEPS,      // Walk N steps (uses step counter sensor)
    NFC_SCAN,        // Tap a registered NFC tag
    BARCODE_SCAN,    // Scan a registered barcode/QR code
    PHOTO_MATCH,     // Photograph a registered location to dismiss
    SQUAT,           // Do N squats (accelerometer)
    WIFI_CONNECT,    // Connect to specific Wi-Fi network
    MAZE,            // Solve a simple maze puzzle
    COUNT_SHEEP,     // Tap exactly N grazing sheep as they drift across the screen
    SIMON_SAYS,      // v1.5.0: Watch a growing color sequence and repeat it back
    DATE_BACKWARDS,  // v1.5.0: Type today's date in reverse (YYYY-MM-DD -> DD-MM-YYYY reversed)
    STROOP,          // v1.5.0: Tap the ink color of a color-word, not the word itself
    ROCK_PAPER_SCISSORS, // v1.6.0: Best-of-5 RPS against the computer (first to 3 wins)
    EMOJI_MEMORY,    // v1.6.0: Match 8 emoji pairs on a face-down 4x4 grid
    TYPING_SPEED,    // v1.6.0: Type a phrase at >= N wpm with <= maxErrors word mistakes
    WORDLE,          // v1.6.0: Guess the 5-letter target word in <= 6 attempts
    PVT,             // v1.14.10: Psychomotor vigilance — tap N times when the stimulus appears
    PUSH_UP,         // v1.15.0: Push-up challenge — accelerometer-based push-up detection
    PLANK_HOLD       // v1.15.0: Plank hold challenge — hold the phone level for N seconds
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

    // F3: Typing challenge — must type the displayed phrase exactly
    data class TypingChallenge(
        override val type: ChallengeType = ChallengeType.TYPING,
        val phrase: String
    ) : Challenge()

    data class VoicePhraseChallenge(
        override val type: ChallengeType = ChallengeType.VOICE_PHRASE,
        val phrase: String
    ) : Challenge()

    data class HandwritingChallenge(
        override val type: ChallengeType = ChallengeType.HANDWRITING,
        val targetText: String
    ) : Challenge()

    // F4: Walk-steps challenge — step counter sensor must accumulate N steps
    data class WalkChallenge(
        override val type: ChallengeType = ChallengeType.WALK_STEPS,
        val requiredSteps: Int = 30
    ) : Challenge()

    // F2: NFC tag challenge — must tap the pre-registered NFC tag
    data class NfcChallenge(
        override val type: ChallengeType = ChallengeType.NFC_SCAN,
        val registeredTagId: String  // hex ID of the registered tag
    ) : Challenge()

    // F1: Barcode/QR challenge — must scan the pre-registered barcode
    data class BarcodeChallenge(
        override val type: ChallengeType = ChallengeType.BARCODE_SCAN,
        val registeredValue: String  // barcode payload string
    ) : Challenge()

    // F16: Photo match challenge — must photograph the registered location
    data class PhotoMatchChallenge(
        override val type: ChallengeType = ChallengeType.PHOTO_MATCH,
        val referencePhotoUri: String  // URI to reference photo in filesDir
    ) : Challenge()

    // v1.2.0: Squat challenge — accelerometer-based squat detection
    data class SquatChallenge(
        override val type: ChallengeType = ChallengeType.SQUAT,
        val requiredSquats: Int = 10
    ) : Challenge()

    data class WifiChallenge(
        override val type: ChallengeType = ChallengeType.WIFI_CONNECT,
        val requiredSsid: String
    ) : Challenge()

    data class MazeChallenge(
        override val type: ChallengeType = ChallengeType.MAZE,
        val gridSize: Int = 5,
        val walls: Set<Int> = emptySet(),  // Cell indices that are walls
        val startPos: Int = 0,
        val endPos: Int = 24
    ) : Challenge()

    // v1.4.0: Count-the-Sheep challenge — tap sheep as they drift across the screen.
    // Tapping a sheep increments the count; decoy "goats" penalise wrong taps.
    data class CountSheepChallenge(
        override val type: ChallengeType = ChallengeType.COUNT_SHEEP,
        val targetCount: Int
    ) : Challenge()

    // v1.5.0: Simon-says — app lights up a sequence of N colored tiles;
    // user taps them back in order. One wrong tap restarts the round.
    data class SimonSaysChallenge(
        override val type: ChallengeType = ChallengeType.SIMON_SAYS,
        val sequence: List<Int>,     // Indices 0..3 into a 4-color pad
        val showDurationMs: Long = 600
    ) : Challenge()

    // v1.5.0: Type today's date backwards — e.g. "2026-04-17" -> "71-40-6202".
    // Simple cognitive gate that's easy on groggy motor skills but
    // impossible to solve without waking up enough to read.
    data class DateBackwardsChallenge(
        override val type: ChallengeType = ChallengeType.DATE_BACKWARDS,
        val targetDate: String,      // ISO date shown to the user
        val expectedInput: String    // Date reversed character-by-character
    ) : Challenge()

    // v1.5.0: Stroop test — display a color-word painted in a different ink
    // color and ask the user to tap the ink color (not the word). Classic
    // cognitive-interference task.
    data class StroopChallenge(
        override val type: ChallengeType = ChallengeType.STROOP,
        val wordLabel: String,       // e.g. "RED"
        val inkColorIndex: Int,      // 0..3 (index into a four-color palette)
        val choices: List<Int>       // Shuffled list of the four palette indices
    ) : Challenge()

    // v1.6.0: Rock-paper-scissors — best-of-5 against the computer.
    data class RockPaperScissorsChallenge(
        override val type: ChallengeType = ChallengeType.ROCK_PAPER_SCISSORS,
        val requiredWins: Int = 3
    ) : Challenge()

    // v1.6.0: Emoji memory — classic pairs card-matching on a 4x4 grid.
    // All 16 cards face-up for revealDurationMs, then face-down.
    data class EmojiMemoryChallenge(
        override val type: ChallengeType = ChallengeType.EMOJI_MEMORY,
        val cards: List<Int>,        // 16 entries; each value 0..7 appears exactly twice
        val revealDurationMs: Long = 3000
    ) : Challenge()

    // v1.6.0: Typing-speed gate — type the phrase at >= minWpm wpm with <= maxErrors word mistakes.
    data class TypingSpeedChallenge(
        override val type: ChallengeType = ChallengeType.TYPING_SPEED,
        val phrase: String,
        val minWpm: Int = 15,
        val maxErrors: Int = 2
    ) : Challenge()

    // v1.6.0: Wordle — guess the 5-letter target word in <= maxGuesses attempts.
    data class WordleChallenge(
        override val type: ChallengeType = ChallengeType.WORDLE,
        val target: String,
        val maxGuesses: Int = 6
    ) : Challenge()

    data class PvtChallenge(
        override val type: ChallengeType = ChallengeType.PVT,
        val totalTrials: Int = 5,
        val maxAverageMs: Int = 500
    ) : Challenge()

    // v1.15.0: Push-up challenge — accelerometer-based push-up detection.
    // Reuses the same pattern as SquatChallenge with a PushUpDetector.
    data class PushUpChallenge(
        override val type: ChallengeType = ChallengeType.PUSH_UP,
        val requiredPushUps: Int = 10
    ) : Challenge()

    // v1.15.0: Plank hold — hold the phone level (face-down) for N seconds.
    // The accelerometer verifies the phone stays roughly horizontal.
    data class PlankHoldChallenge(
        override val type: ChallengeType = ChallengeType.PLANK_HOLD,
        val requiredSeconds: Int = 30
    ) : Challenge()
}

private val TYPING_PHRASES = listOf(
    "The early bird catches the worm",
    "Rise and shine, it is time to wake up",
    "Good morning, today is going to be a great day",
    "Wake up and be awesome",
    "You have things to do and dreams to chase",
    "Every morning is a fresh start",
    "The sun is up and so should you be",
    "Do not go back to sleep",
    "Seize the day before the day seizes you",
    "Success is built one morning at a time",
    "Get up, dress up, show up",
    "Today is your day to shine",
    "Motivation is what gets you started",
    "You are stronger than your snooze button",
    "Turn off the alarm and turn on your life"
)

private val TYPING_SPEED_PHRASES = listOf(
    "Wake up and start the day",
    "Rise and shine right now",
    "Good morning time to move",
    "Get up stand up and go",
    "Today is a great day to begin",
    "Eyes open brain on feet on floor",
    "Morning has arrived time to rise",
    "Put your feet on the floor now",
    "A new day is here seize it",
    "The alarm is off get moving"
)

internal val WORDLE_WORDS = listOf(
    "EARLY", "AWAKE", "ALARM", "CLOCK", "LIGHT", "BRAIN", "WATER", "PHONE", "GOALS", "FOCUS",
    "POWER", "READY", "STAND", "ARISE", "CLEAR", "SHARP", "DRIVE", "ALERT", "THINK", "SMART",
    "GRACE", "BLOOM", "SWIFT", "BRAVE", "FRESH", "CRISP", "START", "SOLAR", "VIGOR", "VITAL",
    "SUNNY", "PROUD", "QUICK", "LEMON", "GLEAM", "SPARK", "BOUND", "CREST", "FLAME", "PLUCK",
    "POISE", "TRUST", "SIREN", "EMBER", "FLINT", "CLOUD", "STORM", "PRIME", "RAISE", "EAGLE"
)

private val HANDWRITING_WORDS = listOf(
    "AWAKE",
    "ALARM",
    "RISE",
    "LIGHT",
    "READY",
    "FOCUS",
    "START",
    "WATER",
    "MORNING",
    "SUNRISE"
)

object ChallengeGenerator {

    fun generate(type: ChallengeType, customPhrases: String = ""): Challenge = when (type) {
        ChallengeType.NONE -> Challenge.MathChallenge(ChallengeType.NONE, "0", 0, listOf(0))
        ChallengeType.MATH_EASY -> generateMathEasy()
        ChallengeType.MATH_MEDIUM -> generateMathMedium()
        ChallengeType.MATH_HARD -> generateMathHard()
        ChallengeType.SHAKE -> Challenge.ShakeChallenge(requiredShakes = 30)
        ChallengeType.SEQUENCE -> generateSequence()
        ChallengeType.MEMORY_PATTERN -> generateMemoryPattern()
        ChallengeType.TYPING -> {
            val allPhrases = TYPING_PHRASES.toMutableList()
            if (customPhrases.isNotBlank()) {
                allPhrases.addAll(customPhrases.split("\n").filter { it.isNotBlank() })
            }
            Challenge.TypingChallenge(phrase = allPhrases.random())
        }
        ChallengeType.VOICE_PHRASE -> {
            val allPhrases = TYPING_PHRASES.toMutableList()
            if (customPhrases.isNotBlank()) {
                allPhrases.addAll(customPhrases.split("\n").filter { it.isNotBlank() })
            }
            Challenge.VoicePhraseChallenge(phrase = allPhrases.random())
        }
        ChallengeType.HANDWRITING -> Challenge.HandwritingChallenge(
            targetText = HANDWRITING_WORDS.random()
        )
        // WALK_STEPS, NFC_SCAN, BARCODE_SCAN, PHOTO_MATCH require alarm-level data;
        // constructed directly in AlarmFiringViewModel using alarm fields
        ChallengeType.WALK_STEPS -> Challenge.WalkChallenge(requiredSteps = 30)
        ChallengeType.NFC_SCAN -> Challenge.NfcChallenge(registeredTagId = "")
        ChallengeType.BARCODE_SCAN -> Challenge.BarcodeChallenge(registeredValue = "")
        ChallengeType.PHOTO_MATCH -> Challenge.PhotoMatchChallenge(referencePhotoUri = "")
        ChallengeType.SQUAT -> Challenge.SquatChallenge(requiredSquats = 10)
        ChallengeType.WIFI_CONNECT -> Challenge.WifiChallenge(requiredSsid = "")
        ChallengeType.MAZE -> generateMaze()
        ChallengeType.COUNT_SHEEP -> Challenge.CountSheepChallenge(
            targetCount = Random.nextInt(6, 11)
        )
        ChallengeType.SIMON_SAYS -> Challenge.SimonSaysChallenge(
            sequence = List(Random.nextInt(4, 7)) { Random.nextInt(0, 4) }
        )
        ChallengeType.DATE_BACKWARDS -> {
            val today = java.time.LocalDate.now().toString() // YYYY-MM-DD
            Challenge.DateBackwardsChallenge(
                targetDate = today,
                expectedInput = today.reversed()
            )
        }
        ChallengeType.STROOP -> {
            // Four colors: red, green, blue, yellow — both words and inks.
            val inkIndex = Random.nextInt(0, 4)
            // Pick a DIFFERENT word so the interference actually exists.
            val wordIndex = ((inkIndex + Random.nextInt(1, 4)) % 4)
            val labels = listOf("RED", "GREEN", "BLUE", "YELLOW")
            Challenge.StroopChallenge(
                wordLabel = labels[wordIndex],
                inkColorIndex = inkIndex,
                choices = listOf(0, 1, 2, 3).shuffled()
            )
        }
        ChallengeType.ROCK_PAPER_SCISSORS -> Challenge.RockPaperScissorsChallenge()
        ChallengeType.EMOJI_MEMORY -> generateEmojiMemory()
        ChallengeType.TYPING_SPEED -> Challenge.TypingSpeedChallenge(
            phrase = TYPING_SPEED_PHRASES.random()
        )
        ChallengeType.WORDLE -> Challenge.WordleChallenge(
            target = WORDLE_WORDS.random()
        )
        ChallengeType.PVT -> Challenge.PvtChallenge()
        ChallengeType.PUSH_UP -> Challenge.PushUpChallenge(requiredPushUps = 10)
        ChallengeType.PLANK_HOLD -> Challenge.PlankHoldChallenge(requiredSeconds = 30)
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
            expression = "$a + ($b x $c) = ?",
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

    private fun generateMaze(): Challenge.MazeChallenge {
        val size = 5
        val totalCells = size * size
        val start = 0
        val end = totalCells - 1
        val wallCount = (totalCells * 0.3).toInt()

        // Bounded retry: with 30% wall density a solvable layout is found in
        // 1–2 attempts almost always, but cap attempts so we cannot deadlock the
        // alarm-firing flow if the RNG is pathological. Falls back to the
        // walls-around-the-edge maze, which is always solvable.
        repeat(50) {
            val walls = (1 until totalCells - 1)
                .shuffled()
                .take(wallCount)
                .toSet()

            if (isMazeSolvable(size, walls, start, end)) {
                return Challenge.MazeChallenge(
                    gridSize = size,
                    walls = walls,
                    startPos = start,
                    endPos = end
                )
            }
        }
        // Guaranteed-solvable fallback: a single corridor along row 0 then col 4.
        return Challenge.MazeChallenge(
            gridSize = size,
            walls = emptySet(),
            startPos = start,
            endPos = end
        )
    }

    private fun isMazeSolvable(size: Int, walls: Set<Int>, start: Int, end: Int): Boolean {
        val visited = mutableSetOf(start)
        val queue = ArrayDeque<Int>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (cell == end) return true

            val row = cell / size
            val col = cell % size
            val neighbors = listOfNotNull(
                if (row > 0) cell - size else null,       // up
                if (row < size - 1) cell + size else null, // down
                if (col > 0) cell - 1 else null,           // left
                if (col < size - 1) cell + 1 else null     // right
            )
            for (n in neighbors) {
                if (n !in walls && n !in visited) {
                    visited.add(n)
                    queue.add(n)
                }
            }
        }
        return false
    }

    private fun generateChoices(answer: Int): List<Int> {
        val choices = mutableSetOf(answer)
        while (choices.size < 4) {
            val offset = Random.nextInt(-10, 11)
            val choice = answer + offset
            if (offset != 0 && choice >= 0) choices.add(choice)
        }
        return choices.shuffled()
    }

    private fun generateEmojiMemory(): Challenge.EmojiMemoryChallenge {
        val cards = (List(8) { it } + List(8) { it }).shuffled()
        return Challenge.EmojiMemoryChallenge(cards = cards)
    }
}
