package com.example.myapplication.HelperViews


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {
    private val _textSize = MutableStateFlow(16f)  // Default text size
    val textSize: StateFlow<Float> = _textSize

    private val _isDarkMode = MutableStateFlow(false)  // Default dark mode setting
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _useMiles = MutableStateFlow(true)  // Default unit (miles)
    val useMiles: StateFlow<Boolean> = _useMiles

    private val _use24HourFormat = MutableStateFlow(true)  // Default to 24-hour format
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat


    // Toggle dark mode
    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Update text size
    fun updateTextSize(newSize: Float) {
        _textSize.value = newSize
    }

    // Toggle units (miles or kilometers)
    fun toggleUnits() {
        _useMiles.value = !_useMiles.value
    }

    // Toggle 24-hour format
    fun toggleTimeFormat() {
        _use24HourFormat.value = !_use24HourFormat.value
    }

}
