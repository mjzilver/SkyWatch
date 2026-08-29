package com.silversky.skywatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SkyWatchDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties =
        DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    buttons: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
  Dialog(
      onDismissRequest = onDismiss,
      properties = properties,
  ) {
    Column(
        modifier =
            modifier
                .fillMaxWidth(0.8f)
                .widthIn(max = 800.dp)
                .background(
                    Color(0xFF202020),
                    RoundedCornerShape(12.dp),
                )
                .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Column(
          modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
          content = content,
      )

      if (buttons != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = buttons,
        )
      }
    }
  }
}
