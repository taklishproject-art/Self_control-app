package com.example.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Modern Full-Screen Floating Gesture Navigation Engine (Samsung & iPhone style Gesture Bar)
 * Operates completely in the background without any dashboard clutter.
 * 
 * Gestures:
 * 1. Swipe Up -> Return to Home (GLOBAL_ACTION_HOME)
 * 2. Swipe Up and Hold -> Show Recents / App Switcher (GLOBAL_ACTION_RECENTS)
 * 3. Swipe Left or Right -> Go Back (GLOBAL_ACTION_BACK)
 * 4. Tap / Double Tap -> Quick Back
 */
class ModernGestureNavigationEngine(
    private val service: AccessibilityService
) {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val vibrator = service.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val handler = Handler(Looper.getMainLooper())

    private var gestureRootView: FrameLayout? = null
    private var isGestureBarAttached = false

    // Touch tracking
    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L
    private var isHoldTriggered = false

    private val holdRunnable = Runnable {
        isHoldTriggered = true
        triggerHaptic()
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    fun start() {
        if (isGestureBarAttached) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(service)) {
            return
        }

        handler.post {
            try {
                attachGestureBar()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        handler.post {
            try {
                if (gestureRootView != null && isGestureBarAttached) {
                    windowManager.removeView(gestureRootView)
                    gestureRootView = null
                    isGestureBarAttached = false
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachGestureBar() {
        val density = service.resources.displayMetrics.density
        val barHeight = (28 * density).toInt()
        val pillWidth = (140 * density).toInt()
        val pillHeight = (5 * density).toInt()

        val root = FrameLayout(service).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Elegant Rounded Pill View (like Samsung OneUI & iOS Home Bar)
        val pillView = View(service).apply {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f * density
                setColor(Color.WHITE)
            }
            background = drawable
            alpha = 0.75f
        }

        val pillParams = FrameLayout.LayoutParams(pillWidth, pillHeight).apply {
            gravity = Gravity.CENTER
        }
        root.addView(pillView, pillParams)

        root.setOnTouchListener { _, event ->
            handleTouchEvent(event, pillView)
            true
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            barHeight,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        windowManager.addView(root, params)
        gestureRootView = root
        isGestureBarAttached = true
    }

    private fun handleTouchEvent(event: MotionEvent, pillView: View) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                startTime = System.currentTimeMillis()
                isHoldTriggered = false
                pillView.animate().scaleX(1.15f).scaleY(1.3f).alpha(1.0f).setDuration(120).start()
                // Schedule swipe-and-hold for Recents
                handler.postDelayed(holdRunnable, 350L)
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = startY - event.rawY
                val deltaX = event.rawX - startX

                if (abs(deltaX) > 40 || deltaY > 30) {
                    // Finger moved enough; cancel simple hold if it wasn't triggered
                    if (deltaY < 50 && abs(deltaX) > 40) {
                        handler.removeCallbacks(holdRunnable)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(holdRunnable)
                pillView.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.75f).setDuration(180).start()

                if (isHoldTriggered) {
                    return
                }

                val endX = event.rawX
                val endY = event.rawY
                val deltaX = endX - startX
                val deltaY = startY - endY // positive means swiped up
                val duration = System.currentTimeMillis() - startTime

                when {
                    // 1. Swipe Up -> Go to Home
                    deltaY > 50 && abs(deltaX) < 120 -> {
                        triggerHaptic()
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    }

                    // 2. Swipe Left or Right -> Go Back
                    abs(deltaX) > 60 && deltaY < 80 -> {
                        triggerHaptic()
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    }

                    // 3. Simple quick tap on pill -> Go Back
                    duration < 250 && abs(deltaX) < 25 && abs(deltaY) < 25 -> {
                        triggerHaptic()
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    }
                }
            }
        }
    }

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
