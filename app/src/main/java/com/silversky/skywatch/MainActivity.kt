package com.silversky.skywatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import com.silversky.skywatch.logger.AndroidLogger
import com.silversky.skywatch.ui.theme.SkyWatchTheme

class MainActivity : ComponentActivity() {

    private val logger =
        AndroidLogger("SkyWatch")

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            SkyWatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    SkyWatchApp(
                        logger = logger,
                        context = this@MainActivity
                    )
                }
            }
        }
    }
}