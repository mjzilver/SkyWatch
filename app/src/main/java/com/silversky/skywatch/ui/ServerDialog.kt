package com.silversky.skywatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

data class ServerConnectionInput(
    val name: String?,
    val address: String,
    val username: String,
    val password: String
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ServerDialog(
    onDismiss: () -> Unit,
    onConnect: (ServerConnectionInput) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var address by remember {
        mutableStateOf("")
    }

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add SMB Server",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            TvTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = "Name"
            )

            TvTextField(
                value = address,
                onValueChange = {
                    address = it
                },
                label = "IP address"
            )

            TvTextField(
                value = username,
                onValueChange = {
                    username = it
                },
                label = "Username"
            )

            TvTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = "Password"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    if (address.isNotBlank()) {
                        onConnect(
                            ServerConnectionInput(
                                name = name.ifBlank {
                                    null
                                },
                                address = address.trim(),
                                username = username,
                                password = password
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(
                MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                )
        )
    }
}