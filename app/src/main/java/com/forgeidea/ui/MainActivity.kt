package com.forgeidea.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.forgeidea.domain.model.PresetTheme
import com.forgeidea.ui.chat.ChatScreen
import com.forgeidea.ui.git.GitScreen
import com.forgeidea.ui.settings.SettingsScreen
import com.forgeidea.ui.terminal.TerminalScreen
import com.forgeidea.ui.theme.ForgeIdeaTheme

object Routes {
    const val CHAT = "chat"
    const val TERMINAL = "terminal"
    const val GIT = "git"
    const val SETTINGS = "settings"
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by remember { mutableStateOf(PresetTheme.QZ_PURPLE) }
            val navController = rememberNavController()
            val navItems = listOf(
                NavItem(Routes.CHAT, "聊天", Icons.AutoMirrored.Filled.Message),
                NavItem(Routes.TERMINAL, "终端", Icons.Default.Terminal),
                NavItem(Routes.GIT, "Git", Icons.Default.Terminal),
                NavItem(Routes.SETTINGS, "设置", Icons.Default.Settings)
            )
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

            ForgeIdeaTheme(theme = theme) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(Routes.CHAT) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Routes.CHAT,
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(Routes.CHAT) {
                            ChatScreen(onOpenSettings = { navController.navigate(Routes.SETTINGS) })
                        }
                        composable(Routes.TERMINAL) {
                            TerminalScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.GIT) {
                            GitScreen(onBack = { navController.popBackStack() })
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
}
