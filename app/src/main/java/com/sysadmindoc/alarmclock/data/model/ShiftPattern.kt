package com.sysadmindoc.alarmclock.data.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

data class ShiftPattern(
    val key: String,
    val title: String,
    val shortLabel: String,
    val pattern: String,
    val description: String
) {
    val cycleLength: Int = pattern.length

    fun shiftCodeFor(startDate: LocalDate, date: LocalDate): Char {
        val days = ChronoUnit.DAYS.between(startDate, date)
        val index = Math.floorMod(days, cycleLength.toLong()).toInt()
        return pattern[index]
    }

    fun isWorkDate(startDate: LocalDate, date: LocalDate): Boolean {
        return shiftCodeFor(startDate, date) in WORK_CODES
    }

    companion object {
        private val WORK_CODES = setOf('D', 'N', 'W')

        val presets: List<ShiftPattern> = listOf(
            ShiftPattern(
                key = "DDNNO",
                title = "DDNNO",
                shortLabel = "DDNNO",
                pattern = "DDNNO",
                description = "2 day shifts, 2 night shifts, 1 off day."
            ),
            ShiftPattern(
                key = "FOUR_ON_FOUR_OFF",
                title = "4-on-4-off",
                shortLabel = "4 on / 4 off",
                pattern = "WWWWOOOO",
                description = "4 work days followed by 4 off days."
            ),
            ShiftPattern(
                key = "PANAMA",
                title = "Panama (2-2-3)",
                shortLabel = "Panama",
                pattern = "DDOODDDOODDOOO",
                description = "2 on, 2 off, 3 on, then 2 off, 2 on, 3 off."
            ),
            ShiftPattern(
                key = "DUPONT",
                title = "DuPont",
                shortLabel = "DuPont",
                pattern = "NNNNOOODDDONNNOOODDDDOOOOOOO",
                description = "4 nights, 3 off, 3 days, 1 off, 3 nights, 3 off, 4 days, 7 off."
            ),
            ShiftPattern(
                key = "PITMAN",
                title = "Pitman (2-3-2)",
                shortLabel = "Pitman",
                pattern = "NNOONNNOONNOOO",
                description = "2 on, 2 off, 3 on, 2 off, 2 on, 3 off."
            )
        )

        val validKeys: Set<String> = presets.map { it.key }.toSet()

        fun fromKey(key: String): ShiftPattern? {
            val normalized = key.trim().uppercase(Locale.US)
            return presets.firstOrNull { it.key == normalized }
        }

        fun normalizedKey(key: String): String {
            return fromKey(key)?.key.orEmpty()
        }
    }
}
