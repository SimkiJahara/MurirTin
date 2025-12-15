

package com.example.muritin

import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.dp
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

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusLiveTrackingScreen(
    navController: NavHostController,
    user: FirebaseUser,
    busId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State variables
    var bus by remember { mutableStateOf<Bus?>(null) }
    var activeSchedule by remember { mutableStateOf<Schedule?>(null) }
    var conductorLocation by remember { mutableStateOf<ConductorLocation?>(null) }
    var conductorName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(true) }
    var showRouteDetails by remember { mutableStateOf(false) }
    var selectedMapType by remember { mutableStateOf(MapType.NORMAL) }
    var showTrafficLayer by remember { mutableStateOf(false) }
    var routePolyline by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var estimatedSpeed by remember { mutableStateOf(0.0) }
    var distanceTraveled by remember { mutableStateOf(0.0) }
    var previousLocation by remember { mutableStateOf<ConductorLocation?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(23.8103, 90.4125), 12f)
    }

    // Pulsing animation for live indicator
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

    // Rotation animation for bus icon
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Calculate speed and distance
    fun calculateMetrics(current: ConductorLocation, previous: ConductorLocation?) {
        if (previous != null) {
            val distance = calculateDistances(
                previous.lat, previous.lng,
                current.lat, current.lng
            )
            val timeDiff = (current.timestamp - previous.timestamp) / 1000.0 / 3600.0 // hours

            if (timeDiff > 0) {
                estimatedSpeed = distance / timeDiff // km/h
                distanceTraveled += distance
            }
        }
    }

    // Function to refresh location data
    suspend fun refreshLocation() {
        try {
            isRefreshing = true

            // Get bus details
            bus = AuthRepository().getBus(busId)

            if (bus == null) {
                error = "বাস পাওয়া যায়নি"
                return
            }

            // Get active schedule
            val schedules = AuthRepository().getSchedulesForBus(busId)
            val now = System.currentTimeMillis()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

            activeSchedule = schedules.firstOrNull {
                it.date == today && it.startTime <= now && it.endTime >= now
            }

            if (activeSchedule == null) {
                error = "কোনো সক্রিয় শিডিউল নেই"
                conductorLocation = null
                conductorName = null
                routePolyline = emptyList()
                return
            }

            // Get conductor name
            val conductorResult = AuthRepository().getUser(activeSchedule!!.conductorId)
            conductorName = conductorResult.getOrNull()?.name

            // Store previous location
            previousLocation = conductorLocation

            // Get conductor location
            val newLocation = AuthRepository().getConductorLocation(activeSchedule!!.conductorId)

            if (newLocation != null) {
                conductorLocation = newLocation

                // Calculate metrics
                calculateMetrics(newLocation, previousLocation)

                // Update camera to conductor location
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(newLocation.lat, newLocation.lng),
                    15f
                )

                // Build route polyline from schedule
                activeSchedule?.tripRoute?.let { route ->
                    val points = mutableListOf<LatLng>()

                    route.originLoc?.let {
                        points.add(LatLng(it.latitude, it.longitude))
                    }

                    route.stopPointsLoc.forEach {
                        points.add(LatLng(it.latitude, it.longitude))
                    }

                    route.destinationLoc?.let {
                        points.add(LatLng(it.latitude, it.longitude))
                    }

                    routePolyline = points
                }

                error = null
            } else {
                error = "কন্ডাক্টরের লোকেশন পাওয়া যায়নি"
            }
        } catch (e: Exception) {
            error = "লোকেশন লোড ব্যর্থ: ${e.message}"
            Log.e("BusLiveTracking", "Error: ${e.message}", e)
        } finally {
            isRefreshing = false
            isLoading = false
        }
    }

    // Function to center on bus location
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

    // Function to show entire route
    fun showFullRoute() {
        if (routePolyline.isNotEmpty()) {
            scope.launch {
                val bounds = LatLngBounds.builder().apply {
                    routePolyline.forEach { include(it) }
                    conductorLocation?.let { include(LatLng(it.lat, it.lng)) }
                }.build()

                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                )
            }
        }
    }

    // Initial load
    LaunchedEffect(busId) {
        refreshLocation()
    }

    // Auto-refresh every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            if (!isRefreshing && activeSchedule != null) {
                refreshLocation()
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        // Map Display
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "লোকেশন খুঁজছি...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "অজানা ত্রুটি",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { scope.launch { refreshLocation() } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("আবার চেষ্টা করুন")
                        }
                    }
                }
            }
        } else {
            // Google Map with Enhanced Features
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = selectedMapType,
                    isTrafficEnabled = showTrafficLayer,
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                )
            ) {
                // Draw route polyline with gradient
                if (routePolyline.isNotEmpty()) {
                    Polyline(
                        points = routePolyline,
                        color = Color(0xFF2196F3),
                        width = 12f,
                        geodesic = true,
                        pattern = listOf(
                            Dot(),
                            Gap(10f)
                        )
                    )

                    // Add markers for stops
                    activeSchedule?.tripRoute?.let { route ->
                        // Origin marker
                        route.originLoc?.let { origin ->
                            Marker(
                                state = MarkerState(position = LatLng(origin.latitude, origin.longitude)),
                                title = "শুরু: ${origin.address}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }

                        // Stop markers
                        route.stopPointsLoc.forEachIndexed { index, stop ->
                            Marker(
                                state = MarkerState(position = LatLng(stop.latitude, stop.longitude)),
                                title = "স্টপ ${index + 1}: ${stop.address}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                            )
                        }

                        // Destination marker
                        route.destinationLoc?.let { dest ->
                            Marker(
                                state = MarkerState(position = LatLng(dest.latitude, dest.longitude)),
                                title = "শেষ: ${dest.address}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                    }
                }

                // Bus location marker with custom design
                conductorLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = LatLng(location.lat, location.lng)),
                        title = "${bus?.name ?: "বাস"}",
                        snippet = "কন্ডাক্টর: ${conductorName ?: "N/A"}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )

                    // Pulsing circle around bus
                    Circle(
                        center = LatLng(location.lat, location.lng),
                        radius = 50.0,
                        fillColor = Color(0x332196F3),
                        strokeColor = Color(0xFF2196F3).copy(alpha = pulseAlpha),
                        strokeWidth = 3f
                    )
                }
            }
        }

        // Top App Bar with gradient
        if (!isLoading && error == null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        IconButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White, CircleShape)
                                .shadow(4.dp, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }

                        // Live indicator
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            modifier = Modifier.shadow(4.dp, RoundedCornerShape(24.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color.Red.copy(alpha = pulseAlpha), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "সরাসরি সম্প্রচার",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                        }

                        // Refresh button
                        IconButton(
                            onClick = { scope.launch { refreshLocation() } },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White, CircleShape)
                                .shadow(4.dp, CircleShape),
                            enabled = !isRefreshing
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
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
                }
            }
        }

        // Control Panel (Right side)
        if (!isLoading && error == null) {
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
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Center on Bus",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Show full route
                FloatingActionButton(
                    onClick = { showFullRoute() },
                    modifier = Modifier.size(56.dp),
                    containerColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = "Show Full Route",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                }

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
        }


        AnimatedVisibility(
            visible = showInfoCard && !isLoading && error == null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bus?.name ?: "N/A",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                            Text(
                                text = "নম্বর: ${bus?.number ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }

                        Row {
                            // Route details button
                            IconButton(
                                onClick = { showRouteDetails = !showRouteDetails }
                            ) {
                                Icon(
                                    if (showRouteDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Route Details",
                                    tint = Color(0xFF2196F3)
                                )
                            }

                            // Minimize button
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

                    if (activeSchedule != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Speed and Distance Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MetricCard(
                                icon = Icons.Default.Speed,
                                label = "গতি",
                                value = String.format("%.1f km/h", estimatedSpeed),
                                color = Color(0xFF4CAF50)
                            )

                            MetricCard(
                                icon = Icons.Default.Route,
                                label = "দূরত্ব",
                                value = String.format("%.2f km", distanceTraveled),
                                color = Color(0xFFFF9800)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Conductor Info
                        EnhancedInfoChip(
                            icon = Icons.Default.Person,
                            label = "কন্ডাক্টর",
                            value = conductorName ?: "লোড হচ্ছে...",
                            backgroundColor = Color(0xFFE3F2FD)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Schedule Info Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EnhancedInfoChip(
                                icon = Icons.Default.LocationOn,
                                label = "দিক",
                                value = if (activeSchedule!!.direction == "going") "যাচ্ছি" else "ফিরছি",
                                backgroundColor = Color(0xFFFFF3E0),
                                modifier = Modifier.weight(1f)
                            )

                            EnhancedInfoChip(
                                icon = Icons.Default.AccessTime,
                                label = "সময়",
                                value = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(Date(activeSchedule!!.startTime)),
                                backgroundColor = Color(0xFFF3E5F5),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Route Details Section
                        AnimatedVisibility(visible = showRouteDetails) {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "রুট বিস্তারিত",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                activeSchedule?.tripRoute?.let { route ->
                                    // Origin
                                    route.originLoc?.let { origin ->
                                        RouteStopItem(
                                            title = "শুরু",
                                            address = origin.address,
                                            icon = Icons.Default.PlayCircle,
                                            color = Color(0xFF4CAF50),
                                            isFirst = true
                                        )
                                    }

                                    // Stops
                                    route.stopPointsLoc.forEachIndexed { index, stop ->
                                        RouteStopItem(
                                            title = "স্টপ ${index + 1}",
                                            address = stop.address,
                                            icon = Icons.Default.LocationOn,
                                            color = Color(0xFFFF9800)
                                        )
                                    }

                                    // Destination
                                    route.destinationLoc?.let { dest ->
                                        RouteStopItem(
                                            title = "শেষ",
                                            address = dest.address,
                                            icon = Icons.Default.Flag,
                                            color = Color(0xFFF44336),
                                            isLast = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (conductorLocation != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Last Update Info
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
                                            .format(Date(conductorLocation!!.timestamp)),
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

        // Floating Action Button to show info card when minimized
        AnimatedVisibility(
            visible = !showInfoCard && !isLoading && error == null,
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

// Helper function to calculate distance between two points
fun calculateDistances(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371 // Earth's radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

// Enhanced Info Chip Component
@Composable
fun EnhancedInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    backgroundColor: Color = Color(0xFFF5F5F5),
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

// Metric Card Component
@Composable
fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// Route Stop Item Component
@Composable
fun RouteStopItem(
    title: String,
    address: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon with connecting line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(Color.LightGray)
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape)
                    .border(2.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Stop info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 2
            )
        }
    }
}

// Keep the original InfoChip for backward compatibility
@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}