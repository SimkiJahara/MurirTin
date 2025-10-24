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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorDashboard(navController: NavHostController, user: FirebaseUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var assignedBus by remember { mutableStateOf<Bus?>(null) }
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var pendingRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var acceptedRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedStartTime by remember { mutableStateOf("") }
    var selectedEndTime by remember { mutableStateOf("") }

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
                    } else {
                        error = "লোকেশন পাওয়া যায়নি"
                        scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                    }
                } catch (e: Exception) {
                    error = "লোকেশন পুনরুদ্ধারে ত্রুটি: ${e.message}"
                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                    Log.e("ConductorDashboard", "Location fetch failed: ${e.message}", e)
                }
            }
        } else {
            error = "লোকেশন পারমিশন প্রয়োজন। অনুগ্রহ করে সেটিংস থেকে অনুমতি দিন।"
            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
        }
    }

    LaunchedEffect(user.uid) {
        try {
            val snapshot = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("busAssignments")
                .orderByChild("conductorId")
                .equalTo(user.uid)
                .get()
                .await()
            val busId = snapshot.children.firstOrNull()?.key
            if (busId != null) {
                assignedBus = AuthRepository().getBus(busId)
                schedules = AuthRepository().getSchedulesForConductor(user.uid)
                schedules = schedules.filter { it.endTime >= System.currentTimeMillis() }
                acceptedRequests = AuthRepository().getAcceptedRequestsForConductor(user.uid)
            } else {
                error = "কোনো বাস অ্যাসাইন করা হয়নি"
            }

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
                    }
                } else {
                    error = "লোকেশন পাওয়া যায়নি"
                }
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            isLoading = false
        } catch (e: Exception) {
            error = "তথ্য পুনরুদ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
            Log.e("ConductorDashboard", "Error fetching data: ${e.message}", e)
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
            } else if (error != null) {
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
                if (assignedBus != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "অ্যাসাইনড বাস: ${assignedBus!!.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "নম্বর: ${assignedBus!!.number}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "স্টপস: ${assignedBus!!.stops.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "ভাড়া তালিকা:",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            assignedBus!!.fares.forEach { (stop, dests) ->
                                dests.forEach { (dest, fare) ->
                                    Text(
                                        text = "$stop থেকে $dest: $fare টাকা",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "কোনো বাস অ্যাসাইন করা হয়নি",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "অ্যাকাউন্ট এর তথ্য আসবে", Toast.LENGTH_SHORT).show()
                        navController.navigate("show_account_info")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("অ্যাকাউন্ট এর তথ্য")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showScheduleDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = assignedBus != null
                ) {
                    Text("শিডিউল তৈরি করুন")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "শিডিউল তালিকা",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                if (schedules.isEmpty()) {
                    Text(
                        text = "কোনো শিডিউল নেই",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(schedules) { schedule ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "বাস: ${assignedBus?.name ?: "N/A"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "তারিখ: ${schedule.date}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "শুরুর সময়: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(schedule.startTime))}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "শেষের সময়: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(schedule.endTime))}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "পেন্ডিং রিকোয়েস্টসমূহ",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                if (pendingRequests.isEmpty()) {
                    Text(
                        text = if (currentLocation == null) "লোকেশন পাওয়া যায়নি, রিকোয়েস্ট দেখানো যাচ্ছে না" else "কোনো পেন্ডিং রিকোয়েস্ট নেই",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(pendingRequests) { request ->
                            Card(modifier = Modifier.padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "পিকআপ: ${request.pickup}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "গন্তব্য: ${request.destination}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "সিট: ${request.seats}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "ভাড়া: ${request.fare}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Button(onClick = {
                                        scope.launch {
                                            val result = AuthRepository().acceptRequest(request.id, user.uid)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "অ্যাকসেপ্ট সফল", Toast.LENGTH_SHORT).show()
                                                if (currentLocation != null && assignedBus != null) {
                                                    pendingRequests = AuthRepository().getPendingRequestsForConductor(
                                                        conductorId = user.uid,
                                                        currentLocation = currentLocation!!,
                                                        bus = assignedBus!!,
                                                        apiKey = apiKey,
                                                        directionsApi = directionsApi
                                                    )
                                                    acceptedRequests = AuthRepository().getAcceptedRequestsForConductor(user.uid)
                                                }
                                            } else {
                                                Toast.makeText(context, "অ্যাকসেপ্ট ব্যর্থ: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }) {
                                        Text("অ্যাকসেপ্ট করুন")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "অ্যাকসেপ্টেড রিকোয়েস্টসমূহ",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                if (acceptedRequests.isEmpty()) {
                    Text(
                        text = "কোনো অ্যাকসেপ্টেড রিকোয়েস্ট নেই",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(acceptedRequests) { request ->
                            var rider by remember { mutableStateOf<User?>(null) }
                            LaunchedEffect(request.riderId) {
                                rider = AuthRepository().getUser(request.riderId).getOrNull()
                            }
                            Card(modifier = Modifier.padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "রাইডার: ${rider?.name ?: "লোড হচ্ছে"} (${rider?.phone ?: "N/A"})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "পিকআপ: ${request.pickup}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "গন্তব্য: ${request.destination}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "সিট: ${request.seats}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "ভাড়া: ${request.fare}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "OTP: ${request.otp ?: "N/A"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
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
                                    if (assignedBus != null) {
                                        pendingRequests = AuthRepository().getPendingRequestsForConductor(
                                            conductorId = user.uid,
                                            currentLocation = currentLocation!!,
                                            bus = assignedBus!!,
                                            apiKey = apiKey,
                                            directionsApi = directionsApi
                                        )
                                    }
                                } else {
                                    Toast.makeText(context, "লোকেশন আপডেট ব্যর্থ", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "লোকেশন পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("লোকেশন আপডেট করুন")
                }
            }
        }
    }

    if (showScheduleDialog && assignedBus != null) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text("শিডিউল তৈরি করুন") },
            text = {
                Column {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedDate.isNotBlank() && !selectedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedStartTime,
                        onValueChange = { selectedStartTime = it },
                        label = { Text("শুরুর সময় (HH:MM, 24-ঘন্টা ফরম্যাট)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedStartTime.isNotBlank() && !selectedStartTime.matches(Regex("\\d{2}:\\d{2}"))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedEndTime,
                        onValueChange = { selectedEndTime = it },
                        label = { Text("শেষের সময় (HH:MM, 24-ঘন্টা ফরম্যাট)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedEndTime.isNotBlank() && !selectedEndTime.matches(Regex("\\d{2}:\\d{2}"))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!selectedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            scope.launch { snackbarHostState.showSnackbar("বৈধ তারিখ প্রয়োজন (YYYY-MM-DD)") }
                        } else if (!selectedStartTime.matches(Regex("\\d{2}:\\d{2}"))) {
                            scope.launch { snackbarHostState.showSnackbar("বৈধ শুরুর সময় প্রয়োজন (HH:MM)") }
                        } else if (!selectedEndTime.matches(Regex("\\d{2}:\\d{2}"))) {
                            scope.launch { snackbarHostState.showSnackbar("বৈধ শেষের সময় প্রয়োজন (HH:MM)") }
                        } else {
                            scope.launch {
                                try {
                                    val startTimeParts = selectedStartTime.split(":")
                                    val endTimeParts = selectedEndTime.split(":")
                                    val calendar = Calendar.getInstance()
                                    calendar.set(
                                        selectedDate.substring(0, 4).toInt(),
                                        selectedDate.substring(5, 7).toInt() - 1,
                                        selectedDate.substring(8, 10).toInt(),
                                        startTimeParts[0].toInt(),
                                        startTimeParts[1].toInt()
                                    )
                                    val startTime = calendar.timeInMillis
                                    calendar.set(
                                        selectedDate.substring(0, 4).toInt(),
                                        selectedDate.substring(5, 7).toInt() - 1,
                                        selectedDate.substring(8, 10).toInt(),
                                        endTimeParts[0].toInt(),
                                        endTimeParts[1].toInt()
                                    )
                                    val endTime = calendar.timeInMillis
                                    if (endTime <= startTime) {
                                        scope.launch { snackbarHostState.showSnackbar("শেষের সময় শুরুর সময়ের পরে হতে হবে") }
                                        return@launch
                                    }
                                    val result = AuthRepository().createSchedule(
                                        busId = assignedBus!!.busId,
                                        conductorId = user.uid,
                                        startTime = startTime,
                                        endTime = endTime,
                                        date = selectedDate
                                    )
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "শিডিউল তৈরি সফল", Toast.LENGTH_SHORT).show()
                                        schedules = AuthRepository().getSchedulesForConductor(user.uid)
                                        schedules = schedules.filter { it.endTime >= System.currentTimeMillis() }
                                        showScheduleDialog = false
                                        selectedDate = ""
                                        selectedStartTime = ""
                                        selectedEndTime = ""
                                    } else {
                                        error = result.exceptionOrNull()?.message ?: "শিডিউল তৈরি ব্যর্থ"
                                        scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                    }
                                } catch (e: Exception) {
                                    error = "শিডিউল তৈরি ব্যর্থ: ${e.message}"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                }
                            }
                        }
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}