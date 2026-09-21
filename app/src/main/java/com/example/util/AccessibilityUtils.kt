package com.example.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.receiver.SafeGuardAdminReceiver
import com.example.service.ContentBlockerAccessibilityService

object AccessibilityUtils {

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (ContentBlockerAccessibilityService.isServiceRunning) {
            return true
        }

        val expectedServiceName = "${context.packageName}/${ContentBlockerAccessibilityService::class.java.name}"
        val expectedShortName = "${context.packageName}/.service.ContentBlockerAccessibilityService"

        // Method 1: Check enabled services from AccessibilityManager
        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            if (enabledServices != null) {
                for (service in enabledServices) {
                    val id = service.id
                    if (id.contains(context.packageName) && id.contains("ContentBlockerAccessibilityService")) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Method 2: Check Settings.Secure
        try {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = settingValue.split(":")
            for (component in colonSplitter) {
                if (component.equals(expectedServiceName, ignoreCase = true) ||
                    component.equals(expectedShortName, ignoreCase = true) ||
                    (component.contains(context.packageName) && component.contains("ContentBlockerAccessibilityService"))
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val componentName = ComponentName(context, SafeGuardAdminReceiver::class.java)
        return dpm?.isAdminActive(componentName) == true
    }

    fun requestDeviceAdmin(context: Context) {
        val componentName = ComponentName(context, SafeGuardAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "SafeGuard መተግበሪያ በስሜታዊነት እንዳይጠፋ ወይም እንዳይሰረዝ (Uninstall እንዳይሆን) ለመከላከል የተዘጋጀ አስተዳዳሪ ጥበቃ።"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
