package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val DarkColorScheme =
  darkColorScheme(
    primary = WorldCupGoldTrophy,
    secondary = WorldCupPitchGreen,
    tertiary = WorldCupYellowCard,
    background = WorldCupDarkBackground,
    surface = WorldCupCardBackground,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = WorldCupRedCard,
    onError = Color.White
  )

private val LightColorScheme = DarkColorScheme // Always use dark scheme for the premium stadium feel requested!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for stadium night lights!
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve our World Cup palette
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
