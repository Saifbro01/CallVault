package com.callvault.app.ui.navigation

sealed class Screen(val route: String) {
    data object Recordings : Screen("recordings")
    data object Trash : Screen("trash")
    data object Settings : Screen("settings")
}
