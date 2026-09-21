package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.BlockPreferences
import com.example.ui.ShieldScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AccessibilityUtils

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Automatically enable protections and anti-uninstall by default
    val prefs = BlockPreferences(this)
    prefs.isAntiUninstallLockEnabled = true
    prefs.isUnbreakableModeEnabled = true
    prefs.isProtectionEnabled = true

    if (!AccessibilityUtils.isDeviceAdminActive(this) && !prefs.hasPromptedAdminOnLaunch) {
      prefs.hasPromptedAdminOnLaunch = true
      try {
        AccessibilityUtils.requestDeviceAdmin(this)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    setContent {
      MyApplicationTheme {
        ShieldScreen()
      }
    }
  }
}

