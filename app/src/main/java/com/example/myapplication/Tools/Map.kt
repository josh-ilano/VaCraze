package com.example.myapplication.Tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.BuildConfig
import com.example.myapplication.HelperViews.CalendarViewModel
import com.example.myapplication.Pages.PlaceDetailsDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.launch

enum class SearchMode {
    SPECIFIC_PLACE,
    AREA_EXPLORATION
}

@SuppressLint("PotentialBehaviorOverride", "ConfigurationScreenWidthHeight")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapWithSearchScreen(calendarViewModel: CalendarViewModel = viewModel()) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<AutocompletePrediction>()) }
    var isMapReady by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Track which search mode is active
    var searchMode by remember { mutableStateOf(SearchMode.SPECIFIC_PLACE) }

    // For area exploration mode
    var exploreMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var activeFilters by remember { mutableStateOf(setOf("tourist_attraction", "restaurant", "museum")) }

    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }
    val apiKey = BuildConfig.MAPS_API_KEY

    // Initialize Places API
    val placesClient = remember {
        if (!Places.isInitialized()) {
            Places.initialize(context, apiKey)
        }
        Places.createClient(context)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val defaultLocation = LatLng(41.685, -71.2701)
    var userLocation by remember { mutableStateOf<LatLng>(defaultLocation) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<PlaceItem?>(null) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Configuration for responsive layout
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            userLocation = LatLng(it.latitude, it.longitude)
                            Log.d("Location", "LatLng: $userLocation")
                        }
                    }
            } catch (e: SecurityException) {
                Log.e("Location", "Permission denied: ${e.message}")
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { googleMap ->
            googleMapRef = googleMap
            googleMap.uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = true
            }

            googleMap.setOnMarkerClickListener { marker ->
                val placeId = marker.tag as? String
                placeId?.let {
                    isLoading = true
                    val placeFields = listOf(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.ADDRESS,
                        Place.Field.RATING,
                        Place.Field.USER_RATINGS_TOTAL,
                        Place.Field.PRICE_LEVEL,
                        Place.Field.OPENING_HOURS,
                        Place.Field.PHOTO_METADATAS,
                        Place.Field.TYPES,
                        Place.Field.PHONE_NUMBER,
                        Place.Field.WEBSITE_URI
                    )

                    val request = FetchPlaceRequest.builder(placeId, placeFields).build()

                    placesClient.fetchPlace(request)
                        .addOnSuccessListener { response ->
                            val place = response.place
                            selectedPlace = mapPlaceToPlaceItem(place)
                            showDialog = true
                            isLoading = false
                        }
                        .addOnFailureListener { e ->
                            Log.e("Places", "Error fetching place details: ${e.message}")
                            isLoading = false
                        }
                }
                true
            }

            isMapReady = true
        }
    }

    LaunchedEffect(userLocation, isMapReady) {
        if (isMapReady) {
            googleMapRef?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12f))
        }
    }

    // Main layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

            // Search header with mode toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search type selector
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = searchMode == SearchMode.SPECIFIC_PLACE,
                            onClick = { searchMode = SearchMode.SPECIFIC_PLACE },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Specific Place"
                                )
                            },
                            label = { Text("Specific Place") },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        )

                        SegmentedButton(
                            selected = searchMode == SearchMode.AREA_EXPLORATION,
                            onClick = { searchMode = SearchMode.AREA_EXPLORATION },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Explore Area"
                                )
                            },
                            label = { Text("Explore Area") },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input
                    TextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            searchJob?.cancel()

                            if (query.isNotEmpty()) {
                                searchJob = coroutineScope.launch {
                                    kotlinx.coroutines.delay(300)

                                    when (searchMode) {
                                        SearchMode.SPECIFIC_PLACE -> {
                                            // Original place search functionality
                                            val request =
                                                FindAutocompletePredictionsRequest.builder()
                                                    .setQuery(query)
                                                    .build()

                                            placesClient.findAutocompletePredictions(request)
                                                .addOnSuccessListener { response ->
                                                    suggestions = response.autocompletePredictions
                                                }
                                                .addOnFailureListener { exception ->
                                                    Log.e(
                                                        "Places",
                                                        "Error finding predictions: ${exception.message}"
                                                    )
                                                    suggestions = emptyList()
                                                }
                                        }

                                        SearchMode.AREA_EXPLORATION -> {
                                            // For area search, we'll use autocomplete but handle it differently
                                            val request =
                                                FindAutocompletePredictionsRequest.builder()
                                                    .setQuery(query)
                                                    .setTypesFilter(
                                                        listOf(
                                                            PlaceTypes.LOCALITY,
                                                            PlaceTypes.ADMINISTRATIVE_AREA_LEVEL_1
                                                        )
                                                    )
                                                    .build()

                                            placesClient.findAutocompletePredictions(request)
                                                .addOnSuccessListener { response ->
                                                    suggestions = response.autocompletePredictions
                                                }
                                                .addOnFailureListener { exception ->
                                                    println("Places Error finding area predictions: ${exception.message}")
                                                    suggestions = emptyList()
                                                }
                                        }
                                    }
                                }
                            } else {
                                suggestions = emptyList()
                            }
                        },
                        label = {
                            Text(
                                when (searchMode) {
                                    SearchMode.SPECIFIC_PLACE -> "Search for a specific place..."
                                    SearchMode.AREA_EXPLORATION -> "Enter a city or region to explore..."
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Filter chips for area exploration mode
                    if (searchMode == SearchMode.AREA_EXPLORATION) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Filter Places:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    label = "Attractions",
                                    isSelected = activeFilters.contains("tourist_attraction"),
                                    onToggle = {
                                        activeFilters =
                                            if (activeFilters.contains("tourist_attraction")) {
                                                activeFilters - "tourist_attraction"
                                            } else {
                                                activeFilters + "tourist_attraction"
                                            }
                                    }
                                )

                                FilterChip(
                                    label = "Restaurants",
                                    isSelected = activeFilters.contains("restaurant"),
                                    onToggle = {
                                        activeFilters = if (activeFilters.contains("restaurant")) {
                                            activeFilters - "restaurant"
                                        } else {
                                            activeFilters + "restaurant"
                                        }
                                    }
                                )

                                FilterChip(
                                    label = "Museums",
                                    isSelected = activeFilters.contains("museum"),
                                    onToggle = {
                                        activeFilters = if (activeFilters.contains("museum")) {
                                            activeFilters - "museum"
                                        } else {
                                            activeFilters + "museum"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    Text("Loading places...")
                }
            }

            // Suggestions list
            if (suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (isLandscape || isTablet) 0.2f else 0.3f)
                ) {
                    items(suggestions) { prediction ->
                        Text(
                            text = prediction.getFullText(null).toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isLoading = true
                                    val placeId = prediction.placeId

                                    when (searchMode) {
                                        SearchMode.SPECIFIC_PLACE -> {
                                            // Original functionality for specific place
                                            val placeFields = listOf(
                                                Place.Field.ID,
                                                Place.Field.NAME,
                                                Place.Field.ADDRESS,
                                                Place.Field.LAT_LNG,
                                                Place.Field.RATING,
                                                Place.Field.USER_RATINGS_TOTAL,
                                                Place.Field.OPENING_HOURS,
                                                Place.Field.PHOTO_METADATAS
                                            )
                                            val request =
                                                FetchPlaceRequest.builder(placeId, placeFields)
                                                    .build()

                                            placesClient.fetchPlace(request)
                                                .addOnSuccessListener { response ->
                                                    val place = response.place
                                                    val latLng = place.latLng

                                                    if (latLng != null) {
                                                        googleMapRef?.let { googleMap ->
                                                            googleMap.clear()
                                                            val marker = googleMap.addMarker(
                                                                MarkerOptions()
                                                                    .position(latLng)
                                                                    .title(place.name)
                                                            )
                                                            marker?.tag = place.id

                                                            googleMap.animateCamera(
                                                                CameraUpdateFactory.newLatLngZoom(
                                                                    latLng,
                                                                    15f
                                                                )
                                                            )
                                                        }
                                                    }

                                                    isLoading = false
                                                    searchQuery = ""
                                                    suggestions = emptyList()
                                                }
                                                .addOnFailureListener { exception ->
                                                    Log.e(
                                                        "Places",
                                                        "Error fetching place: ${exception.message}"
                                                    )
                                                    isLoading = false
                                                }
                                        }

                                        SearchMode.AREA_EXPLORATION -> {
                                            // New functionality for area exploration
                                            val placeFields = listOf(
                                                Place.Field.ID,
                                                Place.Field.NAME,
                                                Place.Field.LAT_LNG,
                                                Place.Field.VIEWPORT
                                            )
                                            val request =
                                                FetchPlaceRequest.builder(placeId, placeFields)
                                                    .build()

                                            placesClient.fetchPlace(request)
                                                .addOnSuccessListener { response ->
                                                    val place = response.place
                                                    val latLng = place.latLng
                                                    val viewport = place.viewport

                                                    if (latLng != null) {
                                                        googleMapRef?.let { googleMap ->
                                                            // Clear existing markers
                                                            googleMap.clear()

                                                            // Move camera to area
                                                            if (viewport != null) {
                                                                googleMap.animateCamera(
                                                                    CameraUpdateFactory.newLatLngBounds(
                                                                        viewport,
                                                                        50
                                                                    )
                                                                )


                                                                // Search for places in the area based on active filters
                                                                coroutineScope.launch {
                                                                    isLoading = true

                                                                    findPlacesInArea(
                                                                        placesClient = placesClient,
                                                                        center = latLng,
                                                                        radius = 5000,
                                                                        placeTypes = activeFilters,
                                                                        apiKey = apiKey,
                                                                        onSuccess = { nearbyPlaces ->
                                                                            val markers =
                                                                                mutableListOf<Marker>()
                                                                            googleMapRef?.let { map ->
                                                                                nearbyPlaces.forEach { placeItem ->
                                                                                    placeItem.latLang?.let { latLng ->
                                                                                        val marker =
                                                                                            map.addMarker(
                                                                                                MarkerOptions()
                                                                                                    .position(
                                                                                                        latLng
                                                                                                    )
                                                                                                    .title(
                                                                                                        placeItem.name
                                                                                                    )
                                                                                            )
                                                                                        marker?.tag =
                                                                                            placeItem.id
                                                                                        marker?.let {
                                                                                            markers.add(
                                                                                                it
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                }
                                                                                exploreMarkers =
                                                                                    markers
                                                                            }
                                                                            isLoading = false
                                                                        },
                                                                        onFailure = { error ->
                                                                            Log.e(
                                                                                "Places",
                                                                                "Explore Area Error: $error"
                                                                            )
                                                                            isLoading = false
                                                                        }
                                                                    )

                                                                }
                                                            } else {
                                                                // If viewport not available, we can just center on the location
                                                                googleMap.animateCamera(
                                                                    CameraUpdateFactory.newLatLngZoom(
                                                                        latLng,
                                                                        12f
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }

                                                    isLoading = false
                                                    searchQuery = ""
                                                    suggestions = emptyList()
                                                }
                                                .addOnFailureListener { exception ->
                                                    Log.e(
                                                        "Places",
                                                        "Error fetching area: ${exception.message}"
                                                    )
                                                    isLoading = false
                                                }
                                        }
                                    }
                                }
                                .padding(12.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Info about current location in small text
            Text(
                "Your location: ${userLocation.latitude.toInt()}.${(userLocation.latitude % 1 * 100).toInt()}, " +
                        "${userLocation.longitude.toInt()}.${(userLocation.longitude % 1 * 100).toInt()}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Map view
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (suggestions.isEmpty()) 1f else if (isLandscape || isTablet) 0.8f else 0.7f)
            )
        }
    }

    // Place details dialog
    if (showDialog && selectedPlace != null) {
        PlaceDetailsDialog(
            place = selectedPlace!!,
            placesClient = placesClient,
            onDismiss = {
                showDialog = false
                selectedPlace = null
            },
            calendarViewModel = calendarViewModel
        )
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onToggle() }
            .padding(4.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.width(4.dp))

            Switch(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}


suspend fun findPlacesInArea(
    placesClient: PlacesClient,
    center: LatLng,
    radius: Int = 5000,
    placeTypes: Set<String>,
    apiKey: String,
    onSuccess: (List<PlaceItem>) -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val allPlaces = mutableListOf<PlaceItem>()

        for (type in placeTypes) {
            val location = "${center.latitude},${center.longitude}"

            val response = RetrofitClient.apiService.getNearbyPlaces(
                location = location,
                radius = radius,
                type = type,
                apiKey = apiKey
            )

            if (response.status == "OK") {
                for (result in response.results) {
                    val placeId = result.placeId

                    val detailedPlace = fetchPlaceDetailsSuspend(placesClient, placeId)
                    detailedPlace?.let {
                        allPlaces.add(it)
                    }
                }
            } else {
                println("NearbySearch Failed for type $type with status ${response.status}")
            }
        }

        onSuccess(allPlaces)
    } catch (e: Exception) {
        onFailure("Error during Nearby Search: ${e.message}")
    }
}



 //Maps a Google Place to your PlaceItem model
private fun mapPlaceToPlaceItem(place: Place): PlaceItem {
    return PlaceItem(
        id = place.id ?: "",
        name = place.name ?: "",
        address = place.formattedAddress ?: "",
        rating = place.rating,
        userRatingsTotal = place.userRatingCount,
        priceLevel = place.priceLevel,
        openingHours = place.openingHours?.weekdayText,
        photoMetadatas = place.photoMetadatas,
        types = place.types?.map { it.name },
        phoneNumber = place.phoneNumber,
        website = place.websiteUri?.toString(),
        latLang = place.latLng
    )
}



@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = androidx.core.view.ViewCompat.generateViewId()
            onCreate(null)
        }
    }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                mapView.onCreate(Bundle())
            }
            override fun onStart(owner: LifecycleOwner) {
                mapView.onStart()
            }
            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }
            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }
            override fun onStop(owner: LifecycleOwner) {
                mapView.onStop()
            }
            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    return mapView
}

