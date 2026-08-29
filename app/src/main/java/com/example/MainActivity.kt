package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.counter.CounterScreen
import com.example.ui.screens.counter.CounterViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.onboarding.WelcomeScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.AhlAlQuranTheme

enum class MainTab(val titleArabic: String, val testTag: String) {
    HOME("الرئيسية", "tab_home"),
    COUNTER("المسبحة", "tab_counter"),
    REPORTS("التقرير", "tab_reports"),
    SETTINGS("الإعدادات", "tab_settings")
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val counterViewModel: CounterViewModel by viewModels()
    private val reportsViewModel: ReportsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()

            AhlAlQuranTheme(
                darkTheme = settingsState.isDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme()
            ) {
                // Ensure full RTL layout for Arabic
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (!settingsState.isOnboarded) {
                            WelcomeScreen(
                                onStartClick = { city ->
                                    settingsViewModel.completeOnboarding(city)
                                    homeViewModel.loadTodayData()
                                }
                            )
                        } else {
                            MainAppContent(
                                homeViewModel = homeViewModel,
                                counterViewModel = counterViewModel,
                                reportsViewModel = reportsViewModel,
                                settingsViewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    homeViewModel: HomeViewModel,
    counterViewModel: CounterViewModel,
    reportsViewModel: ReportsViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var targetCounterKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val (icon, selectedIcon) = when (tab) {
                        MainTab.HOME -> Icons.Outlined.Home to Icons.Filled.Home
                        MainTab.COUNTER -> Icons.Outlined.TouchApp to Icons.Filled.TouchApp
                        MainTab.REPORTS -> Icons.Outlined.BarChart to Icons.Filled.BarChart
                        MainTab.SETTINGS -> Icons.Outlined.Settings to Icons.Filled.Settings
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) selectedIcon else icon,
                                contentDescription = tab.titleArabic
                            )
                        },
                        label = {
                            Text(
                                text = tab.titleArabic,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToCounter = { key ->
                            targetCounterKey = key
                            currentTab = MainTab.COUNTER
                        },
                        onNavigateToReports = {
                            currentTab = MainTab.REPORTS
                        },
                        onNavigateToSettings = {
                            currentTab = MainTab.SETTINGS
                        }
                    )
                }
                MainTab.COUNTER -> {
                    CounterScreen(
                        viewModel = counterViewModel,
                        initialCounterKey = targetCounterKey
                    )
                }
                MainTab.REPORTS -> {
                    ReportsScreen(
                        viewModel = reportsViewModel,
                        onNavigateBack = { currentTab = MainTab.HOME }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { currentTab = MainTab.HOME }
                    )
                }
            }
        }
    }
}
