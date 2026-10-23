package com.example.attendly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.attendly.ui.components.GlassmorphicBottomNavigation
import com.example.attendly.ui.navigation.AttendlyNavGraph
import com.example.attendly.ui.navigation.NavigationActions
import com.example.attendly.ui.navigation.Screen
import com.example.attendly.ui.theme.AttendlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.window_background)
        enableEdgeToEdge()
        setContent {
            AttendlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AttendlyApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.attendly.widget.WidgetUpdateWorker.updateNow(this)
    }
}

@Composable
fun AttendlyApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { (context.applicationContext as AttendlyApplication).preferences }
    val isRegistered = remember { preferences.isRegistrationCompleted() }
    val startDestination = if (isRegistered) Screen.Home.route else Screen.Onboarding.route

    val navController = rememberNavController()
    val navActions = remember(navController) { NavigationActions(navController) }
    val items = listOf(
        Screen.Home,
        Screen.Attendance,
        Screen.Timetable
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = items.any { it.route == currentDestination?.route }

    Box(modifier = Modifier.fillMaxSize()) {
        // App NavGraph fills full screen size so content naturally scrolls underneath floating glass dock
        AttendlyNavGraph(
            navController = navController,
            navActions = navActions,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        )

        // Floating glass navbar sits overlaying at the bottom with a solid dark bottom background
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            ) {
                GlassmorphicBottomNavigation(
                    items = items,
                    currentDestination = currentDestination,
                    onItemClick = { screen ->
                        navActions.navigateToTopLevelDestination(screen)
                    }
                )
            }
        }
    }
}
