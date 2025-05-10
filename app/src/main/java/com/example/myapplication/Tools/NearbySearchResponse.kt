package com.example.myapplication.Tools

import com.google.gson.annotations.SerializedName

// NearbySearchResponse.kt
data class NearbySearchResponse(
    @SerializedName("results") val results: List<NearbyPlace>,
    @SerializedName("status") val status: String
)

data class NearbyPlace(
    @SerializedName("place_id") val placeId: String,
    @SerializedName("name") val name: String,
    @SerializedName("geometry") val geometry: Geometry,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("photos") val photos: List<Photo>?
)

data class Geometry(
    @SerializedName("location") val location: LatLngResponse
)

data class LatLngResponse(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class Photo(
    @SerializedName("photo_reference") val photoReference: String
)
