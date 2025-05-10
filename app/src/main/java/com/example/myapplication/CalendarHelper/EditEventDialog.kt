package com.example.myapplication.CalendarHelper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.Pages.Event
import com.example.myapplication.Pages.isLeapYear
import com.example.myapplication.Tools.formatTime
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


@Composable
fun EditEventDialog(
    event: Event,
    eventId: String,
    eventDate: LocalDate,
    onEventUpdated: (String, LocalDate, String, LocalTime, LocalTime) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onDismiss: () -> Unit,
    use24HourFormat: Boolean = true
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf(event.title) }

    var year by remember { mutableIntStateOf(eventDate.year) }
    var month by remember { mutableIntStateOf(eventDate.month.value) }
    var day by remember { mutableIntStateOf(eventDate.dayOfMonth) }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val initialStartHour = event.startTime.hour
    val initialEndHour = event.endTime.hour

    var startPeriod by remember { mutableStateOf(if (initialStartHour >= 12) "PM" else "AM") }
    var endPeriod by remember { mutableStateOf(if (initialEndHour >= 12) "PM" else "AM") }

    var startHour by remember {
        mutableIntStateOf(
            if (use24HourFormat) initialStartHour else (if (initialStartHour % 12 == 0) 12 else initialStartHour % 12)
        )
    }
    var startMinute by remember { mutableIntStateOf(event.startTime.minute) }

    var endHour by remember {
        mutableIntStateOf(
            if (use24HourFormat) initialEndHour else (if (initialEndHour % 12 == 0) 12 else initialEndHour % 12)
        )
    }
    var endMinute by remember { mutableIntStateOf(event.endTime.minute) }

    val selectedDate = LocalDate(year, Month.of(month), day)

    fun to24Hour(hour: Int, period: String): Int {
        return if (use24HourFormat) hour else {
            if (period == "AM") if (hour == 12) 0 else hour
            else if (hour == 12) 12 else hour + 12
        }
    }

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val currentTime = now.time

    val startTime = LocalTime(to24Hour(startHour, startPeriod), startMinute)
    val endTime = LocalTime(to24Hour(endHour, endPeriod), endMinute)

    val isDateValid = selectedDate >= today
    val isTimeValid by remember(startHour, startMinute, endHour, endMinute, startPeriod, endPeriod, selectedDate) {
        derivedStateOf {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentTime = now.time

            // IMPORTANT: Convert to 24-hour format
            val startHour24 = to24Hour(startHour, startPeriod)
            val endHour24 = to24Hour(endHour, endPeriod)

            val start = LocalTime(startHour24, startMinute)
            val end = LocalTime(endHour24, endMinute)

            val validStart = if (selectedDate == now.date) start > currentTime else true
            val validEnd = end > start

            validStart && validEnd
        }
    }




    val canConfirm = isDateValid && isTimeValid && title.isNotBlank()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEvent(eventId)
                        showDeleteConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Event", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true
                )

                if (title.isBlank()) {
                    Text("Title cannot be empty", color = MaterialTheme.colorScheme.error)
                }

                Text(
                    "${selectedDate.dayOfMonth} ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.year}\n" +
                            "${formatTime(startTime, use24HourFormat)} to ${formatTime(endTime, use24HourFormat)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                if (!isDateValid) {
                    Text("Date cannot be in the past", color = MaterialTheme.colorScheme.error)
                }

                if (!isTimeValid) {
                    Text(
                        if (selectedDate == now.date && LocalTime(to24Hour(startHour, startPeriod), startMinute) <= currentTime) {
                            "Start time must be in the future"
                        } else {
                            "End time must be after start time"
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }


                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Date") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Start") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("End") })
                }

                when (selectedTab) {
                    0 -> {
                        DatePickerContent(
                            year, month, day, today,
                            daysInMonth = when (month) {
                                1, 3, 5, 7, 8, 10, 12 -> 31
                                4, 6, 9, 11 -> 30
                                2 -> if (isLeapYear(year)) 29 else 28
                                else -> 30
                            },
                            onYearChange = { year = it },
                            onMonthChange = { month = it },
                            onDayChange = { day = it }
                        )
                    }

                    1 -> {
                        EditTimePickerContent(
                            hour = startHour,
                            minute = startMinute,
                            period = startPeriod,
                            use24HourFormat = use24HourFormat,
                            onHourChange = { startHour = it },
                            onMinuteChange = { startMinute = it },
                            onPeriodChange = { startPeriod = it }
                        )
                    }

                    2 -> {
                        EditTimePickerContent(
                            hour = endHour,
                            minute = endMinute,
                            period = endPeriod,
                            use24HourFormat = use24HourFormat,
                            onHourChange = { newHour ->
                                endHour = newHour
                                val newEndHour24 = to24Hour(newHour, endPeriod)
                                val startHour24 = to24Hour(startHour, startPeriod)

                                // Check if the new time would be invalid
                                if (newEndHour24 < startHour24 || (newEndHour24 == startHour24 && endMinute < startMinute))
                                {
                                    // Auto-adjust to 1 hour after start
                                    val adjustedHour24 = (startHour24 + 1) % 24

                                    endMinute = startMinute

                                    if (use24HourFormat) {
                                        endHour = adjustedHour24
                                    } else {
                                        endHour = if (adjustedHour24 == 0) 12 else if (adjustedHour24 > 12) adjustedHour24 - 12 else adjustedHour24
                                    }

                                    endPeriod = if (adjustedHour24 >= 12) "PM" else "AM"
                                }
                            },
                            onMinuteChange = { newMinute ->
                                endMinute = newMinute
                                val endHour24 = to24Hour(endHour, endPeriod)
                                val startHour24 = to24Hour(startHour, startPeriod)

                                // Check if the new time would be invalid
                                if (endHour24 < startHour24 || (endHour24 == startHour24 && newMinute < startMinute))
                                {
                                    // If same hour and invalid minutes, keep the hour but set minutes ahead
                                    if (endHour24 == startHour24) {
                                        endMinute = (startMinute + 5) % 60
                                        // If minutes wrap around, increment hour
                                        if (endMinute <= startMinute) {
                                            val adjustedHour24 = (startHour24 + 1) % 24

                                            if (use24HourFormat) {
                                                endHour = adjustedHour24
                                            } else {
                                                endHour = if (adjustedHour24 == 0) 12 else if (adjustedHour24 > 12) adjustedHour24 - 12 else adjustedHour24
                                            }

                                            endPeriod = if (adjustedHour24 >= 12) "PM" else "AM"
                                        }
                                    } else {
                                        // Different hour, auto-adjust to 1 hour after start
                                        val adjustedHour24 = (startHour24 + 1) % 24

                                        endMinute = startMinute

                                        if (use24HourFormat) {
                                            endHour = adjustedHour24
                                        } else {
                                            endHour = if (adjustedHour24 == 0) 12 else if (adjustedHour24 > 12) adjustedHour24 - 12 else adjustedHour24
                                        }

                                        endPeriod = if (adjustedHour24 >= 12) "PM" else "AM"
                                    }
                                }
                            },
                            onPeriodChange = { newPeriod ->
                                endPeriod = newPeriod
                                val newEndHour24 = to24Hour(endHour, newPeriod)
                                val startHour24 = to24Hour(startHour, startPeriod)

                                // Check if the new time would be invalid
                                if (newEndHour24 < startHour24 || (newEndHour24 == startHour24 && endMinute < startMinute))
                                {
                                    // Auto-adjust to 1 hour after start
                                    val adjustedHour24 = (startHour24 + 1) % 24

                                    endMinute = startMinute

                                    if (use24HourFormat) {
                                        endHour = adjustedHour24
                                    } else {
                                        endHour = if (adjustedHour24 == 0) 12 else if (adjustedHour24 > 12) adjustedHour24 - 12 else adjustedHour24
                                    }

                                    endPeriod = if (adjustedHour24 >= 12) "PM" else "AM"
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalStart = LocalTime(to24Hour(startHour, startPeriod), startMinute)
                    val finalEnd = LocalTime(to24Hour(endHour, endPeriod), endMinute)
                    onEventUpdated(eventId, selectedDate, title, finalStart, finalEnd)
                    onDismiss()
                },
                enabled = canConfirm
            ) {
                Text("Save Changes")
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
fun EditTimePickerContent(
    hour: Int,
    minute: Int,
    period: String,
    use24HourFormat: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onPeriodChange: (String) -> Unit
) {
    // Convert display hour + period to 24-hour for preview
    val previewHour = if (use24HourFormat) {
        hour
    } else {
        if (period == "AM") {
            if (hour == 12) 0 else hour
        } else {
            if (hour == 12) 12 else hour + 12
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Preview of selected time
        Text(
            text = formatTime(LocalTime(previewHour, minute), use24HourFormat),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!use24HourFormat) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onPeriodChange("AM") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (period == "AM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("AM")
                }

                Button(
                    onClick = { onPeriodChange("PM") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (period == "PM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Hour", style = MaterialTheme.typography.bodyMedium)
                val hourRange = if (use24HourFormat) (0..23).toList() else (1..12).toList()
                EditWheelNumberPicker(
                    items = hourRange,
                    selectedItem = hour,
                    onValueChange = onHourChange
                )
            }

            Text(":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Minute", style = MaterialTheme.typography.bodyMedium)
                EditWheelNumberPicker(
                    items = (0..59 step 5).toList(),
                    selectedItem = minute - (minute % 5),
                    onValueChange = onMinuteChange
                )
            }
        }
    }
}

@Composable
fun EditWheelNumberPicker(
    items: List<Int>,
    selectedItem: Int,
    onValueChange: (Int) -> Unit,
    visibleItemsCount: Int = 5
) {
    val listState = rememberLazyListState()

    // Scroll to selected item whenever it changes
    LaunchedEffect(selectedItem) {
        val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
        listState.animateScrollToItem(selectedIndex)
    }

    val itemHeight = 40.dp
    val centerIndex = visibleItemsCount / 2

    Box(
        modifier = Modifier
            .height(itemHeight * visibleItemsCount)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * centerIndex),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable { onValueChange(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item == selectedItem) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Optional: Overlay highlight at center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                )
        )
    }
}
