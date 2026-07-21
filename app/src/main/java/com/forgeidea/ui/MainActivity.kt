package com.forgeidea.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.forgeidea.ui.chat.ChatScreen
import com.forgeidea.ui.settings.SettingsScreen
import com.forgeidea.ui.theme.ForgeIdeaTheme
import com.forgeidea.ui.theme.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = koinViewModel()
            val theme by themeViewModel.theme.collectAsState()
            val navController = rememberNavController()

            ForgeIdeaTheme(theme = theme) {
                NavHost(navController = navController, startDestination = Routes.CHAT) {
                    composable(Routes.CHAT) {
                        ChatScreen(
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                        )
                    }
                    composable(
                        route = Routes.SETTINGS,
                        enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } },
                        exitTransition = { fadeOut(animationSpec = tween(300)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                        popExitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } }
                    ) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
