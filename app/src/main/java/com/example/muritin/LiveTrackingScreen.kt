package com.example.muritin

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LiveTrackingScreen(
    navController: NavHostController,
    user: FirebaseUser,
    requestId: String
) {
    val scope = rememberCoroutineScope()
    var request by remember { mutableStateOf<Request?>(null) }
    var conductorLocation by remember { mutableStateOf<ConductorLocation?>(null) }
    var busName by remember { mutableStateOf<String?>(null) }
    var conductorName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(true) }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    val pickAndDestLLofBus = remember { mutableStateListOf<PointLocation>() }

    val cameraPositionState = rememberCameraPositionState()

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
                }
            }
        } catch (e: Exception) {
            Log.e("LiveTrackingScreen", "Refresh error: ${e.message}")
        } finally {
            isRefreshing = false
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "লোকেশন খুঁজছি...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomGesturesEnabled = true,
                    zoomControlsEnabled = false,
                    compassEnabled = true
                ),
                properties = MapProperties(isTrafficEnabled = true)
            ) {
                // Bus location marker
                conductorLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = LatLng(loc.lat, loc.lng)),
                        title = busName ?: "বাস",
                        snippet = "শেষ আপডেট: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(loc.timestamp))}",
                        icon = BitmapDescriptorFactory.fromResource(R.drawable.bus_marker_icon)
                    )
                }

                // Pickup location marker
                pickAndDestLLofBus.firstOrNull()?.let { pickup ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(pickup.latitude, pickup.longitude)
                        ),
                        title = "পিকআপ",
                        snippet = pickup.address,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }

                // Destination location marker
                pickAndDestLLofBus.lastOrNull()?.let { dest ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(dest.latitude, dest.longitude)
                        ),
                        title = "গন্তব্য",
                        snippet = dest.address,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
            }

            // Top Bar with gradient overlay
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        FilledIconButton(
                            onClick = { navController.navigateUp() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.9f)
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }

                        // Live indicator
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.shadow(4.dp, RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "LIVE ট্র্যাকিং",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                        }

                        // Refresh button
                        FilledIconButton(
                            onClick = { scope.launch { refreshLocation() } },
                            enabled = !isRefreshing,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.9f)
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
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

            // Bottom Info Card
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
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header with OTP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "আপনার ট্রিপ",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                request?.otp?.let { otp ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "OTP: $otp",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(onClick = { showInfoCard = false }) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Minimize"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Trip details with names instead of IDs
                        TripDetailRow(
                            icon = Icons.Default.DirectionsBus,
                            label = "বাস",
                            value = busName ?: "লোড হচ্ছে..."
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TripDetailRow(
                            icon = Icons.Default.Person,
                            label = "কন্ডাক্টর",
                            value = conductorName ?: "লোড হচ্ছে..."
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        request?.let { req ->
                            TripDetailRow(
                                icon = Icons.Default.LocationOn,
                                label = "পিকআপ",
                                value = req.pickup
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TripDetailRow(
                                icon = Icons.Default.Flag,
                                label = "গন্তব্য",
                                value = req.destination
                            )
                        }

                        if (lastUpdateTime > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
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
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "শেষ আপডেট: ${
                                            SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                                                .format(Date(lastUpdateTime))
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
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
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Show Info")
                }
            }

            // Zoom controls
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.zoomIn()
                            )
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.Black)
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.zoomOut()
                            )
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.Black)
                }
            }
        }
    }
}

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