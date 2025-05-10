package com.example.myapplication.CalendarHelper

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.HelperViews.CalendarViewModel
import com.example.myapplication.HelperViews.SettingsViewModel
import com.example.myapplication.Pages.Event
import com.example.myapplication.Pages.TimeSlotWithEvents
import com.example.myapplication.Pages.TimeSlotWithEventsItem
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn


@Composable
fun DayView(
    date: LocalDate,
    isTablet: Boolean,
    orientation: Int,
    calendarViewModel: CalendarViewModel,
    settingsViewModel: SettingsViewModel,
    selectedDate: MutableState<LocalDate>,
    isGestureInProgress: Boolean = false
) {
    var showDateTimePicker  by remember { mutableStateOf(false) }
    var showEditEventDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    val events by calendarViewModel.events.collectAsState()
    val eventAddResult by calendarViewModel.eventAddResult.collectAsState()

    // Time format preference
    val use24HourFormat by settingsViewModel.use24HourFormat.collectAsState()

    // Show success/error messages
    LaunchedEffect(eventAddResult) {
        if (eventAddResult is CalendarViewModel.EventAddResult.Success ||
            eventAddResult is CalendarViewModel.EventAddResult.Error) {
            // Reset after a short delay
            delay(3000)
            calendarViewModel.resetEventAddResult()
        }
    }

    // Get events for the selected date
    val dateEvents = events[date] ?: emptyList()

    // Format for day header
    val dayHeaderText = "${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${date.dayOfMonth} " +
            "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year}"

    // Generate time slots with events - using full range 0-23 to include all hours
    val timeSlots = remember(dateEvents) {
        (0..23).map { hour ->
            // Filter events that start in this hour
            val hourEvents = dateEvents.filter { it.startTime.hour == hour }
            TimeSlotWithEvents(hour, hourEvents)
        }
    }



    // for debugging and testing
    LaunchedEffect(timeSlots) {
        println("Generated ${timeSlots.size} time slots")
        timeSlots.forEach { slot ->
            println("Hour ${slot.hour}: ${slot.events.size} events")
        }
    }


    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        // Horizontal layout for landscape mode
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Left panel - time grid with events
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayHeaderText,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    // Add event button
                    Button(
                        onClick = { showDateTimePicker  = true },
                        enabled = !isGestureInProgress
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Event",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add Event")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Time slots with events - ensure we see all hours
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    state = rememberLazyListState()
                ) {
                    items(timeSlots.size) { index ->
                        val slot = timeSlots[index]
                        TimeSlotWithEventsItem(
                            timeSlot = slot,
                            use24HourFormat = use24HourFormat,
                            onEventLongPress = { event ->
                                selectedEvent = event
                                showEditEventDialog = true
                            }

                        )

                        // Add a small spacer after each item
                        if (index < timeSlots.size - 1) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }


                // Event add result message
                when (val result = eventAddResult) {
                    is CalendarViewModel.EventAddResult.Success -> {
                        Text(
                            "Event added successfully!",
                            color = Color.Green,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    is CalendarViewModel.EventAddResult.Error -> {
                        Text(
                            result.message,
                            color = Color.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {}
                }
            }

            // Right panel - calendar navigation
            if (isTablet) {
                HorizontalDivider(
                    modifier = Modifier
                        .height(480.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mini month view for navigation
                    Text(
                        "Calendar",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // LazyColumn containing the MonthView
                    // MonthView directly in the column
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        MonthView(
                            currentDate = date,
                            isTablet = true,
                            orientation = Configuration.ORIENTATION_PORTRAIT,
                            eventDates = events.keys,
                            onDaySelected = { selectedNewDate ->
                                selectedDate.value = selectedNewDate
                            },
                            onMonthChanged  = { newMonth  ->
                                selectedDate.value = newMonth
                            }
                        )
                    }

                }
            }
        }
    } else {
        // Vertical layout for portrait mode
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with day and time format toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayHeaderText,
                    style = MaterialTheme.typography.headlineSmall
                )

            }

            Spacer(Modifier.height(16.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // "Today" button to return to current day
                OutlinedButton(
                    onClick = {
                        // Only update if not already on today to prevent unnecessary recomposition
                        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                        if (selectedDate.value != today) {
                            selectedDate.value = today
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    enabled = !isGestureInProgress
                ) {
                    Spacer(Modifier.width(4.dp))
                    Text("Today")
                }

                // Add event button
                Button(
                    onClick = { showDateTimePicker  = true },
                    enabled = !isGestureInProgress
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Event",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Add Event")
                }

            }

            Spacer(Modifier.height(8.dp))

            // Event add result message
            when (val result = eventAddResult) {
                is CalendarViewModel.EventAddResult.Success -> {
                    Text(
                        "Event added successfully!",
                        color = Color.Green,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                is CalendarViewModel.EventAddResult.Error -> {
                    Text(
                        result.message,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {}
            }

            // Time slots with events - ensure we see all hours
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 80.dp),  // Add more bottom padding for visibility
                // Add state to remember scroll position
                state = rememberLazyListState()
            ) {
                // Explicitly use all time slots
                items(timeSlots.size) { index ->
                    val slot = timeSlots[index]
                    TimeSlotWithEventsItem(
                        timeSlot = slot,
                        use24HourFormat = use24HourFormat,
                        onEventLongPress = { event ->
                            selectedEvent = event
                            showEditEventDialog = true
                        }

                    )

                    // Add a small spacer after each item for better visibility
                    if (index < timeSlots.size - 1) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

        }
    }

    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialDate = date,
            initialStartTime = LocalTime(12, 0),
            initialEndTime = LocalTime(13, 0),
            onDateTimeSelected = { selectedDate, startTime, endTime ->
                // Adding default title - user can update event details later
                calendarViewModel.addEvent(selectedDate, "New Event", startTime, endTime)
                showDateTimePicker = false

            },
            onDismiss = { showDateTimePicker = false },
            use24HourFormat = use24HourFormat
        )

    }

    // Edit Event Dialog
    if (showEditEventDialog && selectedEvent != null) {
        EditEventDialog(
            event = selectedEvent!!,
            eventId = selectedEvent!!.id,
            eventDate = date, // Pass the current date
            onEventUpdated = { id, newDate, newTitle, newStartTime, newEndTime ->
                calendarViewModel.updateEvent(id, newDate, newTitle, newStartTime, newEndTime)
            },
            onDeleteEvent = { id ->
                calendarViewModel.deleteEvent(id)
            },
            onDismiss = {
                showEditEventDialog = false
                selectedEvent = null
            },
            use24HourFormat = use24HourFormat
        )

    }


}