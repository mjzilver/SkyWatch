package com.silversky.skywatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.silversky.skywatch.logger.AndroidLogger
import com.silversky.skywatch.ui.theme.SkyWatchTheme

class MainActivity : ComponentActivity() {

    private val logger = AndroidLogger()

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SkyWatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    HomeScreen(
                        onScanNetwork = {
                            logger.info("Scan Network clicked")
                        }
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeScreen(
    onScanNetwork: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 72.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column {
            Text(
                text = "SKYWATCH", style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "SMB MEDIA PLAYER", style = MaterialTheme.typography.bodyLarge
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onScanNetwork
            ) {
                Text("Scan Network")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    // TODO: add server
                }) {
                Text("Add Server")
            }
        }

        Column {
            Text(
                text = "Recent Servers", style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            ServerItem(
                name = "DEBIAN", address = "192.168.1.83"
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ServerItem(
    name: String, address: String, onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name)

            Text(
                text = address, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}