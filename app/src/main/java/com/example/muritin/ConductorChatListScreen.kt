package com.example.muritin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
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

    // Function to fetch all accepted requests where chat is still available
    suspend fun fetchChatableRequests() {
        try {
            Log.d("ConductorChatList", "Fetching chatable requests for conductor ${user.uid}")
            val allAccepted = AuthRepository().getAllAcceptedRequestsForConductor(user.uid)
            // Filter only those where chat is enabled (within 3 days of trip end)
            chatableRequests = allAccepted.filter { request ->
                AuthRepository().isChatEnabled(request.id)
            }
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
                        Icon(Icons.Default.ArrowBack, "Back")
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
                                MaterialTheme.colorScheme.outline
                            else
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null && chatableRequests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            error ?: "অজানা ত্রুটি",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(onClick = {
                            scope.launch {
                                isLoading = true
                                fetchChatableRequests()
                                isLoading = false
                            }
                        }) {
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
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            "যাত্রা শেষের ৩ দিনের মধ্যে রাইডারদের সাথে চ্যাট করতে পারবেন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                "মোট সক্রিয় চ্যাট: ${chatableRequests.size}",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    items(chatableRequests) { request ->
                        var rider by remember { mutableStateOf<User?>(null) }
                        var scheduleEndTime by remember { mutableStateOf<Long?>(null) }

                        LaunchedEffect(request.riderId, request.scheduleId) {
                            rider = AuthRepository().getUser(request.riderId).getOrNull()
                            request.scheduleId?.let { scheduleId ->
                                scheduleEndTime = AuthRepository().getSchedule(scheduleId)?.endTime
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                rider?.let {
                                    Text(
                                        "রাইডার: ${it.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "ফোন: ${it.phone}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } ?: Text(
                                    "রাইডার: লোড হচ্ছে...",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("পিকআপ: ${request.pickup}")
                                Text("গন্তব্য: ${request.destination}")
                                Text("ভাড়া: ${request.fare} টাকা")
                                Text("সিট: ${request.seats}")
                                Text("OTP: ${request.otp ?: "N/A"}")

                                scheduleEndTime?.let { endTime ->
                                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    Text(
                                        "যাত্রা শেষ: ${dateFormat.format(Date(endTime))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    // Calculate time remaining for chat
                                    val chatExpiryTime = endTime + 432000000L // 3 days in milliseconds
                                    val now = System.currentTimeMillis()
                                    val hoursLeft = ((chatExpiryTime - now) / (1000 * 60 * 60)).toInt()

                                    if (hoursLeft > 0) {
                                        Text(
                                            "চ্যাট উপলব্ধ: আর ${hoursLeft} ঘণ্টা",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        navController.navigate("chat/${request.id}")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("চ্যাট করুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}