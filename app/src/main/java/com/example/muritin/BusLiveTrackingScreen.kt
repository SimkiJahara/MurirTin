package com.example.muritin

import android.annotation.SuppressLint
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    var bus by remember { mutableStateOf<Bus?>(null) }
    var activeSchedule by remember { mutableStateOf<Schedule?>(null) }
    var conductorLocation by remember { mutableStateOf<ConductorLocation?>(null) }
    var conductorName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showInfoCard by remember { mutableStateOf(true) }

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
                return
            }

            // Get conductor name
            val conductorResult = AuthRepository().getUser(activeSchedule!!.conductorId)
            conductorName = conductorResult.getOrNull()?.name

            // Get conductor location
            conductorLocation = AuthRepository().getConductorLocation(activeSchedule!!.conductorId)

            if (conductorLocation != null) {
                // Update camera to conductor location
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(conductorLocation!!.lat, conductorLocation!!.lng),
                    15f
                )
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
        // Map
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
                    shape = RoundedCornerShape(16.dp)
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
                            color = MaterialTheme.colorScheme.onErrorContainer
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
        } else if (conductorLocation != null) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
                )
            ) {
                // Bus location marker
                Marker(
                    state = MarkerState(
                        position = LatLng(conductorLocation!!.lat, conductorLocation!!.lng)
                    ),
                    title = bus?.name ?: "Bus",
                    snippet = "কন্ডাক্টর: ${conductorName ?: "N/A"}\nশেষ আপডেট: ${
                        SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                            .format(Date(conductorLocation!!.timestamp))
                    }"
                )

                // Route markers
                bus?.route?.let { route ->
                    route.originLoc?.let { origin ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(origin.latitude, origin.longitude)
                            ),
                            title = "শুরু",
                            snippet = origin.address
                        )
                    }

                    route.stopPointsLoc.forEach { stop ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(stop.latitude, stop.longitude)
                            ),
                            title = "স্টপ",
                            snippet = stop.address
                        )
                    }

                    route.destinationLoc?.let { dest ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(dest.latitude, dest.longitude)
                            ),
                            title = "শেষ",
                            snippet = dest.address
                        )
                    }
                }
            }
        }

        // Top Bar
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
                    if (activeSchedule != null && conductorLocation != null && error == null) {
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
                                    "LIVE",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
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
            visible = showInfoCard && !isLoading && error == null,
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
                    // Header
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "নম্বর: ${bus?.number ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showInfoCard = false }
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize"
                            )
                        }
                    }

                    if (activeSchedule != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Conductor name
                        InfoChip(
                            icon = Icons.Default.Person,
                            label = "কন্ডাক্টর",
                            value = conductorName ?: "লোড হচ্ছে..."
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Schedule Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoChip(
                                icon = Icons.Default.LocationOn,
                                label = "দিক",
                                value = if (activeSchedule!!.direction == "going") "যাচ্ছি" else "ফিরছি"
                            )

                            InfoChip(
                                icon = Icons.Default.AccessTime,
                                label = "সময়",
                                value = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    .format(Date(activeSchedule!!.startTime))
                            )
                        }
                    }

                    if (conductorLocation != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "শেষ আপডেট: ${
                                        SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                                            .format(Date(conductorLocation!!.timestamp))
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

        // Floating action button to show info card when minimized
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
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Info, contentDescription = "Show Info")
            }
        }

        // Zoom controls
        if (!isLoading && error == null) {
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