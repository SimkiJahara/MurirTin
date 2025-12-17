package com.example.muritin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    suspend fun fetchRequests() {
        try {
            Log.d("MyRequestsScreen", "Fetching requests for user ${user.uid}")
            // getRequestsForUser now already filters out completed trips
            requests = AuthRepository().getRequestsForUser(user.uid)
            error = null
        } catch (e: Exception) {
            error = "রিকোয়েস্ট পুনরধারে ত্রুটি: ${e.message}"
            Log.e("MyRequestsScreen", "Fetch failed: ${e.message}", e)
        }
    }

    // Initial load
    LaunchedEffect(user.uid) {
        isLoading = true
        fetchRequests()
        isLoading = false
    }

    // Auto-refresh every 15 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            if (!isRefreshing && !isLoading) {
                isRefreshing = true
                fetchRequests()
                isRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "আমার রিকোয়েস্ট",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                fetchRequests()
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "রিফ্রেশ",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "লোড হচ্ছে...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                error ?: "অজানা ত্রুটি",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        fetchRequests()
                                        isLoading = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("পুনরায় চেষ্টা করুন")
                            }
                        }
                    }
                }

                requests.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "কোনো রিকোয়েস্ট নেই",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "নতুন যাত্রার জন্য রিকোয়েস্ট পাঠান",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            if (isRefreshing) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        items(requests) { request ->
                            RequestCard(
                                request = request,
                                onRefresh = {
                                    scope.launch {
                                        isRefreshing = true
                                        fetchRequests()
                                        isRefreshing = false
                                    }
                                },
                                onOpenChat = { requestId ->
                                    navController.navigate("chat/$requestId")
                                },
                                onNavigateToTracking = { requestId ->
                                    navController.navigate("live_tracking/$requestId")
                                },
                                user = user
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RequestCard(
    request: Request,
    onRefresh: () -> Unit,
    onOpenChat: (String) -> Unit,
    onNavigateToTracking: (String) -> Unit,
    user: FirebaseUser
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCancelling by remember { mutableStateOf(false) }
    var conductorData by remember { mutableStateOf<User?>(null) }
    var isLoadingConductor by remember { mutableStateOf(false) }

    // Load conductor data when request is accepted
    LaunchedEffect(request.conductorId, request.status) {
        if (request.status == "Accepted" && request.conductorId.isNotEmpty() && conductorData == null) {
            isLoadingConductor = true
            try {
                val result = AuthRepository().getUser(request.conductorId)
                conductorData = result.getOrNull()
            } catch (e: Exception) {
                Log.e("RequestCard", "Failed to load conductor: ${e.message}")
            }
            isLoadingConductor = false
        }
    }

    // AUTO-START MONITORING when rider boards the bus
    // AUTO-START MONITORING when rider boards the bus
    LaunchedEffect(request.rideStatus?.inBusTravelling) {
        if (request.rideStatus?.inBusTravelling == true &&
            request.rideStatus.tripCompleted != true) {

            // ✅ CHECK PERMISSIONS BEFORE STARTING SERVICE
            if (!context.hasLocationPermissions()) {
                Log.w("RequestCard", "Location permissions not granted, cannot start monitoring")
                Toast.makeText(
                    context,
                    "যাত্রা নিরীক্ষণের জন্য লোকেশন অনুমতি প্রয়োজন",
                    Toast.LENGTH_LONG
                ).show()
                return@LaunchedEffect
            }

            Log.d("RequestCard", "Starting automatic trip monitoring for ${request.id}")
            try {
                TripMonitoringService.startMonitoring(context, request.id)

                Toast.makeText(
                    context,
                    "স্বয়ংক্রিয় যাত্রা পর্যবেক্ষণ শুরু হয়েছে",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: SecurityException) {
                Log.e("RequestCard", "SecurityException starting monitoring: ${e.message}", e)
                Toast.makeText(
                    context,
                    "যাত্রা নিরীক্ষণ শুরু করতে ব্যর্থ: অনুমতি প্রয়োজন",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("RequestCard", "Error starting monitoring: ${e.message}", e)
                Toast.makeText(
                    context,
                    "যাত্রা নিরীক্ষণ শুরু করতে ত্রুটি",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // AUTO-STOP MONITORING when trip is completed
    LaunchedEffect(request.rideStatus?.tripCompleted) {
        if (request.rideStatus?.tripCompleted == true) {
            Log.d("RequestCard", "Stopping trip monitoring for ${request.id}")
            TripMonitoringService.stopMonitoring(context)
        }
    }

    // Cleanup monitoring if card is removed
    DisposableEffect(Unit) {
        onDispose {
            if (request.rideStatus?.inBusTravelling == true &&
                request.rideStatus.tripCompleted != true) {
                // Don't stop here - let the service complete naturally
            }
        }
    }

    // Determine status color and text
    val statusColor = when {
        request.status == "Completed" -> Color(0xFF4CAF50)
        request.rideStatus?.inBusTravelling == true -> Color(0xFF2196F3)
        request.status == "Accepted" -> RouteGreen
        request.status == "Pending" -> RouteOrange
        else -> Error
    }

    val statusText = when {
        request.status == "Completed" -> "সম্পন্ন"
        request.rideStatus?.inBusTravelling == true -> "বাসে ভ্রমণরত (স্বয়ংক্রিয় নিরীক্ষণ)"
        request.rideStatus?.otpVerified == true -> "OTP যাচাইকৃত"
        request.status == "Accepted" -> "গৃহীত"
        request.status == "Pending" -> "অপেক্ষমান (৩ মিনিটের মধ্যে রিকোয়েস্ট অ্যাক্সেপ্ট করা না হলে রিকোয়েস্ট টি মুছে যাবে)"
        request.status == "Cancelled" -> "বাতিল"
        else -> request.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        // Show auto-monitoring indicator
                        if (request.rideStatus?.inBusTravelling == true &&
                            request.rideStatus.tripCompleted != true) {
                            Text(
                                "স্বয়ংক্রিয় নিয়ন্ত্রণ সক্রিয়",
                                style = MaterialTheme.typography.bodySmall,
                                color = RouteBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Text(
                    text = formatTimestamp(request.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fare Display
            CompactFareDisplay(request = request)

            Spacer(modifier = Modifier.height(16.dp))

            // Ride Status Indicator (if travelling)
            if (request.rideStatus?.inBusTravelling == true) {
                RideStatusCard(request.rideStatus)
                Spacer(modifier = Modifier.height(12.dp))

                // ✅ NEW: Live Distance Display
                LiveDistanceDisplay(request = request)
                Spacer(modifier = Modifier.height(12.dp))

                // Auto-monitoring info card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RouteBlue.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = RouteBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "স্বয়ংক্রিয় পর্যবেক্ষণ চলছে",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = RouteBlue
                            )
                            Text(
                                "আপনি গন্তব্যে পৌঁছালে বা রুট পরিবর্তন করলে স্বয়ংক্রিয়ভাবে আপডেট হবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Route details
            TripRouteRow(
                icon = Icons.Filled.TripOrigin,
                label = "পিকআপ স্থান",
                value = request.pickup,
                iconColor = RouteGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            TripRouteRow(
                icon = Icons.Filled.LocationOn,
                label = "গন্তব্য স্থান",
                value = request.destination,
                iconColor = Error
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Conductor Info Card (if accepted)
            if (request.status == "Accepted" && request.conductorId.isNotEmpty()) {
                ConductorInfoCard(
                    conductorData = conductorData,
                    isLoading = isLoadingConductor
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Trip details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TripDetailChip(
                    icon = Icons.Filled.EventSeat,
                    label = "আসন",
                    value = "${request.seats}",
                    modifier = Modifier.weight(1f),
                    color = RouteBlue
                )

                TripDetailChip(
                    icon = Icons.Filled.Payment,
                    label = "ভাড়া",
                    value = if (request.rideStatus?.actualFare != null && request.rideStatus.actualFare > 0) {
                        "৳${request.rideStatus.actualFare}"
                    } else {
                        "৳${request.fare}"
                    },
                    modifier = Modifier.weight(1f),
                    color = RouteOrange
                )
            }

            // Show actual fare update message if changed
            if (request.rideStatus?.actualFare != null &&
                request.rideStatus.actualFare != request.fare &&
                request.rideStatus.actualFare > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RouteOrange.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = RouteOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "ভাড়া স্বয়ংক্রিয়ভাবে আপডেট হয়েছে",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = RouteOrange
                            )
                            Text(
                                "মূল: ৳${request.fare} → নতুন: ৳${request.rideStatus.actualFare}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // OTP Display (if accepted but not boarded)
            if (request.status == "Accepted" &&
                request.rideStatus?.inBusTravelling != true &&
                request.otp != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RouteGreen.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "আপনার OTP",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                request.otp,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = RouteGreen
                            )
                            Text(
                                "কন্ডাক্টরকে দেখান",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = RouteGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(16.dp))

            // Monitoring status (if travelling)
            if (request.rideStatus?.inBusTravelling == true &&
                request.rideStatus.tripCompleted != true) {

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.AutoMode,
                            contentDescription = null,
                            tint = RouteGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "স্বয়ংক্রিয় পর্যবেক্ষণ সক্রিয়",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RouteGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "আপনি গন্তব্যে পৌঁছালে স্বয়ংক্রিয়ভাবে যাত্রা সম্পূর্ণ হবে। কোন ক্লিক করার প্রয়োজন নেই।",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Chat and Location buttons (if accepted and not completed)
            val isChatEnabled = request.status == "Accepted" &&
                    request.scheduleId != null &&
                    request.rideStatus?.tripCompleted != true

            if (request.status == "Accepted" && request.rideStatus?.tripCompleted != true) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onNavigateToTracking(request.id)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RouteBlue
                        )
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("অবস্থান")
                    }

                    Button(
                        onClick = {
                            if (isChatEnabled) {
                                onOpenChat(request.id)
                            } else {
                                Toast.makeText(
                                    context,
                                    "চ্যাট শুধুমাত্র সক্রিয় ট্রিপের জন্য উপলব্ধ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isChatEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isChatEnabled) RouteGreen else TextSecondary
                        )
                    ) {
                        Icon(
                            Icons.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("চ্যাট")
                    }
                }
            }

            // Cancel Button for Pending
            if (request.status == "Pending") {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!isCancelling) {
                            isCancelling = true
                            scope.launch {
                                val result = AuthRepository().cancelTripRequest(request.id, user.uid)
                                isCancelling = false
                                if (result.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        "রিকোয়েস্ট বাতিল করা হয়েছে",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onRefresh()
                                } else {
                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()?.message ?: "বাতিল ব্যর্থ",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCancelling,
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("রিকোয়েস্ট বাতিল করুন")
                    }
                }
            }
        }
    }
}


@Composable
fun LiveDistanceDisplay(
    request: Request,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var distanceToDestination by remember { mutableStateOf<Double?>(null) }
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Location callback for real-time updates
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLocation = location

                    // Calculate distance to destination
                    request.destinationLatLng?.let { dest ->
                        val distance = calculateDistanceInKm(
                            location.latitude,
                            location.longitude,
                            dest.lat,
                            dest.lng
                        )
                        distanceToDestination = distance

                        Log.d("LiveDistance", "Distance to ${request.destination}: ${String.format("%.2f", distance)}km")
                    }
                }
            }
        }
    }

    // Start/stop location updates
    DisposableEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000 // Update every 5 seconds
            ).apply {
                setMinUpdateIntervalMillis(3000)
            }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Display card
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                distanceToDestination == null -> Color(0xFFFFF3E0)
                distanceToDestination!! < 0.1 -> Color(0xFFE8F5E9) // Green - Very close (<100m)
                distanceToDestination!! < 0.5 -> Color(0xFFFFF9C4) // Yellow - Close (<500m)
                else -> Color(0xFFE3F2FD) // Blue - Far
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Icon and destination
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.NearMe,
                    contentDescription = null,
                    tint = when {
                        distanceToDestination == null -> RouteOrange
                        distanceToDestination!! < 0.1 -> RouteGreen
                        distanceToDestination!! < 0.5 -> RouteOrange
                        else -> RouteBlue
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "গন্তব্য পর্যন্ত দূরত্ব",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        request.destination,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right side - Distance number
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (distanceToDestination != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = when {
                                distanceToDestination!! < 0.1 -> {
                                    // Show in meters when less than 100m
                                    "${(distanceToDestination!! * 1000).toInt()}"
                                }
                                distanceToDestination!! < 1.0 -> {
                                    // Show 1 decimal for 100m-1km
                                    String.format("%.1f", distanceToDestination!!)
                                }
                                else -> {
                                    // Show 2 decimals for 1km+
                                    String.format("%.2f", distanceToDestination!!)
                                }
                            },
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                distanceToDestination!! < 0.1 -> RouteGreen
                                distanceToDestination!! < 0.5 -> RouteOrange
                                else -> RouteBlue
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (distanceToDestination!! < 0.1) "মি" else "কিমি",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }

                    // Status text
                    Text(
                        text = when {
                            distanceToDestination!! < 0.1 -> "খুব কাছে! 🎯"
                            distanceToDestination!! < 0.5 -> "প্রায় পৌঁছেছেন"
                            distanceToDestination!! < 2.0 -> "আসছেন"
                            else -> "চলছেন"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            distanceToDestination!! < 0.1 -> RouteGreen
                            distanceToDestination!! < 0.5 -> RouteOrange
                            else -> TextSecondary
                        },
                        fontWeight = if (distanceToDestination!! < 0.1) FontWeight.Bold else FontWeight.Normal
                    )
                } else {
                    // Loading state
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = RouteBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "হিসাব করা হচ্ছে...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    // Arrival alert when very close
    if (distanceToDestination != null && distanceToDestination!! < 0.15) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = RouteGreen.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = RouteGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "আপনি গন্তব্যে পৌঁছে গেছেন!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = RouteGreen
                    )
                    Text(
                        "যাত্রা স্বয়ংক্রিয়ভাবে সম্পূর্ণ হবে",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
// ============================================
// PART 4: Supporting Components
// ============================================

@Composable
fun ConductorInfoCard(
    conductorData: User?,
    isLoading: Boolean
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "কন্ডাক্টর তথ্য লোড হচ্ছে...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else if (conductorData != null) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "কন্ডাক্টর",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            conductorData.name?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                        }
                    }

                    // Call button
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${conductorData.phone}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "কল করতে ব্যর্থ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Primary, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = "কল করুন",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Phone,
                        contentDescription = null,
                        tint = Primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    conductorData.phone?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "কন্ডাক্টর তথ্য পাওয়া যায়নি",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun RideStatusCard(rideStatus: RideStatus) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.DirectionsBus,
                    contentDescription = null,
                    tint = RouteBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "বাসে ভ্রমণরত",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RouteBlue
                    )
                    Text(
                        "আপনি ${SimpleDateFormat("h:mm a", Locale.US).format(Date(rideStatus.boardedAt))} বাসে উঠেছেন",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Early/Late exit status
            if (rideStatus.earlyExitRequested) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = RouteOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "আগাম নামার অনুরোধ করা হয়েছে: ${rideStatus.earlyExitStop}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            if (rideStatus.lateExitRequested) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = RouteOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "পরে নামার অনুরোধ করা হয়েছে: ${rideStatus.lateExitStop}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun TripRouteRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun TripDetailChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

// Helper function
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "এইমাত্র"
        diff < 3600000 -> "${diff / 60000} মিনিট আগে"
        diff < 86400000 -> SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(timestamp))
    }
}

// Helper function for distance calculation
private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}