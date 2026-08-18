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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
    val password: String,
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

  val nameFocus = remember { FocusRequester() }
  val addressFocus = remember { FocusRequester() }
  val usernameFocus = remember { FocusRequester() }
  val passwordFocus = remember { FocusRequester() }
  val scanFocus = remember { FocusRequester() }
  val connectFocus = remember { FocusRequester() }

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
          focusRequester = nameFocus,
          upFocus = null,
          downFocus = addressFocus,
      )

      TvTextField(
          value = address,
          onValueChange = { address = it },
          label = "IP address",
          focusRequester = addressFocus,
          upFocus = nameFocus,
          downFocus = usernameFocus,
      )

      TvTextField(
          value = username,
          onValueChange = { username = it },
          label = "Username",
          focusRequester = usernameFocus,
          upFocus = addressFocus,
          downFocus = passwordFocus,
      )

      TvTextField(
          value = password,
          onValueChange = { password = it },
          label = "Password",
          focusRequester = passwordFocus,
          upFocus = usernameFocus,
          downFocus = connectFocus,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
            onClick = {
              if (address.isNotBlank()) {
                onConnect(
                    ServerConnectionInput(
                        name = name.ifBlank { null },
                        address = address.trim(),
                        username = username,
                        password = password,
                    )
                )
              }
            },
            modifier =
                Modifier.weight(1f).focusRequester(connectFocus).onPreviewKeyEvent { event ->
                  if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                  }

                  when (event.key) {
                    Key.DirectionUp -> {
                      passwordFocus.requestFocus()
                      true
                    }

                    Key.DirectionLeft -> {
                      scanFocus.requestFocus()
                      true
                    }

                    else -> false
                  }
                },
        ) {
          Text(if (isEditing) "Save" else "Connect")
        }

        Button(
            onClick = onScan,
            modifier =
                Modifier.weight(1f).focusRequester(scanFocus).onPreviewKeyEvent { event ->
                  if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                  }

                  when (event.key) {
                    Key.DirectionUp -> {
                      passwordFocus.requestFocus()
                      true
                    }

                    Key.DirectionRight -> {
                      connectFocus.requestFocus()
                      true
                    }

                    else -> false
                  }
                },
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
    focusRequester: FocusRequester,
    upFocus: FocusRequester?,
    downFocus: FocusRequester?,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
    )

    Spacer(modifier = Modifier.height(6.dp))

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier =
            Modifier.fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                  if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                  }

                  when (event.key) {
                    Key.DirectionUp -> {
                      upFocus?.requestFocus() ?: return@onPreviewKeyEvent false

                      true
                    }

                    Key.DirectionDown -> {
                      downFocus?.requestFocus() ?: return@onPreviewKeyEvent false

                      true
                    }

                    else -> false
                  }
                }
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp,
                ),
    )
  }
}
