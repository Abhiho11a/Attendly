package com.example.attendly.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.attendly.ui.screens.attendance.AttendanceScreen
import com.example.attendly.ui.screens.home.HomeScreen
import com.example.attendly.ui.screens.import.ImportTimetableScreen
import com.example.attendly.ui.screens.settings.SettingsScreen
import com.example.attendly.ui.screens.subject.SubjectDetailsScreen
import com.example.attendly.ui.screens.timetable.TimetableScreen

@Composable
fun AttendlyNavGraph(
    navController: NavHostController,
    navActions: NavigationActions,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    val topLevelFadeEnter = fadeIn(animationSpec = tween(160, easing = FastOutSlowInEasing))
    val topLevelFadeExit = fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { topLevelFadeEnter },
        exitTransition = { topLevelFadeExit },
        popEnterTransition = { topLevelFadeEnter },
        popExitTransition = { topLevelFadeExit }
    ) {
        composable(
            route = Screen.Onboarding.route,
            enterTransition = { topLevelFadeEnter },
            exitTransition = { topLevelFadeExit }
        ) {
            com.example.attendly.ui.screens.onboarding.OnboardingScreen(
                onOnboardingCompleted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.Home.route,
            enterTransition = { topLevelFadeEnter },
            exitTransition = { topLevelFadeExit }
        ) {
            HomeScreen(
                onNavigateToTimetable = {
                    navActions.navigateToTopLevelDestination(Screen.Timetable)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(
            route = Screen.Attendance.route,
            enterTransition = { topLevelFadeEnter },
            exitTransition = { topLevelFadeExit }
        ) {
            AttendanceScreen(
                onNavigateToSubjectDetails = { subjectId ->
                    navController.navigate(Screen.SubjectDetails.createRoute(subjectId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(
            route = Screen.Timetable.route,
            enterTransition = { topLevelFadeEnter },
            exitTransition = { topLevelFadeExit }
        ) {
            TimetableScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToImport = {
                    navController.navigate(Screen.ImportTimetable.route)
                }
            )
        }
        composable(
            route = Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 10 }, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(200, easing = FastOutSlowInEasing)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 10 }, animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(180, easing = FastOutSlowInEasing)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 10 }, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(200, easing = FastOutSlowInEasing)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 10 }, animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(180, easing = FastOutSlowInEasing)) }
        ) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.ImportTimetable.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 10 }, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(200, easing = FastOutSlowInEasing)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 10 }, animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(180, easing = FastOutSlowInEasing)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 10 }, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(200, easing = FastOutSlowInEasing)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 10 }, animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(180, easing = FastOutSlowInEasing)) }
        ) {
            ImportTimetableScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.SubjectDetails.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 10 }, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(200, easing = FastOutSlowInEasing)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 10 }, animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(180, easing = FastOutSlowInEasing)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 10 }, animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(200, easing = FastOutSlowInEasing)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 10 }, animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(180, easing = FastOutSlowInEasing)) }
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: return@composable
            SubjectDetailsScreen(
                subjectId = subjectId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
