package com.callvault.app.data.model

data class CompatibilityReport(
    val manufacturer: String,
    val model: String,
    val supportLevel: DeviceSupportLevel,
    val notes: String
)
