package com.thehub.hb.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

enum class HubButtonVariant {
    Primary,
    Secondary,
    Ghost
}

@Composable
fun HubButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HubButtonVariant = HubButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String = "hub_button"
) {
    val shape = RoundedCornerShape(percent = 50)

    when (variant) {
        HubButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(testTag),
                shape = shape,
                enabled = enabled && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HubWhite,
                    contentColor = HubBlack,
                    disabledContainerColor = HubSurfaceElevated,
                    disabledContentColor = HubMuted
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = HubBlack
                    )
                } else {
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.2).sp
                    )
                }
            }
        }
        HubButtonVariant.Secondary -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(testTag),
                shape = shape,
                enabled = enabled && !isLoading,
                border = BorderStroke(1.dp, HubBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HubSurfaceElevated,
                    contentColor = HubWhite,
                    disabledContainerColor = HubSurfaceElevated,
                    disabledContentColor = HubMuted
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = HubWhite
                    )
                } else {
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        HubButtonVariant.Ghost -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .height(52.dp)
                    .testTag(testTag),
                shape = shape,
                enabled = enabled && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = HubWhite,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = HubMuted
                )
            ) {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
