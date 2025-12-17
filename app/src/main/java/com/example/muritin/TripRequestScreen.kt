package com.example.muritin

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
import com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
import com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
import com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
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

    // State variables
    var pickupAddr by remember { mutableStateOf("") }
    var destinationAddr by remember { mutableStateOf("") }
    var seats by remember { mutableStateOf("1") }
    var fare by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // API setup
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

    // Location states
    var pickupLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    val initialPosition = LatLng(23.8103, 90.4125) // Dhaka

    // Separate camera states for each map to avoid conflicts
    val pickupCameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }
    val destinationCameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }

    // Step management
    var currentStep by remember { mutableStateOf(TripStep.PICKUP) }

    // Stop management
    var nearbyPickupStops by remember { mutableStateOf<List<Pair<BusRoute, PointLocation>>>(emptyList()) }
    var nearbyDestStops by remember { mutableStateOf<List<Pair<BusRoute, PointLocation>>>(emptyList()) }
    var selectedPickupStop by remember { mutableStateOf<PointLocation?>(null) }
    var selectedDestStop by remember { mutableStateOf<PointLocation?>(null) }
    var selectedRoute by remember { mutableStateOf(BusRoute()) }
    var isFetchingStops by remember { mutableStateOf(false) }

    // Get current location on start
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
                    pickupCameraState.animate(
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
        }
    }

    Scaffold(
        topBar = {
            ModernTopBar(
                currentStep = currentStep,
                onBackClick = {
                    when (currentStep) {
                        TripStep.PICKUP -> navController.navigateUp()
                        TripStep.DESTINATION -> {
                            currentStep = TripStep.PICKUP
                            nearbyDestStops = emptyList()
                            selectedDestStop = null
                            destinationAddr = ""
                            destinationLatLng = null
                        }
                        TripStep.CONFIRMATION -> {
                            currentStep = TripStep.DESTINATION
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                CustomSnackbar(it)
            }
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Progress indicator
                StepProgressIndicator(currentStep = currentStep)

                // Main content
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        TripStep.PICKUP -> PickupStepContent(
                            pickupAddr = pickupAddr,
                            pickupLatLng = pickupLatLng,
                            nearbyPickupStops = nearbyPickupStops,
                            selectedPickupStop = selectedPickupStop,
                            isFetchingStops = isFetchingStops,
                            apiKey = apiKey,
                            placesClient = placesClient,
                            geocodingApi = geocodingApi,
                            cameraPositionState = pickupCameraState,
                            onPickupAddressChange = { addr, latlng ->
                                pickupAddr = addr
                                pickupLatLng = latlng
                            },
                            onMapClick = { latLng ->
                                pickupLatLng = latLng
                                pickupCameraState.position = CameraPosition.fromLatLngZoom(
                                    latLng,
                                    pickupCameraState.position.zoom
                                )
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
                            },
                            onFindStops = {
                                isFetchingStops = true
                                scope.launch {
                                    nearbyPickupStops = AuthRepository().getNearbyPickup(pickupLatLng!!)
                                    isFetchingStops = false
                                }
                            },
                            onStopSelected = { route, stop ->
                                selectedRoute = route
                                selectedPickupStop = stop
                                pickupAddr = stop.address
                                pickupLatLng = LatLng(stop.latitude, stop.longitude)
                                scope.launch {
                                    pickupCameraState.animate(
                                        CameraUpdateFactory.newLatLngZoom(pickupLatLng!!, 16f)
                                    )
                                }
                            },
                            onNext = {
                                if (selectedPickupStop != null) {
                                    currentStep = TripStep.DESTINATION
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("অনুগ্রহ করে একটি পিকআপ স্টপ নির্বাচন করুন")
                                    }
                                }
                            }
                        )

                        TripStep.DESTINATION -> DestinationStepContent(
                            destinationAddr = destinationAddr,
                            destinationLatLng = destinationLatLng,
                            pickupLatLng = pickupLatLng,
                            pickupAddr = pickupAddr,
                            nearbyDestStops = nearbyDestStops,
                            selectedDestStop = selectedDestStop,
                            selectedPickupStop = selectedPickupStop,
                            selectedRoute = selectedRoute,
                            isFetchingStops = isFetchingStops,
                            fare = fare,
                            apiKey = apiKey,
                            placesClient = placesClient,
                            geocodingApi = geocodingApi,
                            cameraPositionState = destinationCameraState,
                            onDestinationAddressChange = { addr, latlng ->
                                destinationAddr = addr
                                destinationLatLng = latlng
                            },
                            onMapClick = { latLng ->
                                destinationLatLng = latLng
                                destinationCameraState.position = CameraPosition.fromLatLngZoom(
                                    latLng,
                                    destinationCameraState.position.zoom
                                )
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
                            },
                            onFindStops = {
                                isFetchingStops = true
                                scope.launch {
                                    nearbyDestStops = AuthRepository().getNearbyDestStops(
                                        selectedRoute,
                                        selectedPickupStop,
                                        destinationLatLng!!
                                    )
                                    isFetchingStops = false
                                }
                            },
                            onStopSelected = { stop ->
                                selectedDestStop = stop
                                destinationAddr = stop.address
                                destinationLatLng = LatLng(stop.latitude, stop.longitude)
                                scope.launch {
                                    destinationCameraState.animate(
                                        CameraUpdateFactory.newLatLngZoom(destinationLatLng!!, 16f)
                                    )
                                }
                            },
                            onNext = {
                                if (selectedDestStop != null) {
                                    currentStep = TripStep.CONFIRMATION
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("অনুগ্রহ করে একটি গন্তব্য স্টপ নির্বাচন করুন")
                                    }
                                }
                            }
                        )

                        TripStep.CONFIRMATION -> ConfirmationStepContent(
                            pickupAddr = pickupAddr,
                            destinationAddr = destinationAddr,
                            seats = seats,
                            fare = fare,
                            selectedPickupStop = selectedPickupStop,
                            selectedDestStop = selectedDestStop,
                            isLoading = isLoading,
                            onSeatsChange = { seats = it },
                            onSubmit = {
                                if (selectedPickupStop == null || selectedDestStop == null || seats.toIntOrNull() == null) {
                                    error = "অনুগ্রহ করে সব ক্ষেত্র পূরণ করুন"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "") }
                                    return@ConfirmationStepContent
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
                                        Toast.makeText(context, "রিকোয়েস্ট সফলভাবে জমা দেওয়া হয়েছে!", Toast.LENGTH_SHORT).show()
                                        navController.navigate("my_requests")
                                    } else {
                                        error = result.exceptionOrNull()?.message ?: "রিকোয়েস্ট ব্যর্থ হয়েছে"
                                        snackbarHostState.showSnackbar(error ?: "")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==================== STEP ENUM ====================
enum class TripStep {
    PICKUP, DESTINATION, CONFIRMATION
}

// ==================== MODERN TOP BAR ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    currentStep: TripStep,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = when (currentStep) {
                    TripStep.PICKUP -> "পিকআপ পয়েন্ট নির্বাচন করুন"
                    TripStep.DESTINATION -> "গন্তব্য নির্বাচন করুন"
                    TripStep.CONFIRMATION -> "রিকোয়েস্ট নিশ্চিত করুন"
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = TextPrimary,
            navigationIconContentColor = Primary
        ),
        modifier = Modifier.shadow(2.dp)
    )
}

// ==================== STEP PROGRESS INDICATOR ====================
@Composable
fun StepProgressIndicator(currentStep: TripStep) {
    val steps = listOf("পিকআপ", "গন্তব্য", "নিশ্চিত")
    val currentIndex = when (currentStep) {
        TripStep.PICKUP -> 0
        TripStep.DESTINATION -> 1
        TripStep.CONFIRMATION -> 2
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                StepIndicatorItem(
                    stepNumber = index + 1,
                    stepName = step,
                    isActive = index == currentIndex,
                    isCompleted = index < currentIndex,
                    modifier = Modifier.weight(1f)
                )

                if (index < steps.size - 1) {
                    StepConnector(
                        isCompleted = index < currentIndex,
                        modifier = Modifier.weight(0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StepIndicatorItem(
    stepNumber: Int,
    stepName: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = when {
                        isCompleted -> Success
                        isActive -> Primary
                        else -> Color(0xFFE0E0E0)
                    },
                    shape = CircleShape
                )
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    color = if (isActive) Color.White else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stepName,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive || isCompleted) TextPrimary else TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StepConnector(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .height(2.dp)
            .background(
                color = if (isCompleted) Success else Color(0xFFE0E0E0)
            )
    )
}

// ==================== PICKUP STEP CONTENT ====================
@Composable
fun PickupStepContent(
    pickupAddr: String?,
    pickupLatLng: LatLng?,
    nearbyPickupStops: List<Pair<BusRoute, PointLocation>>?,
    selectedPickupStop: PointLocation?,
    isFetchingStops: Boolean,
    apiKey: String,
    placesClient: com.google.android.libraries.places.api.net.PlacesClient,
    geocodingApi: GeocodingApi,
    cameraPositionState: CameraPositionState,
    onPickupAddressChange: (String, LatLng) -> Unit,
    onMapClick: (LatLng) -> Unit,
    onFindStops: () -> Unit,
    onStopSelected: (BusRoute, PointLocation) -> Unit,
    onNext: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Safe handling of nullable parameters
    val safePickupAddr = pickupAddr ?: ""
    val safeNearbyStops = nearbyPickupStops ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info Card
        item {
            InfoCard(
                icon = Icons.Default.LocationOn,
                title = "আপনার পিকআপ পয়েন্ট",
                description = "আপনি কোথা থেকে যাত্রা শুরু করতে চান তা নির্বাচন করুন। মানচিত্রে ক্লিক করুন বা ঠিকানা অনুসন্ধান করুন।"
            )
        }

        // Search Location
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SearchLocation(
                        label = "পিকআপ লোকেশন অনুসন্ধান করুন",
                        apiKey = apiKey,
                        placesClient = placesClient,
                        geocodingApi = geocodingApi,
                        coroutineScope = scope,
                        context = context,
                        onLocationSelected = onPickupAddressChange,
                        cameraPositionState = cameraPositionState
                    )
                }
            }
        }

        // Map
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isTrafficEnabled = true),
                    uiSettings = MapUiSettings(zoomGesturesEnabled = true, zoomControlsEnabled = true),
                    onMapClick = onMapClick
                ) {
                    pickupLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "আপনার অবস্থান",
                            snippet = safePickupAddr,
                            icon = BitmapDescriptorFactory.defaultMarker(HUE_RED)
                        )
                    }
                    safeNearbyStops.forEach { item ->
                        val pos = LatLng(item.second.latitude, item.second.longitude)
                        Marker(
                            state = MarkerState(position = pos),
                            title = item.second.address,
                            snippet = "পিকআপ স্টপ",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (selectedPickupStop == item.second) HUE_GREEN else HUE_BLUE
                            )
                        )
                    }
                }
            }
        }

        // Find Stops Button
        item {
            if (pickupLatLng != null && safeNearbyStops.isEmpty()) {
                ActionButton(
                    text = "নিকটবর্তী বাস স্টপ খুঁজুন",
                    icon = Icons.Default.Search,
                    onClick = onFindStops,
                    isLoading = isFetchingStops,
                    gradient = Brush.horizontalGradient(
                        colors = listOf(Primary, PrimaryLight)
                    )
                )
            }
        }

        // Nearby Stops List
        if (safeNearbyStops.isNotEmpty()) {
            item {
                Text(
                    text = "নিকটবর্তী বাস স্টপসমূহ (২.৫ কিমি এর মধ্যে)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(safeNearbyStops) { item ->
                StopCard(
                    stop = item.second,
                    isSelected = selectedPickupStop == item.second,
                    onSelect = { onStopSelected(item.first, item.second) }
                )
            }
        }

        // Next Button
        if (selectedPickupStop != null) {
            item {
                ActionButton(
                    text = "পরবর্তী: গন্তব্য নির্বাচন করুন",
                    icon = Icons.Default.ArrowForward,
                    onClick = onNext,
                    gradient = Brush.horizontalGradient(
                        colors = listOf(Secondary, SecondaryLight)
                    )
                )
            }
        }

        // Bottom Spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==================== DESTINATION STEP CONTENT ====================
@Composable
fun DestinationStepContent(
    destinationAddr: String?,
    destinationLatLng: LatLng?,
    pickupLatLng: LatLng?,
    pickupAddr: String?,
    nearbyDestStops: List<Pair<BusRoute, PointLocation>>?,
    selectedDestStop: PointLocation?,
    selectedPickupStop: PointLocation?,
    selectedRoute: BusRoute?,
    isFetchingStops: Boolean,
    fare: Int,
    apiKey: String,
    placesClient: com.google.android.libraries.places.api.net.PlacesClient,
    geocodingApi: GeocodingApi,
    cameraPositionState: CameraPositionState,
    onDestinationAddressChange: (String, LatLng) -> Unit,
    onMapClick: (LatLng) -> Unit,
    onFindStops: () -> Unit,
    onStopSelected: (PointLocation) -> Unit,
    onNext: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Safe handling of nullable parameters
    val safeDestinationAddr = destinationAddr ?: ""
    val safePickupAddr = pickupAddr ?: ""
    val safeNearbyStops = nearbyDestStops ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selected Pickup Display
        item {
            SelectedLocationCard(
                icon = Icons.Default.LocationOn,
                title = "নির্বাচিত পিকআপ পয়েন্ট",
                address = safePickupAddr,
                color = Primary
            )
        }

        // Info Card
        item {
            InfoCard(
                icon = Icons.Default.Place,
                title = "আপনার গন্তব্য",
                description = "আপনি কোথায় যেতে চান তা নির্বাচন করুন। আপনার পিকআপ স্টপ থেকে উপলব্ধ গন্তব্যস্থল দেখুন।"
            )
        }

        // Search Location
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SearchLocation(
                        label = "গন্তব্য লোকেশন অনুসন্ধান করুন",
                        apiKey = apiKey,
                        placesClient = placesClient,
                        geocodingApi = geocodingApi,
                        coroutineScope = scope,
                        context = context,
                        onLocationSelected = onDestinationAddressChange,
                        cameraPositionState = cameraPositionState
                    )
                }
            }
        }

        // Map
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isTrafficEnabled = true),
                    uiSettings = MapUiSettings(zoomGesturesEnabled = true, zoomControlsEnabled = true),
                    onMapClick = onMapClick
                ) {
                    pickupLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "পিকআপ পয়েন্ট",
                            snippet = safePickupAddr,
                            icon = BitmapDescriptorFactory.defaultMarker(HUE_RED)
                        )
                    }
                    destinationLatLng?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "আনুমানিক গন্তব্য",
                            snippet = safeDestinationAddr,
                            icon = BitmapDescriptorFactory.defaultMarker(HUE_ORANGE)
                        )
                    }
                    safeNearbyStops.forEach { item ->
                        val pos = LatLng(item.second.latitude, item.second.longitude)
                        Marker(
                            state = MarkerState(position = pos),
                            title = item.second.address,
                            snippet = "গন্তব্য স্টপ",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (selectedDestStop == item.second) HUE_GREEN else HUE_BLUE
                            )
                        )
                    }
                }
            }
        }

        // Find Stops Button
        item {
            if (destinationLatLng != null && safeNearbyStops.isEmpty()) {
                ActionButton(
                    text = "নিকটবর্তী গন্তব্য স্টপ খুঁজুন",
                    icon = Icons.Default.Search,
                    onClick = onFindStops,
                    isLoading = isFetchingStops,
                    gradient = Brush.horizontalGradient(
                        colors = listOf(Primary, PrimaryLight)
                    )
                )
            }
        }

        // Nearby Stops List
        if (safeNearbyStops.isNotEmpty()) {
            item {
                Text(
                    text = "নিকটবর্তী গন্তব্য স্টপসমূহ (২.৫ কিমি এর মধ্যে)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(safeNearbyStops) { item ->
                StopCard(
                    stop = item.second,
                    isSelected = selectedDestStop == item.second,
                    onSelect = { onStopSelected(item.second) }
                )
            }

            if (safeNearbyStops.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "আপনার পিকআপ স্টপ থেকে এই এলাকায় কোনো গন্তব্য স্টপ নেই। অনুগ্রহ করে অন্য লোকেশন নির্বাচন করুন।"
                    )
                }
            }
        }

        // Fare Display
        if (selectedDestStop != null && fare > 0) {
            item {
                FareCard(fare = fare)
            }
        }

        // Next Button
        if (selectedDestStop != null) {
            item {
                ActionButton(
                    text = "পরবর্তী: নিশ্চিত করুন",
                    icon = Icons.Default.ArrowForward,
                    onClick = onNext,
                    gradient = Brush.horizontalGradient(
                        colors = listOf(Secondary, SecondaryLight)
                    )
                )
            }
        }

        // Bottom Spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==================== CONFIRMATION STEP CONTENT ====================
@Composable
fun ConfirmationStepContent(
    pickupAddr: String?,
    destinationAddr: String?,
    seats: String,
    fare: Int,
    selectedPickupStop: PointLocation?,
    selectedDestStop: PointLocation?,
    isLoading: Boolean,
    onSeatsChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    // Safe handling of nullable parameters
    val safePickupAddr = pickupAddr ?: ""
    val safeDestinationAddr = destinationAddr ?: ""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Info Card
        item {
            InfoCard(
                icon = Icons.Default.CheckCircle,
                title = "আপনার ট্রিপ নিশ্চিত করুন",
                description = "নিচের তথ্য যাচাই করুন এবং আপনার ট্রিপ রিকোয়েস্ট জমা দিন।"
            )
        }

        // Trip Summary Card
        item {
            TripSummaryCard(
                pickupAddr = safePickupAddr,
                destinationAddr = safeDestinationAddr,
                fare = fare
            )
        }

        // Seats Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "সিটের সংখ্যা",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = seats,
                        onValueChange = {
                            if (it.isEmpty() || it.toIntOrNull() != null) {
                                onSeatsChange(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("সিট নম্বর লিখুন") },
                        placeholder = { Text("১") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedLabelColor = Primary
                        )
                    )

                    Text(
                        text = "সর্বোচ্চ ৪টি সিট বুক করতে পারবেন",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Submit Button
        item {
            ActionButton(
                text = if (isLoading) "জমা হচ্ছে..." else "রিকোয়েস্ট জমা দিন",
                icon = if (isLoading) null else Icons.Default.Send,
                onClick = onSubmit,
                isLoading = isLoading,
                gradient = Brush.horizontalGradient(
                    colors = listOf(Success, Color(0xFF388E3C))
                ),
                enabled = !isLoading
            )
        }

        // Important Notes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Info.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Info,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "গুরুত্বপূর্ণ তথ্য",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Info
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• কন্ডাক্টর আপনার রিকোয়েস্ট গ্রহণ করলে আপনি নোটিফিকেশন পাবেন\n• ভাড়া নগদে পরিশোধ করতে হবে\n• সময়মতো পিকআপ পয়েন্টে উপস্থিত থাকুন",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Bottom Spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==================== REUSABLE COMPONENTS ====================

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun StopCard(
    stop: PointLocation,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Primary, RoundedCornerShape(12.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isSelected) Primary else Color(0xFFE3F2FD),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stop.address,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    gradient: Brush,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedLocationCard(
    icon: ImageVector,
    title: String,
    address: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = address,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun TripSummaryCard(
    pickupAddr: String,
    destinationAddr: String,
    fare: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "ট্রিপ সারাংশ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pickup
            TripDetailRow(
                icon = Icons.Default.LocationOn,
                label = "পিকআপ",
                value = pickupAddr,
                iconColor = Primary
            )

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Divider
            )

            // Destination
            TripDetailRow(
                icon = Icons.Default.Place,
                label = "গন্তব্য",
                value = destinationAddr,
                iconColor = Secondary
            )

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Divider
            )

            // Fare
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    Icon(
//                        imageVector = Icons.Default.AccountBalanceWallet,
//                        contentDescription = null,
//                        tint = Success,
//                        modifier = Modifier.size(20.dp)
//                    )
////                    Spacer(modifier = Modifier.width(12.dp))
////                    Text(
////                        text = "আনুমানিক ভাড়া",
////                        fontSize = 14.sp,
////                        color = TextSecondary
////                    )
//                }
//                Text(
//                    text = "৳$fare",
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    color = Success
//                )
//            }
        }
    }
}

@Composable
fun TripDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun FareCard(fare: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Success.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "আনুমানিক ভাড়া",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "৳$fare",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
            }
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Warning.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                tint = Warning,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun CustomSnackbar(data: SnackbarData) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TextPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.visuals.message,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            data.visuals.actionLabel?.let { actionLabel ->
                TextButton(onClick = { data.performAction() }) {
                    Text(
                        text = actionLabel,
                        color = SecondaryLight
                    )
                }
            }
        }
    }
}