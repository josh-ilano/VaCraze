package com.example.myapplication.CalendarHelper

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.Pages.CalendarDay
import com.example.myapplication.Pages.isLeapYear
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.time.Month

@Composable
fun MonthView(
    currentDate: LocalDate,
    isTablet: Boolean,
    orientation: Int,
    eventDates: Set<LocalDate> = emptySet(),
    onDaySelected: (LocalDate) -> Unit = {},
    onMonthChanged: (LocalDate) -> Unit = {}

) {
    val year = currentDate.year
    val month = currentDate.month
    val firstDay = LocalDate(year, month, 1)
    val dayOfWeekStart = (firstDay.dayOfWeek.isoDayNumber - 1) % 7 // Convert to 0-indexed (Sunday = 0)

    val daysInMonth = when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
    }

    val dates = remember(year, month) {
        (1..daysInMonth).map { LocalDate(year, month, it) }
    }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Determine grid size based on orientation and tablet mode
    val gridColumns = if (orientation == Configuration.ORIENTATION_LANDSCAPE && isTablet) {
        // More columns for landscape tablet for better space utilization
        GridCells.Fixed(14) // Two weeks at once for large tablets
    } else if (orientation == Configuration.ORIENTATION_LANDSCAPE || isTablet) {
        GridCells.Fixed(7) // Standard 7 columns for weeks
    } else {
        GridCells.Fixed(7) // Standard 7 columns for portrait phone
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)) {

        // Month header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous month button - keep in month view by updating date only
            Text(
                text = "< Prev",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        // Create the previous month's date but stay in MONTH view
                        val prevMonthDate = currentDate.minus(1, DateTimeUnit.MONTH)
                        onMonthChanged(prevMonthDate)

                    }
                    .padding(8.dp)
            )

            // Month title
            Text(
                text = "${month.name.lowercase().replaceFirstChar { it.uppercase() }} $year",
                style = MaterialTheme.typography.headlineMedium
            )

            // Next month button - keep in month view by updating date only
            Text(
                text = "Next >",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        // Create the next month's date but stay in MONTH view
                        val nextMonthDate = currentDate.plus(1, DateTimeUnit.MONTH)
                        onMonthChanged(nextMonthDate)
                    }
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Weekday headers
        Row(Modifier.fillMaxWidth()) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Calendar grid
        val gridLayout = if (orientation == Configuration.ORIENTATION_LANDSCAPE && isTablet) {
            // For landscape tablets, show multiple months side by side
            Row(modifier = Modifier.weight(1f)) {
                // Current month
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    userScrollEnabled = false,
                    modifier = Modifier.weight(1f)
                ) {
                    // Empty cells for days before the first day of the month
                    items(dayOfWeekStart) {
                        Box(modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp))
                    }

                    // Days of the month
                    items(dates) { date ->
                        val isToday = date == today
                        val hasEvents = eventDates.contains(date)

                        CalendarDay(
                            date = date,
                            isToday = isToday,
                            hasEvents = hasEvents,
                            onDateSelected = onDaySelected
                        )
                    }
                }

                // Next month preview (only for landscape tablets)
                val nextMonth = currentDate.plus(1, DateTimeUnit.MONTH)
                val nextMonthYear = nextMonth.year
                val nextMonthValue = nextMonth.month
                val nextMonthFirstDay = LocalDate(nextMonthYear, nextMonthValue, 1)
                val nextMonthDayOfWeekStart = (nextMonthFirstDay.dayOfWeek.isoDayNumber - 1) % 7

                val nextMonthDaysInMonth = when (nextMonthValue) {
                    Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
                    Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
                    Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
                    Month.FEBRUARY -> if (isLeapYear(nextMonthYear)) 29 else 28
                }

                val nextMonthDates = remember(nextMonthYear, nextMonthValue) {
                    (1..nextMonthDaysInMonth).map {
                        LocalDate(nextMonthYear, nextMonthValue, it)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${nextMonthValue.name.lowercase().replaceFirstChar { it.uppercase() }} $nextMonthYear",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 8.dp)
                    )

                    // Week day headers for second month
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                            Text(
                                text = it,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        userScrollEnabled = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Empty cells for days before the first day of the month
                        items(nextMonthDayOfWeekStart) {
                            Box(modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp))
                        }

                        // Days of the next month
                        items(nextMonthDates) { date ->
                            val isToday = date == today
                            val hasEvents = eventDates.contains(date)

                            CalendarDay(
                                date = date,
                                isToday = isToday,
                                hasEvents = hasEvents,
                                onDateSelected = onDaySelected
                            )
                        }
                    }
                }
            }
        } else {
            // Standard layout for phones and portrait tablets
            LazyVerticalGrid(
                columns = gridColumns,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) {
                // Empty cells for days before the first day of the month
                items(dayOfWeekStart) {
                    Box(modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp))
                }

                // Days of the month
                items(dates) { date ->
                    val isToday = date == today
                    val hasEvents = eventDates.contains(date)

                    CalendarDay(
                        date = date,
                        isToday = isToday,
                        hasEvents = hasEvents,
                        onDateSelected = onDaySelected
                    )
                }
            }
        }

        // Display the grid layout
        gridLayout
    }
}
