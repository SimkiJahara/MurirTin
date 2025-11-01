
package com.example.muritin

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRequestScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var pickupAddr by remember { mutableStateOf("") }
    var destinationAddr by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("1") }
    var fare by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

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

    var screenForPickup by remember { mutableStateOf(true) }
    var screenForDest by remember { mutableStateOf(false) }
    var screenForSelectingPickupStation by remember { mutableStateOf(false) }

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
                            pickupAddr = response.results[0].geometry.location.toString()
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
            if (screenForPickup) {
                SearchLocation(
                    label = "পিকআপ",
                    apiKey = apiKey,
                    placesClient = placesClient,
                    geocodingApi = geocodingApi,
                    coroutineScope = coroutineScope,
                    context = context,
                    onLocationSelected = { addr: String, latlng: LatLng ->
                        pickupAddr = addr
                        pickupLatLng = latlng
                    },
                    cameraPositionState = cameraPositionState
                )
                // On map click, update the clicked location
                val onMapClick: (LatLng) -> Unit = { latLng ->
                    pickupLatLng = latLng
                    val latLngStr = String.format("%.4f,%.4f", latLng.latitude, latLng.longitude)
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                    coroutineScope.launch {
                        try {
                            // Use Geocoder to get address
                            val geocoder = Geocoder(context)
                            val addresses = geocoder.getFromLocation(
                                latLng.latitude,
                                latLng.longitude,
                                1
                            ) // Get 1 result
                            pickupAddr = if (addresses != null && addresses.isNotEmpty()) {
                                addresses[0].getAddressLine(0)
                                    ?: latLngStr // Use first address line or go back to coordinates
                            } else {
                                "লোকেশন পাওয়া যায়নি: $latLngStr"
                            }
                        } catch (e: Exception) {
                            pickupAddr = "লোকেশন পাওয়া যায়নি: ${e.message}"
                        }
                    }
                }
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isTrafficEnabled = true),
                    uiSettings = MapUiSettings(
                        zoomGesturesEnabled = true,
                        zoomControlsEnabled = false
                    ),
                    onMapClick = onMapClick
                ) {
                    pickupLatLng?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "পিকআপ",
                            snippet = pickupAddr
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        screenForPickup = false
                        screenForDest = true
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
                    coroutineScope = coroutineScope,
                    context = context,
                    onLocationSelected = { addr: String, latlng: LatLng ->
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
                // On map click, update the clicked location
                val onMapClick: (LatLng) -> Unit = { latLng ->
                    destinationLatLng = latLng
                    val latLngStr = String.format("%.4f,%.4f", latLng.latitude, latLng.longitude)
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                    coroutineScope.launch {
                        try {
                            // Use Geocoder to get address
                            val geocoder = Geocoder(context)
                            val addresses = geocoder.getFromLocation(
                                latLng.latitude,
                                latLng.longitude,
                                1
                            ) // Get 1 result
                            destinationAddr = if (addresses != null && addresses.isNotEmpty()) {
                                addresses[0].getAddressLine(0) ?: latLngStr
                            } else {
                                "লোকেশন পাওয়া যায়নি: $latLngStr"
                            }
                        } catch (e: Exception) {
                            destinationAddr = "লোকেশন পাওয়া যায়নি: ${e.message}"
                        }
                    }
                }
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isTrafficEnabled = true),
                    uiSettings = MapUiSettings(
                        zoomGesturesEnabled = true,
                        zoomControlsEnabled = false
                    ),
                    onMapClick = onMapClick
                ) {
                    pickupLatLng?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "পিকআপ",
                            snippet = pickupAddr
                        )
                    }
                    destinationLatLng?.let { latLng ->
                        Marker(
                            state = MarkerState(position = latLng),
                            title = "গন্তব্য",
                            snippet = destinationAddr
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        pickupAddr = ""
                        pickupLatLng = null
                        destinationAddr = ""
                        destinationLatLng = null
                        screenForDest = false
                        screenForPickup = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C),
                        contentColor = Color.White
                    )
                ) {
                    Text("রুট বাতিল করুন")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        screenForPickup = false
                        screenForDest = false
                        screenForSelectingPickupStation = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("পিকআপ স্টেশন দেখুন")
                }
            }
            if (screenForSelectingPickupStation) {
                var matchedPickupStationsList by remember {
                    mutableStateOf<List<Pair<PointLocation, String>>>(
                        emptyList()
                    )
                }
                var isLoading by remember { mutableStateOf(true) }
                var expanded by remember { mutableStateOf(false) }
                var selectedStation by remember { mutableStateOf(matchedPickupStationsList.firstOrNull()) }
                var selectedRouteId by remember { mutableStateOf("") }

                LaunchedEffect(pickupLatLng, destinationLatLng) {
                    if (pickupLatLng == null || destinationLatLng == null) return@LaunchedEffect
                    isLoading = true
                    try {
                        val result = AuthRepository().findNearbyPickupStations(
                            pickupLatLng!!,
                            destinationLatLng!!
                        )
                        matchedPickupStationsList = result
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }
                    matchedPickupStationsList.isEmpty() -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "আপনার মার্ক করা জায়গার ২.৫ কিলোমিটার দূরত্বে কোনো বাসের স্টেশন পাওয়া যাইনি",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                pickupAddr = ""
                                pickupLatLng = null
                                destinationAddr = ""
                                destinationLatLng = null
                                screenForSelectingPickupStation = false
                                screenForDest = false
                                screenForPickup = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("অন্য জায়গা নির্বাচন করুন")
                        }
                        Button(
                            onClick = {
                                navController.navigate("rider_dashboard")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ড্যাশবোর্ডে ফিরে যান")
                        }
                    }
                    else -> {
                        Column (
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Top,
                        ){
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                "আপনার মার্ক করা জায়গার ২.৫ কিলোমিটার দূরত্বে বাসের স্টেশনসমুহ",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            GoogleMap(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                cameraPositionState = cameraPositionState,
                                properties = MapProperties(isTrafficEnabled = true),
                                uiSettings = MapUiSettings(
                                    zoomGesturesEnabled = true,
                                    zoomControlsEnabled = false
                                )
                            ) {
                                pickupLatLng?.let { latLng ->
                                    LaunchedEffect(latLng) {
                                        cameraPositionState.animate(
                                            update = CameraUpdateFactory.newLatLngZoom(latLng, 15f),
                                        )
                                    }
                                    Marker(
                                        state = MarkerState(position = latLng),
                                        title = "আমার পছন্দের পিকআপ",
                                        snippet = pickupAddr,
                                        icon = BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_VIOLET
                                        )
                                    )
                                }

                                for (option in matchedPickupStationsList) {
                                    val location = option.first
                                    val latlng = LatLng(location.latitude, location.longitude)
                                    Marker(
                                        state = MarkerState(position = latlng),
                                        title = "স্টেশন",
                                        icon = BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_RED
                                        )
                                    )
                                }
                            }
                            //ExposedDropdownMenuBox for selecting stations
                            Spacer(modifier = Modifier.height(10.dp))
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.5.dp,
                                        color = if (expanded) Color(0xFF6A1B9A) else Color(0xFFAB47BC),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(Color.White, RoundedCornerShape(8.dp))
                            ) {
                                OutlinedTextField(
                                    value = selectedStation?.first?.address ?: "পিকআপ স্টেশন নির্বাচন করুন",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .padding(0.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF3E5F5))
                                ) {
                                    matchedPickupStationsList.forEach { station ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = station.first.address ?: "Unknown Station",
                                                    color = Color(0xFF4A148C)
                                                )
                                            },
                                            onClick = {
                                                selectedStation = station
                                                expanded = false
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFFAB47BC).copy(alpha = 0.3f), // Soft divider
                                                    shape = RectangleShape
                                                )
                                                .padding(vertical = 8.dp, horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    scope.launch {
                                        val result = AuthRepository().submitTripRequest(
                                            riderId = user.uid,
                                            pickup = selectedStation?.first?.address ?: "",
                                            routeId = selectedStation?.second?:"",
                                            destination = destinationAddr,
                                            seats = seats.toInt(),
                                            pickupLatLng = LatLng(selectedStation?.first?.latitude?: 0.00, selectedStation?.first?.longitude?: 0.00),
                                            destinationLatLng = destinationLatLng!!,
                                            apiKey = apiKey,
                                            directionsApi = directionsApi
                                        )
                                        isLoading = false
                                        if (result.isSuccess) {
                                            Toast.makeText(
                                                context,
                                                "রিকোয়েস্ট জমা দেওয়া হয়েছে",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.navigate("my_requests")
                                        } else {
                                            val errorMsg = result.exceptionOrNull()?.message ?: "অজানা ত্রুটি"
                                            Toast.makeText(context, "ব্যর্থ: $errorMsg", Toast.LENGTH_LONG).show()
                                            navController.navigate("rider_dashboard")
                                        }
                                    }
                                },
                                enabled = selectedStation != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Text("রিকোয়েস্ট জমা দিন")
                            }
                        }
                    }
                }
            }
        }
    }
}
