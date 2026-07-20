package com.qz.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.qz.agent.domain.model.PresetTheme

@Composable
fun QZAgentTheme(
    theme: PresetTheme = PresetTheme.QZ_PURPLE,
    content: @Composable () -> Unit
) {
    val colorScheme = ColorSchemes.forTheme(theme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QzTypography,
        shapes = QzShapes,
        content = {
            Surface(
                modifier = Modifier,
                color = colorScheme.background,
                content = content
            )
        }
    )
}
