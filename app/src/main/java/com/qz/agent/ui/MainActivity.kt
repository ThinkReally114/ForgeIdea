package com.qz.agent.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qz.agent.domain.model.PresetTheme
import com.qz.agent.ui.chat.ChatScreen
import com.qz.agent.ui.theme.QZAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by remember { mutableStateOf(PresetTheme.QZ_PURPLE) }
            QZAgentTheme(theme = theme) {
                ChatScreen()
            }
        }
    }
}
