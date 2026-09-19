package com.thehub.hb.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubBorderLight
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubLightGray
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubWhite
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

private val FRENCH_MONTHS = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
)

/**
 * Custom iOS-style Wheel Date Picker in Jetpack Compose.
 *
 * @param selectedDate The currently selected date.
 * @param onDateSelected Callback fired when the date changes.
 * @param modifier Modifier for styling.
 * @param minYear The minimum year displayed (default: 1900).
 * @param maxYear The maximum year displayed (default: current year).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelDatePicker(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minYear: Int = 1900,
    maxYear: Int = LocalDate.now().year,
    itemHeight: Dp = 44.dp,
    containerBackgroundColor: Color = Color.Unspecified
) {
    val effectiveContainerBg = if (containerBackgroundColor != Color.Unspecified) {
        containerBackgroundColor
    } else {
        HubCard
    }

    val safeMinYear = minYear.coerceAtMost(maxYear)
    val safeMaxYear = maxOf(minYear, maxYear)
    val currentYear = selectedDate.year.coerceIn(safeMinYear, safeMaxYear)
    val currentMonth = selectedDate.monthValue.coerceIn(1, 12)
    val maxDaysInCurrentMonth = remember(currentYear, currentMonth) {
        YearMonth.of(currentYear, currentMonth).lengthOfMonth()
    }
    val currentDay = selectedDate.dayOfMonth.coerceIn(1, maxDaysInCurrentMonth)

    val yearsList = remember(safeMinYear, safeMaxYear) {
        (safeMinYear..safeMaxYear).toList().reversed()
    }
    val daysList = remember(maxDaysInCurrentMonth) {
        (1..maxDaysInCurrentMonth).toList()
    }

    val totalHeight = itemHeight * 5

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .testTag("wheel_date_picker"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle selection area highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(HubWhite.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
        )

        // Upper horizontal separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .offset(y = -(itemHeight / 2))
                .height(1.dp)
                .background(HubBorderLight)
        )

        // Lower horizontal separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .offset(y = (itemHeight / 2))
                .height(1.dp)
                .background(HubBorderLight)
        )

        // Three columns: Mois, Jour, Année
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mois (nom complet, ex: "Septembre")
            WheelColumn(
                items = FRENCH_MONTHS,
                selectedIndex = currentMonth - 1,
                onSelectedIndexChanged = { monthIdx ->
                    val newMonth = monthIdx + 1
                    val newMaxDays = LocalDate.of(currentYear, newMonth, 1).lengthOfMonth()
                    val newDay = currentDay.coerceIn(1, newMaxDays)
                    onDateSelected(LocalDate.of(currentYear, newMonth, newDay))
                },
                labelProvider = { it },
                itemHeight = itemHeight,
                modifier = Modifier
                    .weight(1.35f)
                    .testTag("wheel_month_column")
            )

            // Jour (1..28/29/30/31)
            WheelColumn(
                items = daysList,
                selectedIndex = (currentDay - 1).coerceIn(0, daysList.size - 1),
                onSelectedIndexChanged = { dayIdx ->
                    val newDay = (dayIdx + 1).coerceIn(1, daysList.size)
                    onDateSelected(LocalDate.of(currentYear, currentMonth, newDay))
                },
                labelProvider = { it.toString() },
                itemHeight = itemHeight,
                modifier = Modifier
                    .weight(0.85f)
                    .testTag("wheel_day_column")
            )

            // Année
            val yearIndex = remember(currentYear, yearsList) {
                yearsList.indexOf(currentYear).coerceIn(0, (yearsList.size - 1).coerceAtLeast(0))
            }
            WheelColumn(
                items = yearsList,
                selectedIndex = yearIndex,
                onSelectedIndexChanged = { yearIdx ->
                    val newYear = yearsList.getOrElse(yearIdx) { currentYear }
                    val newMaxDays = LocalDate.of(newYear, currentMonth, 1).lengthOfMonth()
                    val newDay = currentDay.coerceIn(1, newMaxDays)
                    onDateSelected(LocalDate.of(newYear, currentMonth, newDay))
                },
                labelProvider = { it.toString() },
                itemHeight = itemHeight,
                modifier = Modifier
                    .weight(1.0f)
                    .testTag("wheel_year_column")
            )
        }

        // Top edge fade gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 1.5f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            effectiveContainerBg,
                            effectiveContainerBg.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom edge fade gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 1.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            effectiveContainerBg.copy(alpha = 0.7f),
                            effectiveContainerBg
                        )
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T> WheelColumn(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() },
    itemHeight: Dp = 44.dp
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Calculate centered item index
    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            } else {
                val centerOffset = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val closest = visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    abs(itemCenter - centerOffset)
                }
                closest?.index?.coerceIn(0, (items.size - 1).coerceAtLeast(0))
                    ?: selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            }
        }
    }

    // Keep track of previous index to trigger haptic ticks
    var lastTriggeredIndex by remember { mutableIntStateOf(selectedIndex) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(centeredIndex) {
        if (!isInitialized) {
            isInitialized = true
            lastTriggeredIndex = centeredIndex
            return@LaunchedEffect
        }
        if (centeredIndex != lastTriggeredIndex) {
            lastTriggeredIndex = centeredIndex
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectedIndexChanged(centeredIndex)
        }
    }

    // External sync (when month change forces day clamping, etc.)
    LaunchedEffect(selectedIndex, items.size) {
        val target = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (!listState.isScrollInProgress && centeredIndex != target) {
            listState.scrollToItem(target)
            lastTriggeredIndex = target
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight * 2),
        modifier = modifier.height(itemHeight * 5)
    ) {
        items(
            count = items.size,
            key = { index -> items[index].hashCode().toLong() xor (index.toLong() shl 32) }
        ) { index ->
            val distance = abs(index - centeredIndex)
            val isSelected = distance == 0

            val alpha = when (distance) {
                0 -> 1f
                1 -> 0.52f
                else -> 0.20f
            }
            val scale = when (distance) {
                0 -> 1.14f
                1 -> 0.94f
                else -> 0.82f
            }
            val fontWeight = when (distance) {
                0 -> FontWeight.Bold
                1 -> FontWeight.Medium
                else -> FontWeight.Normal
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(items[index]),
                    color = HubWhite.copy(alpha = alpha),
                    fontSize = 17.sp,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
        }
    }
}

/**
 * iOS-styled Wheel Date Picker presented in a Modal Bottom Sheet.
 *
 * @param initialDate The starting date to display.
 * @param onDateConfirmed Callback invoked with the confirmed [LocalDate].
 * @param onDismiss Callback invoked when dismissing without saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelDatePickerBottomSheet(
    initialDate: LocalDate,
    onDateConfirmed: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempSelectedDate by remember(initialDate) { mutableStateOf(initialDate) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HubCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(HubBorder)
            )
        },
        modifier = modifier.testTag("wheel_date_picker_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title
            Text(
                text = "Date de naissance",
                color = HubWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // The iOS-style 3-column wheel picker
            WheelDatePicker(
                selectedDate = tempSelectedDate,
                onDateSelected = { tempSelectedDate = it },
                containerBackgroundColor = HubCard,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Validation button
            HubButton(
                text = "Valider",
                onClick = {
                    onDateConfirmed(tempSelectedDate)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "wheel_date_picker_confirm_button"
            )
        }
    }
}

/**
 * Parses a "dd/MM/yyyy" string into a [LocalDate], or falls back to a default date (e.g. 20 years ago).
 */
fun parseBirthdateOrDefault(dateString: String, default: LocalDate = LocalDate.now().minusYears(20)): LocalDate {
    return try {
        if (dateString.isBlank()) return default
        val parts = dateString.split("/")
        if (parts.size == 3) {
            val day = parts[0].trim().toInt()
            val month = parts[1].trim().toInt()
            val year = parts[2].trim().toInt()
            LocalDate.of(year, month, day)
        } else {
            default
        }
    } catch (_: Exception) {
        default
    }
}

/**
 * Formats a [LocalDate] into standard "dd/MM/yyyy" (JJ/MM/AAAA) string.
 */
fun formatBirthdate(date: LocalDate): String {
    return String.format(java.util.Locale.ROOT, "%02d/%02d/%04d", date.dayOfMonth, date.monthValue, date.year)
}
