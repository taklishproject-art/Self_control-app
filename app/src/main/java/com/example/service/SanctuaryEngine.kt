package com.example.service

import java.util.Calendar
import java.util.TimeZone

/**
 * Satellite / Network Time-Synced Night Sanctuary Engine
 * Coordinates (Ethiopian Local Time UTC+3):
 * - 4:55 PM (22:55 standard): Night Grace Warning
 * - 5:00 PM - 11:00 AM (23:00 - 04:59 standard): Night Shutdown / Lock Mode
 * - 11:00 AM (05:00 AM standard): Morning Alarm, Prayer & Exercise Gate
 * - 11:00 AM - 3:45 AM (05:00 AM - 09:45 AM standard): Social Media & Browser Focus Lock (TikTok, Facebook, Chrome blocked)
 * - 3:45 AM (09:45 AM standard): Full Access Active Window
 */
object SanctuaryEngine {

    enum class SanctuaryPhase {
        ACTIVE_FULL_ACCESS,       // ጠዋት 3:45 እስከ ማታ 4:55 — መደበኛ አጠቃቀም
        PRE_LOCK_WARNING,         // ከ ማታ 4:55 እስከ 5:00 — የቅድመ-ዝግጅት ማሳሰቢያ
        NIGHT_SHUTDOWN_LOCK,      // ከ ማታ 5:00 እስከ ጠዋቱ 11:00 — ሙሉ የስልክ መቆለፊያ (Night Shutdown)
        MORNING_GATE,             // ጠዋት 11:00 — የማለዳ ደወል፣ ጸሎት እና ስፖርት መግቢያ
        FOCUS_RESTRICTED_WINDOW   // ከ ጠዋቱ 11:00 እስከ 3:45 — ቲክቶክ፣ ፌስቡክ እና ክሮም የታገዱበት ሰዓት
    }

    // Default Ethiopian Timezone (Africa/Addis_Ababa, UTC+3)
    private val ETHIOPIA_TZ = TimeZone.getTimeZone("Africa/Addis_Ababa")

    /**
     * Determines current phase based on accurate Ethiopian Local Time (UTC+3)
     */
    fun getCurrentPhase(isMorningCompleted: Boolean = false): SanctuaryPhase {
        val cal = Calendar.getInstance(ETHIOPIA_TZ)
        val hour = cal.get(Calendar.HOUR_OF_DAY) // 0 - 23 (standard 24hr)
        val minute = cal.get(Calendar.MINUTE)    // 0 - 59

        // 1. Night Grace Warning: 22:55 (ከምሽቱ 4:55) እስከ 22:59
        if (hour == 22 && minute >= 55) {
            return SanctuaryPhase.PRE_LOCK_WARNING
        }

        // 2. Night Shutdown Lockdown: 23:00 (ከምሽቱ 5:00) እስከ 04:59 (ጠዋት 11:00)
        if (hour >= 23 || hour < 5) {
            return SanctuaryPhase.NIGHT_SHUTDOWN_LOCK
        }

        // 3. Morning Gate: 05:00 (ጠዋት 11:00) እስከ 05:30 (ጠዋት 11:30)
        if (hour == 5 && minute in 0..29 && !isMorningCompleted) {
            return SanctuaryPhase.MORNING_GATE
        }

        // 4. Focus Restricted Window: 05:00 እስከ 09:44 (ጠዋት 11:00 እስከ 3:45 ሰዓት)
        // TikTok, Facebook, Chrome blocked until 3:45 (09:45 AM standard)
        if (hour < 9 || (hour == 9 && minute < 45)) {
            return SanctuaryPhase.FOCUS_RESTRICTED_WINDOW
        }

        // 5. Active Full Access Window: 09:45 AM (ጠዋት 3:45) እስከ 22:54 PM (ማታ 4:54)
        return SanctuaryPhase.ACTIVE_FULL_ACCESS
    }

    /**
     * Remaining seconds until 5:00 PM lockdown starts
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
        
        val ethHour = when (val h = (hour24 - 6 + 24) % 12) {
            0 -> 12
            else -> h
        }
        val ampm = if (hour24 in 6..17) "ቀን" else "ማታ/ሌሊት"
        return String.format("%02d:%02d (%s %d ሰዓት)", hour24, minute, ampm, ethHour)
    }
}
