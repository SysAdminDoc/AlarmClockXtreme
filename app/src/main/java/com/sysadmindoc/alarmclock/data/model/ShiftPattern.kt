package com.sysadmindoc.alarmclock.data.model

import androidx.annotation.StringRes
import com.sysadmindoc.alarmclock.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

data class ShiftPattern(
    val key: String,
    @StringRes val titleRes: Int,
    @StringRes val shortLabelRes: Int,
    val pattern: String,
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
                titleRes = R.string.shift_ddnno_title,
                shortLabelRes = R.string.shift_ddnno_short,
                pattern = "DDNNO"
            ),
            ShiftPattern(
                key = "FOUR_ON_FOUR_OFF",
                titleRes = R.string.shift_four_title,
                shortLabelRes = R.string.shift_four_short,
                pattern = "WWWWOOOO"
            ),
            ShiftPattern(
                key = "PANAMA",
                titleRes = R.string.shift_panama_title,
                shortLabelRes = R.string.shift_panama_short,
                pattern = "DDOODDDOODDOOO"
            ),
            ShiftPattern(
                key = "DUPONT",
                titleRes = R.string.shift_dupont_title,
                shortLabelRes = R.string.shift_dupont_short,
                pattern = "NNNNOOODDDONNNOOODDDDOOOOOOO"
            ),
            ShiftPattern(
                key = "PITMAN",
                titleRes = R.string.shift_pitman_title,
                shortLabelRes = R.string.shift_pitman_short,
                pattern = "NNOONNNOONNOOO"
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
