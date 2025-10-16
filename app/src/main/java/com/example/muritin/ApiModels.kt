
package com.example.muritin

import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("maps/api/geocode/json")
    suspend fun getLatLng(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GeocodeResponse
}

interface DirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun getRoute(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String? = null,
        @Query("key") apiKey: String
    ): DirectionsResponse
}

data class GeocodeResponse(
    val results: List<GeocodeResult> = emptyList(),
    val status: String
)

data class GeocodeResult(
    val geometry: Geometry
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class DirectionsResponse(
    val routes: List<Route> = emptyList(),
    val status: String
)

data class Route(
    val overview_polyline: OverviewPolyline,
    val legs: List<Leg>
)

data class OverviewPolyline(
    val points: String
)

data class Leg(
    val distance: Distance,
    val duration: Duration
)

data class Distance(
    val value: Int,
    val text: String
)

data class Duration(
    val value: Int,
    val text: String
)
