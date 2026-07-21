package com.qz.agent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qz.agent.domain.model.PresetTheme
import com.qz.agent.ui.chat.ChatScreen
import com.qz.agent.ui.settings.SettingsScreen
import com.qz.agent.ui.theme.ForgeIdeaTheme

object Routes {
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by remember { mutableStateOf(PresetTheme.QZ_PURPLE) }
            val navController = rememberNavController()

            ForgeIdeaTheme(theme = theme) {
                NavHost(navController = navController, startDestination = Routes.CHAT) {
                    composable(Routes.CHAT) {
                        ChatScreen(
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onThemeChanged = { theme = it }
                        )
                    }
                }
            }
        }
    }
}
