package com.pulseforge.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pulseforge.mobile.datalayer.PhoneDataLayerManager
import com.pulseforge.mobile.ui.screens.CalibrationWizardScreen
import com.pulseforge.mobile.ui.screens.DashboardScreen
import com.pulseforge.mobile.ui.screens.HistoryScreen
import com.pulseforge.mobile.ui.screens.LiveSignalScreen
import com.pulseforge.mobile.ui.screens.SettingsScreen
import com.pulseforge.mobile.ui.theme.DarkBackground
import com.pulseforge.mobile.ui.theme.DarkSurface
import com.pulseforge.mobile.ui.theme.NeonGreen
import com.pulseforge.mobile.ui.theme.PulseForgeTheme
import com.pulseforge.mobile.ui.theme.TextMuted

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Signal : Screen("signal", "Signal", Icons.Default.GraphicEq)
    object Calibration : Screen("calibration", "Calibrate", Icons.Default.Speed)
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var dataLayerManager: PhoneDataLayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataLayerManager = PhoneDataLayerManager(this)

        setContent {
            PulseForgeTheme {
                MainApp(dataLayerManager = dataLayerManager)
            }
        }
    }
}

@Composable
fun MainApp(dataLayerManager: PhoneDataLayerManager) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val items = listOf(
        Screen.Dashboard,
        Screen.Signal,
        Screen.Calibration,
        Screen.History,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = NeonGreen
            ) {
                items.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = NeonGreen,
                            indicatorColor = NeonGreen,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    dataLayerManager = dataLayerManager,
                    onNavigateToSignal = { currentScreen = Screen.Signal },
                    onNavigateToCalibration = { currentScreen = Screen.Calibration }
                )
                Screen.Signal -> LiveSignalScreen()
                Screen.Calibration -> CalibrationWizardScreen()
                Screen.History -> HistoryScreen()
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}
