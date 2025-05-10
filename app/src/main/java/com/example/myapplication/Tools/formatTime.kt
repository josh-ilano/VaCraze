package com.example.myapplication.Tools

import kotlinx.datetime.LocalTime

// helper function to format LocalTime with 12/24 hour format option
fun formatTime(time: LocalTime, use24HourFormat: Boolean): String {
    return if (use24HourFormat) {
        // 24-hour format (military time)
        "%02d:%02d".format(time.hour, time.minute)
    } else {
        // 12-hour format with AM/PM
        val hour12 = when {
            time.hour == 0 -> 12
            time.hour > 12 -> time.hour - 12
            else -> time.hour
        }
        val period = if (time.hour >= 12) "PM" else "AM"
        "%d:%02d %s".format(hour12, time.minute, period)
    }
}
