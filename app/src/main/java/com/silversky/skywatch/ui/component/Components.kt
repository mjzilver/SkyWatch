package com.silversky.skywatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.silversky.skywatch.error.AppError

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
  Button(onClick = onClick, modifier = modifier) {
    Text("Back")
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoadingMessage(message: String = "Loading...") {
  Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
  )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
  val backFocus = remember {
    FocusRequester()
  }

  LaunchedEffect(Unit) {
    backFocus.requestFocus()
  }

  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    if (onBack != null) {
      BackButton(onClick = onBack, Modifier.focusRequester(backFocus))

      Spacer(modifier = Modifier.width(24.dp))
    }

    Column {
      Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
      )

      if (subtitle != null) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyMessage(message: String) {
  Column(
      modifier = Modifier.padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
fun TvTextField(
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppErrorOverlay(
    error: AppError,
    onClose: () -> Unit,
) {
  val closeFocus = remember {
    FocusRequester()
  }

  LaunchedEffect(Unit) {
    closeFocus.requestFocus()
  }

  Box(
      modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.90f)),
      contentAlignment = Alignment.Center,
  ) {
    Column(
        modifier =
            Modifier.fillMaxWidth(0.65f)
                .background(
                    Color(0xFF202020),
                    RoundedCornerShape(16.dp),
                )
                .padding(
                    horizontal = 40.dp,
                    vertical = 32.dp,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
          text = "Error Occurred",
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
          text = error.userMessage,
          style = MaterialTheme.typography.bodyLarge,
          color = Color.LightGray,
      )

      if (error.technicalMessage.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = error.technicalMessage,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray.copy(alpha = 0.8f),
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      Button(
          onClick = onClose,
          modifier = Modifier.focusRequester(closeFocus),
      ) {
        Text("Close")
      }
    }
  }
}
