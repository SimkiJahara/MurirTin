package com.example.muritin

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
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
    var showExitOptions by remember { mutableStateOf(false) }
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
        request.rideStatus?.inBusTravelling == true -> "বাসে ভ্রমণরত"
        request.rideStatus?.otpVerified == true -> "OTP যাচাইকৃত"
        request.status == "Accepted" -> "গৃহীত"
        request.status == "Pending" -> "অপেক্ষমান (৩ মিনিটের মধ্যে রিকোয়েস্ট অ্যাক্সেপ্ট করা না হলে রিকোয়েস্ট টি মুছে যাবে)"
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
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Text(
                    text = formatTimestamp(request.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ride Status Indicator (if travelling)
            if (request.rideStatus?.inBusTravelling == true) {
                RideStatusCard(request.rideStatus)
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
                                "ভাড়া আপডেট হয়েছে",
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

            // Travelling Actions
            if (request.rideStatus?.inBusTravelling == true &&
                request.rideStatus.tripCompleted != true) {

                Column {
                    // Exit Options Button
                    if (!request.rideStatus.riderArrivedConfirmed &&
                        !request.rideStatus.earlyExitRequested &&
                        !request.rideStatus.lateExitRequested) {
                        Button(
                            onClick = { showExitOptions = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RouteOrange
                            )
                        ) {
                            Icon(
                                Icons.Filled.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("নামার অপশন")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Confirm Arrival Button
                    if (!request.rideStatus.riderArrivedConfirmed) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = AuthRepository().confirmRiderArrival(
                                        request.id,
                                        user.uid
                                    )
                                    if (result.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "আপনি পৌঁছেছেন নিশ্চিত করা হয়েছে",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onRefresh()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            result.exceptionOrNull()?.message ?: "ব্যর্থ",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RouteGreen
                            )
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("আমি পৌঁছেছি নিশ্চিত করুন")
                        }
                    } else if (!request.rideStatus.tripCompleted) {
                        // Show waiting for conductor confirmation
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = RouteBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "কন্ডাক্টর নিশ্চিত করার জন্য অপেক্ষা করছে...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
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
                            // Navigate to LiveTrackingScreen
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

    // Exit Options Dialog
    if (showExitOptions) {
        ExitOptionsDialog(
            request = request,
            onDismiss = { showExitOptions = false },
            onEarlyExit = { exitStop, exitLatLng ->
                scope.launch {
                    val result = AuthRepository().requestEarlyExit(
                        request.id,
                        user.uid,
                        exitStop,
                        exitLatLng
                    )
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            "আগাম নামার অনুরোধ পাঠানো হয়েছে",
                            Toast.LENGTH_SHORT
                        ).show()
                        showExitOptions = false
                        onRefresh()
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "ব্যর্থ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onLateExit = { exitStop, exitLatLng ->
                scope.launch {
                    val result = AuthRepository().requestLateExit(
                        request.id,
                        user.uid,
                        exitStop,
                        exitLatLng
                    )
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            "পরে নামার অনুরোধ পাঠানো হয়েছে",
                            Toast.LENGTH_SHORT
                        ).show()
                        showExitOptions = false
                        onRefresh()
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "ব্যর্থ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }
}

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
fun ExitOptionsDialog(
    request: Request,
    onDismiss: () -> Unit,
    onEarlyExit: (String, LatLngData) -> Unit,
    onLateExit: (String, LatLngData) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var selectedStop by remember { mutableStateOf<PointLocation?>(null) }
    var availableStops by remember { mutableStateOf<List<PointLocation>>(emptyList()) }
    var isLoadingStops by remember { mutableStateOf(false) }
    var estimatedFare by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    // Load available stops when option is selected
    LaunchedEffect(selectedOption, request.busId) {
        if (selectedOption != null && request.busId != null) {
            isLoadingStops = true
            try {
                val bus = AuthRepository().getBus(request.busId)
                val route = bus?.route

                if (route != null) {
                    val stops = mutableListOf<PointLocation>()

                    // Get pickup and destination indices
                    val pickupGeoHash = request.pickupLatLng?.let {
                        GeoFireUtils.getGeoHashForLocation(
                            GeoLocation(it.lat, it.lng), 5
                        )
                    }
                    val destGeoHash = request.destinationLatLng?.let {
                        GeoFireUtils.getGeoHashForLocation(
                            GeoLocation(it.lat, it.lng), 5
                        )
                    }

                    // Find pickup index in route
                    var pickupIndex = -1
                    if (route.originLoc?.geohash == pickupGeoHash) {
                        pickupIndex = -1 // Before all stops
                    } else {
                        route.stopPointsLoc.forEachIndexed { index, stop ->
                            if (stop.geohash == pickupGeoHash) {
                                pickupIndex = index
                            }
                        }
                    }

                    // Find destination index
                    var destIndex = -1
                    route.stopPointsLoc.forEachIndexed { index, stop ->
                        if (stop.geohash == destGeoHash) {
                            destIndex = index
                        }
                    }
                    if (route.destinationLoc?.geohash == destGeoHash) {
                        destIndex = route.stopPointsLoc.size
                    }

                    when (selectedOption) {
                        "early" -> {
                            // Show stops between pickup and destination
                            if (pickupIndex == -1) {
                                // Pickup is origin, show all stops before destination
                                route.stopPointsLoc.forEachIndexed { index, stop ->
                                    if (index < destIndex) {
                                        stops.add(stop)
                                    }
                                }
                            } else {
                                // Show stops between pickup and destination
                                route.stopPointsLoc.forEachIndexed { index, stop ->
                                    if (index > pickupIndex && index < destIndex) {
                                        stops.add(stop)
                                    }
                                }
                            }
                        }
                        "late" -> {
                            // Show stops after destination
                            if (destIndex < route.stopPointsLoc.size) {
                                route.stopPointsLoc.forEachIndexed { index, stop ->
                                    if (index > destIndex) {
                                        stops.add(stop)
                                    }
                                }
                                // Add final destination if not already there
                                route.destinationLoc?.let { stops.add(it) }
                            }
                        }
                    }

                    availableStops = stops
                }
            } catch (e: Exception) {
                Log.e("ExitOptionsDialog", "Failed to load stops: ${e.message}")
            }
            isLoadingStops = false
        }
    }

    // Calculate estimated fare when stop is selected
    LaunchedEffect(selectedStop, request.pickupLatLng) {
        if (selectedStop != null && request.pickupLatLng != null) {
            try {
                val pickupLat = request.pickupLatLng.lat
                val pickupLng = request.pickupLatLng.lng
                val stopLat = selectedStop!!.latitude
                val stopLng = selectedStop!!.longitude

                // Calculate distance using haversine formula
                val distance = calculateDistance(pickupLat, pickupLng, stopLat, stopLng)

                // Base fare calculation: 10 taka per km, minimum 20 taka
                val baseFare = (distance * 10).toInt().coerceAtLeast(20)
                estimatedFare = baseFare * request.seats

            } catch (e: Exception) {
                Log.e("ExitOptionsDialog", "Failed to calculate fare: ${e.message}")
            }
        } else {
            estimatedFare = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "নামার অপশন",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "আপনি কি আগে নামতে চান, নাকি পরে?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Early Exit Option
                Card(
                    onClick = {
                        selectedOption = "early"
                        selectedStop = null
                        estimatedFare = null
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedOption == "early")
                            RouteOrange.copy(alpha = 0.1f)
                        else Color.White
                    ),
                    border = if (selectedOption == "early")
                        CardDefaults.outlinedCardBorder()
                    else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.RemoveCircle,
                            contentDescription = null,
                            tint = RouteOrange
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "আগে নামব",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "মূল গন্তব্যের আগে নামতে চাই",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Late Exit Option
                Card(
                    onClick = {
                        selectedOption = "late"
                        selectedStop = null
                        estimatedFare = null
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedOption == "late")
                            RouteBlue.copy(alpha = 0.1f)
                        else Color.White
                    ),
                    border = if (selectedOption == "late")
                        CardDefaults.outlinedCardBorder()
                    else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = RouteBlue
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column ( modifier = Modifier.verticalScroll(rememberScrollState())){
                            Text(
                                "পরে নামব",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "মূল গন্তব্যের পরে নামতে চাই",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Show available stops if option is selected
                if (selectedOption != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "স্টপেজ নির্বাচন করুন",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingStops) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = if (selectedOption == "early") RouteOrange else RouteBlue
                            )
                        }
                    } else if (availableStops.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Error.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
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
                                    "কোনো স্টপেজ উপলব্ধ নেই",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableStops) { stop ->
                                Card(
                                    onClick = { selectedStop = stop },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedStop == stop)
                                            (if (selectedOption == "early") RouteOrange else RouteBlue).copy(alpha = 0.1f)
                                        else Color(0xFFF5F5F5)
                                    ),
                                    border = if (selectedStop == stop)
                                        CardDefaults.outlinedCardBorder()
                                    else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = if (selectedStop == stop)
                                                (if (selectedOption == "early") RouteOrange else RouteBlue)
                                            else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            stop.address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = if (selectedStop == stop) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Show estimated fare if stop is selected
                if (estimatedFare != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = RouteGreen.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "আনুমানিক ভাড়া",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    "৳$estimatedFare",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = RouteGreen
                                )
                                Text(
                                    "পূর্বের ভাড়া: ৳${request.fare}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                Icons.Filled.Payment,
                                contentDescription = null,
                                tint = RouteGreen,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বাতিল")
                    }

                    Button(
                        onClick = {
                            if (selectedStop != null) {
                                val latLng = LatLngData(
                                    selectedStop!!.latitude,
                                    selectedStop!!.longitude
                                )
                                when (selectedOption) {
                                    "early" -> onEarlyExit(selectedStop!!.address, latLng)
                                    "late" -> onLateExit(selectedStop!!.address, latLng)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedStop != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedOption) {
                                "early" -> RouteOrange
                                "late" -> RouteBlue
                                else -> TextSecondary
                            }
                        )
                    ) {
                        Text("নিশ্চিত করুন")
                    }
                }
            }
        }
    }
}

// Helper function to calculate distance between two points
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371 // Radius of Earth in kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
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