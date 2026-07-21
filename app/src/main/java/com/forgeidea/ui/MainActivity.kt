package com.forgeidea.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.forgeidea.ui.chat.ChatScreen
import com.forgeidea.ui.theme.ForgeIdeaTheme
import com.forgeidea.ui.theme.ThemeViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = koinViewModel()
            val theme by themeViewModel.theme.collectAsState()

            ForgeIdeaTheme(theme = theme) {
                ChatScreen()
            }
        }
    }
}
