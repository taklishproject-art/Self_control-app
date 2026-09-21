package com.example.service

import java.util.Calendar
import java.util.TimeZone

/**
 * Satellite / Network Time-Synced Night Sanctuary Engine
 * Coordinates:
 * - 4:55 PM / Night Grace Warning (ከ 4:55 ጀምሮ የቅድመ-ዝግጅት ማስጠንቀቂያ)
 * - 5:00 PM / Night Sanctuary Total Lockdown (ከ 5:00 ጀምሮ ሙሉ የሌሊት መቆለፊያ)
 * - 11:00 AM / Morning Sanctuary Release (እስከ 11:00 ጠዋት ድረስ)
 * - Morning Prayer & Physical Fitness Gate (የማለዳ ጸሎት እና ስፖርት መግቢያ)
 */
object SanctuaryEngine {

    enum class SanctuaryPhase {
        NORMAL_DAY,         // የቀን መደበኛ ጥበቃ
        PRE_LOCK_WARNING,   // ከ 4:55 እስከ 5:00 (Grace period countdown)
        NIGHT_LOCKDOWN,     // ከ 5:00 ሌሊት ጀምሮ ሙሉ መቆለፊያ
        MORNING_GATE        // 11:00 ጠዋት ላይ የሚከፈት የጸሎትና ስፖርት በር
    }

    // Default Ethiopian Timezone (Africa/Addis_Ababa, UTC+3)
    private val ETHIOPIA_TZ = TimeZone.getTimeZone("Africa/Addis_Ababa")

    /**
     * Determines current phase based on accurate Ethiopian Local Time (UTC+3)
     */
    fun getCurrentPhase(isMorningCompleted: Boolean = false): SanctuaryPhase {
        val cal = Calendar.getInstance(ETHIOPIA_TZ)
        val hour = cal.get(Calendar.HOUR_OF_DAY) // 0 - 23
        val minute = cal.get(Calendar.MINUTE)    // 0 - 59

        // 1. Night Grace Warning: 22:55 (ከምሽቱ 4:55) እስከ 22:59
        if (hour == 22 && minute >= 55) {
            return SanctuaryPhase.PRE_LOCK_WARNING
        }

        // 2. Night Sanctuary Lockdown: 23:00 (ከምሽቱ 5:00) እስከ 05:00 (ጠዋት 11:00)
        // In 24-hr time: 23:00 to 04:59 is night lockdown
        if (hour >= 23 || hour < 5) {
            return SanctuaryPhase.NIGHT_LOCKDOWN
        }

        // 3. Morning Sanctuary Gate: 05:00 (ጠዋት 11:00) እስከ 06:00 (ጠዋት 12:00)
        if (hour in 5..6) {
            return if (!isMorningCompleted) SanctuaryPhase.MORNING_GATE else SanctuaryPhase.NORMAL_DAY
        }

        return SanctuaryPhase.NORMAL_DAY
    }

    /**
     * Remaining seconds until 5:00 PM lockdown starts (when in warning phase)
     */
    fun getSecondsUntilLockdown(): Int {
        val cal = Calendar.getInstance(ETHIOPIA_TZ)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val totalSecCurrent = minute * 60 + second
        val targetSec = 60 * 60 // end of hour 22
        return (targetSec - totalSecCurrent).coerceAtLeast(0)
    }

    /**
     * Formatted Ethiopian time string for display
     */
    fun getEthiopianTimeString(): String {
        val cal = Calendar.getInstance(ETHIOPIA_TZ)
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        
        // Convert 24hr to Ethiopian clock (approx. (hour24 - 6) mod 12)
        val ethHour = when (val h = (hour24 - 6 + 24) % 12) {
            0 -> 12
            else -> h
        }
        val ampm = if (hour24 in 6..17) "ቀን" else "ማታ"
        return String.format("%02d:%02d (%s %d ሰዓት)", hour24, minute, ampm, ethHour)
    }
}
