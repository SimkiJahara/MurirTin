package com.example.muritin

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ViewList
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.BackgroundLight
import com.example.muritin.ui.theme.Error
import com.example.muritin.ui.theme.Info
import com.example.muritin.ui.theme.Primary
import com.example.muritin.ui.theme.PrimaryLight
import com.example.muritin.ui.theme.RouteBlue
import com.example.muritin.ui.theme.RouteGreen
import com.example.muritin.ui.theme.RouteOrange
import com.example.muritin.ui.theme.RoutePurple
import com.example.muritin.ui.theme.TextPrimary
import com.example.muritin.ui.theme.TextSecondary
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorDashboard(
    navController: NavHostController,
    user: FirebaseUser,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database =
        FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
    var userData by remember { mutableStateOf<User?>(null) }
    val scrollState = rememberScrollState()
    var assignedBus by remember { mutableStateOf<Bus?>(null) }
    var pendingReqCount by remember { mutableStateOf(0) }
    var acceptedReqCount by remember { mutableStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var pendingRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var acceptedRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isInActiveSchedule by remember { mutableStateOf(false) }
    var locationCallback by remember { mutableStateOf<LocationCallback?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val apiKey = context.getString(R.string.map_api_key)

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val directionsApi = retrofit.create(DirectionsApi::class.java)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        Toast.makeText(context, "লোকেশন পাওয়া গেছে", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ConductorDashboard", "Location fetch failed: ${e.message}", e)
                }
            }
        }
    }

    // ── CHECK IF IN ACTIVE SCHEDULE ──────────────────────────────────────────────
    suspend fun checkActiveSchedule(): Boolean {
        return try {
            val now = System.currentTimeMillis()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val schedules = AuthRepository().getSchedulesForConductor(user.uid)
            val activeSchedule = schedules.firstOrNull {
                it.date == today && it.startTime <= now && it.endTime >= now
            }
            activeSchedule != null
        } catch (e: Exception) {
            Log.e("ConductorDashboard", "Check active schedule failed: ${e.message}", e)
            false
        }
    }

    // ── START LOCATION UPDATES ────────────────────────────────────────────────────
    fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // Update every 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // Fastest update: 5 seconds
            setMaxUpdateDelayMillis(15000L)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val newLocation = LatLng(location.latitude, location.longitude)
                    currentLocation = newLocation

                    // Update location in Firebase
                    scope.launch {
                        try {
                            AuthRepository().updateConductorLocation(user.uid, newLocation)
                            Log.d("ConductorDashboard", "Location updated: ${newLocation.latitude}, ${newLocation.longitude}")
                        } catch (e: Exception) {
                            Log.e("ConductorDashboard", "Failed to update location: ${e.message}", e)
                        }
                    }
                }
            }
        }

        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(locationRequest, callback, null)
        Log.d("ConductorDashboard", "Location updates started")
    }

    // ── STOP LOCATION UPDATES ─────────────────────────────────────────────────────
    fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
            Log.d("ConductorDashboard", "Location updates stopped")
        }
    }

    // ── REFRESH FUNCTION ──────────────────────────────────────────────────────────
    suspend fun refreshData() {
        isRefreshing = true
        try {
            val result = AuthRepository().getUser(user.uid)
            userData = result.getOrNull()

            val snapshot = database.getReference("busAssignments")
                .orderByChild("conductorId")
                .equalTo(user.uid)
                .get()
                .await()

            val busId = snapshot.children.firstOrNull()?.key
            if (busId != null) {
                assignedBus = AuthRepository().getBus(busId)
            } else {
                assignedBus = null
            }

            // Check if conductor is in active schedule
            val inActiveSchedule = checkActiveSchedule()

            if (inActiveSchedule != isInActiveSchedule) {
                isInActiveSchedule = inActiveSchedule

                if (inActiveSchedule) {
                    // Start location updates when schedule becomes active
                    startLocationUpdates()
                    Toast.makeText(context, "শিডিউল শুরু - লোকেশন ট্র্যাকিং চালু", Toast.LENGTH_SHORT).show()
                } else {
                    // Stop location updates when schedule ends
                    stopLocationUpdates()
                }
            }

            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    if (assignedBus != null && isInActiveSchedule) {
                        pendingRequests = AuthRepository().getPendingRequestsForConductor(
                            conductorId = user.uid,
                            currentLocation = currentLocation!!,
                            bus = assignedBus!!,
                            apiKey = apiKey,
                            directionsApi = directionsApi
                        )
                        pendingReqCount = pendingRequests.size
                        acceptedRequests = AuthRepository().getAcceptedRequestsForConductor(user.uid)
                        acceptedReqCount = acceptedRequests.size
                    } else {
                        pendingRequests = emptyList()
                    }
                }
            } else {
                pendingRequests = emptyList()
            }

            error = null
        } catch (e: Exception) {
            error = "রিফ্রেশ ব্যর্থ: ${e.message}"
            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
            Log.e("ConductorDashboard", "Refresh failed: ${e.message}", e)
        } finally {
            isRefreshing = false
        }
    }

    // ── INITIAL LOAD ──────────────────────────────────────────────────────────────
    LaunchedEffect(user.uid) {
        isLoading = true
        refreshData()
        isLoading = false
    }

    // ── AUTO-REFRESH EVERY 15 SECONDS ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000) // 15 seconds
            if (!isRefreshing) {
                refreshData()
            }
        }
    }

    // ── CLEANUP ON DISPOSE ────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            stopLocationUpdates()
        }
    }

    // UI Content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        if (isLoading) {
            // Loading State
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
        } else if (userData?.role != "Conductor") {
            // Unauthorized Access
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
                        modifier = Modifier.size(80.dp),
                        tint = Error
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "অননুমোদিত প্রবেশ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Error
                    )
                    Text(
                        "শুধুমাত্র কন্ডাক্টররা এই পৃষ্ঠা দেখতে পারেন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    LaunchedEffect(Unit) {
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
            }
        } else {
            // Main Dashboard Content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Section with Gradient Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Primary, PrimaryLight)
                            )
                        )
                        .padding(top = 40.dp, bottom = 100.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "কন্ডাক্টর ড্যাশবোর্ড",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = userData?.name ?: "কন্ডাক্টর",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { navController.navigate("show_account_info") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "প্রোফাইল",
                                    modifier = Modifier.size(32.dp),
                                    tint = Primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Location Tracking Status
                        if (isInActiveSchedule) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = RouteGreen.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.DirectionsBus,
                                        contentDescription = null,
                                        tint = RouteGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "সক্রিয় যাত্রা - লোকেশন ট্র্যাকিং চালু",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Quick Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OwnerStatCard(
                                icon = Icons.Outlined.DirectionsBus,
                                label = "মোট পেন্ডিং রিকোয়েস্ট",
                                value = pendingReqCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            OwnerStatCard(
                                icon = Icons.Outlined.Person,
                                label = "মোট অ্যাক্সেপ্ট করা রিকোয়েস্ট",
                                value = acceptedReqCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .offset(y = (-70).dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp)
                ) {
                    // Bus Management Card
                    ManagementSectionCard(
                        title = "পেন্ডিং রিকোয়েস্ট",
                        icon = Icons.Filled.People,
                        iconColor = RouteBlue
                    ) {
                        if (!isInActiveSchedule) {
                            // Not in active schedule
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "কোনো সক্রিয় শিডিউল নেই",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "যাত্রা শুরু করলে রিকোয়েস্ট দেখা যাবে",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else if (pendingRequests.isEmpty()) {
                            // Empty State
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "কোনো পেন্ডিং রিকোয়েস্ট নেই",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pendingRequests) { request ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Text("পিকআপ: ${request.pickup}", style = MaterialTheme.typography.bodyLarge)
                                            Text("গন্তব্য: ${request.destination}", style = MaterialTheme.typography.bodyLarge)
                                            Text("সিট: ${request.seats}", style = MaterialTheme.typography.bodyLarge)
                                            Text("ভাড়া: ${request.fare}", style = MaterialTheme.typography.bodyLarge)

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        val result = AuthRepository().acceptRequest(request.id, user.uid)
                                                        if (result.isSuccess) {
                                                            Toast.makeText(
                                                                context,
                                                                "অ্যাকসেপ্ট সফল",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            refreshData()
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "ব্যর্থ: ${result.exceptionOrNull()?.message}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = RouteBlue
                                                )
                                            ) {
                                                Text("অ্যাকসেপ্ট করুন")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Trip Management Card
                    ManagementSectionCard(
                        title = "যাত্রা ব্যবস্থাপনা",
                        icon = Icons.Filled.DirectionsBus,
                        iconColor = RoutePurple
                    ) {
                        ManagementActionButton(
                            icon = Icons.Outlined.PersonAdd,
                            title = "যাত্রার শিডিউল",
                            subtitle = "শিডিউল দেখুন এবং তৈরি করুন",
                            onClick = { navController.navigate("conductor_schedule_page") },
                            iconColor = RouteOrange
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ManagementActionButton(
                            icon = Icons.Outlined.Group,
                            title = "গৃহীত রিকোয়েস্ট দেখুন",
                            subtitle = "বোর্ডিং ও ড্রপ-অফ পরিচালনা",
                            onClick = { navController.navigate("conductor_accepted_requests") },
                            iconColor = RouteGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Additional Features
                    Text(
                        text = "আরও বৈশিষ্ট্য",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureCard(
                            icon = Icons.AutoMirrored.Outlined.Message,
                            title = "রাইডারের সাথে চ্যাট",
                            onClick = { navController.navigate("conductor_chat_list") },
                            modifier = Modifier.weight(1f),
                            iconColor = RoutePurple
                        )
                        FeatureCard(
                            icon = Icons.Outlined.DirectionsBus,
                            title = "আমার বাস",
                            onClick = { navController.navigate("conductor_assignedbus_info") },
                            modifier = Modifier.weight(1f),
                            iconColor = RouteGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureCard(
                            icon = Icons.AutoMirrored.Outlined.Help,
                            title = "সহায়তা",
                            onClick = { navController.navigate("conductor_help") },
                            modifier = Modifier.weight(1f),
                            iconColor = Info
                        )
                        FeatureCard(
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            title = "লগআউট",
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.weight(1f),
                            iconColor = Error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TEST BUTTON - For demonstration
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = RouteOrange.copy(alpha = 0.2f)
                        ),
                        onClick = { navController.navigate("trip_monitoring_test") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.DirectionsBus,
                                    contentDescription = null,
                                    tint = RouteOrange,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "🧪 Test Trip Monitoring",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RouteOrange
                                    )
                                    Text(
                                        "Simulate fare calculations & auto-completion",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = RouteOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        OwnerLogoutDialog(
            onConfirm = {
                Log.d("ConductorDashboard", "Logging out")
                stopLocationUpdates()
                Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
                onLogout()
                showLogoutDialog = false
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}