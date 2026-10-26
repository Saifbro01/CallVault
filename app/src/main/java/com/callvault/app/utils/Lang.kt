package com.callvault.app.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Lang {
    var bangla by mutableStateOf(false)
}

fun tr(en: String, bn: String): String = if (Lang.bangla) bn else en
