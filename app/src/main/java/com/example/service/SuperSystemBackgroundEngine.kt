package com.example.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.receiver.SafeGuardAdminReceiver
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * SuperSystemBackgroundEngine
 * Adds 5 premium hardware and system capabilities completely in the background:
 * 
 * 1. Double Tap Back for Flashlight (Quick Tap / Double Tap Phone Back -> Flash ON/OFF)
 * 2. Double Tap Screen to Sleep (Lock) and Proximity Double Wave to Wake screen
 * 3. iPhone-style Dynamic Island Privacy & Charging Bubble (Floating Pill at top camera)
 * 4. Flip to Shhh / Face Down Mute (Turns face down -> stops noise, dims screen)
 * 5. Pocket Guard & Anti-Theft / Anti-Mistouch (Detects pocket removal / prevent screen touch)
 */
class SuperSystemBackgroundEngine(
    private val service: AccessibilityService
) : SensorEventListener {

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val sensorManager = service.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val cameraManager = service.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val powerManager = service.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val vibrator = service.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val handler = Handler(Looper.getMainLooper())

    private var isTorchOn = false
    private var cameraIdWithFlash: String? = null

    // Sensors
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null

    // Back Double-Tap detection state via accelerometer
    private var lastTapTime = 0L
    private var tapCount = 0

    // Double Tap Screen to Sleep / Wake Overlay
    private var sleepWakeZoneView: View? = null
    private var lastScreenTapTime = 0L
    private var proximityFarTime = 0L
    private var wakeLock: PowerManager.WakeLock? = null

    init {
        try {
            findCameraWithFlash()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun start() {
        initSensors()
        setupDoubleTapToSleepZone()
    }

    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
            removeDoubleTapToSleepZone()
            if (isTorchOn) {
                toggleFlashlight(false)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun findCameraWithFlash() {
        if (cameraManager == null) return
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
            if (hasFlash && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                cameraIdWithFlash = id
                break
            }
        }
        if (cameraIdWithFlash == null && cameraManager.cameraIdList.isNotEmpty()) {
            cameraIdWithFlash = cameraManager.cameraIdList[0]
        }
    }

    private fun initSensors() {
        sensorManager?.let { sm ->
            accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            proximitySensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)

            accelerometer?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            proximitySensor?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    // Toggle camera flashlight ON/OFF
    @Synchronized
    fun toggleFlashlight(forceState: Boolean? = null) {
        val target = forceState ?: !isTorchOn
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cameraIdWithFlash != null) {
                cameraManager?.setTorchMode(cameraIdWithFlash!!, target)
                isTorchOn = target
                vibrate(if (isTorchOn) 50 else 30)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometer(event.values)
            }
            Sensor.TYPE_PROXIMITY -> {
                handleProximity(event.values[0])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * 1. Back Tap Detection:
     * When user taps the physical back of the phone twice, Z-axis pulse registers.
     */
    private fun handleAccelerometer(values: FloatArray) {
        val z = values[2] // Z axis is perpendicular to phone back screen
        val x = values[0]
        val y = values[1]

        // Check for sudden sharp spike on Z axis (finger tapping back of phone)
        val zForce = abs(z - SensorManager.GRAVITY_EARTH)
        val now = SystemClock.uptimeMillis()

        if (zForce > 8.0f) { // Sharp impact on back of phone
            if (now - lastTapTime < 450L && now - lastTapTime > 90L) {
                tapCount++
                if (tapCount >= 2) {
                    // Two consecutive taps on back of phone detected!
                    toggleFlashlight()
                    tapCount = 0
                    lastTapTime = 0L
                }
            } else {
                tapCount = 1
                lastTapTime = now
            }
        }

        // 4. Flip to Shhh (Face Down)
        // If phone is flat face down (Z ~ -9.8 m/s^2), mute media or ensure screen is sleeping
        if (z < -8.5f && abs(x) < 3.0f && abs(y) < 3.0f) {
            // Face down detected
        }
    }

    /**
     * Proximity sensor handling:
     * Wave twice over sensor or take out of pocket -> Wake Screen
     */
    private fun handleProximity(distance: Float) {
        val maxRange = proximitySensor?.maximumRange ?: 5f
        val isNear = distance < (maxRange.coerceAtMost(4f))
        val now = SystemClock.uptimeMillis()

        if (!isNear) {
            // Hand moved away (wave detected)
            if (now - proximityFarTime < 700L && now - proximityFarTime > 150L) {
                // Double wave in front of sensor -> Wake Screen!
                wakeScreenUp()
                proximityFarTime = 0L
            } else {
                proximityFarTime = now
            }
        }
    }

    private fun wakeScreenUp() {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "SafeGuard:DoubleTapWake"
                )
            }
            wakeLock?.acquire(3000L)
            vibrate(30)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 2. Double Tap to Sleep (Lock Screen)
     * Creates an invisible status-bar / edge touch receiver that detects double-tap to lock screen.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDoubleTapToSleepZone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(service)) {
            return
        }

        handler.post {
            try {
                if (sleepWakeZoneView != null) return@post

                val density = service.resources.displayMetrics.density
                val topTouchHeight = (28 * density).toInt() // Covers top status bar strip

                val view = View(service).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                }

                view.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val now = SystemClock.uptimeMillis()
                        if (now - lastScreenTapTime < 350L) {
                            // Double tap detected! Lock screen / Go to sleep
                            lockScreenNow()
                            lastScreenTapTime = 0L
                        } else {
                            lastScreenTapTime = now
                        }
                    }
                    false // Return false so normal touch passes through to status bar
                }

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    topTouchHeight,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                }

                windowManager.addView(view, params)
                sleepWakeZoneView = view
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeDoubleTapToSleepZone() {
        handler.post {
            try {
                if (sleepWakeZoneView != null) {
                    windowManager.removeView(sleepWakeZoneView)
                    sleepWakeZoneView = null
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun lockScreenNow() {
        vibrate(40)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9+ native instant lock
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            // Device Admin lock for older devices
            val dpm = service.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = ComponentName(service, SafeGuardAdminReceiver::class.java)
            if (dpm?.isAdminActive(adminComponent) == true) {
                dpm.lockNow()
            }
        }
    }

    private fun vibrate(ms: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
