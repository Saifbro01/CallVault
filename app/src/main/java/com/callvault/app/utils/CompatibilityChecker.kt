package com.callvault.app.utils

import android.os.Build
import com.callvault.app.data.model.CompatibilityReport
import com.callvault.app.data.model.DeviceSupportLevel

object CompatibilityChecker {
    fun generateReport(): CompatibilityReport {
        val maker = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.uppercase()
        val (level, note) = when {
            model.contains("CPH2219") ->
                DeviceSupportLevel.FULL to tr(
                    "OPPO F19 detected (ColorOS). Fully supported: microphone recording, widget, quick tile and import from the built-in recorder.",
                    "OPPO F19 (ColorOS) পাওয়া গেছে। পুরোপুরি সাপোর্টেড: মাইক রেকর্ডিং, উইজেট, কুইক টাইল এবং ফোনের নিজস্ব রেকর্ডার থেকে ইম্পোর্ট।"
                )
            maker.contains("oppo") || maker.contains("realme") || maker.contains("oneplus") ->
                DeviceSupportLevel.FULL to tr(
                    "ColorOS/OxygenOS device. Allow auto-launch and unrestricted battery for best results.",
                    "ColorOS/OxygenOS ফোন। ভালো কাজের জন্য অটো-লঞ্চ ও ব্যাটারি আনরেস্ট্রিক্টেড দিন।"
                )
            maker.contains("samsung") ->
                DeviceSupportLevel.FULL to tr(
                    "Samsung One UI. Set battery usage to Unrestricted.",
                    "Samsung One UI। ব্যাটারি ব্যবহার Unrestricted করে দিন।"
                )
            maker.contains("xiaomi") || maker.contains("redmi") || maker.contains("poco") ->
                DeviceSupportLevel.FULL to tr(
                    "Xiaomi/HyperOS. Enable Autostart and set battery saver to No restrictions.",
                    "Xiaomi/HyperOS। Autostart চালু করুন ও ব্যাটারি সেভার No restrictions দিন।"
                )
            else -> DeviceSupportLevel.PARTIAL to tr(
                "Microphone-based recording works on all Android phones.",
                "মাইক-ভিত্তিক রেকর্ডিং সব অ্যান্ড্রয়েড ফোনে কাজ করে।"
            )
        }
        return CompatibilityReport(Build.MANUFACTURER, Build.MODEL, level, note)
    }
}
