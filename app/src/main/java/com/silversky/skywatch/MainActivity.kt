package com.silversky.skywatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.silversky.skywatch.ui.theme.SkyWatchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @OptIn(ExperimentalTvMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      SkyWatchTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
        ) {
          SkyWatchApp()
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
  }
}
