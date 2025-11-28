package com.example.muritin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(23.8103, 90.4125), 12f) // Dhaka default
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
                return
            }

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
            delay(10_000) // 10 seconds
            if (!isRefreshing && activeSchedule != null) {
                refreshLocation()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বাস লাইভ লোকেশন") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { refreshLocation() } },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            "Refresh",
                            tint = if (isRefreshing)
                                MaterialTheme.colorScheme.outline
                            else
                                LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Bus Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = bus?.name ?: "N/A",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "নম্বর: ${bus?.number ?: "N/A"}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (activeSchedule != null) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "সক্রিয় শিডিউল",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "দিক: ${if (activeSchedule!!.direction == "going") "যাচ্ছি" else "ফিরছি"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "সময়: ${
                                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        .format(Date(activeSchedule!!.startTime))
                                } - ${
                                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        .format(Date(activeSchedule!!.endTime))
                                }",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (conductorLocation != null) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "শেষ আপডেট: ${
                                    SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                                        .format(Date(conductorLocation!!.timestamp))
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        if (isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }

                // Map
                if (error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error ?: "অজানা ত্রুটি",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else if (conductorLocation != null) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = false
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = false
                        )
                    ) {
                        // Bus location marker
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    conductorLocation!!.lat,
                                    conductorLocation!!.lng
                                )
                            ),
                            title = bus?.name ?: "Bus",
                            snippet = "শেষ আপডেট: ${
                                SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                                    .format(Date(conductorLocation!!.timestamp))
                            }"
                        )

                        // Route markers if available
                        bus?.route?.let { route ->
                            // Origin marker
                            route.originLoc?.let { origin ->
                                Marker(
                                    state = MarkerState(
                                        position = LatLng(origin.latitude, origin.longitude)
                                    ),
                                    title = "শুরু",
                                    snippet = origin.address
                                )
                            }

                            // Stop markers
                            route.stopPointsLoc.forEach { stop ->
                                Marker(
                                    state = MarkerState(
                                        position = LatLng(stop.latitude, stop.longitude)
                                    ),
                                    title = "স্টপ",
                                    snippet = stop.address
                                )
                            }

                            // Destination marker
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
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "লোকেশন ডেটা পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}