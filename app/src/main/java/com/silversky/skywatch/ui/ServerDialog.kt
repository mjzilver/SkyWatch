package com.silversky.skywatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Checkbox
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
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
    onConnect: (ServerConnectionInput) -> Unit,
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

  Dialog(onDismissRequest = onDismiss) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = if (isEditing) "Edit SMB Server" else "Add SMB Server",
          style = MaterialTheme.typography.headlineSmall,
      )

      Spacer(modifier = Modifier.height(8.dp))

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
          onClick = {
            useGuestAccount = !useGuestAccount
          },
          modifier = Modifier.fillMaxWidth().focusProperties {
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

      Spacer(modifier = Modifier.height(8.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
            onClick = {
              if (name.isNotBlank() && address.isNotBlank()) {
                onConnect(
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
          Text(if (isEditing) "Save" else "Connect")
        }

        Button(
            onClick = onScan,
            modifier = Modifier.weight(1f),
        ) {
          Text("Scan Network")
        }
      }
    }
  }
}

@Composable
private fun TvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
  Column(
      modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
    )

    Spacer(modifier = Modifier.height(4.dp))

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                color =
                    if (enabled) {
                      MaterialTheme.colorScheme.onSurface
                    } else {
                      MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    if (enabled) {
                      MaterialTheme.colorScheme.surface
                    } else {
                      MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)
                    },
                )
                .border(
                    width = 2.dp,
                    color =
                        if (enabled) {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp,
                ),
    )
  }
}
