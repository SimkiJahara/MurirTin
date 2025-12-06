package com.example.muritin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorChatListScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chatableRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Function to fetch all requests (both active and completed) where chat is still available
    suspend fun fetchChatableRequests() {
        try {
            Log.d("ConductorChatList", "Fetching chatable requests for conductor ${user.uid}")

            // Get ALL accepted requests (including completed ones)
            val allAccepted = AuthRepository().getAllAcceptedRequestsForConductor(user.uid)

            Log.d("ConductorChatList", "Total accepted requests: ${allAccepted.size}")

            // Filter only those where chat is enabled (within 5 days of trip end)
            chatableRequests = allAccepted.filter { request ->
                val isChatEnabled = AuthRepository().isChatEnabled(request.id)
                Log.d("ConductorChatList", "Request ${request.id}: status=${request.status}, tripCompleted=${request.rideStatus?.tripCompleted}, chatEnabled=$isChatEnabled")
                isChatEnabled
            }.sortedByDescending { it.acceptedAt } // Sort by most recent first

            Log.d("ConductorChatList", "Filtered chatable requests: ${chatableRequests.size}")
            error = null
        } catch (e: Exception) {
            error = "রিকোয়েস্ট পুনরুদ্ধারে ত্রুটি: ${e.message}"
            Log.e("ConductorChatList", "Error: ${e.message}", e)
            scope.launch {
                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
            }
        }
    }

    LaunchedEffect(user.uid) {
        fetchChatableRequests()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("রাইডারদের সাথে চ্যাট") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                fetchChatableRequests()
                                isRefreshing = false
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isRefreshing)
                                Color.White.copy(alpha = 0.5f)
                            else
                                Color.White
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
            } else if (error != null && chatableRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            error ?: "অজানা ত্রুটি",
                            color = Error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    fetchChatableRequests()
                                    isLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("আবার চেষ্টা করুন")
                        }
                    }
                }
            } else if (chatableRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "কোনো সক্রিয় চ্যাট নেই",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "যাত্রা শেষের ৫ দিনের মধ্যে রাইডারদের সাথে চ্যাট করতে পারবেন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                "মোট সক্রিয় চ্যাট: ${chatableRequests.size}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }

                    items(chatableRequests) { request ->
                        ConductorChatCard(
                            request = request,
                            onOpenChat = { requestId ->
                                navController.navigate("chat/$requestId")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConductorChatCard(
    request: Request,
    onOpenChat: (String) -> Unit
) {
    var rider by remember { mutableStateOf<User?>(null) }
    var scheduleEndTime by remember { mutableStateOf<Long?>(null) }
    var isLoadingRider by remember { mutableStateOf(true) }

    LaunchedEffect(request.riderId, request.scheduleId) {
        isLoadingRider = true
        rider = AuthRepository().getUser(request.riderId).getOrNull()
        request.scheduleId?.let { scheduleId ->
            scheduleEndTime = AuthRepository().getSchedule(scheduleId)?.endTime
        }
        isLoadingRider = false
    }

    // Determine if trip is completed
    val isCompleted = request.status == "Completed" ||
            (request.status == "Accepted" && request.rideStatus?.tripCompleted == true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    Surface(
                        color = RouteGreen.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = RouteGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "সম্পন্ন",
                                style = MaterialTheme.typography.bodySmall,
                                color = RouteGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        color = RouteBlue.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "চলমান",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = RouteBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Chat expiry time
                scheduleEndTime?.let { endTime ->
                    val chatExpiryTime = endTime + 432000000L // 5 days
                    val now = System.currentTimeMillis()
                    val hoursLeft = ((chatExpiryTime - now) / (1000 * 60 * 60)).toInt()

                    if (hoursLeft > 0) {
                        Text(
                            "আর ${hoursLeft}ঘ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rider info
            if (isLoadingRider) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "রাইডার তথ্য লোড হচ্ছে...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                rider?.let { riderData ->
                    Text(
                        "রাইডার: ${riderData.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text(
                        "ফোন: ${riderData.phone}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } ?: Text(
                    "রাইডার: তথ্য পাওয়া যায়নি",
                    style = MaterialTheme.typography.titleMedium,
                    color = Error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Trip details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "পিকআপ",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        request.pickup,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = RouteGreen
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "গন্তব্য",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        request.destination,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoItem("ভাড়া", "${request.fare} ৳", RouteOrange)
                InfoItem("সিট", "${request.seats}", RouteBlue)
                request.otp?.let { otp ->
                    InfoItem("OTP", otp, Primary)
                }
            }

            scheduleEndTime?.let { endTime ->
                Spacer(modifier = Modifier.height(8.dp))
                val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                Text(
                    "যাত্রা শেষ: ${dateFormat.format(Date(endTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chat button
            Button(
                onClick = { onOpenChat(request.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("চ্যাট করুন")
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.7f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}