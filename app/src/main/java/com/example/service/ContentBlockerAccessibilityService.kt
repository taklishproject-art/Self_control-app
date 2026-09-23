package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.data.BlockPreferences
import com.example.data.ContentFilterEngine
import com.example.ui.SanctuaryLockActivity
import com.example.ui.SoberingBlockActivity

class ContentBlockerAccessibilityService : AccessibilityService() {

    private lateinit var prefs: BlockPreferences
    private var smartDisplayController: SmartDisplayController? = null
    private var gestureNavigationEngine: ModernGestureNavigationEngine? = null
    private var superSystemEngine: SuperSystemBackgroundEngine? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastBlockTimestamp = 0L
    private var lastSanctuaryCheckTimestamp = 0L
    private var lastDisplayEvaluationTimestamp = 0L
    private val DEBOUNCE_MS = 1500L // Prevent duplicate rapid blocks for the same screen

    companion object {
        var isServiceRunning = false
            private set
        const val ACTION_CONTENT_BLOCKED = "com.example.safeguard.ACTION_CONTENT_BLOCKED"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_BLOCKED_TERM = "extra_blocked_term"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = BlockPreferences(applicationContext)
        smartDisplayController = SmartDisplayController(applicationContext)
        gestureNavigationEngine = ModernGestureNavigationEngine(this).apply {
            start()
        }
        superSystemEngine = SuperSystemBackgroundEngine(this).apply {
            start()
        }
        isServiceRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        smartDisplayController?.removeOverlay()
        smartDisplayController = null
        gestureNavigationEngine?.stop()
        gestureNavigationEngine = null
        superSystemEngine?.stop()
        superSystemEngine = null
        isServiceRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!::prefs.isInitialized) {
            prefs = BlockPreferences(applicationContext)
        }
        if (smartDisplayController == null) {
            smartDisplayController = SmartDisplayController(applicationContext)
        }
        if (gestureNavigationEngine == null) {
            gestureNavigationEngine = ModernGestureNavigationEngine(this).apply {
                start()
            }
        }
        if (superSystemEngine == null) {
            superSystemEngine = SuperSystemBackgroundEngine(this).apply {
                start()
            }
        }

        // Check if master protection is enabled
        if (!prefs.isProtectionEnabled) return

        val packageName = event.packageName?.toString() ?: return

        // 0. Background Display Optimization (Samsung-style Smart Light & Force Dark Layer)
        // Evaluates every 4 seconds or on window changes completely in background
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastDisplayEvaluationTimestamp > 4000L || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastDisplayEvaluationTimestamp = nowTime
            smartDisplayController?.evaluateAndApplyDisplayAdjustments(packageName)
        }

        // Handle System Settings Tampering Protection (Anti-Uninstall / Anti-Deactivate)
        if (packageName.contains("settings") || packageName.contains("packageinstaller")) {
            if (prefs.isAntiUninstallLockEnabled) {
                checkAndBlockSettingsTampering()
            }
            return
        }

        // ENGINE 2: VPN & Proxy Tunnel Guardian - Block attempts to launch VPN bypass apps
        if (ContentFilterEngine.VPN_PACKAGES.contains(packageName.lowercase()) ||
            packageName.contains("vpn") ||
            packageName.contains("proxy") ||
            packageName.contains("tunnel")
        ) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            triggerHapticAlert()
            handler.post {
                Toast.makeText(
                    applicationContext,
                    "🛡️ SafeGuard ጥበቃ፦ የጥበቃ ማለፊያ (VPN/Proxy) መክፈት በቋሚነት የተከለከለ ነው!",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        // Skip our own app package to allow settings configuration
        if (packageName == applicationContext.packageName) return

        // SANCTUARY ENGINE: Night Grace Warning (4:55), Night Shutdown Lock (5:00 - 11:00), Morning Gate (11:00), Focus Restriction until 3:45 AM
        val now = System.currentTimeMillis()
        if (now - lastSanctuaryCheckTimestamp > 3000L) {
            lastSanctuaryCheckTimestamp = now
            val morningCompleted = prefs.isMorningSanctuaryCompletedToday()
            val phase = SanctuaryEngine.getCurrentPhase(morningCompleted)

            when (phase) {
                SanctuaryEngine.SanctuaryPhase.NIGHT_SHUTDOWN_LOCK -> {
                    // Instantly lock screen / kick home and show Night Sanctuary
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    }
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    val lockIntent = Intent(applicationContext, SanctuaryLockActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(SanctuaryLockActivity.EXTRA_MODE, SanctuaryLockActivity.MODE_NIGHT_LOCK)
                    }
                    startActivity(lockIntent)
                    return
                }
                SanctuaryEngine.SanctuaryPhase.MORNING_GATE -> {
                    // Launch morning alarm gate with prayer & exercise checklist
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    val gateIntent = Intent(applicationContext, SanctuaryLockActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(SanctuaryLockActivity.EXTRA_MODE, SanctuaryLockActivity.MODE_MORNING_GATE)
                    }
                    startActivity(gateIntent)
                    return
                }
                SanctuaryEngine.SanctuaryPhase.FOCUS_RESTRICTED_WINDOW -> {
                    // Block TikTok, Facebook, Chrome and other browsers/social media until 3:45 AM Ethiopian (9:45 AM standard)
                    if (ContentFilterEngine.isMonitoredPackage(packageName, blockBrowsers = true, blockSocial = true)) {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        triggerHapticAlert()
                        handler.post {
                            Toast.makeText(
                                applicationContext,
                                "🛡️ SafeGuard ጥበቃ፦ ቲክቶክ፣ ፌስቡክ እና ክሮም የሚፈቀዱት ጠዋት 3:45 ሰዓት ሲሆን ብቻ ነው! እስከዚያው በጸሎትና በስራ ላይ በርታ።",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return
                    }
                }
                SanctuaryEngine.SanctuaryPhase.PRE_LOCK_WARNING -> {
                    // Warn once during the 4:55 to 5:00 window
                    handler.post {
                        Toast.makeText(
                            applicationContext,
                            "🌙 SafeGuard፦ ከ 5:00 ጀምሮ ስልክህ ሙሉ በሙሉ ይቆለፋል (Night Shutdown)! ስልክህን ለእረፍት አዘጋጅ።",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                SanctuaryEngine.SanctuaryPhase.ACTIVE_FULL_ACCESS -> {
                    // Normal filtering continues
                }
            }
        }

        // Check if package is monitored (Chrome, Phoenix, TikTok, Instagram, Facebook, etc.)
        val isMonitored = ContentFilterEngine.isMonitoredPackage(
            packageName = packageName,
            blockBrowsers = prefs.blockBrowsers,
            blockSocial = prefs.blockSocial
        )
        if (!isMonitored) return

        // Debounce rapid repeated triggers
        if (now - lastBlockTimestamp < DEBOUNCE_MS) return

        // ENGINE 4: Keystroke & Input Watcher
        // When typing in any search bar or editable field (e.g. TikTok, Facebook, Chrome, YouTube, etc.)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val typedText = event.text?.joinToString(" ") ?: ""
            if (typedText.isNotBlank()) {
                val match = ContentFilterEngine.checkContent(
                    text = typedText,
                    customKeywords = prefs.getCustomKeywords(),
                    strictMode = prefs.isStrictMode
                )
                if (match != null) {
                    // Instantly wipe keyboard / input text to keep screen clean and undefiled
                    clearInappropriateSearchInput(event.source, rootInActiveWindow)
                    triggerBlock(packageName, match.matchedTerm, "በመተየብ ላይ እንዳለ ቃሉ በቅጽበት ተሰርዟል፤ ወደ Home ተመልሷል")
                    return
                }
            }
        }

        // 1. Inspect text directly from the event if present
        val eventTexts = event.text
        if (!eventTexts.isNullOrEmpty()) {
            for (charSeq in eventTexts) {
                val match = ContentFilterEngine.checkContent(
                    text = charSeq.toString(),
                    customKeywords = prefs.getCustomKeywords(),
                    strictMode = prefs.isStrictMode
                )
                if (match != null) {
                    clearInappropriateSearchInput(event.source, rootInActiveWindow)
                    triggerBlock(packageName, match.matchedTerm, match.reasonAmharic)
                    return
                }
            }
        }

        // 2. Scan window node tree (search bar, text views, captions, web titles, and Incognito tabs)
        val rootNode = rootInActiveWindow ?: return
        try {
            // ENGINE 1: Incognito / Private Tab Slayer
            if (detectAndBlockIncognito(rootNode, packageName)) {
                return
            }

            val matchedTerm = scanNodeTree(rootNode, maxDepth = 6, maxNodes = 60)
            if (matchedTerm != null) {
                clearInappropriateSearchInput(event.source, rootNode)
                triggerBlock(packageName, matchedTerm, "ተገቢ ያልሆነ ይዘት ታግዷል እና ተሰርዟል")
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * ENGINE 1: Incognito / Private Tab Slayer
     * Detects if user opens an Incognito/Private/InPrivate tab in any browser and closes it instantly.
     */
    private fun detectAndBlockIncognito(rootNode: AccessibilityNodeInfo, packageName: String): Boolean {
        val incognitoKeywords = listOf(
            "incognito", "private tab", "private browsing", "inprivate",
            "new incognito tab", "አዲስ ማንነቱ ያልታወቀ ትር", "ማንነቱ ያልታወቀ"
        )

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)
        var count = 0

        while (queue.isNotEmpty() && count < 35) {
            val node = queue.removeFirst()
            count++

            val text = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
            val lower = text.lowercase()

            for (kw in incognitoKeywords) {
                if (lower.contains(kw)) {
                    // Instantly kick user out and close incognito tab
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    triggerHapticAlert()
                    handler.post {
                        Toast.makeText(
                            applicationContext,
                            "🛡️ SafeGuard ጥበቃ፦ ስውር (Incognito / Private) መስኮት መጠቀም በጥብቅ የተከለከለ ነው!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return true
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    private fun scanNodeTree(node: AccessibilityNodeInfo?, maxDepth: Int, maxNodes: Int): String? {
        if (node == null || maxDepth <= 0) return null

        var nodeCount = 0
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(Pair(node, 0))

        val customKeywords = prefs.getCustomKeywords()
        val strictMode = prefs.isStrictMode

        while (queue.isNotEmpty() && nodeCount < maxNodes) {
            val (current, depth) = queue.removeFirst()
            nodeCount++

            try {
                // If the user is actively typing in this node (isEditable and isFocused), skip inspecting it
                // until they finish typing/search, to avoid closing the app on incomplete prefixes like "por"
                val isActivelyTyping = current.isEditable && current.isFocused

                if (!isActivelyTyping) {
                    // Inspect text and content description
                    val text = current.text?.toString()
                    if (!text.isNullOrBlank()) {
                        val match = ContentFilterEngine.checkContent(text, customKeywords, strictMode)
                        if (match != null) {
                            return match.matchedTerm
                        }
                    }

                    val desc = current.contentDescription?.toString()
                    if (!desc.isNullOrBlank()) {
                        val match = ContentFilterEngine.checkContent(desc, customKeywords, strictMode)
                        if (match != null) {
                            return match.matchedTerm
                        }
                    }
                }

                // Inspect child nodes if depth allows
                if (depth < maxDepth) {
                    val childCount = current.childCount
                    for (i in 0 until childCount) {
                        val child = current.getChild(i)
                        if (child != null) {
                            queue.add(Pair(child, depth + 1))
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore transient node access exceptions when window hierarchy updates
            }
        }

        return null
    }

    private fun checkAndBlockSettingsTampering() {
        val rootNode = rootInActiveWindow ?: return
        try {
            val isTampering = detectSettingsTampering(rootNode, maxDepth = 6, maxNodes = 60)
            if (isTampering) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                triggerHapticAlert()
                handler.post {
                    Toast.makeText(
                        applicationContext,
                        "🔒 ራስን የመግዛት ጽኑ ጥበቃ፦ SafeGuard ን ማጥፋት ወይም መሰረዝ (Uninstall) የተከለከለ ነው!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } finally {
            try {
                rootNode.recycle()
            } catch (e: Exception) {
                // Recycle safely
            }
        }
    }

    private fun detectSettingsTampering(node: AccessibilityNodeInfo?, maxDepth: Int, maxNodes: Int): Boolean {
        if (node == null || maxDepth <= 0) return false

        var nodeCount = 0
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(Pair(node, 0))

        var mentionsSafeGuard = false
        var mentionsTamperAction = false
        var mentionsAccessibilityService = false

        val tamperKeywords = listOf(
            "uninstall", "force stop", "disable", "clear data", "clear storage",
            "deactivate", "remove", "turn off", "off", "stop", "delete",
            "አጥፋ", "አቁም", "ውሂብ አጽዳ", "አስተዳዳሪ አንሳ", "አትጠቀም", "ሰርዝ"
        )

        while (queue.isNotEmpty() && nodeCount < maxNodes) {
            val (current, depth) = queue.removeFirst()
            nodeCount++

            try {
                val text = (current.text?.toString() ?: "") + " " + (current.contentDescription?.toString() ?: "")
                val lower = text.lowercase()

                if (lower.contains("safeguard") || lower.contains("የይዘት መቆጣጠሪያ")) {
                    mentionsSafeGuard = true
                }
                if (lower.contains("accessibility") || lower.contains("ተደራሽነት") || lower.contains("safe guard")) {
                    mentionsAccessibilityService = true
                }
                for (tk in tamperKeywords) {
                    if (lower.contains(tk)) {
                        mentionsTamperAction = true
                    }
                }

                // If on SafeGuard's App Info or SafeGuard's Accessibility Service page and attempting to disable/uninstall
                if ((mentionsSafeGuard || mentionsAccessibilityService) && mentionsTamperAction) return true

                if (depth < maxDepth) {
                    for (i in 0 until current.childCount) {
                        current.getChild(i)?.let { queue.add(Pair(it, depth + 1)) }
                    }
                }
            } catch (e: Exception) {
                // Ignore transient hierarchy exceptions
            }
        }

        return (mentionsSafeGuard || mentionsAccessibilityService) && mentionsTamperAction
    }

    /**
     * Finds and instantly clears inappropriate search inputs or URL bars across Chrome, Phoenix, TikTok, YouTube, Facebook
     */
    private fun clearInappropriateSearchInput(sourceNode: AccessibilityNodeInfo?, rootNode: AccessibilityNodeInfo?) {
        try {
            // 1. Try directly on event source if editable
            if (sourceNode != null && sourceNode.isEditable) {
                wipeNodeText(sourceNode)
                return
            }

            // 2. Search root node for focused or editable nodes to wipe
            if (rootNode != null) {
                val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focusedNode != null && focusedNode.isEditable) {
                    wipeNodeText(focusedNode)
                    focusedNode.recycle()
                    return
                }

                // Scan for editable search boxes or clear buttons
                val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
                queue.add(Pair(rootNode, 0))
                var count = 0
                while (queue.isNotEmpty() && count < 35) {
                    val (current, depth) = queue.removeFirst()
                    count++

                    if (current.isEditable) {
                        wipeNodeText(current)
                        return
                    }

                    // Check for "Clear query" or "Clear text" button (X button) in search bars
                    val desc = current.contentDescription?.toString()?.lowercase() ?: ""
                    if (current.isClickable && (desc.contains("clear query") || desc.contains("clear text") || desc.contains("delete") || desc.contains("አጥፋ"))) {
                        current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return
                    }

                    if (depth < 4) {
                        for (i in 0 until current.childCount) {
                            current.getChild(i)?.let { queue.add(Pair(it, depth + 1)) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle safely
        }
    }

    private fun wipeNodeText(node: AccessibilityNodeInfo) {
        try {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } catch (e: Exception) {
            // Safely ignore
        }
    }

    private fun triggerBlock(packageName: String, term: String, reason: String) {
        lastBlockTimestamp = System.currentTimeMillis()
        val appDisplayName = ContentFilterEngine.getAppDisplayName(packageName)

        // Launch the Full-screen Bible Verse & Sobering Warning Activity immediately over Chrome/Phoenix
        try {
            val soberingIntent = Intent(applicationContext, SoberingBlockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(SoberingBlockActivity.EXTRA_APP_NAME, appDisplayName)
                putExtra(SoberingBlockActivity.EXTRA_BLOCKED_TERM, term)
            }
            startActivity(soberingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 1. Immediately kick user out of explicit content (Back or Home)
        val backSuccess = performGlobalAction(GLOBAL_ACTION_BACK)
        if (!backSuccess) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        // 2. Vibrate warning feedback
        if (prefs.vibrationAlert) {
            triggerHapticAlert()
        }

        // 3. Record in preferences log
        prefs.recordBlock(
            appName = appDisplayName,
            packageName = packageName,
            blockedTerm = term
        )

        // 4. Show SafeGuard alert Toast
        handler.post {
            Toast.makeText(
                applicationContext,
                "🛡️ SafeGuard ጥበቃ፦ ተገቢ ያልሆነ ይዘት በ $appDisplayName ላይ ታግዷል!",
                Toast.LENGTH_LONG
            ).show()
        }

        // 5. Send broadcast for live UI update
        val intent = Intent(ACTION_CONTENT_BLOCKED).apply {
            setPackage(applicationContext.packageName)
            putExtra(EXTRA_APP_NAME, appDisplayName)
            putExtra(EXTRA_BLOCKED_TERM, term)
        }
        sendBroadcast(intent)
    }

    private fun triggerHapticAlert() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 150, 100, 200),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(250)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }
}
