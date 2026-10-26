package com.callvault.app

import android.app.Application
import com.callvault.app.data.database.AppDatabase

class CallVaultApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
}
