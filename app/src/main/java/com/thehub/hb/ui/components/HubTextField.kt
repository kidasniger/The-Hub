package com.thehub.hb.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubField
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite

/**
 * Champ de saisie « Aurora Glass » (.input de glass.css) :
 * fond --field, contour --stroke-soft 1 px, rayon --r-md 18, texte 14/500 ;
 * focus → contour --accent 65 % + halo extérieur 3.5 px --accent 18 %
 * (box-shadow 0 0 0 3.5px color-mix(in srgb, var(--accent) 18%, transparent)).
 */
@Composable
fun HubTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minHeight: Int = 48,
    errorMessage: String? = null,
    testTag: String = "hub_text_field"
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    val borderColor by animateColorAsState(
        targetValue = when {
            errorMessage != null -> HubError
            focused -> HubAccent.copy(alpha = 0.65f)
            else -> HubStrokeSoft
        },
        animationSpec = tween(160),
        label = "hub_text_field_border"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight.dp.coerceAtLeast(48.dp))
                .clip(shape)
                .background(HubField)
                .border(
                    width = 1.dp,
                    brush = SolidColor(borderColor),
                    shape = shape
                )
                .then(
                    if (focused && errorMessage == null) {
                        Modifier.drawBehind {
                            // Halo de focus 3.5 px entièrement à l'extérieur du contour.
                            val ring = 3.5.dp.toPx()
                            drawRoundRect(
                                color = HubAccent.copy(alpha = 0.18f),
                                topLeft = Offset(-ring, -ring),
                                size = Size(size.width + 2 * ring, size.height + 2 * ring),
                                cornerRadius = CornerRadius(21.5.dp.toPx()),
                                style = Stroke(width = ring)
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 16.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!singleLine) Modifier.padding(vertical = 12.dp) else Modifier),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = HubTextTertiary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(testTag),
                        textStyle = TextStyle(
                            color = HubWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(HubAccent),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        singleLine = singleLine,
                        maxLines = maxLines
                    )
                }

                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = HubError,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}
