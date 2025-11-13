package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var autoRefreshEnabled by remember { mutableStateOf(true) } // Toggle for auto-refresh

    // Function to fetch requests
    suspend fun fetchRequests() {
        try {
            Log.d("MyRequestsScreen", "Fetching requests for user ${user.uid}")
            requests = AuthRepository().getRequestsForUser(user.uid)
            isLoading = false
        } catch (e: Exception) {
            error = "রিকোয়েস্ট পুনরুদ্ধারে ত্রুটি"
            isLoading = false
            scope.launch {
                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
            }
        }
    }

    // Auto-refresh every 3 seconds if enabled
    LaunchedEffect(user.uid, autoRefreshEnabled) {
        fetchRequests() // Initial fetch
        if (autoRefreshEnabled) {
            while (true) {
                delay(3000) // Wait 3 seconds
                Log.d("MyRequestsScreen", "Auto-refreshing requests")
                fetchRequests()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("আমার রিকোয়েস্টসমূহ") },
                actions = {
                    // Toggle auto-refresh
                    IconButton(onClick = {
                        autoRefreshEnabled = !autoRefreshEnabled
                        Log.d("MyRequestsScreen", "Auto-refresh ${if (autoRefreshEnabled) "enabled" else "disabled"}")
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (autoRefreshEnabled) "অটো-রিফ্রেশ চালু" else "অটো-রিফ্রেশ বন্ধ"
                            )
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Toggle Auto-Refresh",
                            tint = if (autoRefreshEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    // Dashboard Button
                    TextButton(onClick = {
                        Log.d("MyRequestsScreen", "Navigating to RiderDashboard")
                        navController.navigate("rider_dashboard")
                    }) {
                        Text("ড্যাশবোর্ড")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Text(error ?: "অজানা ত্রুটি", color = MaterialTheme.colorScheme.error)
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(requests) { request: Request ->
                    var bus by remember { mutableStateOf<Bus?>(null) }
                    var conductor by remember { mutableStateOf<User?>(null) }
                    var isChatEnabled by remember { mutableStateOf(false) }

                    LaunchedEffect(request.busId, request.conductorId) {
                        request.busId?.let { busId ->
                            bus = AuthRepository().getBus(busId)
                        }
                        conductor = AuthRepository().getUser(request.conductorId).getOrNull()
                        isChatEnabled = AuthRepository().isChatEnabled(request.id)
                    }

                    Card(modifier = Modifier.padding(8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("স্ট্যাটাস: ${request.status}")
                            Text("পিকআপ: ${request.pickup}")
                            Text("গন্তব্য: ${request.destination}")
                            Text("ভাড়া: ${request.fare} টাকা")
                            if (request.status == "Accepted") {
                                bus?.let { Text("বাস: ${it.name} (${it.number})") }
                                conductor?.let { Text("কন্ডাক্টর: ${it.name} (${it.phone})") }
                                Text("OTP: ${request.otp ?: "N/A"}")
                                Button(onClick = { navController.navigate("live_tracking/${request.id}") }) {
                                    Text("লাইভ ট্র্যাকিং")
                                }
                                if (isChatEnabled) {
                                    Button(onClick = { navController.navigate("chat/${request.id}") }) {
                                        Text("Chat with Conductor")
                                    }
                                } else {
                                    Text("Chat expired", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (request.status == "Pending") {
                                var isCancelling by remember { mutableStateOf(false) }
                                Button(
                                    onClick = {
                                        if (!isCancelling) {
                                            isCancelling = true
                                            scope.launch {
                                                val result = AuthRepository().cancelTripRequest(request.id, user.uid)
                                                isCancelling = false
                                                if (result.isSuccess) {
                                                    Toast.makeText(context, "রিকোয়েস্ট বাতিল করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                    requests = AuthRepository().getRequestsForUser(user.uid)
                                                } else {
                                                    val errorMsg = result.exceptionOrNull()?.message ?: "বাতিল ব্যর্থ হয়েছে"
                                                    Log.e("MyRequestsScreen", "Cancel failed for request ${request.id}: $errorMsg")
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(errorMsg)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isCancelling
                                ) {
                                    if (isCancelling) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    else Text("বাতিল করুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
