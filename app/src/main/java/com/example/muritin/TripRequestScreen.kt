package com.example.muritin

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
import com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.*
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
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

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
    // Nearby stops with distance
    var nearbyPickupStops by remember { mutableStateOf<List<Pair<BusRoute, PointLocation>>>(emptyList()) }
    var nearbyDestStops by remember { mutableStateOf<List<Pair<BusRoute, PointLocation>>>(emptyList())}
    var selectedPickupStop by remember { mutableStateOf<PointLocation?>(null) }
    var selectedDestStop by remember { mutableStateOf<PointLocation?>(null) }
    var selectedRoute: BusRoute by remember { mutableStateOf(BusRoute()) }
    var isFetchingStops by remember { mutableStateOf(false) }

    //Getting current location
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
                    // Reverse geocode
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
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- UI ----------
    Scaffold(
        topBar = { TopAppBar(title = { Text("ট্রিপ রিকোয়েস্ট") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

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

                    val onMapClick: (LatLng) -> Unit = { latLng ->
                        pickupLatLng = latLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
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
                            Marker(state = MarkerState(position = it), title = "Approx Pickup", snippet = pickupAddr)
                        }
                        nearbyPickupStops.forEach { item ->
                            val pos = LatLng(item.second.latitude, item.second.longitude)
                            Marker(
                                state = MarkerState(position = pos),
                                title = item.second.address,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    if (selectedPickupStop == item.second) HUE_GREEN else HUE_BLUE
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Fetch nearby pickup stops from schedule
                    if (pickupLatLng != null) {
                        Button(
                            onClick = {
                                isFetchingStops = true
                                scope.launch {
                                    nearbyPickupStops = AuthRepository().getNearbyPickup(pickupLatLng!!)
                                    isFetchingStops = false
                                }
                            },
                            enabled = !isFetchingStops
                        ) {
                            if (isFetchingStops) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Text("নিকটবর্তী পিকআপ স্টপ খুঁজুন")
                        }
                    }
                    // List of nearby pickup stops
                    if (nearbyPickupStops.isNotEmpty()) {
                        Text("নিকটবর্তী পিকআপ স্টপ (২.৫ কিলমিটার এর মধ্যে):")
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            items(nearbyPickupStops) { item ->
                                ListItem(
                                    headlineContent = {
                                        Text("${item.second.address}")
                                    },
                                    modifier = Modifier.clickable {
                                        selectedPickupStop = item.second
                                        pickupAddr = item.second.address
                                        pickupLatLng = LatLng(item.second.latitude, item.second.longitude)
                                        selectedRoute = item.first
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(pickupLatLng!!, 16f)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    } else{
                        Text("এই মুহুর্তে কোনো নিকটবর্তী পিকআপ স্টপ নেই ২.৫ কিলোমিটার এর মধ্যে। দয়া করে অন্য জায়গা নির্বাচন করুন।")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (selectedPickupStop == null) {
                                error = "অনুগ্রহ করে একটি পিকআপ স্টপ নির্বাচন করুন"
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
                        label = { Text("Seats") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("আনুমানিক ভাড়া: ৳$fare")
                    val onMapClickDest: (LatLng) -> Unit = { latLng ->
                        destinationLatLng = latLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
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
                        onMapClick = onMapClickDest
                    ) {
                        pickupLatLng?.let {
                            Marker(state = MarkerState(position = it), title = "Pickup", snippet = pickupAddr)
                        }
                        destinationLatLng?.let {
                            Marker(state = MarkerState(position = it), title = "Approx Dest", snippet = destinationAddr)
                        }
                        nearbyDestStops.forEach { item ->
                            val pos = LatLng(item.second.latitude, item.second.longitude)
                            Marker(
                                state = MarkerState(position = pos),
                                title = item.second.address,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    if (selectedDestStop == item.second) HUE_GREEN else HUE_BLUE
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (destinationLatLng != null) {
                        Button(
                            onClick = {
                                isFetchingStops = true
                                scope.launch {
                                    nearbyDestStops = AuthRepository().getNearbyDestStops(selectedRoute, selectedPickupStop, destinationLatLng!!)
                                    isFetchingStops = false
                                }
                            },
                            enabled = !isFetchingStops
                        ) {
                            if (isFetchingStops) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Text("নিকটবর্তী গন্তব্য স্টপ খুঁজুন")
                        }
                    }
                    if (nearbyDestStops.isNotEmpty()) {
                        Text("নিকটবর্তী গন্তব্য (২.৫ কিলোমিটার এর মধ্যে):")
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            items(nearbyDestStops) { item ->
                                ListItem(
                                    headlineContent = {
                                        Text("${item.second.address}")
                                    },
                                    modifier = Modifier.clickable {
                                        selectedDestStop = item.second
                                        destinationAddr = item.second.address
                                        destinationLatLng = LatLng(item.second.latitude, item.second.longitude)
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(destinationLatLng!!, 16f)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        Text("আপনার পিকআপ স্টেশন থেকে এখানে কোনো গন্তব্য স্টেশন নেই ২.৫ কিলোমিটার এর মধ্যে। দয়া করে অন্য জায়গা নির্বাচন করুন।")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Reset
                    Button(
                        onClick = {
                            pickupAddr = ""; pickupLatLng = null
                            destinationAddr = ""; destinationLatLng = null
                            nearbyPickupStops = emptyList(); nearbyDestStops = emptyList()
                            selectedPickupStop = null; selectedDestStop = null
                            screenForDest = false; screenForPickup = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = Color.White)
                    ) {
                        Text("রুট বাতিল করুন")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Submit
                    Button(
                        onClick = {
                            if (selectedPickupStop == null || selectedDestStop == null || seats.toIntOrNull() == null) {
                                error = "অনুগ্রহ করে সব ক্ষেত্র পূরণ করুন"
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
                                    directionsApi = directionsApi,
                                    route = selectedRoute
                                )
                                isLoading = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "রিকোয়েস্ট জমা দেওয়া হয়েছে", Toast.LENGTH_SHORT).show()
                                    navController.navigate("my_requests")
                                } else {
                                    error = result.exceptionOrNull()?.message ?: "রিকোয়েস্ট ব্যর্থ হয়েছে"
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
                    Spacer(modifier = Modifier.height(16.dp)) // Ensure bottom padding for scrolling
                }
            }
        }
    }
}