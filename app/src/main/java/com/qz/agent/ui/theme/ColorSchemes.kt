package com.qz.agent.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.qz.agent.domain.model.PresetTheme

object ColorSchemes {
    val QzPurpleDark = darkColorScheme(
        primary = Color(0xFFC4A3FF),
        onPrimary = Color(0xFF1A1B2E),
        primaryContainer = Color(0xFF2D1B4E),
        onPrimaryContainer = Color(0xFFEDE9FE),
        secondary = Color(0xFF7FD4FF),
        onSecondary = Color(0xFF0F1A2E),
        secondaryContainer = Color(0xFF1B3A4E),
        background = Color(0xFF0F0F1E),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF1A1B2E),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF27272A),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outline = Color(0xFF3F3F46)
    )

    val DeepSpaceDark = darkColorScheme(
        primary = Color(0xFF7FD4FF),
        onPrimary = Color(0xFF0F1A2E),
        primaryContainer = Color(0xFF0C2740),
        onPrimaryContainer = Color(0xFFE0F2FE),
        secondary = Color(0xFFC4A3FF),
        onSecondary = Color(0xFF1A1B2E),
        background = Color(0xFF050A14),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF0A1428),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF162238),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF2E4057)
    )

    val AuroraGreenDark = darkColorScheme(
        primary = Color(0xFF7FFFD4),
        onPrimary = Color(0xFF0A1F1A),
        primaryContainer = Color(0xFF0F2E26),
        onPrimaryContainer = Color(0xFFD1FAE5),
        secondary = Color(0xFFC4A3FF),
        onSecondary = Color(0xFF1A1B2E),
        background = Color(0xFF0A1410),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF0F1A14),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF1A2A20),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF2E4035)
    )

    val WarmOrangeDark = darkColorScheme(
        primary = Color(0xFFFFB877),
        onPrimary = Color(0xFF2A1400),
        primaryContainer = Color(0xFF3D1F00),
        onPrimaryContainer = Color(0xFFFFE0C2),
        secondary = Color(0xFFFF7F7F),
        onSecondary = Color(0xFF2A0000),
        background = Color(0xFF1A0F0A),
        onBackground = Color(0xFFF4F4F5),
        surface = Color(0xFF241410),
        onSurface = Color(0xFFF4F4F5),
        surfaceVariant = Color(0xFF33201A),
        onSurfaceVariant = Color(0xFFA1A1AA),
        outline = Color(0xFF4D322A)
    )

    fun forTheme(theme: PresetTheme): ColorScheme = when (theme) {
        PresetTheme.QZ_PURPLE -> QzPurpleDark
        PresetTheme.DEEP_SPACE -> DeepSpaceDark
        PresetTheme.AURORA_GREEN -> AuroraGreenDark
        PresetTheme.WARM_ORANGE -> WarmOrangeDark
    }
}
