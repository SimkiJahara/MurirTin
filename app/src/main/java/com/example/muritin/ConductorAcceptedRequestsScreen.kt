package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorAcceptedRequestsScreen(
    navController: NavHostController,
    user: FirebaseUser
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var acceptedRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var riderDataMap by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showOtpDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<Request?>(null) }
    var enteredOtp by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }



    suspend fun loadAcceptedRequests() {
        try {
            Log.d("ConductorAcceptedRequests", "Loading accepted requests for conductor: ${user.uid}")
            val requests = AuthRepository().getAcceptedRequestsForConductor(user.uid)
            Log.d("ConductorAcceptedRequests", "Found ${requests.size} accepted requests")

            // Filter out completed trips - they should not show here
            val activeRequests = requests.filter { request ->
                request.rideStatus?.tripCompleted != true && request.status != "Completed"
            }

            // Load rider data for each ACTIVE request
            val riderMap = mutableMapOf<String, User>()
            activeRequests.forEach { request ->
                Log.d("ConductorAcceptedRequests", "Loading rider data for riderId: ${request.riderId}")
                try {
                    val riderResult = AuthRepository().getUser(request.riderId)
                    riderResult.getOrNull()?.let { rider ->
                        Log.d("ConductorAcceptedRequests", "Loaded rider: ${rider.name}, phone: ${rider.phone}")
                        riderMap[request.riderId] = rider
                    } ?: Log.e("ConductorAcceptedRequests", "Failed to load rider data for ${request.riderId}")
                } catch (e: Exception) {
                    Log.e("ConductorAcceptedRequests", "Exception loading rider ${request.riderId}: ${e.message}", e)
                }
            }

            riderDataMap = riderMap
            acceptedRequests = activeRequests

            Log.d("ConductorAcceptedRequests", "Total active requests: ${activeRequests.size}, Total rider data loaded: ${riderMap.size}")
            error = null
        } catch (e: Exception) {
            error = "ডেটা লোড করতে ব্যর্থ: ${e.message}"
            Log.e("ConductorAcceptedRequests", "Load failed: ${e.message}", e)
        }
    }

    // Initial load
    LaunchedEffect(user.uid) {
        isLoading = true
        loadAcceptedRequests()
        isLoading = false
    }

    // Auto-refresh every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            if (!isRefreshing && !isLoading) {
                isRefreshing = true
                loadAcceptedRequests()
                isRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "গৃহীত রিকোয়েস্ট",
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
                                loadAcceptedRequests()
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                                        loadAcceptedRequests()
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

                acceptedRequests.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "কোনো গৃহীত রিকোয়েস্ট নেই",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "নতুন রিকোয়েস্ট গ্রহণ করুন",
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

                        items(acceptedRequests) { request ->
                            val rider = riderDataMap[request.riderId]
                            ConductorRequestCard(
                                request = request,
                                rider = rider,
                                onVerifyOtpClick = {
                                    selectedRequest = request
                                    enteredOtp = ""
                                    showOtpDialog = true
                                },
                                onOpenChat = { requestId ->
                                    navController.navigate("chat/$requestId")
                                },
                                onRefresh = {
                                    scope.launch {
                                        isRefreshing = true
                                        loadAcceptedRequests()
                                        isRefreshing = false
                                    }
                                },
                                conductorId = user.uid
                            )
                        }
                    }
                }
            }
        }
    }

    // OTP Verification Dialog
    if (showOtpDialog && selectedRequest != null) {
        OtpVerificationDialog(
            enteredOtp = enteredOtp,
            onOtpChange = { enteredOtp = it },
            onDismiss = {
                showOtpDialog = false
                enteredOtp = ""
            },
            onVerify = {
                scope.launch {
                    val result = AuthRepository().verifyOtpAndBoardRider(
                        selectedRequest!!.id,
                        enteredOtp,
                        user.uid
                    )
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            "OTP যাচাই সফল! যাত্রী বাসে উঠেছেন",
                            Toast.LENGTH_SHORT
                        ).show()
                        showOtpDialog = false
                        enteredOtp = ""
                        loadAcceptedRequests()
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "যাচাই ব্যর্থ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }
}
// Replace the ConductorRequestCard in ConductorAcceptedRequestsScreen.kt
// This version removes manual fare collection buttons since everything is automatic

@Composable
fun ConductorRequestCard(
    request: Request,
    rider: User?,
    onVerifyOtpClick: () -> Unit,
    onOpenChat: (String) -> Unit,
    onRefresh: () -> Unit,
    conductorId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Rider Info
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = rider?.name ?: "লোড হচ্ছে...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = rider?.phone ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Travelling Status (if boarded)
            if (request.rideStatus?.inBusTravelling == true) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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
                                "যাত্রী বাসে ভ্রমণরত",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RouteBlue
                            )
                            Text(
                                "বোর্ড: ${SimpleDateFormat("h:mm a", Locale.US).format(Date(request.rideStatus.boardedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // In ConductorRequestCard
                FareDisplayCard(request = request)


                Spacer(modifier = Modifier.height(12.dp))

                // AUTO-MONITORING INDICATOR
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
                            Icons.Filled.AutoMode,
                            contentDescription = null,
                            tint = RouteGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "স্বয়ংক্রিয় পর্যবেক্ষণ সক্রিয়",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = RouteGreen
                            )
                            Text(
                                "গন্তব্যে পৌঁছানো এবং ভাড়া সংগ্রহ স্বয়ংক্রিয়ভাবে হবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Route details
            InfoRow(
                icon = Icons.Filled.TripOrigin,
                label = "পিকআপ স্থান",
                value = request.pickup,
                iconColor = RouteGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(
                icon = Icons.Filled.LocationOn,
                label = "গন্তব্য স্থান",
                value = request.destination,
                iconColor = Error
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Trip details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    icon = Icons.Filled.EventSeat,
                    label = "আসন",
                    value = "${request.seats}",
                    backgroundColor = RouteBlue.copy(alpha = 0.1f),
                    contentColor = RouteBlue
                )

                InfoChip(
                    icon = Icons.Filled.Payment,
                    label = "ভাড়া",
                    value = if (request.rideStatus?.actualFare != null && request.rideStatus.actualFare > 0) {
                        "৳${request.rideStatus.actualFare}"
                    } else {
                        "৳${request.fare}"
                    },
                    backgroundColor = RouteOrange.copy(alpha = 0.1f),
                    contentColor = RouteOrange
                )
            }

            // Early Exit Request Card (ONLY APPROVAL, NO MANUAL ENTRY)
            if (request.rideStatus?.earlyExitRequested == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RouteOrange.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = RouteOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "যাত্রী আগাম নামতে চায় (স্বয়ংক্রিয়)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RouteOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "স্থান: ${request.rideStatus.earlyExitStop}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            "ভাড়া স্বয়ংক্রিয়ভাবে পুনর্গণনা করা হয়েছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = RouteGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Late Exit Request Card (ONLY APPROVAL, NO MANUAL ENTRY)
            if (request.rideStatus?.lateExitRequested == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RouteBlue.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = RouteBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "যাত্রী পরে নামতে চায় (স্বয়ংক্রিয়)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = RouteBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "স্থান: ${request.rideStatus.lateExitStop}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            "ভাড়া স্বয়ংক্রিয়ভাবে পুনর্গণনা করা হয়েছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = RouteGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            if (request.rideStatus?.inBusTravelling == true) {
                // REMOVED MANUAL FARE COLLECTION BUTTON
                // Everything is automatic now!

                if (!request.rideStatus.tripCompleted) {
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
                                "স্বয়ংক্রিয় পর্যবেক্ষণ চলছে",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RouteGreen,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "যাত্রী গন্তব্যে পৌঁছালে ভাড়া স্বয়ংক্রিয়ভাবে সংগ্রহ হবে",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Trip completed
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = RouteGreen.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = RouteGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "যাত্রা সম্পূর্ণ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RouteGreen
                                )
                                Text(
                                    "স্বয়ংক্রিয়ভাবে সম্পন্ন হয়েছে",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chat button
                Button(
                    onClick = { onOpenChat(request.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RouteGreen
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
            } else {
                // If not boarded yet, show OTP and chat buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onOpenChat(request.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RouteGreen
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

                    Button(
                        onClick = onVerifyOtpClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("OTP যাচাই")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
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
fun InfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun OtpVerificationDialog(
    enteredOtp: String,
    onOtpChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onVerify: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(RouteGreen, RouteBlue)
                            ),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "OTP যাচাই করুন",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "যাত্রীর কাছ থেকে ৪ সংখ্যার OTP নিন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // OTP Input Field
                OutlinedTextField(
                    value = enteredOtp,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            onOtpChange(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OTP লিখুন") },
                    singleLine = true,
                    placeholder = { Text("0000") },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RouteGreen,
                        focusedLabelColor = RouteGreen
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
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
                        onClick = onVerify,
                        modifier = Modifier.weight(1f),
                        enabled = enteredOtp.length == 4,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RouteGreen
                        )
                    ) {
                        Text("যাচাই করুন")
                    }
                }
            }
        }
    }
}