package com.example.muritin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext   // <-- ADD THIS
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current               // <-- use LocalContext.current
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.uid) {
        try {
            requests = AuthRepository().getRequestsForUser(user.uid)
            isLoading = false
        } catch (e: Exception) {
            error = "রিকোয়েস্ট পুনরুদ্ধারে ত্রুটি"
            isLoading = false
            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("আমার রিকোয়েস্টসমূহ") })
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
                    var isChatEnabled by remember { mutableStateOf(false) }  // NEW
                    LaunchedEffect(request.busId, request.conductorId) {
                        request.busId?.let { busId ->
                            bus = AuthRepository().getBus(busId)
                        }
                        conductor = AuthRepository().getUser(request.conductorId).getOrNull()
                        isChatEnabled = AuthRepository().isChatEnabled(request.id)  // NEW: Check if chat is allowed
                    }
                    Card(modifier = Modifier.padding(8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("স্ট্যাটাস: ${request.status}")
                            Text("পিকআপ: ${request.pickup}")
                            Text("গন্তব্য: ${request.destination}")
                            Text("ভাড়া: ${request.fare} টাকা")
                            if (request.status == "Accepted") {
                                bus?.let {
                                    Text("বাস: ${it.name} (${it.number})")
                                }
                                conductor?.let {
                                    Text("কন্ডাক্টর: ${it.name} (${it.phone})")
                                }
                                Text("OTP: ${request.otp ?: "N/A"}")
                                Button(onClick = { navController.navigate("live_tracking/${request.id}") }) {
                                    Text("লাইভ ট্র্যাকিং")
                                }
                                if (isChatEnabled) {  // NEW
                                    Button(onClick = { navController.navigate("chat/${request.id}") }) {
                                        Text("Chat with Conductor")
                                    }
                                } else {
                                    Text("Chat expired", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            if (request.status == "Pending") {
                                Button(onClick = {
                                    scope.launch {
                                        val updates = mapOf("status" to "Cancelled")
                                        FirebaseDatabase.getInstance().getReference("requests").child(request.id).updateChildren(updates).await()
                                        Toast.makeText(context, "রিকোয়েস্ট বাতিল করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        requests = AuthRepository().getRequestsForUser(user.uid)
                                    }
                                }) {
                                    Text("বাতিল করুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
