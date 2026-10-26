package com.callvault.app.data.prefs

import android.content.Context
import java.security.MessageDigest

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("callvault_prefs", Context.MODE_PRIVATE)

    var autoRecord: Boolean
        get() = sp.getBoolean("auto_record", false)
        set(v) { sp.edit().putBoolean("auto_record", v).apply() }

    var retentionDays: Int
        get() = sp.getInt("retention_days", 30)
        set(v) { sp.edit().putInt("retention_days", v).apply() }

    var biometrics: Boolean
        get() = sp.getBoolean("biometrics", true)
        set(v) { sp.edit().putBoolean("biometrics", v).apply() }

    var bitrateKbps: Int
        get() = sp.getInt("bitrate_kbps", 96)
        set(v) { sp.edit().putInt("bitrate_kbps", v).apply() }

    fun hasPin(): Boolean = sp.getString("pin_hash", null) != null

    fun setPin(pin: String) { sp.edit().putString("pin_hash", hash(pin)).apply() }

    fun clearPin() { sp.edit().remove("pin_hash").apply() }

    fun checkPin(pin: String): Boolean = sp.getString("pin_hash", null) == hash(pin)

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256").digest("callvault:$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }
}
