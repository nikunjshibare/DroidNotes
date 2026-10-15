package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = AppleNotesGoldDark,
    onPrimary = Color.Black,
    primaryContainer = AppleNotesGoldContainerDark,
    onPrimaryContainer = AppleNotesGoldDark,
    secondary = AppleDarkTextSecondary,
    onSecondary = AppleDarkTextPrimary,
    background = AppleDarkBg,
    onBackground = AppleDarkTextPrimary,
    surface = AppleDarkCard,
    onSurface = AppleDarkTextPrimary,
    surfaceVariant = AppleDarkCardBorder,
    onSurfaceVariant = AppleDarkTextSecondary,
    outline = AppleDarkCardBorder,
    error = AppleNotesRed
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AppleNotesGold,
    onPrimary = Color.White,
    primaryContainer = AppleNotesGoldContainerLight,
    onPrimaryContainer = AppleNotesGold,
    secondary = AppleLightTextSecondary,
    onSecondary = AppleLightTextPrimary,
    background = AppleLightBg,
    onBackground = AppleLightTextPrimary,
    surface = AppleLightCard,
    onSurface = AppleLightTextPrimary,
    surfaceVariant = AppleLightCardBorder,
    onSurfaceVariant = AppleLightTextSecondary,
    outline = AppleLightCardBorder,
    error = AppleNotesRed
  )

@Composable
fun DroidNotesTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
