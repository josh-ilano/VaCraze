package com.example.travelapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.travelapp.ui.theme.TravelAPPTheme
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

class MainActivity : ComponentActivity() {
    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize the Places SDK if it hasn't been initialized already.
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        // Replace with a valid Place ID.
        val placeId = "ChIJFzjlY_B544kRyL6j4ABuNCs"
        val placeFields = listOf(
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.OPENING_HOURS
        )
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        // Mutable state to hold the fetched place details.
        val fetchedPlaceInfo = mutableStateOf("Fetching place info...")

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val name = place.name ?: "No name available"
                val address = place.address ?: "No address available"
                val openingHours = place.openingHours?.weekdayText?.joinToString("\n")
                    ?: "No opening hours available"
                fetchedPlaceInfo.value = "Name: $name\nAddress: $address\nOpening Hours:\n$openingHours"
            }
            .addOnFailureListener { exception ->
                fetchedPlaceInfo.value = "Error fetching place: ${exception.message}"
            }

        setContent {
            TravelAPPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlaceInfoDisplay(
                        info = fetchedPlaceInfo.value,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceInfoDisplay(info: String, modifier: Modifier = Modifier) {
    Text(text = info, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun PlaceInfoPreview() {
    TravelAPPTheme {
        PlaceInfoDisplay(info = "Sample Place Info")
    }
}
