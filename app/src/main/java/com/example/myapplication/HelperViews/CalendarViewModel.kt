package com.example.myapplication.HelperViews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.Pages.Event
import com.example.myapplication.Tools.PlaceItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.atomic.AtomicBoolean


class CalendarViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // State for UI feedback
    private val _eventAddResult = MutableStateFlow<EventAddResult>(EventAddResult.None)
    val eventAddResult: StateFlow<EventAddResult> = _eventAddResult

    // Store events for each date in a Map
    private val _events = MutableStateFlow<Map<LocalDate, List<Event>>>(emptyMap())
    val events: StateFlow<Map<LocalDate, List<Event>>> = _events

    // Flag to prevent multiple simultaneous operations - using atomic boolean for thread safety
    private val isLoading = AtomicBoolean(false)

    init {
        loadEvents()
    }

    // Result of adding an event
    sealed class EventAddResult {
        object None : EventAddResult()
        object Success : EventAddResult()
        data class Error(val message: String) : EventAddResult()
    }

    // Add an event to Firestore with validation
    fun addEvent(date: LocalDate, title: String, startTime: LocalTime, endTime: LocalTime) {
        // Make sure we're not already busy before starting
        if (!isLoading.compareAndSet(false, true)) {
            _eventAddResult.value = EventAddResult.Error("Another operation in progress")
            return
        }

        val user = auth.currentUser
        if (user == null) {
            _eventAddResult.value = EventAddResult.Error("User not authenticated")
            isLoading.set(false) // Reset loading flag
            return
        }

        // Validate that the event is not in the past
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val today = now.date
        val currentTime = now.time

        if (date < today || (date == today && startTime < currentTime)) {
            _eventAddResult.value = EventAddResult.Error("Cannot add event in the past")
            isLoading.set(false) // Reset loading flag
            return
        }

        // Prepare event data
        val eventData = hashMapOf(
            "date" to date.toString(),
            "title" to title,
            "startTime" to startTime.toString(),
            "endTime" to endTime.toString()
        )

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("events")
                        .add(eventData)
                        .await()

                    // Immediately update the local state after successful add
                    val currentEvents = _events.value.toMutableMap()
                    val dayEvents = currentEvents[date]?.toMutableList() ?: mutableListOf()

                    // Create a new event with a temporary ID (will be replaced when loaded from Firebase)
                    val newEvent = Event(
                        id = "temp_${System.currentTimeMillis()}",
                        title = title,
                        startTime = startTime,
                        endTime = endTime
                    )
                    dayEvents.add(newEvent)
                    dayEvents.sortBy { it.startTime }
                    currentEvents[date] = dayEvents

                    withContext(Dispatchers.Main) {
                        _events.value = currentEvents
                        _eventAddResult.value = EventAddResult.Success
                    }

                    // Then load from Firebase to get the real ID
                    loadEvents()
                }
            } catch (e: Exception) {
                println("Error here at add Evetn: $e.message")
            } finally {
                isLoading.set(false)
            }
        }

    }

    // Add an event from a PlaceItem to the calendar
    fun addEventToCalendar(
        place: PlaceItem,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ) {
        // Reset the eventAddResult to None before starting a new operation
        _eventAddResult.value = EventAddResult.None

        // Check if we're already processing another operation
        if (!isLoading.compareAndSet(false, true)) {
            _eventAddResult.value = EventAddResult.Error("Another operation in progress")
            return
        }

        // Create a meaningful title that includes the place name
        val eventTitle = "Visit ${place.name}"

        // Release the lock before calling addEvent which will acquire it again
        isLoading.set(false)

        // Add the event to Firestore
        addEvent(date, eventTitle, startTime, endTime)
    }

    // Reset event add result
    fun resetEventAddResult() {
        _eventAddResult.value = EventAddResult.None
    }

    // Clear loading flag - can be called from UI if operation seems stuck
    fun clearLoadingState() {
        isLoading.set(false)
    }

    // Load events from Firestore
    fun loadEvents() {
        // Don't block other operations if we're just loading events
        // Instead, we'll set the flag but allow other operations to proceed
        if (!isLoading.compareAndSet(false, true)) {
            return
        }

        val user = auth.currentUser
        if (user == null) {
            isLoading.set(false)
            return
        }

        viewModelScope.launch {
            try {
                // Use Dispatchers.IO for Firestore operations
                withContext(Dispatchers.IO) {
                    val snapshot = firestore.collection("users")
                        .document(user.uid)
                        .collection("events")
                        .get()
                        .await()

                    val eventMap = mutableMapOf<LocalDate, MutableList<Event>>()

                    for (document in snapshot.documents) {
                        try {
                            val dateStr = document.getString("date")
                            val title = document.getString("title")
                            val startTimeStr = document.getString("startTime")
                            val endTimeStr = document.getString("endTime")

                            if (dateStr != null && title != null && startTimeStr != null && endTimeStr != null) {
                                val date = LocalDate.parse(dateStr)
                                val startTime = LocalTime.parse(startTimeStr)
                                val endTime = LocalTime.parse(endTimeStr)
                                val event = Event(document.id, title, startTime, endTime)

                                if (eventMap.containsKey(date)) {
                                    eventMap[date]?.add(event)
                                } else {
                                    eventMap[date] = mutableListOf(event)
                                }
                            }
                        } catch (e: Exception) {
                            println("Error parsing event: ${e.message}")
                        }
                    }

                    // Sort events by time for each day
                    eventMap.forEach { (_, events) ->
                        events.sortBy { it.startTime }
                    }

                    // Update the state on the main thread
                    withContext(Dispatchers.Main) {
                        _events.value = eventMap
                    }
                }
            } catch (e: Exception) {
                println("Error loading events: ${e.message}")
            } finally {
                // CRITICAL: Always reset the loading flag
                isLoading.set(false)
            }
        }
    }

    fun updateEvent(
        eventId: String,
        date: LocalDate,
        title: String,
        startTime: LocalTime,
        endTime: LocalTime
    ) {
        // Check if we're already processing another operation
        if (!isLoading.compareAndSet(false, true)) {
            _eventAddResult.value = EventAddResult.Error("Another operation in progress")
            return
        }

        val user = auth.currentUser
        if (user == null) {
            _eventAddResult.value = EventAddResult.Error("User not authenticated")
            isLoading.set(false)
            return
        }

        // Prepare updated event data
        val eventData = hashMapOf(
            "date" to date.toString(),
            "title" to title,
            "startTime" to startTime.toString(),
            "endTime" to endTime.toString()
        )

        viewModelScope.launch {
            try {
                // Use Dispatchers.IO for Firestore operations
                withContext(Dispatchers.IO) {
                    // Update the event in Firestore
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("events")
                        .document(eventId)
                        .update(eventData as Map<String, Any>)
                        .await()

                    // Create the updated event
                    val updatedEvent = Event(
                        id = eventId,
                        title = title,
                        startTime = startTime,
                        endTime = endTime
                    )

                    withContext(Dispatchers.Main) {
                        val currentEvents = _events.value.toMutableMap()

                        // Remove the event from all dates (in case the date changed)
                        currentEvents.forEach { (existingDate, events) ->
                            val mutableEvents = events.toMutableList()
                            mutableEvents.removeAll { it.id == eventId }
                            currentEvents[existingDate] = mutableEvents
                        }

                        // Add the updated event to the new/same date
                        val dayEvents = currentEvents[date]?.toMutableList() ?: mutableListOf()
                        dayEvents.add(updatedEvent)
                        dayEvents.sortBy { it.startTime }
                        currentEvents[date] = dayEvents

                        _events.value = currentEvents
                        _eventAddResult.value = EventAddResult.Success
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _eventAddResult.value = EventAddResult.Error("Error updating event: ${e.message}")
                }
                println("Error updating event: ${e.message}")
            } finally {
                // Always reset the loading flag
                isLoading.set(false)
            }
        }
    }

    // Delete an event
    fun deleteEvent(eventId: String) {
        // Check if we're already processing another operation
        if (!isLoading.compareAndSet(false, true)) {
            _eventAddResult.value = EventAddResult.Error("Another operation in progress")
            return
        }

        val user = auth.currentUser
        if (user == null) {
            _eventAddResult.value = EventAddResult.Error("User not authenticated")
            isLoading.set(false)
            return
        }

        viewModelScope.launch {
            try {
                // Use Dispatchers.IO for Firestore operations
                withContext(Dispatchers.IO) {
                    // Delete the event from Firestore
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("events")
                        .document(eventId)
                        .delete()
                        .await()

                    withContext(Dispatchers.Main) {
                        val currentEvents = _events.value.toMutableMap()

                        // Remove the event from all dates
                        currentEvents.forEach { (date, events) ->
                            val mutableEvents = events.toMutableList()
                            mutableEvents.removeAll { it.id == eventId }
                            currentEvents[date] = mutableEvents
                        }

                        _events.value = currentEvents
                        _eventAddResult.value = EventAddResult.Success
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _eventAddResult.value = EventAddResult.Error("Error deleting event: ${e.message}")
                }
                println("Error deleting event: ${e.message}")
            } finally {
                // Always reset the loading flag
                isLoading.set(false)
            }
        }
    }
}








