package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core DAYFORM Palette from Reference Design
val CharcoalBg = Color(0xFF191919)
val CharcoalSurface = Color(0xFF232323)
val CharcoalElevated = Color(0xFF2C2C2C)
val CharcoalBorder = Color(0xFF343434)

val NeonLime = Color(0xFFDFFF65)
val NeonLimeDarkText = Color(0xFF384210)
val NeonLimeMuted = Color(0xFFC8E84E)

val LavenderPurple = Color(0xFFC279F8)
val LavenderDark = Color(0xFF9B4FDB)

val OffWhiteSurface = Color(0xFFF6F7F8)
val OffWhiteCardBorder = Color(0xFFE3E5E8)
val SubtaskFooterGrey = Color(0xFFDCDFE3)

val PrimaryDarkText = Color(0xFF171717)
val SecondaryDarkText = Color(0xFF5E6064)
val SecondaryText = Color(0xFF989898)
val LightText = Color(0xFFF9FAFB)

val DeadlineOrange = Color(0xFFFF9F31)
val OverdueRed = Color(0xFFFF5C5C)
val SuccessGreen = Color(0xFF4ADE80)

val StarterCategoryColors = listOf(
    "#DFFF65", // Neon Lime
    "#C279F8", // Lavender Purple
    "#FF9F31", // Warm Orange
    "#5CD6FF", // Sky Cyan
    "#FF6B8B", // Coral Pink
    "#5EEAD4", // Mint Teal
    "#FBBF24", // Amber Gold
    "#A78BFA"  // Soft Indigo
)

fun parseHexColor(hex: String, fallback: Color = NeonLime): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val value = clean.toLong(16)
        if (clean.length == 6) {
            Color(value or 0xFF000000)
        } else if (clean.length == 8) {
            Color(value)
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
