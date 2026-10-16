package com.tindaflow.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TindaColors {
    val Forest = Color(0xFF064B37)
    val Deep = Color(0xFF043F30)
    val Canvas = Color(0xFFFAF9F6)
    val Lime = Color(0xFFC9F181)
    val Ink = Color(0xFF0A1714)
    val Muted = Color(0xFF6B7474)
    val Border = Color(0xFFE8EAE6)
    val LightGreen = Color(0xFFE8F5E8)
    val Amber = Color(0xFF986000)
    val LightAmber = Color(0xFFFFF1D2)
    val Red = Color(0xFFBA2937)
    val LightRed = Color(0xFFFFE8E9)
}

private val colors = lightColorScheme(
    primary = TindaColors.Forest,
    secondary = TindaColors.Lime,
    background = TindaColors.Canvas,
    surface = Color.White,
    onSurface = TindaColors.Ink,
    onBackground = TindaColors.Ink
)

private val type = Typography(
    headlineLarge = TextStyle(fontSize = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, fontFamily = FontFamily.SansSerif),
    headlineMedium = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, fontFamily = FontFamily.SansSerif),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun TindaFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}
