package com.ollamachat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ollamachat.ui.chat.ChatScreen
import com.ollamachat.ui.chat.ChatViewModel
import com.ollamachat.ui.settings.SettingsScreen
import com.ollamachat.ui.settings.SettingsViewModel

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    chatViewModel: ChatViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CHAT
    ) {
        composable(Routes.CHAT) { entry ->
            val keySaved = entry.savedStateHandle.get<Boolean>("key_saved") ?: false
            LaunchedEffect(keySaved) {
                if (keySaved) {
                    chatViewModel.refreshSettingsState()
                    entry.savedStateHandle.remove<Boolean>("key_saved")
                }
            }
            ChatScreen(
                viewModel = chatViewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = {
                    settingsViewModel.onNavigatedAway()
                    navController.popBackStack()
                },
                onKeySaved = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("key_saved", true)
                    navController.popBackStack()
                    settingsViewModel.onNavigatedAway()
                }
            )
        }
    }
}
