package com.example.myapplication.Tools

import android.graphics.Bitmap
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


// place details fetching function that gets all photos
suspend fun fetchPlaceDetailsSuspend(client: PlacesClient, placeId: String): PlaceItem? = suspendCancellableCoroutine { cont ->
    val fields = listOf(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.LAT_LNG,
        Place.Field.RATING,
        Place.Field.REVIEWS,
        Place.Field.PHOTO_METADATAS,
        Place.Field.OPENING_HOURS,
        Place.Field.USER_RATINGS_TOTAL,
        Place.Field.TYPES,
        Place.Field.PRICE_LEVEL,
        Place.Field.PHONE_NUMBER,
        Place.Field.WEBSITE_URI
    )

    val request = FetchPlaceRequest.builder(placeId, fields).build()

    client.fetchPlace(request)
        .addOnSuccessListener { response ->
            val place = response.place
            val placeItem = mapToPlaceItem(place)

            // We'll now let the PlaceDetailsDialog handle fetching photos
            // This keeps the details fetching faster and lets photos load asynchronously
            cont.resume(placeItem)
        }
        .addOnFailureListener {
            cont.resume(null)
        }
}

// Function to fetch photos for a place
suspend fun fetchPlacePhotos(placesClient: PlacesClient, photoMetadatas: List<PhotoMetadata>): List<Bitmap> =
    suspendCancellableCoroutine { continuation ->
        val bitmaps = mutableListOf<Bitmap>()
        var fetchedPhotos = 0
        val totalPhotos = photoMetadatas.size.coerceAtMost(5) // Limit to first 5 photos

        if (totalPhotos == 0) {
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        for (i in 0 until totalPhotos) {
            val photoMetadata = photoMetadatas[i]

            // Create a FetchPhotoRequest with a maximum width or height
            val photoRequest = FetchPhotoRequest.builder(photoMetadata)
                .setMaxWidth(1000) // Higher resolution for better display
                .setMaxHeight(600) // Higher resolution for better display
                .build()

            placesClient.fetchPhoto(photoRequest)
                .addOnSuccessListener { fetchPhotoResponse ->
                    val bitmap = fetchPhotoResponse.bitmap
                    bitmaps.add(bitmap)

                    fetchedPhotos++
                    if (fetchedPhotos == totalPhotos) {
                        continuation.resume(bitmaps)
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failures individually
                    println("Error fetching photo: ${exception.message}")
                    fetchedPhotos++
                    if (fetchedPhotos == totalPhotos) {
                        continuation.resume(bitmaps)
                    }
                }
        }
    }
