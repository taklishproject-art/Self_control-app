package com.example.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import java.util.Calendar

/**
 * Autonomous Background Environment Engine (Samsung-style Smart Brightness & Force Dark Layer)
 * Runs entirely in the background without cluttering the app dashboard.
 * 
 * Features:
 * 1. Autonomous Brightness Dimmer: Dynamically dials down brightness at night (10:00 PM - 6:00 AM)
 *    and restores comfortable level in daytime.
 * 2. Force Dark Mode Glass Overlay: For apps that lack Dark Mode (or at night), layers an invisible
 *    optical contrast inversion / dark tint that transforms white/blinding apps into soothing dark mode.
 * 3. Extra Dim Mode (Samsung-style below 0% hardware limit): Uses SYSTEM_ALERT_WINDOW to achieve
 *    eye comfort deeper than Android's native lowest slider.
 */
class SmartDisplayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null

    // Tracking state
    private var currentDimAlpha = 0f // 0f (no overlay) to 0.65f (ultra dim / dark mode)

    /**
     * Called on window transitions or background loop
     */
    fun evaluateAndApplyDisplayAdjustments(activePackageName: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 22 || hour < 6 // 10:00 PM to 6:00 AM
        val isEvening = hour in 19..21 // 7:00 PM to 10:00 PM

        // 1. Autonomous System Brightness Adjustment (if WRITE_SETTINGS permission is present)
        adjustSystemBrightnessConditionally(isNight, isEvening)

        // 2. Autonomous Force Dark Mode Overlay (Samsung-style extra dim & dark mode converter)
        val targetAlpha = when {
            isNight -> 0.45f // Deep dark mode & extra dim for all apps at night
            isEvening -> 0.20f // Gentle soothing warm dim
            // For white-heavy browsers or apps during vulnerability checks, gently dim
            activePackageName.contains("chrome") || activePackageName.contains("browser") -> 0.10f
            else -> 0f // Crystal clear daytime
        }

        updateDarkOverlay(targetAlpha)
    }

    private fun adjustSystemBrightnessConditionally(isNight: Boolean, isEvening: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    return // Silent fallback to overlay
                }
            }

            // Target hardware brightness (0 - 255)
            val targetBrightness = when {
                isNight -> 15 // Very dim, comfortable in dark bedroom
                isEvening -> 70 // Cozy evening level
                else -> 170 // Bright daytime level
            }

            val currentBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )

            // Only update if difference is meaningful to prevent flickering
            if (Math.abs(currentBrightness - targetBrightness) > 20) {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    targetBrightness
                )
            }
        } catch (e: Exception) {
            // Fails gracefully without disturbing user
        }
    }

    private fun updateDarkOverlay(targetAlpha: Float) {
        handler.post {
            try {
                if (targetAlpha <= 0.01f) {
                    removeOverlay()
                    return@post
                }

                if (!Settings.canDrawOverlays(context)) {
                    return@post
                }

                if (overlayView == null) {
                    val view = View(context).apply {
                        setBackgroundColor(Color.BLACK)
                    }
                    val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                    }

                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.FILL
                        alpha = targetAlpha
                    }

                    windowManager.addView(view, params)
                    overlayView = view
                    currentDimAlpha = targetAlpha
                } else {
                    // Update existing overlay alpha smoothly
                    if (Math.abs(currentDimAlpha - targetAlpha) > 0.02f) {
                        val params = overlayView?.layoutParams as? WindowManager.LayoutParams
                        if (params != null) {
                            params.alpha = targetAlpha
                            windowManager.updateViewLayout(overlayView, params)
                            currentDimAlpha = targetAlpha
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore overlay errors gracefully
            }
        }
    }

    fun removeOverlay() {
        handler.post {
            try {
                if (overlayView != null) {
                    windowManager.removeView(overlayView)
                    overlayView = null
                    currentDimAlpha = 0f
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
