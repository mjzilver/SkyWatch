package com.silversky.skywatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

data class ServerConnectionInput(
    val name: String?,
    val address: String,
    val username: String,
    val password: String,
    val isGuest: Boolean,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ServerDialog(
    onDismiss: () -> Unit,
    onSave: (ServerConnectionInput) -> Unit,
    onScan: () -> Unit,
    initialAddress: String = "",
    initialName: String = "",
    initialUsername: String = "",
    initialPassword: String = "",
    initialIsGuest: Boolean = false,
    isEditing: Boolean = false,
) {
  var name by
      remember(initialName) {
        mutableStateOf(initialName)
      }

  var address by
      remember(initialAddress) {
        mutableStateOf(initialAddress)
      }

  var username by
      remember(initialUsername) {
        mutableStateOf(initialUsername)
      }

  var password by
      remember(initialPassword) {
        mutableStateOf(initialPassword)
      }

  var useGuestAccount by remember {
    mutableStateOf(initialIsGuest)
  }

  val connectButtonRequester = remember { FocusRequester() }

  SkyWatchDialog(
      title = if (isEditing) "Edit SMB Server" else "Add SMB Server",
      onDismiss = onDismiss,
      buttons = {
        Button(
            onClick = {
              if (name.isNotBlank() && address.isNotBlank()) {
                onSave(
                    ServerConnectionInput(
                        name = name.trim(),
                        address = address.trim(),
                        username = username,
                        password = password,
                        isGuest = useGuestAccount,
                    )
                )
              }
            },
            modifier = Modifier.weight(1f).focusRequester(connectButtonRequester),
        ) {
          Text("Save")
        }

        Button(
            onClick = onScan,
            modifier = Modifier.weight(1f),
        ) {
          Text("Scan Network")
        }
      },
  ) {
    TvTextField(
        value = name,
        onValueChange = { name = it },
        label = "Name",
    )

    TvTextField(
        value = address,
        onValueChange = { address = it },
        label = "IP address",
    )

    Button(
        onClick = { useGuestAccount = !useGuestAccount },
        modifier =
            Modifier.fillMaxWidth().focusProperties {
              if (useGuestAccount) {
                down = connectButtonRequester
              }
            },
    ) {
      Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Checkbox(
            checked = useGuestAccount,
            onCheckedChange = null,
        )

        Text("Use guest account")
      }
    }

    TvTextField(
        value = username,
        onValueChange = { username = it },
        label = "Username",
        enabled = !useGuestAccount,
    )

    TvTextField(
        value = password,
        onValueChange = { password = it },
        label = "Password",
        enabled = !useGuestAccount,
        modifier = Modifier.focusProperties { down = connectButtonRequester },
    )
  }
}
