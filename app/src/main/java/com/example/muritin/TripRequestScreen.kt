
package com.example.muritin

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.collections.isNotEmpty
import androidx.compose.foundation.clickable

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import com.google.android.gms.maps.model.BitmapDescriptorFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRequestScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient: FusedLocationProviderClient =
        remember { LocationServices.getFusedLocationProviderClient(context) }

    // ---------- UI state ----------
    var pickupAddr by remember { mutableStateOf("") }
    var destinationAddr by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("1") }
    var fare by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val apiKey = context.getString(R.string.map_api_key)
    val placesClient = remember { com.google.android.libraries.places.api.Places.createClient(context) }

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val geocodingApi = remember { retrofit.create(GeocodingApi::class.java) }
    val directionsApi = remember { retrofit.create(DirectionsApi::class.java) }

    var pickupLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }

    val initialPosition = LatLng(23.8103, 90.4125) // Dhaka
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }

    var screenForPickup by remember { mutableStateOf(true) }
    var screenForDest by remember { mutableStateOf(false) }

    // ---------- Nearby stops ----------
    var nearbyPickupStops by remember { mutableStateOf<List<PointLocation>>(emptyList()) }
    var selectedPickupStop by remember { mutableStateOf<PointLocation?>(null) }

    var nearbyDestStops by remember { mutableStateOf<List<PointLocation>>(emptyList()) }
    var selectedDestStop by remember { mutableStateOf<PointLocation?>(null) }

    var isFetchingStops by remember { mutableStateOf(false) }

    // ---------- Get current location ----------
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val location: Location? = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    fusedLocationClient.lastLocation.await()
                }
                if (location != null) {
                    pickupLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(pickupLatLng!!, 15f)
                    )
                    // reverse-geocode (optional)
                    try {
                        val resp = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            geocodingApi.getAddressFromLatLng(
                                "${location.latitude},${location.longitude}",
                                apiKey
                            )
                        }
                        if (resp.status == "OK" && resp.results.isNotEmpty()) {
                            pickupAddr = resp.results[0].formattedAddress
                        }
                    } catch (e: Exception) {
                        Log.e("TripRequestScreen", "Reverse geo failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TripRequestScreen", "Location fetch failed: ${e.message}")
            }
        } else {
            Toast.makeText(context, "লোকেশন পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- UI ----------
    Scaffold(
        topBar = { TopAppBar(title = { Text("ট্রিপ রিকোয়েস্ট") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==================== PICKUP ====================
            if (screenForPickup) {
                SearchLocation(
                    label = "পিকআপ",
                    apiKey = apiKey,
                    placesClient = placesClient,
                    geocodingApi = geocodingApi,
                    coroutineScope = scope,
                    context = context,
                    onLocationSelected = { addr, latlng ->
                        pickupAddr = addr
                        pickupLatLng = latlng
                    },
                    cameraPositionState = cameraPositionState
                )

                // map click → approximate pickup
                val onMapClick: (LatLng) -> Unit = { latLng ->
                    pickupLatLng = latLng
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                    scope.launch {
                        try {
                            val geocoder = Geocoder(context)
                            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                            pickupAddr = if (addresses?.isNotEmpty() == true) {
                                addresses[0].getAddressLine(0) ?: latLng.toString()
                            } else {
                                latLng.toString()
                            }
                        } catch (e: Exception) {
                            pickupAddr = "লোকেশন পাওয়া যায়নি"
                        }
                    }
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isTrafficEnabled = true),
                    uiSettings = MapUiSettings(zoomGesturesEnabled = true, zoomControlsEnabled = false),
                    onMapClick = onMapClick
                ) {
                    pickupLatLng?.let {
                        Marker(state = MarkerState(position = it), title = "আনুমানিক পিকআপ", snippet = pickupAddr)
                    }

                    // nearby stops markers
                    nearbyPickupStops.forEach { stop ->
                        val pos = LatLng(stop.latitude, stop.longitude)
                        Marker(
                            state = MarkerState(position = pos),
                            title = stop.address,
                            icon = if (selectedPickupStop == stop)
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            else
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // fetch nearby stops
                if (pickupLatLng != null) {
                    Button(
                        onClick = {
                            isFetchingStops = true
                            scope.launch {
                                nearbyPickupStops = AuthRepository().getNearbyBusStops(pickupLatLng!!, 2.5)
                                isFetchingStops = false
                                if (nearbyPickupStops.isEmpty()) {
                                    error = "2.5 কিমি এর মধ্যে কোনো স্টপ পাওয়া যায়নি"
                                    snackbarHostState.showSnackbar(error ?: "")
                                }
                            }
                        },
                        enabled = !isFetchingStops
                    ) {
                        if (isFetchingStops) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text("কাছাকাছি পিকআপ লোকেশন খুঁজুন")
                    }
                }

                // list of nearby stops
                if (nearbyPickupStops.isNotEmpty()) {
                    Text("কাছাকাছি পিকআপ লোকেশনসমূহ (2.5 কিমি):")
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(nearbyPickupStops) { stop ->
                            ListItem(
                                headlineContent = { Text(stop.address) },
                                modifier = Modifier.clickable {
                                    selectedPickupStop = stop
                                    pickupAddr = stop.address
                                    pickupLatLng = LatLng(stop.latitude, stop.longitude)
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(pickupLatLng!!, 15f)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (selectedPickupStop == null) {
                            error = "একটি পিকআপ স্টপ নির্বাচন করুন"
                            scope.launch { snackbarHostState.showSnackbar(error ?: "") }
                        } else {
                            screenForPickup = false
                            screenForDest = true
                        }
                    }
                ) {
                    Text("গন্তব্য নির্বাচন করুন")
                }
            }

            // ==================== DESTINATION ====================
            if (screenForDest) {
                SearchLocation(
                    label = "গন্তব্য",
                    apiKey = apiKey,
                    placesClient = placesClient,
                    geocodingApi = geocodingApi,
                    coroutineScope = scope,
                    context = context,
                    onLocationSelected = { addr, latlng ->
                        destinationAddr = addr
                        destinationLatLng = latlng
                    },
                    cameraPositionState = cameraPositionState
                )

                OutlinedTextField(
                    value = seats,
                    onValueChange = { seats = it },
                    label = { Text("সিট সংখ্যা") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("আনুমানিক ভাড়া: $fare টাকা")

                // map click → approximate destination
                val onMapClick: (LatLng) -> Unit = { latLng ->
                    destinationLatLng = latLng
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                    scope.launch {
                        try {
                            val geocoder = Geocoder(context)
                            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                            destinationAddr = if (addresses?.isNotEmpty() == true) {
                                addresses[0].getAddressLine(0) ?: latLng.toString()
                            } else {
                                latLng.toString()
                            }
                        } catch (e: Exception) {
                            destinationAddr = "লোকেশন পাওয়া যায়নি"
                        }
                    }
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isTrafficEnabled = true),
                    uiSettings = MapUiSettings(zoomGesturesEnabled = true, zoomControlsEnabled = false),
                    onMapClick = onMapClick
                ) {
                    pickupLatLng?.let {
                        Marker(state = MarkerState(position = it), title = "পিকআপ", snippet = pickupAddr)
                    }
                    destinationLatLng?.let {
                        Marker(state = MarkerState(position = it), title = "আনুমানিক গন্তব্য", snippet = destinationAddr)
                    }

                    // nearby dest stops markers
                    nearbyDestStops.forEach { stop ->
                        val pos = LatLng(stop.latitude, stop.longitude)
                        Marker(
                            state = MarkerState(position = pos),
                            title = stop.address,
                            icon = if (selectedDestStop == stop)
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            else
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // fetch nearby destination stops
                if (destinationLatLng != null) {
                    Button(
                        onClick = {
                            isFetchingStops = true
                            scope.launch {
                                nearbyDestStops = AuthRepository().getNearbyBusStops(destinationLatLng!!, 2.5)
                                isFetchingStops = false
                                if (nearbyDestStops.isEmpty()) {
                                    error = "2.5 কিমি এর মধ্যে কোনো স্টপ পাওয়া যায়নি"
                                    snackbarHostState.showSnackbar(error ?: "")
                                }
                            }
                        },
                        enabled = !isFetchingStops
                    ) {
                        if (isFetchingStops) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text("কাছাকাছি গন্তব্য লোকেশন খুঁজুন")
                    }
                }

                // list of nearby destination stops
                if (nearbyDestStops.isNotEmpty()) {
                    Text("কাছাকাছি গন্তব্য লোকেশনসমূহ (2.5 কিমি):")
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(nearbyDestStops) { stop ->
                            ListItem(
                                headlineContent = { Text(stop.address) },
                                modifier = Modifier.clickable {
                                    selectedDestStop = stop
                                    destinationAddr = stop.address
                                    destinationLatLng = LatLng(stop.latitude, stop.longitude)
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(destinationLatLng!!, 15f)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reset button
                Button(
                    onClick = {
                        pickupAddr = ""; pickupLatLng = null
                        destinationAddr = ""; destinationLatLng = null
                        nearbyPickupStops = emptyList(); nearbyDestStops = emptyList()
                        selectedPickupStop = null; selectedDestStop = null
                        screenForDest = false; screenForPickup = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C),
                        contentColor = Color.White
                    )
                ) { Text("রুট বাতিল করুন") }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit request
                Button(
                    onClick = {
                        if (selectedPickupStop == null || selectedDestStop == null || seats.toIntOrNull() == null) {
                            error = "সকল তথ্য পূরণ করুন"
                            scope.launch { snackbarHostState.showSnackbar(error ?: "") }
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val result = AuthRepository().submitTripRequest(
                                riderId = user.uid,
                                pickup = selectedPickupStop!!.address,
                                destination = selectedDestStop!!.address,
                                seats = seats.toInt(),
                                pickupLatLng = LatLng(selectedPickupStop!!.latitude, selectedPickupStop!!.longitude),
                                destinationLatLng = LatLng(selectedDestStop!!.latitude, selectedDestStop!!.longitude),
                                apiKey = apiKey,
                                directionsApi = directionsApi
                            )
                            isLoading = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "রিকোয়েস্ট জমা দেওয়া হয়েছে", Toast.LENGTH_SHORT).show()
                                navController.navigate("my_requests")
                            } else {
                                error = result.exceptionOrNull()?.message ?: "রিকোয়েস্ট ব্যর্থ"
                                snackbarHostState.showSnackbar(error ?: "")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("রিকোয়েস্ট জমা দিন")
                }
            }
        }
    }
}