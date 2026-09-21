package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class BlockLog(
    val id: String = System.currentTimeMillis().toString(),
    val appName: String,
    val packageName: String,
    val blockedTerm: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BlockPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_STRICT_MODE = "strict_mode"
        private const val KEY_BLOCK_BROWSERS = "block_browsers"
        private const val KEY_BLOCK_SOCIAL = "block_social"
        private const val KEY_CUSTOM_KEYWORDS = "custom_keywords"
        private const val KEY_BLOCKED_LOGS = "blocked_logs"
        private const val KEY_TOTAL_BLOCKS = "total_blocks"
        private const val KEY_TODAY_BLOCKS = "today_blocks"
        private const val KEY_LAST_DAY_TIMESTAMP = "last_day_timestamp"
        private const val KEY_STREAK_START = "streak_start"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_VIBRATION_ALERT = "vibration_alert"
        private const val KEY_ANTI_UNINSTALL_LOCK = "anti_uninstall_lock"
        private const val KEY_UNBREAKABLE_MODE = "unbreakable_mode"
    }

    var isProtectionEnabled: Boolean
        get() = true
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, true).apply()

    var isStrictMode: Boolean
        get() = true
        set(value) = prefs.edit().putBoolean(KEY_STRICT_MODE, true).apply()

    var blockBrowsers: Boolean
        get() = true
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_BROWSERS, true).apply()

    var blockSocial: Boolean
        get() = true
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_SOCIAL, true).apply()

    var vibrationAlert: Boolean
        get() = true
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ALERT, true).apply()

    var pinCode: String?
        get() = prefs.getString(KEY_PIN_CODE, null)
        set(value) = prefs.edit().putString(KEY_PIN_CODE, value).apply()

    var isAntiUninstallLockEnabled: Boolean
        get() = true
        set(value) = prefs.edit().putBoolean(KEY_ANTI_UNINSTALL_LOCK, true).apply()

    var isUnbreakableModeEnabled: Boolean
        get() = true
        set(value) = prefs.edit().putBoolean(KEY_UNBREAKABLE_MODE, true).apply()

    var hasPromptedAdminOnLaunch: Boolean
        get() = prefs.getBoolean("has_prompted_admin_on_launch", false)
        set(value) = prefs.edit().putBoolean("has_prompted_admin_on_launch", value).apply()

    fun getCustomKeywords(): Set<String> {
        return prefs.getStringSet(KEY_CUSTOM_KEYWORDS, emptySet()) ?: emptySet()
    }

    fun addCustomKeyword(keyword: String) {
        val current = getCustomKeywords().toMutableSet()
        current.add(keyword.trim().lowercase())
        prefs.edit().putStringSet(KEY_CUSTOM_KEYWORDS, current).apply()
    }

    fun removeCustomKeyword(keyword: String) {
        val current = getCustomKeywords().toMutableSet()
        current.remove(keyword.trim().lowercase())
        prefs.edit().putStringSet(KEY_CUSTOM_KEYWORDS, current).apply()
    }

    fun getStreakDays(): Int {
        val start = prefs.getLong(KEY_STREAK_START, 0L)
        if (start == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_STREAK_START, now).apply()
            return 1
        }
        val diffMillis = System.currentTimeMillis() - start
        val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        return (days + 1).coerceAtLeast(1)
    }

    fun resetStreak() {
        prefs.edit().putLong(KEY_STREAK_START, System.currentTimeMillis()).apply()
    }

    fun getTotalBlocks(): Int = prefs.getInt(KEY_TOTAL_BLOCKS, 0)

    fun getTodayBlocks(): Int {
        checkAndResetDailyCount()
        return prefs.getInt(KEY_TODAY_BLOCKS, 0)
    }

    private fun checkAndResetDailyCount() {
        val lastDay = prefs.getLong(KEY_LAST_DAY_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        val calendarNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val calendarLast = java.util.Calendar.getInstance().apply { timeInMillis = lastDay }

        val isDifferentDay = calendarNow.get(java.util.Calendar.DAY_OF_YEAR) !=
                calendarLast.get(java.util.Calendar.DAY_OF_YEAR) ||
                calendarNow.get(java.util.Calendar.YEAR) !=
                calendarLast.get(java.util.Calendar.YEAR)

        if (isDifferentDay) {
            prefs.edit()
                .putInt(KEY_TODAY_BLOCKS, 0)
                .putLong(KEY_LAST_DAY_TIMESTAMP, now)
                .apply()
        }
    }

    fun recordBlock(appName: String, packageName: String, blockedTerm: String) {
        checkAndResetDailyCount()
        val total = getTotalBlocks() + 1
        val today = prefs.getInt(KEY_TODAY_BLOCKS, 0) + 1

        val log = BlockLog(
            appName = appName,
            packageName = packageName,
            blockedTerm = blockedTerm,
            timestamp = System.currentTimeMillis()
        )

        val logs = getBlockedLogs().toMutableList()
        logs.add(0, log)
        val trimmedLogs = logs.take(50) // keep latest 50 logs

        val jsonArray = JSONArray()
        for (item in trimmedLogs) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("appName", item.appName)
                put("packageName", item.packageName)
                put("blockedTerm", item.blockedTerm)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }

        prefs.edit()
            .putInt(KEY_TOTAL_BLOCKS, total)
            .putInt(KEY_TODAY_BLOCKS, today)
            .putString(KEY_BLOCKED_LOGS, jsonArray.toString())
            .apply()
    }

    fun getBlockedLogs(): List<BlockLog> {
        val jsonStr = prefs.getString(KEY_BLOCKED_LOGS, null) ?: return emptyList()
        val result = mutableListOf<BlockLog>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    BlockLog(
                        id = obj.optString("id", i.toString()),
                        appName = obj.optString("appName", "App"),
                        packageName = obj.optString("packageName", ""),
                        blockedTerm = obj.optString("blockedTerm", ""),
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun clearLogs() {
        prefs.edit().remove(KEY_BLOCKED_LOGS).apply()
    }

    // Morning Prayer & Exercise Gate Completion Tracking
    fun isMorningSanctuaryCompletedToday(): Boolean {
        val lastCompletedDay = prefs.getLong("key_morning_sanctuary_completed_day", 0L)
        val now = System.currentTimeMillis()
        val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val calLast = java.util.Calendar.getInstance().apply { timeInMillis = lastCompletedDay }
        return calNow.get(java.util.Calendar.DAY_OF_YEAR) == calLast.get(java.util.Calendar.DAY_OF_YEAR) &&
                calNow.get(java.util.Calendar.YEAR) == calLast.get(java.util.Calendar.YEAR)
    }

    fun markMorningSanctuaryCompletedToday() {
        prefs.edit().putLong("key_morning_sanctuary_completed_day", System.currentTimeMillis()).apply()
    }
}

