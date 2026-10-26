package com.callvault.app.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object PhoneSetup {
    fun isOppoFamily(): Boolean {
        val m = android.os.Build.MANUFACTURER.lowercase()
        return m.contains("oppo") || m.contains("realme") || m.contains("oneplus")
    }

    fun requestBatteryExemption(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                openAppSettings(context)
            }
        }
    }

    fun openAutoStart(context: Context) {
        val targets = listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oplus.battery" to "com.oplus.startupapp.view.StartupAppListActivity",
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity"
        )
        for ((pkg, cls) in targets) {
            try {
                context.startActivity(
                    Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            } catch (e: Exception) {
                // try next
            }
        }
        openAppSettings(context)
    }

    fun openAppSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // nothing else to do
        }
    }
}
