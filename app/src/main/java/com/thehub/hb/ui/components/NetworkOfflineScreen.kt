package com.thehub.hb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubBackground
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubStrings

@Composable
fun NetworkOfflineScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBackground)
            .testTag("network_offline_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .height(72.dp)
                    .testTag("network_offline_indicator"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = HubWhite,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = strings.offlineTitle,
                color = HubWhite,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.offlineMessage,
                color = HubMuted,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            HubButton(
                text = strings.offlineRetry,
                onClick = onRetry,
                modifier = Modifier.testTag("network_offline_retry")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "La connexion sera détectée automatiquement.",
                color = HubMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
