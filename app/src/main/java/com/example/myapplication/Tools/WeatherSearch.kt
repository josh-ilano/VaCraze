package com.example.myapplication.Tools

import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.CalendarHelper.TimePickerContent
import com.example.myapplication.CalendarHelper.WheelNumberPicker
import com.example.myapplication.Pages.isLeapYear
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- 1) Data model for one day’s forecast ---
data class WeatherDay(
    val date: String,
    val description: String,
    val tempMax: Double,
    val tempMin: Double
)

// --- 2) ViewModel to fetch & hold the 7-day forecast ---
class WeatherViewModel : ViewModel() {
    private val _forecast = androidx.lifecycle.MutableLiveData<List<WeatherDay>>(emptyList())
    val forecast = _forecast as androidx.lifecycle.LiveData<List<WeatherDay>>
    private val client = OkHttpClient()

    fun loadForecast(lat: Double, lon: Double, apiKey: String) {
        viewModelScope.launch {
            // URL for the Google Weather API preview
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("weather.googleapis.com")
                .addPathSegments("v1/forecast/days:lookup")
                .addQueryParameter("location.latitude", lat.toString())
                .addQueryParameter("location.longitude", lon.toString())
                .addQueryParameter("days", "7")
                .addQueryParameter("key", apiKey)
                .build()

            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("WeatherVM", "Network error", e)
                }

                override fun onResponse(call: Call, resp: Response) {
                    if (!resp.isSuccessful) {
                        Log.e("WeatherVM", "HTTP ${resp.code}: ${resp.body?.string()}")
                        return
                    }
                    val body = resp.body?.string().orEmpty()
                    val root = try {
                        JSONObject(body)
                    } catch (e: JSONException) {
                        Log.e("WeatherVM", "Invalid JSON", e)
                        return
                    }

                    val arr = root.optJSONArray("forecastDays") ?: run {
                        Log.e("WeatherVM", "Missing forecastDays")
                        return
                    }

                    val df = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                    val list = mutableListOf<WeatherDay>()
                    for (i in 0 until arr.length()) {
                        val day = arr.getJSONObject(i)

                        // 4) Build the display date from the "displayDate" object
                        val disp = day.getJSONObject("displayDate")
                        val year  = disp.optInt("year", 1970)
                        val month = disp.optInt("month", 1)
                        val dayOfMonth = disp.optInt("day", 1)
                        val cal = Calendar.getInstance().apply {
                            set(year, month - 1, dayOfMonth)
                        }
                        val date = df.format(cal.time)

                        // 5) Extract the high & low temperatures
                        val maxTempObj = day.getJSONObject("maxTemperature")
                        val minTempObj = day.getJSONObject("minTemperature")
                        val maxValue = maxTempObj.optDouble("degrees", Double.NaN)
                        val minValue = minTempObj.optDouble("degrees", Double.NaN)

                        // 6) Get the daytime weather description
                        val desc = day
                            .getJSONObject("daytimeForecast")
                            .getJSONObject("weatherCondition")
                            .getJSONObject("description")
                            .optString("text", "")

                        list += WeatherDay(
                            date        = date,
                            description = desc,
                            tempMax     = maxValue,
                            tempMin     = minValue
                        )
                    }
                    _forecast.postValue(list)
                }



            })
        }
    }
}


@Composable
fun WeatherDayCard(day: WeatherDay) {

    // Determining weather color based on conditions
    val weatherColor = when {
        day.description.contains("rain", ignoreCase = true) ->
            Color(0xFF4286f4) // Blue for rain
        day.description.contains("cloud", ignoreCase = true) ->
            Color(0xFF78909c) // Gray for clouds
        day.description.contains("sun", ignoreCase = true) ||
                day.description.contains("clear", ignoreCase = true) ->
            Color(0xFFffb300) // Yellow/orange for sun/clear
        day.description.contains("snow", ignoreCase = true) ->
            Color(0xFFe0e0e0) // Light gray for snow
        day.description.contains("storm", ignoreCase = true) ||
                day.description.contains("thunder", ignoreCase = true) ->
            Color(0xFF5c6bc0) // Purple for storms
        else ->
            Color(0xFF78909c) // Default gray
    }

    // Determining if this day has extreme weather (for border color)
    val hasBadWeather = day.description.contains("rain", ignoreCase = true) ||
            day.description.contains("storm", ignoreCase = true) ||
            day.description.contains("thunder", ignoreCase = true) ||
            day.description.contains("snow", ignoreCase = true) ||
            day.description.contains("hail", ignoreCase = true) ||
            day.description.contains("severe", ignoreCase = true) ||
            day.tempMax > 95 || day.tempMin < 40

    val borderColor = if (hasBadWeather) {
        Color.Red.copy(alpha = 0.7f)
    } else {
        Color.Transparent
    }

    Card(
        modifier = Modifier
            .padding(end = 8.dp)
            .width(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (hasBadWeather) BorderStroke(2.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Weather color indicator - a circle with the weather color
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(weatherColor, CircleShape)
                    .border(1.dp, Color.DarkGray.copy(alpha = 0.3f), CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Temperature range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "${day.tempMax.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFe57373) // Warm color for high temp
                )
                Text(
                    text = "${day.tempMin.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4fc3f7) // Cool color for low temp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Weather description
            Text(
                text = day.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WeatherAwareDateTimePickerDialog(
    initialDate: LocalDate,
    initialStartTime: LocalTime = LocalTime(12, 0),
    initialEndTime: LocalTime = LocalTime(13, 0),
    onDateTimeSelected: (LocalDate, LocalTime, LocalTime) -> Unit,
    onDismiss: () -> Unit,
    use24HourFormat: Boolean,
    weatherForecast: List<WeatherDay> = emptyList()
) {
    // Tab selection state
    var selectedTab by remember { mutableIntStateOf(0) }

    // Date state
    var year by remember { mutableStateOf(initialDate.year) }
    var month by remember { mutableStateOf(initialDate.month.value) }
    var day by remember { mutableStateOf(initialDate.dayOfMonth) }

    // Weather warning state
    var showWeatherWarning by remember { mutableStateOf(false) }
    var weatherWarningMessage by remember { mutableStateOf("") }
    var selectedDateWithWarning by remember { mutableStateOf<LocalDate?>(null) }
    var selectedStartTimeWithWarning by remember { mutableStateOf<LocalTime?>(null) }
    var selectedEndTimeWithWarning by remember { mutableStateOf<LocalTime?>(null) }

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
    var startHour24 by remember { mutableStateOf(initialStartTime.hour) }
    var startMinute by remember { mutableStateOf(initialStartTime.minute) }
    var startHour12 by remember { mutableStateOf(initialStartHour12) }
    var startPeriod by remember { mutableStateOf(initialStartPeriod) }

    // Initialize end time state based on format
    val (initialEndHour12, initialEndPeriod) = to12HourFormat(initialEndTime.hour)
    var endHour24 by remember { mutableStateOf(initialEndTime.hour) }
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
    val startTime = LocalTime(startHour24, startMinute)
    val endTime = LocalTime(endHour24, endMinute)

    // Check if date is valid (not in the past)
    val isDateValid = selectedDate >= today

    // Check if times are valid (end time after start time and not in the past if today)
    val isTimeValid by remember(startHour24, startMinute, endHour24, endMinute, selectedDate) {
        derivedStateOf {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentTime = now.time

            val start = LocalTime(startHour24, startMinute)
            val end = LocalTime(endHour24, endMinute)

            val validStart = if (selectedDate == now.date) start > currentTime else true
            val validEnd = end > start

            validStart && validEnd
        }
    }

    // Get weather for selected date
    val selectedDateWeather = weatherForecast.find {
        // Parse the date from the forecast (format is "Day, Mon D")
        // Example: "Mon, May 13"
        val dateParts = it.date.split(", ")
        if (dateParts.size < 2) return@find false

        val datePartSecond = dateParts[1].split(" ")
        if (datePartSecond.size < 2) return@find false

        val monthStr = datePartSecond[0]
        val dayStr = datePartSecond[1].toIntOrNull() ?: return@find false

        val monthNum = when (monthStr.lowercase()) {
            "jan" -> 1
            "feb" -> 2
            "mar" -> 3
            "apr" -> 4
            "may" -> 5
            "jun" -> 6
            "jul" -> 7
            "aug" -> 8
            "sep" -> 9
            "oct" -> 10
            "nov" -> 11
            "dec" -> 12
            else -> return@find false
        }

        // Check if this matches our selected date
        month == monthNum && day == dayStr
    }

    // Overall validation
    val canConfirm = isDateValid && isTimeValid

    // Function to check for bad weather
    fun checkForWeatherWarning() {
        // Only check if we have weather data and can confirm the date/time
        if (canConfirm && selectedDateWeather != null) {
            val badWeatherKeywords = listOf(
                "rain", "storm", "thunder", "snow", "hail", "sleet", "hurricane",
                "tornado", "blizzard", "foggy", "severe"
            )

            val hasBadWeather = badWeatherKeywords.any {
                selectedDateWeather.description.contains(it, ignoreCase = true)
            }

            // Check for extreme temperatures (below 40°F or above 95°F)
            val hasExtremeTemp = selectedDateWeather.tempMax > 95 || selectedDateWeather.tempMin < 40

            if (hasBadWeather || hasExtremeTemp) {
                // Prepare warning message
                val weatherType = when {
                    selectedDateWeather.description.contains("rain", ignoreCase = true) -> "rain"
                    selectedDateWeather.description.contains("storm", ignoreCase = true) -> "storms"
                    selectedDateWeather.description.contains("snow", ignoreCase = true) -> "snow"
                    selectedDateWeather.description.contains("thunder", ignoreCase = true) -> "thunderstorms"
                    selectedDateWeather.description.contains("hail", ignoreCase = true) -> "hail"
                    selectedDateWeather.tempMax > 95 -> "extreme heat"
                    selectedDateWeather.tempMin < 40 -> "cold temperatures"
                    else -> "bad weather"
                }

                weatherWarningMessage = "Warning: The forecast shows $weatherType on this date. " +
                        "Weather forecast: ${selectedDateWeather.description} with temperatures " +
                        "between ${selectedDateWeather.tempMin.toInt()}° and ${selectedDateWeather.tempMax.toInt()}°."

                selectedDateWithWarning = selectedDate
                selectedStartTimeWithWarning = startTime
                selectedEndTimeWithWarning = endTime

                showWeatherWarning = true
            } else {
                // No bad weather, proceed with event creation
                onDateTimeSelected(selectedDate, startTime, endTime)
            }
        } else {
            // No weather data or invalid date/time, proceed normally
            onDateTimeSelected(selectedDate, startTime, endTime)
        }
    }

    // Weather Warning Dialog
    if (showWeatherWarning) {
        AlertDialog(
            onDismissRequest = { showWeatherWarning = false },
            title = { Text("Weather Warning") },
            text = { Text(weatherWarningMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWeatherWarning = false
                        selectedDateWithWarning?.let { date ->
                            selectedStartTimeWithWarning?.let { startTime ->
                                selectedEndTimeWithWarning?.let { endTime ->
                                    onDateTimeSelected(date, startTime, endTime)
                                }
                            }
                        }
                    }
                ) {
                    Text("Add Anyway")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWeatherWarning = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Main Dialog
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selectedDate.dayOfMonth} ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.year}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Display weather icon if available
                            selectedDateWeather?.let { weather ->
                                Spacer(modifier = Modifier.width(8.dp))
                                val weatherColor = when {
                                    weather.description.contains("rain", ignoreCase = true) ->
                                        Color(0xFF4286f4) // Blue for rain
                                    weather.description.contains("cloud", ignoreCase = true) ->
                                        Color(0xFF78909c) // Gray for clouds
                                    weather.description.contains("sun", ignoreCase = true) ||
                                            weather.description.contains("clear", ignoreCase = true) ->
                                        Color(0xFFffb300) // Yellow/orange for sun/clear
                                    weather.description.contains("snow", ignoreCase = true) ->
                                        Color(0xFFe0e0e0) // Light gray for snow
                                    weather.description.contains("storm", ignoreCase = true) ||
                                            weather.description.contains("thunder", ignoreCase = true) ->
                                        Color(0xFF5c6bc0) // Purple for storms
                                    else ->
                                        Color(0xFF78909c) // Default gray
                                }

                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(weatherColor, CircleShape)
                                        .border(1.dp, Color.DarkGray.copy(alpha = 0.3f), CircleShape)
                                )
                            }
                        }

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

                        // Show weather info if available
                        selectedDateWeather?.let { weather ->
                            Text(
                                "${weather.description}, ${weather.tempMin.toInt()}° - ${weather.tempMax.toInt()}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
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
                        // DATE TAB with weather info
                        DatePickerWithWeatherContent(
                            year = year,
                            month = month,
                            day = day,
                            today = today,
                            daysInMonth = daysInMonth,
                            onYearChange = { year = it },
                            onMonthChange = { month = it },
                            onDayChange = { day = it },
                            weatherForecast = weatherForecast
                        )
                    }
                    1 -> {
                        // START TIME TAB
                        WeatherTimePickerContent(
                            hour24 = startHour24,
                            hour12 = startHour12,
                            minute = startMinute,
                            period = startPeriod,
                            use24HourFormat = use24HourFormat,
                            onHourChange = { newHour ->
                                if (use24HourFormat) {
                                    startHour24 = newHour
                                } else {
                                    startHour12 = newHour
                                    startHour24 = to24HourFormat(newHour, startPeriod)
                                }
                            },
                            onMinuteChange = { startMinute = it },
                            onPeriodChange = { newPeriod ->
                                if (!use24HourFormat) {
                                    startPeriod = newPeriod
                                    startHour24 = to24HourFormat(startHour12, newPeriod)
                                }
                            }
                        )
                    }
                    2 -> {
                        // END TIME TAB
                        WeatherTimePickerContent(
                            hour24 = endHour24,
                            hour12 = endHour12,
                            minute = endMinute,
                            period = endPeriod,
                            use24HourFormat = use24HourFormat,
                            onHourChange = { newHour ->
                                if (use24HourFormat) {
                                    endHour24 = newHour
                                } else {
                                    endHour12 = newHour
                                    endHour24 = to24HourFormat(newHour, endPeriod)
                                }
                            },
                            onMinuteChange = { endMinute = it },
                            onPeriodChange = { newPeriod ->
                                if (!use24HourFormat) {
                                    endPeriod = newPeriod
                                    endHour24 = to24HourFormat(endHour12, newPeriod)
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
                                        val newEndHour24 = (startHour24 + 1) % 24
                                        endHour24 = newEndHour24
                                        endMinute = startMinute

                                        // Update 12-hour format values
                                        val (newHour12, newPeriod) = to12HourFormat(newEndHour24)
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
                    val finalStartTime = LocalTime(startHour24, startMinute)
                    val finalEndTime = LocalTime(endHour24, endMinute)
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
fun WeatherTimePickerContent(
    hour24: Int,
    hour12: Int,
    minute: Int,
    period: String,
    use24HourFormat: Boolean,
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
            text = formatTime(LocalTime(hour24, minute), use24HourFormat),
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
                val displayHour = if (use24HourFormat) hour24 else hour12

                WheelNumberPicker(
                    items = hourRange,
                    selectedItem = displayHour,
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

@Composable
fun DatePickerWithWeatherContent(
    year: Int,
    month: Int,
    day: Int,
    today: LocalDate,
    daysInMonth: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    weatherForecast: List<WeatherDay> = emptyList()
) {
    LazyColumn(
        modifier = Modifier.height(300.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Year picker
        item {
            Text("Year", fontWeight = FontWeight.Bold)

            // Allow selecting from current year with no upper limit
            val minYear = today.year
            val maxYear = minYear + 10

            // Year wheel
            WheelNumberPicker(
                items = (minYear..maxYear).toList(),
                selectedItem = year,
                onValueChange = onYearChange
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Month picker
        item {
            Text("Month", fontWeight = FontWeight.Bold)

            // Determine the minimum allowed month
            val minMonth = if (year == today.year) today.month.value else 1

            // Only allow selecting current or future months
            val validMonths = if (year == today.year) {
                (today.month.value..12).toList()
            } else {
                (1..12).toList()
            }

            // Month chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(validMonths) { monthNum ->
                    val monthName = Month.of(monthNum).name.take(3).lowercase()
                        .replaceFirstChar { it.uppercase() }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (monthNum == month)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surface
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

        // Day picker
        item {
            Text("Day", fontWeight = FontWeight.Bold)

            // Optional: Add a color legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4286f4), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rain", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFffb300), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF5c6bc0), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Storms", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF78909c), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cloudy", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Determine the minimum allowed day
            val minDay = if (year == today.year && month == today.month.value)
                today.dayOfMonth
            else
                1

            // Calendar-style day grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
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

                // Calculate the first day of month offset correctly
                val firstDayOfMonth = try {
                    LocalDate(year, Month.of(month), 1)
                } catch (e: Exception) {
                    // Fallback to current date if there's an issue
                    Log.e("DatePicker", "Error creating date: $e")
                    today
                }

                // Calculate day of week offset: Sunday is 0, Monday is 1, etc.
                // Convert ISO day of week (1-7, Monday-Sunday) to calendar day of week (0-6, Sunday-Saturday)
                val dayOfWeekValue = firstDayOfMonth.dayOfWeek.value % 7
                val dayOfWeekOffset = if (dayOfWeekValue == 0) 6 else dayOfWeekValue - 1

                // Empty boxes for day offset
                items(dayOfWeekOffset) {
                    Box(modifier = Modifier.padding(4.dp))
                }

                // Actual days with weather colors
                items(daysInMonth) { dayNum ->
                    val isSelectable = dayNum >= minDay

                    // Check if we have weather data for this day
                    val dayWeather = weatherForecast.find {
                        // Parse the date from the forecast (format is "Day, Mon D")
                        val dateParts = it.date.split(", ")
                        if (dateParts.size < 2) return@find false

                        val datePartSecond = dateParts[1].split(" ")
                        if (datePartSecond.size < 2) return@find false

                        val monthStr = datePartSecond[0]
                        val dayStr = datePartSecond[1].toIntOrNull() ?: return@find false

                        val monthNum = when (monthStr.lowercase()) {
                            "jan" -> 1
                            "feb" -> 2
                            "mar" -> 3
                            "apr" -> 4
                            "may" -> 5
                            "jun" -> 6
                            "jul" -> 7
                            "aug" -> 8
                            "sep" -> 9
                            "oct" -> 10
                            "nov" -> 11
                            "dec" -> 12
                            else -> return@find false
                        }

                        // Check if this matches our current day
                        month == monthNum && dayNum == dayStr
                    }

                    // Determine weather type and colors
                    val (weatherColor, borderColor) = dayWeather?.let { weather ->
                        // Determine if this day has bad weather
                        val badWeatherKeywords = listOf(
                            "rain", "storm", "thunder", "snow", "hail", "sleet", "hurricane",
                            "tornado", "blizzard", "foggy", "severe"
                        )

                        val hasBadWeather = badWeatherKeywords.any {
                            weather.description.contains(it, ignoreCase = true)
                        } || weather.tempMax > 95 || weather.tempMin < 40

                        // Define the background color based on weather
                        val bgColor = when {
                            weather.description.contains("rain", ignoreCase = true) ->
                                Color(0x334286f4) // Light blue for rain
                            weather.description.contains("cloud", ignoreCase = true) ->
                                Color(0x3378909c) // Light gray for clouds
                            weather.description.contains("sun", ignoreCase = true) ||
                                    weather.description.contains("clear", ignoreCase = true) ->
                                Color(0x33ffb300) // Light yellow for sun/clear
                            weather.description.contains("snow", ignoreCase = true) ->
                                Color(0x33e0e0e0) // Very light gray for snow
                            weather.description.contains("storm", ignoreCase = true) ||
                                    weather.description.contains("thunder", ignoreCase = true) ->
                                Color(0x335c6bc0) // Light purple for storms
                            else ->
                                MaterialTheme.colorScheme.surface // Default
                        }

                        // Border color for bad weather
                        val border = if (hasBadWeather && isSelectable) {
                            Color.Red.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.outline
                        }

                        Pair(bgColor, border)
                    } ?: Pair(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline)

                    // Background color when selected takes precedence
                    val finalBgColor = if (dayNum == day) {
                        MaterialTheme.colorScheme.primary
                    } else if (!isSelectable) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        weatherColor
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(4.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(finalBgColor)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = CircleShape
                            )
                            .then(
                                if (isSelectable) {
                                    Modifier.clickable { onDayChange(dayNum) }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Text(
                            text = dayNum.toString(),
                            color = when {
                                dayNum == day -> MaterialTheme.colorScheme.onPrimary
                                !isSelectable -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (dayWeather != null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}