package com.forgeidea.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.forgeidea.domain.model.PresetTheme

@Composable
fun ForgeIdeaTheme(
    theme: PresetTheme = PresetTheme.PURPLE,
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
