package com.example.muritin

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.location.LocationServices
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
    val database = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")

    var assignedBus by remember { mutableStateOf<Bus?>(null) }
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var acceptedRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }

    // Dialog fields
    var selectedDate by remember { mutableStateOf("") }
    var selectedStartTime by remember { mutableStateOf("") }
    var selectedEndTime by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("going") }

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

    // ── REFRESH FUNCTION ─────────────────────────────────────────────────────
    suspend fun refreshData() {
        isRefreshing = true
        try {
            // 1. Get assigned bus
            val snapshot = database.getReference("busAssignments")
                .orderByChild("conductorId")
                .equalTo(user.uid)
                .get()
                .await()

            val busId = snapshot.children.firstOrNull()?.key
            if (busId != null) {
                assignedBus = AuthRepository().getBus(busId)
                schedules = AuthRepository().getSchedulesForConductor(user.uid)
                    .filter { it.endTime >= System.currentTimeMillis() }
                acceptedRequests = AuthRepository().getAcceptedRequestsForConductor(user.uid)
            } else {
                assignedBus = null
                schedules = emptyList()
                acceptedRequests = emptyList()
            }

            // 2. Get current location
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    if (assignedBus != null) {
                        pendingRequests = AuthRepository().getPendingRequestsForConductor(
                            conductorId = user.uid,
                            currentLocation = currentLocation!!,
                            bus = assignedBus!!,
                            apiKey = apiKey,
                            directionsApi = directionsApi
                        )
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

    // ── INITIAL LOAD ───────────────────────────────────────────────────────
    LaunchedEffect(user.uid) {
        isLoading = true
        refreshData()
        isLoading = false
    }

    // ── AUTO-REFRESH EVERY 15 SECONDS ─────────────────────────────────────
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000) // 15 seconds
            if (!isRefreshing) {
                refreshData()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কন্ডাক্টর ড্যাশবোর্ড") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Manual Refresh Button
                    IconButton(
                        onClick = { scope.launch { refreshData() } },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isRefreshing) MaterialTheme.colorScheme.outline else LocalContentColor.current
                        )
                    }
                    Button(onClick = {
                        Log.d("ConductorDashboard", "Logging out")
                        Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
                        onLogout()
                    }) {
                        Text("লগআউট")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null && !isRefreshing) {
                Text(
                    text = error ?: "অজানা ত্রুটি",
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("লোকেশন পারমিশন দিন")
                }
            } else {
                // Refreshing indicator
                if (isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "কন্ডাক্টর ড্যাশবোর্ড",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "স্বাগতম, ${user.email}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(24.dp))


                OutlinedButton(
                    onClick = {
                        navController.navigate("conductor_help")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Help,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সহায়তা নির্দেশিকা")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Assigned Bus
                if (assignedBus != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("অ্যাসাইনড বাস: ${assignedBus!!.name}", style = MaterialTheme.typography.bodyLarge)
                            Text("নম্বর: ${assignedBus!!.number}", style = MaterialTheme.typography.bodyLarge)
                            Text("স্টপস: ${assignedBus!!.stops.joinToString(", ")}", style = MaterialTheme.typography.bodyLarge)
                            Text("ভাড়া তালিকা:", style = MaterialTheme.typography.bodyLarge)
                            assignedBus!!.fares.forEach { (stop, dests) ->
                                dests.forEach { (dest, fare) ->
                                    Text("$stop থেকে $dest: $fare টাকা", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Button(
                                onClick = {
                                    navController.navigate("analytics_report/${assignedBus!!.busId}")
                                }
                            ) {
                                Text("পরিসংখ্যান রিপোর্ট দেখুন")
                            }
                        }
                    }
                } else {
                    Text("কোনো বাস অ্যাসাইন করা হয়নি", style = MaterialTheme.typography.bodyLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate("show_account_info") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("অ্যাকাউন্ট এর তথ্য") }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = assignedBus != null
                ) { Text("শিডিউল তৈরি করুন") }

                Spacer(modifier = Modifier.height(16.dp))


// NEW: Button to view riders for chat
                Button(
                    onClick = {
                        navController.navigate("conductor_chat_list")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("রাইডারদের সাথে চ্যাট করুন")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── SCHEDULE LIST WITH EDIT & DELETE ─────────────────────────────
                Text("শিডিউল তালিকা", style = MaterialTheme.typography.titleMedium)
                if (schedules.isEmpty()) {
                    Text("কোনো শিডিউল নেই", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                        items(schedules) { schedule ->
                            var showDeleteDialog by remember { mutableStateOf(false) }
                            var showEditDialogLocal by remember { mutableStateOf(false) }
                            val canModify = schedule.startTime > System.currentTimeMillis()

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("বাস: ${assignedBus?.name ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                        Text("তারিখ: ${schedule.date}", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "সময়: ${
                                                SimpleDateFormat("hh:mm a", Locale.getDefault())
                                                    .format(Date(schedule.startTime))
                                            } - ${
                                                SimpleDateFormat("hh:mm a", Locale.getDefault())
                                                    .format(Date(schedule.endTime))
                                            }",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "দিক: ${if (schedule.direction == "going") "যাচ্ছি" else "ফিরছি"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (schedule.direction == "going")
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.tertiary
                                        )
                                    }

                                    Row {
                                        if (canModify) {
                                            IconButton(onClick = {
                                                editingSchedule = schedule
                                                selectedDate = schedule.date
                                                selectedStartTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                    .format(Date(schedule.startTime))
                                                selectedEndTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                    .format(Date(schedule.endTime))
                                                direction = schedule.direction
                                                showEditDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { showDeleteDialog = true }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        } else {
                                            Icon(Icons.Default.Schedule, "Started", tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }

                            // Delete Dialog
                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    title = { Text("শিডিউল মুছে ফেলবেন?") },
                                    text = { Text("তারিখ: ${schedule.date}\nসময়: ${selectedStartTime} - ${selectedEndTime}") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        database.getReference("schedules")
                                                            .child(schedule.scheduleId)
                                                            .removeValue()
                                                            .await()
                                                        Toast.makeText(context, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                                        refreshData()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "ব্যর্থ: ${e.message}", Toast.LENGTH_LONG).show()
                                                    } finally {
                                                        showDeleteDialog = false
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) { Text("মুছে ফেলুন") }
                                    },
                                    dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("বাতিল") } }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pending Requests
                Text("পেন্ডিং রিকোয়েস্টসমূহ", style = MaterialTheme.typography.titleMedium)
                if (pendingRequests.isEmpty()) {
                    Text(
                        text = if (currentLocation == null) "লোকেশন পাওয়া যায়নি" else "কোনো রিকোয়েস্ট নেই",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                        items(pendingRequests) { request ->
                            Card(modifier = Modifier.padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("পিকআপ: ${request.pickup}")
                                    Text("গন্তব্য: ${request.destination}")
                                    Text("সিট: ${request.seats}")
                                    Text("ভাড়া: ${request.fare}")
                                    Button(onClick = {
                                        scope.launch {
                                            val result = AuthRepository().acceptRequest(request.id, user.uid)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "অ্যাকসেপ্ট সফল", Toast.LENGTH_SHORT).show()
                                                refreshData()
                                            } else {
                                                Toast.makeText(context, "ব্যর্থ: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) { Text("অ্যাকসেপ্ট করুন") }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accepted Requests
                Text("অ্যাকসেপ্টেড রিকোয়েস্টসমূহ", style = MaterialTheme.typography.titleMedium)
                if (acceptedRequests.isEmpty()) {
                    Text("কোনো অ্যাকসেপ্টেড রিকোয়েস্ট নেই", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                        items(acceptedRequests) { request ->
                            var rider by remember { mutableStateOf<User?>(null) }
                            var isChatEnabled by remember { mutableStateOf(false) }
                            LaunchedEffect(request.id) {
                                rider = AuthRepository().getUser(request.riderId).getOrNull()
                                isChatEnabled = AuthRepository().isChatEnabled(request.id)
                            }
                            Card(modifier = Modifier.padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("রাইডার: ${rider?.name ?: "লোড হচ্ছে"} (${rider?.phone ?: "N/A"})")
                                    Text("পিকআপ: ${request.pickup}")
                                    Text("গন্তব্য: ${request.destination}")
                                    Text("সিট: ${request.seats}")
                                    Text("ভাড়া: ${request.fare}")
                                    Text("OTP: ${request.otp ?: "N/A"}")
                                    if (isChatEnabled) {
                                        Button(onClick = { navController.navigate("chat/${request.id}") }) {
                                            Text("Chat with Rider")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else if (currentLocation != null) {
                                val result = AuthRepository().updateConductorLocation(user.uid, currentLocation!!)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "লোকেশন আপডেট হয়েছে", Toast.LENGTH_SHORT).show()
                                    refreshData()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("লোকেশন আপডেট করুন") }
            }
        }
    }

    // ── CREATE DIALOG ─────────────────────────────────────────────────────
    if (showCreateDialog && assignedBus != null) {
        ScheduleDialog(
            title = "শিডিউল তৈরি করুন",
            initialDate = "",
            initialStart = "",
            initialEnd = "",
            initialDirection = "going",
            onConfirm = { date, start, end, dir ->
                scope.launch {
                    try {
                        val startMs = parseTime(date, start)
                        val endMs = parseTime(date, end)
                        if (endMs <= startMs) throw Exception("শেষের সময় শুরুর পরে হতে হবে")
                        val result = AuthRepository().createSchedule(
                            busId = assignedBus!!.busId,
                            conductorId = user.uid,
                            startTime = startMs,
                            endTime = endMs,
                            date = date,
                            direction = dir
                        )
                        if (result.isSuccess) {
                            Toast.makeText(context, "তৈরি সফল", Toast.LENGTH_SHORT).show()
                            refreshData()
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: "ত্রুটি")
                    } finally {
                        showCreateDialog = false
                    }
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // ── EDIT DIALOG ───────────────────────────────────────────────────────
    if (showEditDialog && editingSchedule != null) {
        ScheduleDialog(
            title = "শিডিউল সম্পাদনা করুন",
            initialDate = selectedDate,
            initialStart = selectedStartTime,
            initialEnd = selectedEndTime,
            initialDirection = direction,
            onConfirm = { date, start, end, dir ->
                scope.launch {
                    try {
                        val startMs = parseTime(date, start)
                        val endMs = parseTime(date, end)
                        if (endMs <= startMs) throw Exception("শেষের সময় শুরুর পরে হতে হবে")
                        database.getReference("schedules")
                            .child(editingSchedule!!.scheduleId)
                            .updateChildren(mapOf(
                                "date" to date,
                                "startTime" to startMs,
                                "endTime" to endMs,
                                "direction" to dir
                            )).await()
                        Toast.makeText(context, "আপডেট সফল", Toast.LENGTH_SHORT).show()
                        refreshData()
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: "ত্রুটি")
                    } finally {
                        showEditDialog = false
                        editingSchedule = null
                    }
                }
            },
            onDismiss = {
                showEditDialog = false
                editingSchedule = null
            }
        )
    }
}

// ── REUSABLE DIALOG ─────────────────────────────────────────────────────
@Composable
fun ScheduleDialog(
    title: String,
    initialDate: String,
    initialStart: String,
    initialEnd: String,
    initialDirection: String,
    onConfirm: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableStateOf(initialDate) }
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }
    var direction by remember { mutableStateOf(initialDirection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(date, { date = it }, label = { Text("তারিখ (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(start, { start = it }, label = { Text("শুরুর সময় (HH:MM)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(end, { end = it }, label = { Text("শেষের সময় (HH:MM)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("দিক:", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(direction == "going", { direction = "going" }, label = { Text("যাচ্ছি") })
                    FilterChip(direction == "returning", { direction = "returning" }, label = { Text("ফিরছি") })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(date, start, end, direction) }) { Text("সংরক্ষণ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } }
    )
}

// ── HELPER: Parse date + time to milliseconds ───────────────────────────
fun parseTime(date: String, time: String): Long {
    val parts = time.split(":")
    val cal = Calendar.getInstance()
    cal.set(date.substring(0,4).toInt(), date.substring(5,7).toInt() - 1, date.substring(8,10).toInt(), parts[0].toInt(), parts[1].toInt())
    return cal.timeInMillis
}