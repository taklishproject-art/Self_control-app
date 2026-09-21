package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class SafeGuardAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(
            context,
            "🛡️ SafeGuard የስልክ አስተዳዳሪ ሆነ! መተግበሪያው እንዳይሰረዝ ጥበቃ ተደርጓል።",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "⚠️ ማስጠንቀቂያ፦ SafeGuard ን ማጥፋት የራስ-መቆጣጠሪያ ጥበቃዎን ያፈርሳል! እርግጠኛ ነዎት?"
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(
            context,
            "SafeGuard አስተዳዳሪ ጥበቃ ጠፍቷል።",
            Toast.LENGTH_SHORT
        ).show()
    }
}
