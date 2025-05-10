package com.example.myapplication.CalendarHelper

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.Pages.CalendarViewType
import com.example.myapplication.Pages.MonthItem
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.time.Month


@Composable
fun YearView(
    currentDate: LocalDate,
    isTablet: Boolean,
    orientation: Int,
    selectedDate: MutableState<LocalDate>,
    viewType: MutableState<CalendarViewType>
) {
    val currentYear = currentDate.year
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Year header with navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous year button
            Text(
                text = "< Prev",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { selectedDate.value = selectedDate.value.minus(1, DateTimeUnit.YEAR) }
                    .padding(8.dp)
            )

            // Year title
            Text(
                text = currentYear.toString(),
                style = MaterialTheme.typography.headlineMedium
            )

            // Next year button
            Text(
                text = "Next >",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { selectedDate.value = selectedDate.value.plus(1, DateTimeUnit.YEAR) }
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // The grid of months depends on the orientation and device type
        val columns = when {
            orientation == Configuration.ORIENTATION_LANDSCAPE && isTablet -> 4 // 4x3 grid for landscape tablets
            orientation == Configuration.ORIENTATION_LANDSCAPE -> 3 // 3x4 grid for landscape phones
            isTablet -> 3 // 3x4 grid for portrait tablets
            else -> 2 // 2x6 grid for portrait phones
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(Month.entries) { month ->
                MonthItem(
                    month = month,
                    year = currentYear,
                    isCurrentMonth = today.year == currentYear && today.month == month,
                    onMonthSelected = { selectedMonth ->
                        // Set date to the first day of selected month
                        selectedDate.value = LocalDate(currentYear, selectedMonth, 1)
                        // Change view type to MONTH
                        viewType.value = CalendarViewType.MONTH
                    }
                )
            }
        }
    }
}
