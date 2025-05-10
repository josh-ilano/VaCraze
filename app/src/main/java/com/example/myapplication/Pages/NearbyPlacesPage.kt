package com.example.myapplication.Pages


//https://syer10.github.io/accompanist/swiperefresh/
//https://developer.android.com/develop/ui/compose/components/card
//https://developers.google.com/maps/documentation/places/web-service/details + chatGPT
//https://developers.google.com/maps/documentation/places/android-sdk/place-details
//https://developers.google.com/maps/documentation/places/android-sdk/reference/com/google/android/libraries/places/api/model/CircularBounds
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.BuildConfig
import com.example.myapplication.CalendarHelper.DateTimePickerDialog
import com.example.myapplication.HelperViews.CalendarViewModel
import com.example.myapplication.HelperViews.SettingsViewModel
import com.example.myapplication.Tools.PlaceItem
import com.example.myapplication.Tools.RetrofitClient
import com.example.myapplication.Tools.WeatherAwareDateTimePickerDialog
import com.example.myapplication.Tools.WeatherDayCard
import com.example.myapplication.Tools.WeatherViewModel
import com.example.myapplication.Tools.fetchPlaceDetailsSuspend
import com.example.myapplication.Tools.fetchPlacePhotos
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NearbyPlacesPage(navController: NavController) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Initialize Places API before creating a client
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
    }

    // Only create the client after Places is initialized
    val placesClient = remember {
        if (Places.isInitialized()) {
            Places.createClient(context)
        } else {
            null
        }
    }

    var locationPermissionGranted by remember { mutableStateOf(false) }
    var places by remember { mutableStateOf<List<PlaceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedPlace by remember { mutableStateOf<PlaceItem?>(null) }
    var selectedDistance by remember { mutableIntStateOf(10) } // default 10 miles
    var selectedType by remember { mutableStateOf("All") }
    var selectedStar by remember { mutableIntStateOf(0) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Check if location permissions are granted
    val checkLocationPermissions = {
        locationPermissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
    }

    checkLocationPermissions()

    // Request location permission
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            locationPermissionGranted = isGranted
        }

    // Refresh Function to Fetch New Places
    val coroutineScope = rememberCoroutineScope()

    val refreshNearbyPlaces: () -> Unit = {
        coroutineScope.launch {
            isLoading = true
            places = emptyList()
            errorMessage = null

            // Check if Places is initialized and placesClient is not null
            if (!Places.isInitialized()) {
                Places.initialize(context, BuildConfig.MAPS_API_KEY)
            }

            val client = placesClient ?: Places.createClient(context)

            refreshNearbyPlaces(
                context = context,
                fusedLocationClient = fusedLocationClient,
                placesClient = client,
                selectedDistance = selectedDistance,
                selectedType = selectedType,
                selectedStar = selectedStar,
                onSuccess = { newPlaces ->
                    places = newPlaces
                },
                onError = { error ->
                    errorMessage = error
                }
            )

            isLoading = false
        }
    }

    // Trigger refresh when the page is loaded or when the filters change
    LaunchedEffect(locationPermissionGranted, selectedDistance, selectedType, selectedStar) {
        if (locationPermissionGranted && Places.isInitialized()) {
            refreshNearbyPlaces() // Trigger the refresh
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Nearby Places") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->
        //val padding = 10.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            if (!locationPermissionGranted) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Location permission required to view nearby places")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) {
                        Text("Grant Location Permission")
                    }
                }
            } else if (!Places.isInitialized()) {
                // Show message if Places API is not initialized
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Initializing Places API...")
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .padding(padding)
                ) {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        errorMessage != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = errorMessage ?: "Unknown error",
                                    color = Color.Red,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        else -> {
                            val swipeRefreshState =
                                remember { SwipeRefreshState(isRefreshing = isLoading) }

                            SwipeRefresh(
                                state = swipeRefreshState,
                                onRefresh = {
                                    refreshNearbyPlaces() // Refresh data when swipe-to-refresh is triggered
                                }
                            ) {
                                if (isLandscape) {
                                    // Landscape: show 2 columns
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(places) { place ->
                                            PlaceCard(
                                                place = place,
                                                onClick = { selectedPlace = place })
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(places) { place ->
                                            PlaceCard(
                                                place = place,
                                                onClick = { selectedPlace = place })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Show place details dialog
                placesClient?.let { client ->
                    selectedPlace?.let { place ->
                        PlaceDetailsDialog(
                            place = place,
                            placesClient = client,
                            onDismiss = { selectedPlace = null })
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            currentDistance = selectedDistance,
            currentType = selectedType,
            currentStar = selectedStar,
            onDismiss = { showFilterDialog = false },
            onApply = { distance, type, star ->
                selectedDistance = distance
                selectedType = type
                selectedStar = star
                refreshNearbyPlaces() // Trigger refresh when filter is applied
            }
        )
    }
}


@Composable
fun PlaceCard(place: PlaceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                place.photoBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = place.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } ?: Text(
                    text = "No Image Available",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = place.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = place.rating?.toString() ?: "No rating", fontSize = 14.sp)
                }
            }
        }
    }
}


@Composable
fun PlaceDetailsDialog(
    place: PlaceItem,
    placesClient: PlacesClient,
    onDismiss: () -> Unit,
    calendarViewModel: CalendarViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    weatherViewModel: WeatherViewModel = viewModel()  // Add WeatherViewModel
) {
    var images by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDebugOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Collect settings from SettingsViewModel
    val use24HourFormat by settingsViewModel.use24HourFormat.collectAsState()

    // Collect weather forecast from WeatherViewModel
    val forecast by weatherViewModel.forecast.observeAsState(emptyList())

    // API key - In a production app, this should be stored securely
    val weatherApiKey = BuildConfig.MAPS_API_KEY

    // Collect eventAddResult from CalendarViewModel
    val eventAddResult by calendarViewModel.eventAddResult.collectAsState()

    // Monitor eventAddResult changes
    LaunchedEffect(eventAddResult) {
        when (eventAddResult) {
            is CalendarViewModel.EventAddResult.Success -> {
                showConfirmation = true
                errorMessage = null
                // Reset after 3 seconds
                delay(3000)
                showConfirmation = false
                calendarViewModel.resetEventAddResult()
            }
            is CalendarViewModel.EventAddResult.Error -> {
                errorMessage = (eventAddResult as CalendarViewModel.EventAddResult.Error).message
                showConfirmation = false
                // Reset after 3 seconds
                delay(3000)
                errorMessage = null
                calendarViewModel.resetEventAddResult()
            }
            else -> {
                // Do nothing for None state
            }
        }
    }

    // Fetch weather forecast when dialog opens if we have location data
    LaunchedEffect(place.id) {
        // Load images
        val bitmaps = place.photoMetadatas?.let {
            fetchPlacePhotos(placesClient, it)
        } ?: emptyList()
        images = bitmaps

        // Fetch the place details with the Places SDK to get latitude and longitude
        try {
            // Create a request to fetch place details with the Place ID
            val placeRequest = FetchPlaceRequest.newInstance(place.id, listOf(Place.Field.LAT_LNG))

            // Execute the request
            val task = placesClient.fetchPlace(placeRequest)
            task.addOnSuccessListener { response ->
                val fetchedPlace = response.place
                fetchedPlace.latLng?.let { latLng ->
                    // Now we have the latLng, load the weather forecast
                    weatherViewModel.loadForecast(
                        lat = latLng.latitude,
                        lon = latLng.longitude,
                        apiKey = BuildConfig.MAPS_API_KEY
                    )
                }
            }.addOnFailureListener { exception ->
                Log.e("PlaceDetailsDialog", "Error fetching place details: ${exception.message}")
            }
        } catch (e: Exception) {
            Log.e("PlaceDetailsDialog", "Exception fetching place details: ${e.message}")
        }
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text(place.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (images.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(images) { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .fillMaxHeight()
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    Text("No images available", fontSize = 14.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = place.rating?.toString() ?: "No rating")
                }

                Spacer(modifier = Modifier.height(8.dp))

                place.openingHours?.let {
                    Text("Opening Hours:", fontWeight = FontWeight.Bold)
                    it.forEach { day ->
                        Text("• $day", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                place.reviews?.takeIf { it.isNotEmpty() }?.let { reviews ->
                    Text("Reviews:", fontWeight = FontWeight.Bold)
                    reviews.forEach { review ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(review.text, fontSize = 14.sp)
                        Text("- ${review.author}, ${review.time}", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } ?: Text("No reviews available.")

                // Display weather forecast if available
                if (forecast.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Weather Forecast:", fontWeight = FontWeight.Bold)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(forecast) { day ->
                            WeatherDayCard(day)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Add to calendar section - simplified
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Add to calendar button - directly opens date picker
                    Button(
                        onClick = { showDateTimePicker = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Calendar",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Add to Calendar")
                    }

                    // Display current time format
                    Text(
                        text = if (use24HourFormat) "Using 24-hour format" else "Using 12-hour format",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Status messages
                    when {
                        showConfirmation -> {
                            Text(
                                "Event added to calendar!",
                                color = Color.Green,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        errorMessage != null -> {
                            Text(
                                errorMessage ?: "",
                                color = Color.Red,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Debug button to reset loading state if needed
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = { showDebugOptions = !showDebugOptions }
                        ) {
                            Text(if (showDebugOptions) "Hide Debug Options" else "Debug Options")
                        }
                    }

                    if (showDebugOptions) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Reset loading state button
                            OutlinedButton(
                                onClick = {
                                    calendarViewModel.clearLoadingState()
                                    Toast.makeText(context, "Loading state reset", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text("Reset Loading State")
                            }

                            // Toggle time format button
                            OutlinedButton(
                                onClick = {
                                    settingsViewModel.toggleTimeFormat()
                                    Toast.makeText(
                                        context,
                                        "Time format changed to ${if (use24HourFormat) "12-hour" else "24-hour"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text("Toggle Time Format")
                            }
                        }
                    }
                }
            }
        }
    )

    if (showDateTimePicker) {
        WeatherAwareDateTimePickerDialog(
            initialDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            initialStartTime = LocalTime(12, 0),
            initialEndTime = LocalTime(13, 0),
            onDateTimeSelected = { date, startTime, endTime ->
                // Call the ViewModel method to add the event
                calendarViewModel.addEventToCalendar(
                    place = place,
                    date = date,
                    startTime = startTime,
                    endTime = endTime
                )

                // Close the date time picker
                showDateTimePicker = false
            },
            onDismiss = { showDateTimePicker = false },
            use24HourFormat = use24HourFormat,

            weatherForecast = forecast
        )
    }
}


@Composable
fun FilterDialog(
    currentDistance: Int,
    currentType: String,
    currentStar: Int,
    onDismiss: () -> Unit,
    onApply: (Int, String, Int) -> Unit
) {
    val distanceOptions = (5..70 step 10).toList()
    val typeOptions = listOf("All", "Restaurant", "Park", "Museum", "Tourist Attraction", "Cafe", "Art Gallery", "Shopping Mall")
    val starOption = (1..5).toList()

    var selectedDistance by remember { mutableStateOf(currentDistance) }
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedStar by remember { mutableStateOf(currentStar) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Places") },
        text = {
            Column {
                Text("Distance (miles)")
                DropdownSelector(
                    options = distanceOptions.map { it.toString() },
                    selectedOption = selectedDistance.toString(),
                    onOptionSelected = { selectedDistance = it.toInt() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Place Type")
                DropdownSelector(
                    options = typeOptions,
                    selectedOption = selectedType,
                    onOptionSelected = { selectedType = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Star Rating")
                DropdownSelector(
                    options = starOption.map { it.toString() },
                    selectedOption = selectedStar.toString(),
                    onOptionSelected = { selectedStar = it.toInt() }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(selectedDistance, selectedType, selectedStar)
                onDismiss()
            }) {
                Text("Apply")
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
fun DropdownSelector(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedOption)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },

        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .heightIn(max = 200.dp) // Limit height to enable scrolling
                    .verticalScroll(scrollState)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


suspend fun refreshNearbyPlaces(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    placesClient: PlacesClient,
    selectedDistance: Int,
    selectedType: String,
    selectedStar: Int,
    onSuccess: (List<PlaceItem>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            onError("Location permission not granted.")
            return
        }

        val location = fusedLocationClient.lastLocation.await()

        if (location == null) {
            onError("Couldn't get current location.")
            return
        }

        val userLatLng = "${location.latitude},${location.longitude}"
        val radiusMeters = selectedDistance * 1609

        val normalizedType = if (selectedType != "All")
            selectedType.lowercase().replace(" ", "_")
        else null

        val response = RetrofitClient.apiService.getNearbyPlaces(
            location = userLatLng,
            radius = radiusMeters,
            type = normalizedType,
            apiKey = BuildConfig.MAPS_API_KEY
        )

        if (response.status == "OK") {
            val detailedPlaces = mutableListOf<PlaceItem>()
            for (item in response.results.shuffled().take(20)) {
                val placeDetails = fetchPlaceDetailsSuspend(placesClient, item.placeId)

                // Fetch the first photo for each place card
                placeDetails?.let { place ->
                    // Fetch one photo for the place card if photoMetadatas is not empty
                    place.photoMetadatas?.firstOrNull()?.let { metadata ->
                        val photo = fetchPlacePhotos(placesClient, listOf(metadata)).firstOrNull()
                        place.photoBitmap = photo
                    }
                    detailedPlaces.add(place)
                }
            }

            val filteredPlaces = if (selectedStar > 0) {
                detailedPlaces.filter { (it.rating ?: 0.0) >= selectedStar }
            } else {
                detailedPlaces
            }

            onSuccess(filteredPlaces)
        } else {
            onError("API Error: ${response.status}")
        }
    } catch (e: Exception) {
        onError("Exception: ${e.message}")
    }
}
