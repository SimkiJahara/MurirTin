package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusListScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var buses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var conductors by remember { mutableStateOf<List<User>>(emptyList()) }
    var schedules by remember { mutableStateOf<Map<String, List<Schedule>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedBus by remember { mutableStateOf<Bus?>(null) }
    var selectedConductorId by remember { mutableStateOf<String?>(null) }
    var showDeleteBusDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid) {
        try {
            Log.d("BusListScreen", "Fetching buses for ownerId: ${user.uid}, email: ${user.email}")
            val ownerRole = AuthRepository().getUserRole(user.uid)
            Log.d("BusListScreen", "Owner role: $ownerRole")
            if (ownerRole != "Owner") {
                error = "শুধুমাত্র ওনাররা বাস তালিকা দেখতে পারেন"
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                }
                return@LaunchedEffect
            }
            // Clean up expired schedules
            AuthRepository().cleanExpiredSchedules()
            val snapshot = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("buses")
                .orderByChild("ownerId")
                .equalTo(user.uid)
                .get()
                .await()
            Log.d("BusListScreen", "Snapshot received: ${snapshot.childrenCount} children")
            buses = snapshot.children.mapNotNull { it.getValue(Bus::class.java) }
            conductors = AuthRepository().getConductorsForOwner(user.uid)
            schedules = buses.associate { bus ->
                bus.busId to AuthRepository().getSchedulesForBus(bus.busId)
            }
            isLoading = false
            Log.d("BusListScreen", "Fetched ${buses.size} buses")
        } catch (e: Exception) {
            error = "বাস তালিকা পুনরুদ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
            scope.launch {
                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
            }
            Log.e("BusListScreen", "Error fetching buses: ${e.message}", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("আমার বাসসমূহ") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(
                    text = error ?: "অজানা ত্রুটি",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else if (buses.isEmpty()) {
                Text("কোনো বাস পাওয়া যায়নি")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(buses) { bus ->
                        var assignedConductorId by remember(bus.busId) { mutableStateOf<String?>(null) }

                        LaunchedEffect(bus.busId) {
                            assignedConductorId = AuthRepository().getAssignedConductorForBus(bus.busId)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("নাম: ${bus.name}")
                                Text("নম্বর: ${bus.number}")
                                Text("ফিটনেস সার্টিফিকেট: ${bus.fitnessCertificate}")
                                Text("ট্যাক্স টোকেন: ${bus.taxToken}")
                                Text("স্টপস: ${bus.stops.joinToString(", ")}")
                                Text("ভাড়া তালিকা:")
                                bus.fares.forEach { (stop, dests) ->
                                    dests.forEach { (dest, fare) ->
                                        Text("$stop থেকে $dest: $fare টাকা")
                                    }
                                }
                                Text("অ্যাসাইনড কন্ডাক্টর: ${conductors.find { it.uid == assignedConductorId }?.name ?: assignedConductorId ?: "কোনোটি নেই"}")
                                Text("শিডিউল তালিকা (চলমান এবং আসন্ন):")
                                schedules[bus.busId]?.forEach { schedule ->
                                    Text(
                                        "তারিখ: ${schedule.date}, শুরুর সময়: ${
                                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(schedule.startTime))
                                        }, শেষের সময়: ${
                                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(schedule.endTime))
                                        }"
                                    )
                                } ?: Text("কোনো শিডিউল নেই")

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Check if bus has active schedule
                                    val hasActiveSchedule = schedules[bus.busId]?.any { schedule ->
                                        val now = System.currentTimeMillis()
                                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
                                        schedule.date == today && schedule.startTime <= now && schedule.endTime >= now
                                    } ?: false

                                    // Live Location Button (only shown if schedule is active)
                                    if (hasActiveSchedule) {
                                        Button(
                                            onClick = {
                                                navController.navigate("bus_live_tracking/${bus.busId}")
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary
                                            )
                                        ) {
                                            Text("🚌 লাইভ লোকেশন দেখুন")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Button(
                                        onClick = {
                                            navController.navigate("analytics_report/${bus.busId}")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("পরিসংখ্যান রিপোর্ট দেখুন")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            selectedBus = bus
                                            selectedConductorId = assignedConductorId
                                            showAssignDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("কন্ডাক্টর অ্যাসাইন করুন")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            navController.navigate("bus_ratings/${bus.busId}")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("মূল্যায়ন দেখুন")
                                    }

                                    var busRatings by remember(bus.busId) { mutableStateOf<BusRatings?>(null) }

                                    LaunchedEffect(bus.busId) {
                                        busRatings = AuthRepository().getBusRatings(bus.busId)
                                    }

                                    busRatings?.let { ratings ->
                                        if (ratings.totalRatings > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            ) {
                                                Text("রেটিং: ")
                                                RatingDisplay(ratings.averageRating, ratings.totalRatings)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { showDeleteBusDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("মুছুন")
                                    }

                                    if (showDeleteBusDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteBusDialog = false },
                                            title = { Text("বাস মুছে ফেলুন") },
                                            text = { Text("আপনি কি নিশ্চিতভাবে এই বাস মুছে ফেলতে চান? এই ক্রিয়াটি পূর্বাবস্থায় ফেরানো যাবে না।") },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        scope.launch {
                                                            val result = AuthRepository().deleteBus(bus.busId)
                                                            if (result.isSuccess) {
                                                                Toast.makeText(context, "বাস মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                                                buses = AuthRepository().getBusesForOwner(user.uid)
                                                                schedules = buses.associate { bus ->
                                                                    bus.busId to AuthRepository().getSchedulesForBus(bus.busId)
                                                                }
                                                            } else {
                                                                error = "বাস মুছে ফেলতে ব্যর্থ: ${result.exceptionOrNull()?.message}"
                                                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                                            }
                                                        }
                                                        showDeleteBusDialog = false
                                                    }
                                                ) {
                                                    Text("হ্যাঁ")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(
                                                    onClick = { showDeleteBusDialog = false }
                                                ) {
                                                    Text("না")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("কন্ডাক্টর অ্যাসাইন করুন") },
            text = {
                Column {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedConductorId?.let { id ->
                                conductors.find { it.uid == id }?.name ?: id
                            } ?: "কোনোটি নির্বাচন করুন",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("কন্ডাক্টর নির্বাচন করুন") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("কোনোটি নেই") },
                                onClick = {
                                    selectedConductorId = null
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                            conductors.forEach { conductor ->
                                DropdownMenuItem(
                                    text = { Text(conductor.name ?: conductor.email) },
                                    onClick = {
                                        selectedConductorId = conductor.uid
                                        expanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            selectedBus?.busId?.let { busId ->
                                if (selectedConductorId != null) {
                                    val result = AuthRepository().assignConductorToBus(busId, selectedConductorId!!)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "কন্ডাক্টর অ্যাসাইন হয়েছে", Toast.LENGTH_SHORT).show()
                                    } else {
                                        error = result.exceptionOrNull()?.message ?: "কন্ডাক্টর অ্যাসাইন ব্যর্থ"
                                        scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                    }
                                } else {
                                    val result = AuthRepository().removeConductorFromBus(busId)
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "কন্ডাক্টর সরানো হয়েছে", Toast.LENGTH_SHORT).show()
                                    } else {
                                        error = result.exceptionOrNull()?.message ?: "কন্ডাক্টর সরাতে ব্যর্থ"
                                        scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                    }
                                }
                                buses = AuthRepository().getBusesForOwner(user.uid)
                                schedules = buses.associate { bus ->
                                    bus.busId to AuthRepository().getSchedulesForBus(bus.busId)
                                }
                            }
                            showAssignDialog = false
                        }
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) { Text("বাতিল") }
            }
        )
    }
}