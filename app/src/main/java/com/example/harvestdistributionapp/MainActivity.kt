package com.example.harvestdistributionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.harvestdistributionapp.ui.navigation.NavGraph
import com.example.harvestdistributionapp.ui.theme.HarvestDistributionTheme
import com.example.harvestdistributionapp.data.ThemeMode

class MainActivity : ComponentActivity() {
    private val appViewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState = appViewModel.uiState.collectAsStateWithLifecycle().value
            val useDarkTheme = when (uiState.appState.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            HarvestDistributionTheme(darkTheme = useDarkTheme) {
                NavGraph(appViewModel)
            }
        }
    }
}
