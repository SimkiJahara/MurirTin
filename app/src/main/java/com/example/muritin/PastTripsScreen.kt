package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun PastTripsScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var allRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<Request?>(null) }
    var selectedBus by remember { mutableStateOf<Bus?>(null) }
    var selectedConductor by remember { mutableStateOf<User?>(null) }

    suspend fun fetchPastTrips() {
        try {
            Log.d("PastTripsScreen", "Fetching all trips for user ${user.uid}")
            val allUserRequests = AuthRepository().getAllRequestsForUser(user.uid)
            allRequests = allUserRequests.filter { it.status == "Accepted" }
            isLoading = false
        } catch (e: Exception) {
            error = "ট্রিপ পুনরুদ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
            scope.launch {
                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
            }
        }
    }

    LaunchedEffect(user.uid) {
        fetchPastTrips()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("পূর্ববর্তী যাত্রাসমূহ") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        error ?: "অজানা ত্রুটি",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            fetchPastTrips()
                        }
                    }) {
                        Text("আবার চেষ্টা করুন")
                    }
                }
            }
        } else if (allRequests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "কোনো পূর্ববর্তী যাত্রা নেই",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp)
            ) {
                items(allRequests) { request ->
                    var bus by remember { mutableStateOf<Bus?>(null) }
                    var conductor by remember { mutableStateOf<User?>(null) }
                    var isChatEnabled by remember { mutableStateOf(false) }
                    var scheduleEndTime by remember { mutableStateOf<Long?>(null) }
                    var canRate by remember { mutableStateOf(false) }

                    LaunchedEffect(request.busId, request.conductorId, request.scheduleId, request.id) {
                        request.busId?.let { busId ->
                            bus = AuthRepository().getBus(busId)
                        }
                        conductor = AuthRepository().getUser(request.conductorId).getOrNull()
                        isChatEnabled = AuthRepository().isChatEnabled(request.id)
                        canRate = AuthRepository().canRateTrip(request.id, user.uid)

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
                            Text(
                                "পিকআপ: ${request.pickup}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "গন্তব্য: ${request.destination}",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("ভাড়া: ${request.fare} টাকা")
                            Text("সিট: ${request.seats}")

                            bus?.let {
                                Text("বাস: ${it.name} (${it.number})")
                            }

                            conductor?.let {
                                Text("কন্ডাক্টর: ${it.name}")
                                Text("ফোন: ${it.phone}")
                            }

                            Text("OTP: ${request.otp ?: "N/A"}")

                            scheduleEndTime?.let { endTime ->
                                val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                Text(
                                    "যাত্রা শেষ: ${dateFormat.format(Date(endTime))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                val chatExpiryTime = endTime + 432000000L
                                val now = System.currentTimeMillis()

                                if (isChatEnabled) {
                                    val hoursLeft = ((chatExpiryTime - now) / (1000 * 60 * 60)).toInt()
                                    Text(
                                        "চ্যাট উপলব্ধ: আর ${hoursLeft} ঘণ্টা",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            // Show existing rating if present
                            request.rating?.let { rating ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "আপনার মূল্যায়ন",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Column {
                                                Text("কন্ডাক্টর:", style = MaterialTheme.typography.bodySmall)
                                                RatingDisplay(rating.conductorRating)
                                            }
                                            Column {
                                                Text("বাস:", style = MaterialTheme.typography.bodySmall)
                                                RatingDisplay(rating.busRating)
                                            }
                                            Column {
                                                Text("সামগ্রিক:", style = MaterialTheme.typography.bodySmall)
                                                RatingDisplay(rating.overallRating)
                                            }
                                        }
                                        if (rating.comment.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "মন্তব্য: ${rating.comment}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                        Text(
                                            "মূল্যায়ন দেওয়া: ${dateFormat.format(Date(rating.timestamp))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Rating button - show if trip is completed and not rated
                            if (canRate) {
                                Button(
                                    onClick = {
                                        selectedRequest = request
                                        selectedBus = bus
                                        selectedConductor = conductor
                                        showRatingDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Text("যাত্রা মূল্যায়ন করুন")
                                }
                            }

                            // Chat button
                            if (isChatEnabled) {
                                Button(
                                    onClick = {
                                        navController.navigate("chat/${request.id}")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("কন্ডাক্টরের সাথে চ্যাট করুন")
                                }
                                Text(
                                    "যাত্রা শেষের ৩ দিনের মধ্যে চ্যাট করতে পারবেন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                OutlinedButton(
                                    onClick = { /* Disabled */ },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("চ্যাট সময় শেষ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rating Dialog
    if (showRatingDialog && selectedRequest != null) {
        TripRatingDialog(
            request = selectedRequest!!,
            bus = selectedBus,
            conductor = selectedConductor,
            onDismiss = {
                showRatingDialog = false
                selectedRequest = null
                selectedBus = null
                selectedConductor = null
            },
            onRatingSubmitted = {
                showRatingDialog = false
                Toast.makeText(context, "মূল্যায়ন সফলভাবে জমা হয়েছে", Toast.LENGTH_SHORT).show()
                scope.launch {
                    fetchPastTrips()
                }
                selectedRequest = null
                selectedBus = null
                selectedConductor = null
            }
        )
    }
}