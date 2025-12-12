// PART 1 of 3: Enhanced LiveTrackingScreen - Imports and Setup
// Replace the beginning of your LiveTrackingScreen.kt file with this

package com.example.muritin

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    navController: NavHostController,
    user: FirebaseUser,
    requestId: String
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State variables
    var request by remember { mutableStateOf<Request?>(null) }
    var conductorLocation by remember { mutableStateOf<ConductorLocation?>(null) }
    var busName by remember { mutableStateOf<String?>(null) }
    var conductorName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(true) }
    var showTripDetails by remember { mutableStateOf(false) }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    var selectedMapType by remember { mutableStateOf(MapType.NORMAL) }
    var showTrafficLayer by remember { mutableStateOf(true) }
    var estimatedDistance by remember { mutableStateOf(0.0) }
    var estimatedTime by remember { mutableStateOf(0) }

    val pickAndDestLLofBus = remember { mutableStateListOf<PointLocation>() }

    val cameraPositionState = rememberCameraPositionState()

    // Pulsing animation for live indicator and bus marker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Scale animation for bus marker
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Calculate distance between two points
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Update estimated metrics
    fun updateEstimates() {
        conductorLocation?.let { busLoc ->
            request?.pickupLatLng?.let { pickup ->
                estimatedDistance = calculateDistance(
                    busLoc.lat, busLoc.lng,
                    pickup.lat, pickup.lng
                )
                // Rough estimate: 30 km/h average speed in city traffic
                estimatedTime = ((estimatedDistance / 30.0) * 60).toInt()
            }
        }
    }

    // Auto-refresh function
    suspend fun refreshLocation() {
        if (isRefreshing) return

        isRefreshing = true
        try {
            request?.conductorId?.let { conductorId ->
                conductorLocation = AuthRepository().getConductorLocation(conductorId)
                lastUpdateTime = System.currentTimeMillis()

                conductorLocation?.let { loc ->
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(loc.lat, loc.lng),
                        15f
                    )
                    updateEstimates()
                }
            }
        } catch (e: Exception) {
            Log.e("LiveTrackingScreen", "Refresh error: ${e.message}")
        } finally {
            isRefreshing = false
        }
    }

    // Center on bus location
    fun centerOnBus() {
        conductorLocation?.let {
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(
                            LatLng(it.lat, it.lng),
                            17f
                        )
                    )
                )
            }
        }
    }

    // Center on pickup location
    fun centerOnPickup() {
        request?.pickupLatLng?.let {
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(
                            LatLng(it.lat, it.lng),
                            16f
                        )
                    )
                )
            }
        }
    }

    // Show entire route
    fun showFullRoute() {
        scope.launch {
            try {
                val builder = LatLngBounds.builder()
                var hasPoints = false

                conductorLocation?.let {
                    builder.include(LatLng(it.lat, it.lng))
                    hasPoints = true
                }

                pickAndDestLLofBus.forEach { point ->
                    builder.include(LatLng(point.latitude, point.longitude))
                    hasPoints = true
                }

                if (hasPoints) {
                    val bounds = builder.build()
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 150)
                    )
                }
            } catch (e: Exception) {
                Log.e("LiveTrackingScreen", "Error showing route: ${e.message}")
            }
        }
    }

    // Initial load
    LaunchedEffect(requestId) {
        try {
            val allRequests = AuthRepository().getRequestsForUser(user.uid)
            request = allRequests.find { it.id == requestId }

            // Fetch bus name
            request?.busId?.let { busId ->
                val bus = AuthRepository().getBus(busId)
                busName = bus?.name
            }

            // Fetch conductor name
            request?.conductorId?.let { conductorId ->
                val conductorResult = AuthRepository().getUser(conductorId)
                conductorName = conductorResult.getOrNull()?.name

                // Get conductor location
                conductorLocation = AuthRepository().getConductorLocation(conductorId)
                lastUpdateTime = System.currentTimeMillis()
            }

            isLoading = false

            // Load pickup and destination locations
            val busLoc = AuthRepository().getLLofPickupDestofBusForRider(requestId)
            pickAndDestLLofBus.clear()
            pickAndDestLLofBus.addAll(busLoc)

            // Update estimates
            updateEstimates()

            conductorLocation?.let { loc ->
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(loc.lat, loc.lng),
                    15f
                )
            }
        } catch (e: Exception) {
            isLoading = false
            Log.e("LiveTrackingScreen", "Error: ${e.message}")
        }
    }

    // Auto-refresh every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            if (request?.conductorId != null) {
                refreshLocation()
            }
        }
    }

    // PART 2 of 3: Enhanced Map Display and Controls
// Continue from Part 1

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // Enhanced Loading Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2196F3).copy(alpha = 0.1f),
                                Color.White
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Animated bus icon
                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(8.dp, CircleShape),
                        shape = CircleShape,
                        color = Color(0xFF2196F3).copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF2196F3)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF2196F3),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "আপনার যাত্রা খুঁজছি...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "অনুগ্রহ করে অপেক্ষা করুন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // Enhanced Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = selectedMapType,
                    isTrafficEnabled = showTrafficLayer,
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomGesturesEnabled = true,
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                // Draw route polyline between bus and pickup
                conductorLocation?.let { busLoc ->
                    request?.pickupLatLng?.let { pickup ->
                        val routePoints = listOf(
                            LatLng(busLoc.lat, busLoc.lng),
                            LatLng(pickup.lat, pickup.lng)
                        )

                        // Dashed line to pickup
                        Polyline(
                            points = routePoints,
                            color = Color(0xFF2196F3),
                            width = 10f,
                            geodesic = true,
                            pattern = listOf(
                                Dash(30f),
                                Gap(15f)
                            )
                        )
                    }
                }

                // Draw route between pickup and destination
                if (pickAndDestLLofBus.size >= 2) {
                    val pickup = pickAndDestLLofBus[0]
                    val destination = pickAndDestLLofBus[1]

                    Polyline(
                        points = listOf(
                            LatLng(pickup.latitude, pickup.longitude),
                            LatLng(destination.latitude, destination.longitude)
                        ),
                        color = Color(0xFF4CAF50),
                        width = 12f,
                        geodesic = true,
                        pattern = listOf(
                            Dot(),
                            Gap(10f)
                        )
                    )
                }

                // Enhanced Bus location marker with pulsing circle
                conductorLocation?.let { loc ->
                    // Pulsing circle around bus
                    Circle(
                        center = LatLng(loc.lat, loc.lng),
                        radius = 80.0,
                        fillColor = Color(0x332196F3),
                        strokeColor = Color(0xFF2196F3).copy(alpha = pulseAlpha),
                        strokeWidth = 4f
                    )

                    Circle(
                        center = LatLng(loc.lat, loc.lng),
                        radius = 40.0,
                        fillColor = Color(0x552196F3),
                        strokeColor = Color(0xFF2196F3),
                        strokeWidth = 2f
                    )

                    // Bus marker
                    Marker(
                        state = MarkerState(position = LatLng(loc.lat, loc.lng)),
                        title = "🚌 ${busName ?: "বাস"}",
                        snippet = "কন্ডাক্টর: ${conductorName ?: "N/A"}\nশেষ আপডেট: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(loc.timestamp))}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                // Enhanced Pickup location marker
                pickAndDestLLofBus.firstOrNull()?.let { pickup ->
                    // Green circle around pickup
                    Circle(
                        center = LatLng(pickup.latitude, pickup.longitude),
                        radius = 50.0,
                        fillColor = Color(0x334CAF50),
                        strokeColor = Color(0xFF4CAF50),
                        strokeWidth = 3f
                    )

                    Marker(
                        state = MarkerState(
                            position = LatLng(pickup.latitude, pickup.longitude)
                        ),
                        title = "📍 পিকআপ পয়েন্ট",
                        snippet = pickup.address,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }

                // Enhanced Destination location marker
                pickAndDestLLofBus.getOrNull(1)?.let { destination ->
                    // Red circle around destination
                    Circle(
                        center = LatLng(destination.latitude, destination.longitude),
                        radius = 50.0,
                        fillColor = Color(0x33F44336),
                        strokeColor = Color(0xFFF44336),
                        strokeWidth = 3f
                    )

                    Marker(
                        state = MarkerState(
                            position = LatLng(destination.latitude, destination.longitude)
                        ),
                        title = "🏁 গন্তব্য",
                        snippet = destination.address,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
            }

            // Enhanced Top Bar with Gradient
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Top row with back and refresh
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back button
                            IconButton(
                                onClick = { navController.navigateUp() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White, CircleShape)
                                    .shadow(6.dp, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.Black
                                )
                            }

                            // Live indicator with pulse
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White,
                                modifier = Modifier.shadow(6.dp, RoundedCornerShape(24.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                Color(0xFFFF5252).copy(alpha = pulseAlpha),
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "সরাসরি ট্র্যাকিং",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF5252)
                                    )
                                }
                            }

                            // Refresh button
                            IconButton(
                                onClick = { scope.launch { refreshLocation() } },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White, CircleShape)
                                    .shadow(6.dp, CircleShape),
                                enabled = !isRefreshing
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF2196F3)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }

                        // Trip status with estimated time
                        if (request?.status == "Accepted" && estimatedDistance > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.shadow(4.dp, RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Distance
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Route,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            String.format("%.1f km", estimatedDistance),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF9800)
                                        )
                                        Text(
                                            "দূরত্ব",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }

                                    VerticalDivider(
                                        modifier = Modifier.height(40.dp),
                                        color = Color.LightGray
                                    )

                                    // Estimated time
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFF2196F3),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "$estimatedTime মিনিট",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2196F3)
                                        )
                                        Text(
                                            "আনুমানিক সময়",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // PART 3 of 3: Info Cards, Controls and Helper Components
// Continue from Part 2

            // Enhanced Control Panel (Right side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Center on bus
                FloatingActionButton(
                    onClick = { centerOnBus() },
                    modifier = Modifier.size(56.dp),
                    containerColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "বাসে কেন্দ্র করুন",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Center on pickup
                FloatingActionButton(
                    onClick = { centerOnPickup() },
                    modifier = Modifier.size(56.dp),
                    containerColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        Icons.Default.PinDrop,
                        contentDescription = "পিকআপে কেন্দ্র করুন",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Show full route
                FloatingActionButton(
                    onClick = { showFullRoute() },
                    modifier = Modifier.size(56.dp),
                    containerColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = "সম্পূর্ণ রুট দেখুন",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Zoom in
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.Black)
                }

                // Zoom out
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.Black)
                }

                // Map type toggle
                FloatingActionButton(
                    onClick = {
                        selectedMapType = when (selectedMapType) {
                            MapType.NORMAL -> MapType.SATELLITE
                            MapType.SATELLITE -> MapType.HYBRID
                            MapType.HYBRID -> MapType.TERRAIN
                            else -> MapType.NORMAL
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Map Type", tint = Color.Black)
                }

                // Traffic toggle
                FloatingActionButton(
                    onClick = { showTrafficLayer = !showTrafficLayer },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (showTrafficLayer) Color(0xFFFF5722) else Color.White
                ) {
                    Icon(
                        Icons.Default.Traffic,
                        contentDescription = "Traffic",
                        tint = if (showTrafficLayer) Color.White else Color.Black
                    )
                }
            }

            // Enhanced Bottom Info Card
            AnimatedVisibility(
                visible = showInfoCard && request != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Header with status and OTP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Status badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when (request?.status) {
                                        "Accepted" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        "Pending" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                        "Completed" -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                        else -> Color.LightGray.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            when (request?.status) {
                                                "Accepted" -> Icons.Default.CheckCircle
                                                "Pending" -> Icons.Default.Schedule
                                                "Completed" -> Icons.Default.Done
                                                else -> Icons.Default.Info
                                            },
                                            contentDescription = null,
                                            tint = when (request?.status) {
                                                "Accepted" -> Color(0xFF4CAF50)
                                                "Pending" -> Color(0xFFFF9800)
                                                "Completed" -> Color(0xFF2196F3)
                                                else -> Color.Gray
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (request?.status) {
                                                "Accepted" -> "গৃহীত"
                                                "Pending" -> "অপেক্ষমাণ"
                                                "Completed" -> "সম্পন্ন"
                                                else -> request?.status ?: "N/A"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = when (request?.status) {
                                                "Accepted" -> Color(0xFF4CAF50)
                                                "Pending" -> Color(0xFFFF9800)
                                                "Completed" -> Color(0xFF2196F3)
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                }

                                // OTP if available
                                request?.otp?.let { otp ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF2196F3).copy(alpha = 0.1f),
                                        modifier = Modifier.border(
                                            2.dp,
                                            Color(0xFF2196F3).copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFF2196F3),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "আপনার OTP কোড",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = otp,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF2196F3),
                                                    letterSpacing = 4.dp.value.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Expand/Collapse button
                            Column(horizontalAlignment = Alignment.End) {
                                IconButton(
                                    onClick = { showTripDetails = !showTripDetails }
                                ) {
                                    Icon(
                                        if (showTripDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = "Details",
                                        tint = Color(0xFF2196F3)
                                    )
                                }

                                IconButton(
                                    onClick = { showInfoCard = false }
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Minimize",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick Info Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickInfoCard(
                                icon = Icons.Default.DirectionsBus,
                                label = "বাস",
                                value = busName ?: "...",
                                color = Color(0xFF2196F3)
                            )

                            QuickInfoCard(
                                icon = Icons.Default.Person,
                                label = "কন্ডাক্টর",
                                value = conductorName?.take(10) ?: "...",
                                color = Color(0xFF4CAF50)
                            )

                            request?.fare?.let { fare ->
                                QuickInfoCard(
                                    icon = Icons.Default.AccountBalanceWallet,
                                    label = "ভাড়া",
                                    value = "৳$fare",
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }

                        // Expandable Trip Details
                        AnimatedVisibility(visible = showTripDetails) {
                            Column {
                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    "যাত্রা বিস্তারিত",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                request?.let { req ->
                                    EnhancedTripDetailRow(
                                        icon = Icons.Default.LocationOn,
                                        label = "পিকআপ পয়েন্ট",
                                        value = req.pickup,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    EnhancedTripDetailRow(
                                        icon = Icons.Default.Flag,
                                        label = "গন্তব্য",
                                        value = req.destination,
                                        color = Color(0xFFF44336)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    req.seats?.let { seats ->
                                        EnhancedTripDetailRow(
                                            icon = Icons.Default.EventSeat,
                                            label = "সিট সংখ্যা",
                                            value = "$seats টি",
                                            color = Color(0xFF9C27B0)
                                        )
                                    }
                                }
                            }
                        }

                        // Last Update Time
                        if (lastUpdateTime > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Update,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "শেষ আপডেট",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                                                .format(Date(lastUpdateTime)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating action button to show info when minimized
            AnimatedVisibility(
                visible = !showInfoCard && request != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                FloatingActionButton(
                    onClick = { showInfoCard = true },
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    ),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Show Info",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

// Enhanced Quick Info Card Component
@Composable
fun QuickInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// Enhanced Trip Detail Row Component
@Composable
fun EnhancedTripDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 2
            )
        }
    }
}

// Keep original TripDetailRow for backward compatibility
@Composable
fun TripDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}