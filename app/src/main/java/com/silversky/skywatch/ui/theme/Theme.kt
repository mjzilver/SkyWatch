package com.silversky.skywatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SkyWatchTheme(
    content: @Composable () -> Unit,
) {
  val colorScheme =
      darkColorScheme(
          primary = White,
          onPrimary = Black,
          secondary = LightGray,
          onSecondary = Black,
          tertiary = MediumGray,
          onTertiary = White,
          background = Black,
          onBackground = White,
          surface = DarkGray,
          onSurface = White,
          surfaceVariant = Gray,
          onSurfaceVariant = LightGray,
      )

  MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
  )
}
