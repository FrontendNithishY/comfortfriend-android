package com.comfortfriend.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comfortfriend.app.AppContainer

private fun NavHostController.tab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNav(container: AppContainer, mainVm: MainViewModel) {
    val settings by mainVm.settings.collectAsState()
    val s = settings ?: return // wait for DataStore's first read (very fast)
    val nav = rememberNavController()
    val start = if (s.onboarded) Routes.HOME else Routes.WELCOME

    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(onStart = { name ->
                mainVm.finishOnboarding(name)
                nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(container, onNavigate = nav::tab, onOpenChat = { id, mode -> nav.navigate(Routes.chat(id, mode)) })
        }
        composable(Routes.MINDLENS) { MindLensScreen(container, onNavigate = nav::tab) }
        composable(Routes.MOOD) { MoodScreen(container, onNavigate = nav::tab) }
        composable(Routes.MEMORY) { MemoryScreen(container, onNavigate = nav::tab) }
        composable(Routes.SETTINGS) { SettingsScreen(container, onNavigate = nav::tab) }
        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("mode") { type = NavType.StringType },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val mode = entry.arguments?.getString("mode") ?: "AUTO"
            ChatScreen(
                container, id, mode,
                onBack = { nav.popBackStack() },
                onNewChat = { nav.navigate(Routes.chat(0L, "AUTO")) { popUpTo(Routes.HOME) } },
            )
        }
    }
}
