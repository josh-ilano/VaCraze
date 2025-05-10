package com.example.myapplication.Tools

import android.graphics.Bitmap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query



//Model class for place data that will be displayed in the PlaceDetailsDialog
data class PlaceItem(
    val id: String,
    val name: String,
    val address: String?,
    val latLang: LatLng?,
    val rating: Double?,
    var photoBitmap: Bitmap? = null,
    val openingHours: List<String>? = null,
    val reviews: List<PlaceReview>? = null,
    val photoMetadatas: List<PhotoMetadata>? = null,
    val userRatingsTotal: Int? = null,
    val priceLevel: Int? = null,
    val types: List<String>? = null,
    val phoneNumber: String? = null,
    val website: String? = null
)

// Model class for place reviews
data class PlaceReview(
    val author: String,
    val rating: Double,
    val text: String,
    val time: String
)

//Helper model for marker information
data class placeData(
    val placeName: String? = null,
    val openTimes: String = "No hours available",
    val photoUrl: String? = null
)


interface PlaceApiService {
    @GET("place/nearbysearch/json")
    suspend fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String?,
        @Query("key") apiKey: String
    ): NearbySearchResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: PlaceApiService = retrofit.create(PlaceApiService::class.java)
}

// mapping function
fun mapToPlaceItem(place: Place): PlaceItem {
    val reviews = place.reviews?.map { review ->
        PlaceReview(
            author = review.authorAttribution.name ?: "Anonymous",  // Use authorName if available
            rating = review.rating?.toDouble() ?: 0.0,
            text = review.text ?: "",
            time = review.relativePublishTimeDescription ?: "Some time ago" // Use relative time if available
        )
    }

    return PlaceItem(
        id = place.id ?: "",
        name = place.name ?: "Unnamed",
        latLang = place.latLng,
        address = place.formattedAddress,
        rating = place.rating,
        photoMetadatas = place.photoMetadatas,
        openingHours = place.openingHours?.weekdayText,
        reviews = reviews,
        photoBitmap = null,
        userRatingsTotal = place.userRatingsTotal,
        priceLevel = place.priceLevel,
        types = place.types?.map { it.name },
        phoneNumber = place.phoneNumber,
        website = place.websiteUri?.toString()
    )
}





