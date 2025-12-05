package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
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

    // Function to fetch requests
    suspend fun fetchRequests() {
        try {
            Log.d("MyRequestsScreen", "Fetching requests for user ${user.uid}")
            requests = AuthRepository().getRequestsForUser(user.uid)
            error = null
        } catch (e: Exception) {
            error = "রিকোয়েস্ট পুনর্ধারে ত্রুটি: ${e.message}"
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

                            // Info Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Info.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = Info,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "পেন্ডিং রিকোয়েস্ট ৩ মিনিটে স্বয়ংক্রিয়ভাবে মুছে যাবে",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Info
                                    )
                                }
                            }
                        }

                        items(requests) { request ->
                            RequestCard(
                                request = request,
                                user = user,
                                navController = navController,
                                onRefresh = {
                                    scope.launch {
                                        fetchRequests()
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
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
    user: FirebaseUser,
    navController: NavHostController,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bus by remember { mutableStateOf<Bus?>(null) }
    var conductor by remember { mutableStateOf<User?>(null) }
    var isChatEnabled by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(request.busId, request.conductorId) {
        request.busId?.let { busId ->
            bus = AuthRepository().getBus(busId)
        }
        if (request.conductorId.isNotEmpty()) {
            conductor = AuthRepository().getUser(request.conductorId).getOrNull()
        }
        isChatEnabled = AuthRepository().isChatEnabled(request.id)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getStatusColor(request.status).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getStatusIcon(request.status),
                            contentDescription = null,
                            tint = getStatusColor(request.status),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = getStatusText(request.status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getStatusColor(request.status)
                    )
                }

                // Timestamp
                Text(
                    text = formatTimestamp(request.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trip Route
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        BackgroundLight,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                TripRouteRow(
                    icon = Icons.Filled.TripOrigin,
                    label = "পিকআপ",
                    value = request.pickup,
                    iconColor = RouteGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                TripRouteRow(
                    icon = Icons.Filled.Place,
                    label = "গন্তব্য",
                    value = request.destination,
                    iconColor = Error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trip Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TripDetailChip(
                    icon = Icons.Filled.EventSeat,
                    label = "সিট",
                    value = "${request.seats}",
                    modifier = Modifier.weight(1f),
                    color = RouteBlue
                )

                TripDetailChip(
                    icon = Icons.Filled.Money,
                    label = "ভাড়া",
                    value = "৳${request.fare}",
                    modifier = Modifier.weight(1f),
                    color = RouteGreen
                )
            }

            // Accepted Request Details
            if (request.status == "Accepted") {
                Spacer(modifier = Modifier.height(12.dp))

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                // Bus Info
                bus?.let {
                    AcceptedInfoRow(
                        icon = Icons.Filled.DirectionsBus,
                        label = "বাস",
                        value = "${it.name} • ${it.number}",
                        iconColor = RoutePurple
                    )
                }

                // Conductor Info
                conductor?.let {
                    AcceptedInfoRow(
                        icon = Icons.Filled.Person,
                        label = "কন্ডাক্টর",
                        value = it.name,
                        iconColor = RouteOrange
                    )

                    AcceptedInfoRow(
                        icon = Icons.Filled.Phone,
                        label = "ফোন নম্বর",
                        value = it.phone,
                        iconColor = RouteBlue
                    )
                }

                // OTP Display
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = RouteGreen.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = RouteGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "আপনার OTP",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Text(
                            text = request.otp ?: "N/A",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = RouteGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons for Accepted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.navigate("live_tracking/${request.id}") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RoutePurple
                        )
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ট্র্যাক করুন")
                    }

                    Button(
                        onClick = {
                            if (isChatEnabled) {
                                navController.navigate("chat/${request.id}")
                            } else {
                                Toast.makeText(
                                    context,
                                    "চ্যাট সময় শেষ হয়ে গেছে",
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
                                    val errorMsg = result.exceptionOrNull()?.message ?: "বাতিল ব্যর্থ"
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCancelling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Error
                    )
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
fun TripRouteRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
fun AcceptedInfoRow(
    icon: ImageVector,
    label: String,
    value: String?,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }
    }
}

fun getStatusIcon(status: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        "Pending" -> Icons.Filled.Schedule
        "Accepted" -> Icons.Filled.CheckCircle
        "Cancelled" -> Icons.Filled.Cancel
        else -> Icons.Filled.Info
    }
}

fun getStatusColor(status: String): Color {
    return when (status) {
        "Pending" -> RouteOrange
        "Accepted" -> RouteGreen
        "Cancelled" -> Error
        else -> TextSecondary
    }
}

fun getStatusText(status: String): String {
    return when (status) {
        "Pending" -> "অপেক্ষমাণ"
        "Accepted" -> "গৃহীত"
        "Cancelled" -> "বাতিল"
        else -> status
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
