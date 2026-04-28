package com.rainyscanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.rainyscanner.app.data.ScanHistory
import com.rainyscanner.app.ui.screen.HistoryScreen
import com.rainyscanner.app.ui.screen.ScannerScreen
import com.rainyscanner.app.ui.theme.RainyScannerTheme

class MainActivity : ComponentActivity() {
    private lateinit var scanHistory: ScanHistory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanHistory = ScanHistory(applicationContext)
        enableEdgeToEdge()

        setContent {
            RainyScannerTheme {
                App(scanHistory = scanHistory)
            }
        }
    }
}

@Composable
fun App(scanHistory: ScanHistory) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Scanner) }

    when (currentScreen) {
        Screen.Scanner -> {
            ScannerScreen(
                scanHistory = scanHistory,
                onNavigateToHistory = { currentScreen = Screen.History }
            )
        }
        Screen.History -> {
            HistoryScreen(
                scanHistory = scanHistory,
                onNavigateBack = { currentScreen = Screen.Scanner }
            )
        }
    }
}

private sealed class Screen {
    data object Scanner : Screen()
    data object History : Screen()
}