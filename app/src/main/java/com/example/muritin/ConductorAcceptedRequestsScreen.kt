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

    // Function to load accepted requests
    suspend fun loadAcceptedRequests() {
        try {
            val requests = AuthRepository().getAcceptedRequestsForConductor(user.uid)
            acceptedRequests = requests

            // Load rider data for each request
            val riderMap = mutableMapOf<String, User>()
            requests.forEach { request ->
                val riderResult = AuthRepository().getUser(request.riderId)
                riderResult.getOrNull()?.let { rider ->
                    riderMap[request.riderId] = rider
                }
            }
            riderDataMap = riderMap
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
                                modifier = Modifier.size(64.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "কোনো গৃহীত রিকোয়েস্ট নেই",
                                style = MaterialTheme.typography.bodyLarge,
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
                            AcceptedRequestCard(
                                request = request,
                                rider = rider,
                                onChatClick = {
                                    navController.navigate("chat/${request.id}")
                                },
                                onVerifyOtpClick = {
                                    selectedRequest = request
                                    enteredOtp = ""
                                    showOtpDialog = true
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

    // OTP Verification Dialog
    if (showOtpDialog && selectedRequest != null) {
        OtpVerificationDialog(
            request = selectedRequest!!,
            enteredOtp = enteredOtp,
            onOtpChange = { enteredOtp = it },
            onDismiss = {
                showOtpDialog = false
                selectedRequest = null
                enteredOtp = ""
            },
            onVerify = {
                if (enteredOtp == selectedRequest!!.otp) {
                    Toast.makeText(
                        context,
                        "OTP যাচাই সফল! যাত্রী বোর্ড করেছে",
                        Toast.LENGTH_LONG
                    ).show()
                    showOtpDialog = false
                    selectedRequest = null
                    enteredOtp = ""
                } else {
                    Toast.makeText(
                        context,
                        "ভুল OTP! আবার চেষ্টা করুন",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

@Composable
fun AcceptedRequestCard(
    request: Request,
    rider: User?,
    onChatClick: () -> Unit,
    onVerifyOtpClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(RouteBlue, RoutePurple)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rider?.name ?: "লোড হচ্ছে...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = RouteGreen
                    ) {
                        Text(
                            text = "গৃহীত",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rider Information
            InfoRow(
                icon = Icons.Filled.Phone,
                label = "ফোন নম্বর",
                value = rider?.phone ?: "N/A",
                iconColor = RouteOrange
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Trip Details
            InfoRow(
                icon = Icons.Filled.LocationOn,
                label = "পিকআপ",
                value = request.pickup,
                iconColor = RouteGreen
            )

            InfoRow(
                icon = Icons.Filled.Place,
                label = "গন্তব্য",
                value = request.destination,
                iconColor = Error
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Seat and Fare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(
                    icon = Icons.Filled.EventSeat,
                    label = "সিট",
                    value = "${request.seats}",
                    backgroundColor = RouteBlue.copy(alpha = 0.1f),
                    contentColor = RouteBlue
                )

                InfoChip(
                    icon = Icons.Filled.Money,
                    label = "ভাড়া",
                    value = "৳${request.fare}",
                    backgroundColor = RouteGreen.copy(alpha = 0.1f),
                    contentColor = RouteGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chat Button
                OutlinedButton(
                    onClick = onChatClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoutePurple
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

                // OTP Verify Button
                Button(
                    onClick = onVerifyOtpClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RouteGreen
                    )
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
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

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    request: Request,
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

                Spacer(modifier = Modifier.height(8.dp))

                // Show expected OTP for debugging (remove in production)
                Text(
                    text = "সঠিক OTP: ${request.otp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.6f)
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