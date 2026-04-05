package com.contactextractor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.contactextractor.ui.navigation.AppNavGraph
import com.contactextractor.ui.theme.ContactExtractorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContactExtractorTheme {
                AppNavGraph()
            }
        }
    }
}
