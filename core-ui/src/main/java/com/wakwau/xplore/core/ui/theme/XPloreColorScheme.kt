package com.wakwau.xplore.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

@Immutable
data class XPloreColorScheme(
    val isDark: Boolean,
    val backgroundBrush: Brush,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceElevated: Color,
    val surfaceHighlight: Color,
    val topBarContainer: Color,
    val sideBarContainer: Color,
    val border: Color,
    val focusBorder: Color,
    val focusBackground: Color,
    val selectionBackground: Color,
    val selectionFocusBackground: Color,
    val selectionFill: Color,
    val folderSelectionColor: Color,
    val checkmarkColor: Color,
    val badgeBackground: Color,
    val badgeText: Color,
    val badgeBorder: Color,
    val treeLine: Color,
    val treeExpandArrow: Color,
    val emptyText: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val checkMarkUnchecked: Color,
    val checkMarkSelected: Color,
    val iconFolder: Color,
    val iconArchive: Color,
    val iconApk: Color,
    val iconDoc: Color,
    val iconCode: Color,
    val iconImage: Color,
    val iconAudio: Color,
    val iconVideo: Color
) {
    val treeLineColor: Color get() = treeLine
}

val DarkXPloreColors = XPloreColorScheme(
    isDark = true,
    backgroundBrush = SolidColor(DarkBackground),
    primary = Color(0xFFFF9800),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFE65100),
    onPrimaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFF00BCD4),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEEEEEE),
    surfaceElevated = Color(0xFF252528),
    surfaceHighlight = Color(0xFF2C2C30),
    topBarContainer = Color(0xFF141414),
    sideBarContainer = Color(0xFF18181B),
    border = Color(0xFF333338),
    focusBorder = Color(0xFFFF9800),
    focusBackground = Color(0xFF221C16),
    selectionBackground = Color(0xFF2C2419),
    selectionFocusBackground = Color(0xFF382B1C),
    selectionFill = Color(0x332196F3),
    folderSelectionColor = Color(0xFF2196F3),
    checkmarkColor = Color(0xFF42A5F5),
    badgeBackground = Color(0xFF1B382B),
    badgeText = Color(0xFF4CAF50),
    badgeBorder = Color(0x994CAF50),
    treeLine = Color(0xFF3E3E44),
    treeExpandArrow = Color(0xFF00BCD4),
    emptyText = Color(0xFF9E9E9E),
    textPrimary = Color(0xFFEEEEEE),
    textSecondary = Color(0xFF9E9E9E),
    textTertiary = Color(0xFF757575),
    checkMarkUnchecked = Color(0xFF505050),
    checkMarkSelected = Color(0xFF4CAF50),
    iconFolder = Color(0xFFFFB74D),
    iconArchive = Color(0xFF00BCD4),
    iconApk = Color(0xFF81C784),
    iconDoc = Color(0xFF90CAF9),
    iconCode = Color(0xFFFFCC80),
    iconImage = Color(0xFFCE93D8),
    iconAudio = Color(0xFFFFF176),
    iconVideo = Color(0xFFFF8A65)
)

val LightXPloreColors = XPloreColorScheme(
    isDark = false,
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(WhiteSolid, WhiteSolid, SkyBlueGradientEnd)
    ),
    primary = Color(0xFFE65100),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFFE65100),
    secondary = Color(0xFF00838F),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceElevated = Color(0xFFEEEEEE),
    surfaceHighlight = Color(0xFFE0E0E0),
    topBarContainer = Color(0xFFEBEBEB),
    sideBarContainer = Color(0xFFECECEC),
    border = Color(0xFFD0D0D0),
    focusBorder = Color(0xFFE65100),
    focusBackground = Color(0xFFFFF3E0),
    selectionBackground = Color(0xFFFFE0B2),
    selectionFocusBackground = Color(0xFFFFCC80),
    selectionFill = Color(0x221E88E5),
    folderSelectionColor = Color(0xFF1E88E5),
    checkmarkColor = Color(0xFF1976D2),
    badgeBackground = Color(0xFFE8F5E9),
    badgeText = Color(0xFF2E7D32),
    badgeBorder = Color(0x992E7D32),
    treeLine = Color(0xFFBDBDBD),
    treeExpandArrow = Color(0xFF00838F),
    emptyText = Color(0xFF757575),
    textPrimary = Color(0xFF212121),
    textSecondary = Color(0xFF616161),
    textTertiary = Color(0xFF9E9E9E),
    checkMarkUnchecked = Color(0xFFBDBDBD),
    checkMarkSelected = Color(0xFF2E7D32),
    iconFolder = Color(0xFFF57C00),
    iconArchive = Color(0xFF0097A7),
    iconApk = Color(0xFF388E3C),
    iconDoc = Color(0xFF1976D2),
    iconCode = Color(0xFFE65100),
    iconImage = Color(0xFF7B1FA2),
    iconAudio = Color(0xFFFBC02D),
    iconVideo = Color(0xFFD84315)
)

val LocalXPloreColors = staticCompositionLocalOf { DarkXPloreColors }

object XPloreTheme {
    val colors: XPloreColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalXPloreColors.current
}
