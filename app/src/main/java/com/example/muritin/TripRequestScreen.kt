
package com.example.muritin

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRequestScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var pickup by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
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
    val coroutineScope = rememberCoroutineScope()

    var pickupLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    val initialPosition = LatLng(23.8103, 90.4125)  // Dhaka
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val location: Location? = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    fusedLocationClient.lastLocation.await()
                }
                if (location != null) {
                    pickupLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pickupLatLng!!, 15f))
                    try {
                        val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            geocodingApi.getLatLng("${location.latitude},${location.longitude}", apiKey)
                        }
                        if (response.status == "OK" && response.results.isNotEmpty()) {
                            pickup = response.results[0].geometry.location.toString()
                        }
                    } catch (e: Exception) {
                        Log.e("TripRequestScreen", "Reverse geocoding failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TripRequestScreen", "Location fetch failed: ${e.message}")
            }
        } else {
            Toast.makeText(context, "লোকেশন পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ট্রিপ রিকোয়েস্ট") })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SearchLocation(
                label = "পিকআপ",
                apiKey = apiKey,
                placesClient = placesClient,
                geocodingApi = geocodingApi,
                coroutineScope = coroutineScope,
                context = context,
                onLocationSelected = { addr: String, latlng: LatLng ->
                    pickup = addr
                    pickupLatLng = latlng
                },
                cameraPositionState = cameraPositionState
            )
            SearchLocation(
                label = "গন্তব্য",
                apiKey = apiKey,
                placesClient = placesClient,
                geocodingApi = geocodingApi,
                coroutineScope = coroutineScope,
                context = context,
                onLocationSelected = { addr: String, latlng: LatLng ->
                    destination = addr
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
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isTrafficEnabled = true)
            ) {
                pickupLatLng?.let { latLng ->
                    Marker(state = MarkerState(position = latLng), title = "পিকআপ")
                }
                destinationLatLng?.let { latLng ->
                    Marker(state = MarkerState(position = latLng), title = "গন্তব্য")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (pickup.isBlank() || destination.isBlank() || seats.toIntOrNull() == null || pickupLatLng == null || destinationLatLng == null) {
                        error = "সকল তথ্য পূরণ করুন"
                        scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                    } else {
                        isLoading = true
                        scope.launch {
                            val result = AuthRepository().submitTripRequest(
                                riderId = user.uid,
                                pickup = pickup,
                                destination = destination,
                                seats = seats.toInt(),
                                pickupLatLng = pickupLatLng!!,
                                destinationLatLng = destinationLatLng!!,
                                apiKey = apiKey,
                                directionsApi = directionsApi
                            )
                            isLoading = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "রিকোয়েস্ট জমা দেওয়া হয়েছে", Toast.LENGTH_SHORT).show()
                                navController.navigate("my_requests")
                            } else {
                                error = result.exceptionOrNull()?.message ?: "রিকোয়েস্ট ব্যর্থ"
                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                            }
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