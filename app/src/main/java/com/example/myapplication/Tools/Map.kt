package com.example.myapplication.Tools

// used documentation
// Map Configuration - https://developers.google.com/maps/documentation/android-sdk/config#kotlin_1
// Places Data Documentation -  https://developers.google.com/maps/documentation/places/android-sdk/reference/com/google/android/libraries/places/api/model/Place.Field

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.BuildConfig
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapWithSearchScreen() {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(listOf<AutocompletePrediction>()) }
    var isMapReady by remember { mutableStateOf(false) }

    // Use a more persistent reference for the Google Map
    var googleMapRef by remember { mutableStateOf<GoogleMap?>(null) }
    val apiKey = BuildConfig.MAPS_API_KEY

    // Initialize Places API
    val placesClient = remember {
        if (!Places.isInitialized()) {

            Places.initialize(context, apiKey)
        }
        Places.createClient(context)
    }


    //User Permission For Getting their Location, and either using user location or default location of boston
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    //setting the default location to Fall River, MA (where I'm from)
    val defaultLocation = LatLng(41.685, -71.2701)
    var userLocation by remember { mutableStateOf<LatLng>(defaultLocation) }

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            userLocation = LatLng(it.latitude, it.longitude)
                            Log.d("Location", "LatLng: $userLocation");
                        }
                    }
            } catch (e: SecurityException) {
                Log.e("Location", "Permission denied: ${e.message}")
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }


    // Set up the map when it's available
    LaunchedEffect(mapView) {
        mapView.getMapAsync { googleMap ->
            googleMapRef = googleMap
            googleMap.uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = true
            }

            // Enable map click to add custom pins
            googleMap.setOnMapClickListener { latLng ->
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(latLng).title("Custom Pin"))
            }

            isMapReady = true
        }
    }

    //when user location changes or permission changes we want to change map camera location
    LaunchedEffect(userLocation, isMapReady) {
        if (isMapReady) {
            googleMapRef?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12f))
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        // Search TextField with debounce
        var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
        val coroutineScope = rememberCoroutineScope()

        TextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query

                // Cancel previous job if it exists
                searchJob?.cancel()

                if (query.isNotEmpty()) {
                    // Create a new search job with debounce
                    searchJob = coroutineScope.launch {
                        kotlinx.coroutines.delay(300) // Debounce for 300ms

                        val request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(query)
                            .build()

                        placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { response ->
                                suggestions = response.autocompletePredictions
                            }
                            .addOnFailureListener { exception ->
                                android.util.Log.e("Places", "Error finding predictions: ${exception.message}")
                                suggestions = emptyList()
                            }
                    }
                } else {
                    suggestions = emptyList()
                }
            },
            label = { Text("Search places...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        // Display suggestions
        if (suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
            ) {
                items(suggestions) { prediction ->
                    Text(
                        text = prediction.getFullText(null).toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val placeId = prediction.placeId
                                val placeFields = listOf(Place.Field.LOCATION, Place.Field.DISPLAY_NAME, Place.Field.OPENING_HOURS)
                                val request = FetchPlaceRequest.builder(placeId, placeFields).build()



                                placesClient.fetchPlace(request)
                                    .addOnSuccessListener { response ->
                                        val place = response.place
                                        val latLng = place.location
                                        val openhour = place.openingHours?.weekdayText?.joinToString("\n")?: "No hours Avaliable"

                                        val markerInfo = placeData(
                                            placeName = place.displayName,
                                            openTimes = place.openingHours?.weekdayText?.joinToString("\n"),
                                            photoUrl = place.iconMaskUrl
                                        )


                                        googleMapRef?.let { googleMap ->
                                            googleMap.clear()
                                            if (latLng != null) {
                                                googleMap.addMarker(
                                                    MarkerOptions().position(latLng).title(markerInfo.placeName + openhour)

                                                )
                                                googleMap.animateCamera(
                                                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                                                )
                                            }
                                        }

                                        // Clear search UI
                                        searchQuery = ""
                                        suggestions = emptyList()
                                    }
                                    .addOnFailureListener { exception ->
                                        android.util.Log.e("Places", "Error fetching place: ${exception.message}")
                                    }
                            }
                            .padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Your location: ${userLocation.latitude}, ${userLocation.longitude}")
        Spacer(modifier = Modifier.height(12.dp))
        // Map view
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (suggestions.isEmpty()) 1f else 0.7f)
        )
    }
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

    val lifecycle = LocalLifecycleOwner.current.lifecycle
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