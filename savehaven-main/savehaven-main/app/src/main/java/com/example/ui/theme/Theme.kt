package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val WhiteGreenColorScheme =
  lightColorScheme(
    primary = SafeHavenGreen,               // Active Emerald Green
    secondary = SafeHavenMint,                // Bright Mint Green
    tertiary = SafeHavenWarning,             // Warm yellow warning
    background = SafeHavenBackground,         // Crisp light layout background
    surface = SafeHavenCardBg,               // Pure White container surface
    onPrimary = Color.White,                 // High contrast text inside buttons
    onSecondary = Color.White,
    onBackground = SafeHavenDarkBlue,         // Dark Charcoal for clear readability
    onSurface = SafeHavenDarkBlue,           // Dark Charcoal for clear readability
    surfaceVariant = SafeHavenSurfaceLow,     // Beautiful light soft container backing
    outline = SafeHavenOutline,               // Delicate lines / guidelines
    outlineVariant = SafeHavenOutline,
    error = SafeHavenError                   // Clear red warning color
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always enforce our crisp light White Green design theme
  val colorScheme = WhiteGreenColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
