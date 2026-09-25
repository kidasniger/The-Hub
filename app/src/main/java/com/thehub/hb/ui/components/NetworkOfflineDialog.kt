package com.thehub.hb.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import com.thehub.hb.ui.theme.LocalHubStrings

@Composable
fun NetworkOfflineDialog(
    onRetry: () -> Unit
) {
    val strings = LocalHubStrings.current

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = strings.offlineTitle,
                modifier = androidx.compose.ui.Modifier.testTag("network_offline_dialog_title")
            )
        },
        text = {
            Text(
                text = strings.offlineMessage,
                modifier = androidx.compose.ui.Modifier.testTag("network_offline_dialog_message")
            )
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                modifier = androidx.compose.ui.Modifier.testTag("network_offline_dialog_retry")
            ) {
                Text(text = strings.offlineRetry)
            }
        },
        modifier = androidx.compose.ui.Modifier.testTag("network_offline_dialog")
    )
}
