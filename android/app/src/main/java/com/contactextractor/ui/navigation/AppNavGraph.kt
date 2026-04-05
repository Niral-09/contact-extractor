package com.contactextractor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.contactextractor.ui.home.HomeScreen
import com.contactextractor.ui.results.ResultsScreen
import com.contactextractor.ui.settings.SettingsScreen

object Routes {
    const val SETTINGS = "settings"
    const val HOME = "home"
    const val RESULTS = "results"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val startDestination = remember {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context, "secure_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val hasKey = (prefs.getString("gemini_api_key", "") ?: "").isNotBlank()
        val hasSheet = (prefs.getString("spreadsheet_id", "") ?: "").isNotBlank()
        if (hasKey && hasSheet) Routes.HOME else Routes.SETTINGS
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onConfigured = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SETTINGS) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onContactsExtracted = { navController.navigate(Routes.RESULTS) },
                onSettingsTapped = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.RESULTS) {
            ResultsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
