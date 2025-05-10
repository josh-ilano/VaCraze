package com.example.myapplication.Pages

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.CalendarHelper.DayView
import com.example.myapplication.CalendarHelper.MonthView
import com.example.myapplication.CalendarHelper.YearView
import com.example.myapplication.HelperViews.CalendarViewModel
import com.example.myapplication.HelperViews.SettingsViewModel
import com.example.myapplication.Tools.formatTime
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.time.Month
import kotlin.math.abs

data class Event(
    val id: String = "",
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime
)

// Data class to hold a time slot and its events
data class TimeSlotWithEvents(
    val hour: Int,
    val events: List<Event>
)

enum class CalendarViewType {
    DAY, MONTH, YEAR;

    fun zoomOut(): CalendarViewType = when (this) {
        DAY -> MONTH
        MONTH -> YEAR
        YEAR -> YEAR
    }

    fun zoomIn(): CalendarViewType = when (this) {
        YEAR -> MONTH
        MONTH -> DAY
        DAY -> DAY
    }

    fun next(date: LocalDate) = when (this) {
        DAY -> date.plus(1, DateTimeUnit.DAY)
        MONTH -> date.plus(1, DateTimeUnit.MONTH)
        YEAR -> date.plus(1, DateTimeUnit.YEAR)
    }

    fun previous(date: LocalDate): LocalDate = when (this) {
        DAY -> date.minus(1, DateTimeUnit.DAY)
        MONTH -> date.minus(1, DateTimeUnit.MONTH)
        YEAR -> date.minus(1, DateTimeUnit.YEAR)
    }
}

@Composable
fun CalendarPage(calendarViewModel: CalendarViewModel = viewModel(), settingsViewModel : SettingsViewModel) {
    // Load events when the page is initialized
    LaunchedEffect(Unit) {
        calendarViewModel.loadEvents()
    }

    val events by calendarViewModel.events.collectAsState()

    // Extract dates that have events
    val eventDates by remember(events) {
        derivedStateOf { events.keys.toSet() }
    }

    // State for the current view type (day, month, year)
    val viewType = remember { mutableStateOf(CalendarViewType.DAY) }

    // State for the selected date
    val selectedDate = remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }

    // Used to prevent handling multiple gestures simultaneously
    var isGestureInProgress by remember { mutableStateOf(false) }

    // Visual feedback state for transitions
    var transitionFeedback by remember { mutableStateOf("") }
    var showTransitionFeedback by remember { mutableStateOf(false) }

    val density = LocalDensity.current.density

    // Automatically hide feedback after a delay
    LaunchedEffect(showTransitionFeedback) {
        if (showTransitionFeedback) {
            delay(500)
            showTransitionFeedback = false
            isGestureInProgress = false
        }
    }

    val orientation = LocalConfiguration.current.orientation

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Optimized gesture detection
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Only handle gestures when no other gesture is in progress
                    if (!isGestureInProgress) {
                        // Handle pinch zoom
                        if (abs(zoom - 1f) > 0.3f) {
                            isGestureInProgress = true

                            if (zoom < 0.7f) {
                                // Zoom out (pinch in)
                                val newViewType = when {
                                    orientation == Configuration.ORIENTATION_LANDSCAPE && viewType.value == CalendarViewType.DAY -> CalendarViewType.YEAR
                                    orientation == Configuration.ORIENTATION_LANDSCAPE && viewType.value == CalendarViewType.YEAR -> CalendarViewType.YEAR
                                    else -> viewType.value.zoomOut()
                                }

                                if (newViewType != viewType.value) {
                                    viewType.value = newViewType
                                    transitionFeedback = "Zoomed out to ${newViewType.name} view"
                                    showTransitionFeedback = true
                                } else {
                                    isGestureInProgress = false
                                }
                            } else if (zoom > 1.3f) {
                                // Zoom in (pinch out)
                                val newViewType = when {
                                    orientation == Configuration.ORIENTATION_LANDSCAPE && viewType.value == CalendarViewType.YEAR -> CalendarViewType.DAY
                                    orientation == Configuration.ORIENTATION_LANDSCAPE && viewType.value == CalendarViewType.DAY -> CalendarViewType.DAY
                                    else -> viewType.value.zoomIn()
                                }

                                if (newViewType != viewType.value) {
                                    viewType.value = newViewType
                                    transitionFeedback = "Zoomed in to ${newViewType.name} view"
                                    showTransitionFeedback = true
                                } else {
                                    isGestureInProgress = false
                                }
                            }
                        }
                        // Handle horizontal swipe
                        else if (abs(pan.x) > 50 * density) {
                            isGestureInProgress = true

                            if (pan.x > 0) {
                                // Swipe right (previous)
                                selectedDate.value = viewType.value.previous(selectedDate.value)
                                transitionFeedback = "Previous"
                                showTransitionFeedback = true
                            } else {
                                // Swipe left (next)
                                selectedDate.value = viewType.value.next(selectedDate.value)
                                transitionFeedback = "Next"
                                showTransitionFeedback = true
                            }
                        }
                    }
                }
            }
    ) {
        val isTablet = maxWidth > 600.dp


        // Render the appropriate view based on the current view type
        when (viewType.value) {
            CalendarViewType.DAY -> DayView(
                date = selectedDate.value,
                isTablet = isTablet,
                orientation = orientation,
                calendarViewModel = calendarViewModel,
                settingsViewModel = settingsViewModel,
                selectedDate = selectedDate,
                isGestureInProgress = isGestureInProgress
            )
            CalendarViewType.MONTH -> {
                // if the view type is month, then we only want to show portrait mode,
                // landscape mode should just go to day view's landscape mode
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    MonthView(
                        currentDate = selectedDate.value,
                        isTablet = isTablet,
                        orientation = orientation,
                        eventDates = eventDates,
                        onDaySelected = { newDate ->
                            //  Updating the date and change to Day view
                            selectedDate.value = newDate
                            viewType.value = CalendarViewType.DAY
                        },
                        onMonthChanged = { newMonth ->
                            // updating the month, stay in Month View
                            selectedDate.value = newMonth
                        }
                    )
                } else {
                    // Switch to DayView if landscape
                    DayView(
                        date = selectedDate.value,
                        isTablet = isTablet,
                        orientation = orientation,
                        calendarViewModel = calendarViewModel,
                        settingsViewModel = settingsViewModel,
                        selectedDate = selectedDate,
                        isGestureInProgress = isGestureInProgress
                    )
                }
            }
            CalendarViewType.YEAR -> YearView(
                currentDate = selectedDate.value,
                isTablet = isTablet,
                orientation = orientation,
                selectedDate = selectedDate,
                viewType = viewType
            )
        }

        // Navigation controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .padding(start = 60.dp)
                .align(Alignment.BottomStart)
                //.align(Alignment.Center)
        ) {
            // Day view button
            OutlinedButton(
                onClick = { viewType.value = CalendarViewType.DAY },
                //modifier = Modifier.padding(horizontal = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (viewType.value == CalendarViewType.DAY)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Day")
            }

            // Year view button
            OutlinedButton(
                onClick = { viewType.value = CalendarViewType.YEAR },
                //modifier = Modifier.padding(horizontal = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (viewType.value == CalendarViewType.YEAR)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Text("Year")
            }
        }

        // Transition feedback overlay
        if (showTransitionFeedback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = transitionFeedback,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}


@Composable
fun CalendarDay(
    date: LocalDate,
    isToday: Boolean,
    hasEvents: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primary
                    hasEvents -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                }
            )
            .clickable { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Day number
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
            )

            // Event marker - more visible and with label
            if (hasEvents) {
                Spacer(modifier = Modifier.height(2.dp))

                if (isToday) {
                    // For today, show white marker
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(6.dp)
                            .background(Color.White, CircleShape)
                    )
                } else {
                    // For other days, show primary colored marker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )

                        // Optional: Add a small text label
                        Text(
                            text = "Event",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 8.sp,  // Very small text
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MonthItem(
    month: Month,
    year: Int,
    isCurrentMonth: Boolean,
    onMonthSelected: (Month) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1.2f)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isCurrentMonth)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .clickable { onMonthSelected(month) }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Month name
            Text(
                text = month.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mini calendar representation (simplified)
            MiniCalendarGrid(month = month, year = year)
        }
    }
}

@Composable
fun MiniCalendarGrid(month: Month, year: Int) {
    // Create a simple visual representation of the month's calendar
    val daysInMonth = when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
    }

    val firstDay = LocalDate(year, month, 1)
    val dayOfWeekStart = (firstDay.dayOfWeek.isoDayNumber - 1) % 7 // Convert to 0-indexed (Sunday = 0)

    // We'll show a simplified 4x7 grid to represent the month
    val rows = 4 // Simplified representation with just 4 rows

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - dayOfWeekStart + 1
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayIndex in 1..daysInMonth) {
                            // Show dots for days that exist in this month
                            Box(
                                modifier = Modifier
                                    .size(2.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun TimeSlotWithEventsItem(
    timeSlot: TimeSlotWithEvents,
    use24HourFormat: Boolean = true,
    onEventLongPress: ((Event) -> Unit)? = null
) {
    val hour = timeSlot.hour
    val events = timeSlot.events

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Time header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Format hour based on 12/24 hour preference
            val formattedHour = if (use24HourFormat) {
                // Military time format (24 hour)
                hour.toString().padStart(2, '0') + ":00"
            } else {
                // Standard time format (12 hour with AM/PM)
                val h = if (hour % 12 == 0) 12 else hour % 12
                val period = if (hour >= 12) "PM" else "AM"
                "$h:00 $period"
            }

            Text(
                text = formattedHour,
                modifier = Modifier.width(70.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }

        // Events in this time slot
        if (events.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 70.dp) // Align with the time
            ) {
                events.forEach { event ->
                    // Use pointerInput to detect long press
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(event.id) {
                                detectTapGestures(
                                    onLongPress = {
                                        onEventLongPress?.invoke(event)
                                    }
                                )
                            }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    // Display both start and end times
                                    Text(
                                        text = "${formatTime(event.startTime, use24HourFormat)} - ${formatTime(event.endTime, use24HourFormat)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

