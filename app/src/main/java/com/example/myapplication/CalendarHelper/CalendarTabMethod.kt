package com.example.myapplication.CalendarHelper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.Pages.isLeapYear
import com.example.myapplication.Tools.formatTime
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


@Composable
fun DateTimePickerDialog(
    initialDate: LocalDate,
    initialStartTime: LocalTime = LocalTime(12, 0),
    initialEndTime: LocalTime = LocalTime(13, 0),
    onDateTimeSelected: (LocalDate, LocalTime, LocalTime) -> Unit,
    onDismiss: () -> Unit,
    use24HourFormat: Boolean = true
) {
    // Tab selection state
    var selectedTab by remember { mutableIntStateOf(0) }

    // Date state
    var year by remember { mutableStateOf(initialDate.year) }
    var month by remember { mutableStateOf(initialDate.month.value) }
    var day by remember { mutableStateOf(initialDate.dayOfMonth) }

    // Current date for validation
    val today = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    // Helper function to convert 24-hour time to 12-hour format
    fun to12HourFormat(hour24: Int): Pair<Int, String> {
        return when {
            hour24 == 0 -> 12 to "AM"
            hour24 < 12 -> hour24 to "AM"
            hour24 == 12 -> 12 to "PM"
            else -> (hour24 - 12) to "PM"
        }
    }

    // Helper function to convert 12-hour time to 24-hour format
    fun to24HourFormat(hour12: Int, period: String): Int {
        return when {
            period == "AM" && hour12 == 12 -> 0
            period == "AM" -> hour12
            period == "PM" && hour12 == 12 -> 12
            else -> hour12 + 12
        }
    }

    // Initialize start time state based on format
    val (initialStartHour12, initialStartPeriod) = to12HourFormat(initialStartTime.hour)
    var startHour by remember { mutableStateOf(initialStartTime.hour) }
    var startMinute by remember { mutableStateOf(initialStartTime.minute) }
    var startHour12 by remember { mutableStateOf(initialStartHour12) }
    var startPeriod by remember { mutableStateOf(initialStartPeriod) }

    // Initialize end time state based on format
    val (initialEndHour12, initialEndPeriod) = to12HourFormat(initialEndTime.hour)
    var endHour by remember { mutableStateOf(initialEndTime.hour) }
    var endMinute by remember { mutableStateOf(initialEndTime.minute) }
    var endHour12 by remember { mutableStateOf(initialEndHour12) }
    var endPeriod by remember { mutableStateOf(initialEndPeriod) }

    // Calculate days in the selected month
    val daysInMonth = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30 // Default
    }

    // Ensure day is valid for the selected month
    if (day > daysInMonth) {
        day = daysInMonth
    }

    // Create the selected date object
    val selectedDate = LocalDate(year, Month.of(month), day)

    // Create time objects
    val startTime = LocalTime(startHour, startMinute)
    val endTime = LocalTime(endHour, endMinute)

    // Check if date is valid (not in the past)
    val isDateValid = selectedDate >= today

    // Check if times are valid (end time after start time and not in the past if today)
    val isTimeValid by remember(startHour, startMinute, endHour, endMinute, selectedDate) {
        derivedStateOf {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentTime = now.time

            val start = LocalTime(startHour, startMinute)
            val end = LocalTime(endHour, endMinute)

            val validStart = if (selectedDate == now.date) start > currentTime else true
            val validEnd = end > start

            validStart && validEnd
        }
    }

    // Overall validation
    val canConfirm = isDateValid && isTimeValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add to Calendar",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Summary of all selections
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            "${selectedDate.dayOfMonth} ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.year}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatTime(startTime, use24HourFormat),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(" to ", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatTime(endTime, use24HourFormat),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Validation messages
                if (!isDateValid) {
                    Text(
                        "Date cannot be in the past",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (!isTimeValid) {
                    Text(
                        "End time must be after start time",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Tab selection
                TabRow(
                    selectedTabIndex = selectedTab
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Date") },
                        text = { Text("Date") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Start Time") },
                        text = { Text("Start") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "End Time") },
                        text = { Text("End") }
                    )
                }

                // Content based on selected tab
                when (selectedTab) {
                    0 -> {
                        // DATE TAB
                        DatePickerContent(
                            year = year,
                            month = month,
                            day = day,
                            today = today,
                            daysInMonth = daysInMonth,
                            onYearChange = { year = it },
                            onMonthChange = { month = it },
                            onDayChange = { day = it }
                        )
                    }
                    1 -> {
                        // START TIME TAB
                        TimePickerContent(
                            hour = startHour,
                            minute = startMinute,
                            period = startPeriod,
                            use24HourFormat = use24HourFormat,
                            displayHour = if (use24HourFormat) startHour else startHour12,
                            onHourChange = { newHour ->
                                if (use24HourFormat) {
                                    startHour = newHour
                                } else {
                                    startHour12 = newHour
                                    startHour = to24HourFormat(newHour, startPeriod)
                                }
                            },
                            onMinuteChange = { startMinute = it },
                            onPeriodChange = { newPeriod ->
                                if (!use24HourFormat) {
                                    startPeriod = newPeriod
                                    startHour = to24HourFormat(startHour12, newPeriod)
                                }
                            }
                        )
                    }
                    2 -> {
                        // END TIME TAB
                        TimePickerContent(
                            hour = endHour,
                            minute = endMinute,
                            period = endPeriod,
                            use24HourFormat = use24HourFormat,
                            displayHour = if (use24HourFormat) endHour else endHour12,
                            onHourChange = { newHour ->
                                if (use24HourFormat) {
                                    endHour = newHour
                                } else {
                                    endHour12 = newHour
                                    endHour = to24HourFormat(newHour, endPeriod)
                                }
                            },
                            onMinuteChange = { endMinute = it },
                            onPeriodChange = { newPeriod ->
                                if (!use24HourFormat) {
                                    endPeriod = newPeriod
                                    endHour = to24HourFormat(endHour12, newPeriod)
                                }
                            }
                        )

                        // Auto-fix button for invalid time
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = {
                                    if (!isTimeValid) {
                                        // Auto-fix: set end time 1 hour after start time
                                        val newEndHour = (startHour + 1) % 24
                                        endHour = newEndHour
                                        endMinute = startMinute

                                        // Update 12-hour format values
                                        val (newHour12, newPeriod) = to12HourFormat(newEndHour)
                                        endHour12 = newHour12
                                        endPeriod = newPeriod
                                    }
                                }
                            ) {
                                Text("Set 1 hour after start")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalStartTime = LocalTime(startHour, startMinute)
                    val finalEndTime = LocalTime(endHour, endMinute)
                    onDateTimeSelected(selectedDate, finalStartTime, finalEndTime)
                },
                enabled = canConfirm
            ) {
                Text("Add to Calendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun TimePickerContent(
    hour: Int,
    minute: Int,
    period: String,
    use24HourFormat: Boolean,
    displayHour: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onPeriodChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Preview of selected time
        Text(
            text = formatTime(LocalTime(hour, minute), use24HourFormat),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!use24HourFormat) {
            // AM/PM selector for 12-hour format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onPeriodChange("AM") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (period == "AM")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("AM")
                }

                Button(
                    onClick = { onPeriodChange("PM") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (period == "PM")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("PM")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Hour picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Hour", style = MaterialTheme.typography.bodyMedium)

                val hourRange = if (use24HourFormat) (0..23).toList() else (1..12).toList()

                WheelNumberPicker(
                    items = hourRange,
                    selectedItem = if (use24HourFormat) hour else displayHour,
                    onValueChange = onHourChange
                )
            }

            Text(
                ":",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Minute picker
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text("Minute", style = MaterialTheme.typography.bodyMedium)

                WheelNumberPicker(
                    items = (0..59 step 5).toList(),
                    selectedItem = minute - (minute % 5),
                    onValueChange = onMinuteChange
                )
            }
        }
    }
}

// A wheel picker for numeric values
@Composable
fun WheelNumberPicker(
    items: List<Int>,
    selectedItem: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Remembering the first initialization
    val initialized = remember { mutableStateOf(false) }
    val itemHeight = 48.dp
    val visibleItems = 5

    // Calculating the middle position for padding
    val visibleItemsHalf = visibleItems / 2

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Initial scroll to the selected item only once
    LaunchedEffect(Unit) {
        if (!initialized.value) {
            val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
            listState.scrollToItem(selectedIndex)
            initialized.value = true
        }
    }

    // Getting the center item that's in the middle of the visible area
    val centerItem by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val centerPosition = layoutInfo.viewportEndOffset / 2 + layoutInfo.viewportStartOffset / 2

            layoutInfo.visibleItemsInfo
                .firstOrNull { itemInfo ->
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    itemCenter > centerPosition - 10 && itemCenter < centerPosition + 10
                }
                ?.index
                ?.let { items.getOrNull(it) } ?: selectedItem
        }
    }

    // Updating value when scrolling stops
    LaunchedEffect(listState.isScrollInProgress, centerItem) {
        if (!listState.isScrollInProgress && centerItem != selectedItem) {
            onValueChange(centerItem)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItems)
            .clipToBounds()
    ) {
        // Adding a gradient overlay for fade effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = itemHeight * visibleItemsHalf,
                bottom = itemHeight * visibleItemsHalf
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                val isSelected = item == centerItem

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable {
                            val index = items.indexOf(item)
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        }
                ) {
                    Text(
                        text = item.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

    }
}


@Composable
fun DatePickerContent(
    year: Int,
    month: Int,
    day: Int,
    today: LocalDate,
    daysInMonth: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.height(300.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // YEAR PICKER SECTION
        item {
            Text("Year", fontWeight = FontWeight.Bold)

            // Allow selecting from current year to 10 years in the future
            val minYear = today.year
            val maxYear = minYear + 10

            // Wheel picker for year selection
            WheelNumberPicker(
                items = (minYear..maxYear).toList(),
                selectedItem = year,
                onValueChange = onYearChange
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // MONTH PICKER SECTION
        item {
            Text("Month", fontWeight = FontWeight.Bold)

            // Only allow selecting current or future months
            val validMonths = if (year == today.year) {
                (today.month.value..12).toList()  // Only current month onwards for current year
            } else {
                (1..12).toList()  // All months for future years
            }

            // Horizontal row of month chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(validMonths) { monthNum ->
                    val monthName = Month.of(monthNum).name.take(3).lowercase()
                        .replaceFirstChar { it.uppercase() }

                    // Circular chip for each month
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (monthNum == month)
                                    MaterialTheme.colorScheme.primary  // Selected month
                                else
                                    MaterialTheme.colorScheme.surface  // Unselected month
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onMonthChange(monthNum) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = monthName,
                            color = if (monthNum == month)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // DAY PICKER SECTION
        item {
            Text("Day", fontWeight = FontWeight.Bold)

            // Determine minimum selectable day (today if current month, otherwise 1)
            val minDay = if (year == today.year && month == today.month.value)
                today.dayOfMonth
            else
                1

            // Calendar-style grid for day selection
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),  // 7 columns for days of week
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.height(220.dp)
            ) {
                // Day headers (S M T W T F S)
                val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
                items(dayHeaders) { header ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Empty boxes to align first day of month correctly
                val firstDayOfMonth = LocalDate(year, Month.of(month), 1)
                val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value % 7)
                items(dayOfWeekOffset) {
                    Box(modifier = Modifier.padding(4.dp))  // Empty spacer
                }

                // Actual day buttons (1 to daysInMonth)
                items(daysInMonth) { dayNum ->
                    val isSelectable = dayNum >= minDay  // Can't select past days

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(4.dp)
                            .aspectRatio(1f)  // Square cells
                            .clip(CircleShape)
                            .background(
                                when {
                                    dayNum == day -> MaterialTheme.colorScheme.primary  // Selected day
                                    !isSelectable -> MaterialTheme.colorScheme.surfaceVariant  // Disabled day
                                    else -> MaterialTheme.colorScheme.surface  // Selectable day
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .then(
                                if (isSelectable) {
                                    Modifier.clickable { onDayChange(dayNum) }
                                } else {
                                    Modifier  // No click for disabled days
                                }
                            )
                    ) {
                        Text(
                            text = dayNum.toString(),
                            color = when {
                                dayNum == day -> MaterialTheme.colorScheme.onPrimary
                                !isSelectable -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}


