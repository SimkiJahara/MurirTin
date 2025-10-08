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
fun ConductorDashboard(navController: NavHostController, user: FirebaseUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var assignedBus by remember { mutableStateOf<Bus?>(null) }
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    LaunchedEffect(user.uid) {
        try {
            Log.d("ConductorDashboard", "Fetching assignments for conductorId: ${user.uid}")
            val snapshot = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("busAssignments")
                .orderByChild("conductorId")
                .equalTo(user.uid)
                .get()
                .await()
            val busId = snapshot.children.firstOrNull()?.key
            if (busId != null) {
                Log.d("ConductorDashboard", "Found busId: $busId")
                assignedBus = AuthRepository().getBus(busId)
                schedules = AuthRepository().getSchedulesForConductor(user.uid)
            } else {
                Log.d("ConductorDashboard", "No bus assigned to conductor")
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
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
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
            } else {
                Text(
                    text = "কন্ডাক্টর ড্যাশবোর্ড",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text("স্বাগতম, ${user.email}")
                Spacer(modifier = Modifier.height(24.dp))
                if (assignedBus != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("অ্যাসাইনড বাস: ${assignedBus!!.name}")
                            Text("নম্বর: ${assignedBus!!.number}")
                            Text("স্টপস: ${assignedBus!!.stops.joinToString(", ")}")
                            Text("ভাড়া তালিকা:")
                            assignedBus!!.fares.forEach { (stop, dests) ->
                                dests.forEach { (dest, fare) ->
                                    Text("$stop থেকে $dest: $fare টাকা")
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "কোনো বাস অ্যাসাইন করা হয়নি",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (user != null) {
                            Toast.makeText(context, "অ্যাকাউন্ট এর তথ্য আসবে", Toast.LENGTH_SHORT).show()
                            navController.navigate("show_account_info")
                        } else {
                            Toast.makeText(context, "দয়া করে লগইন করুন", Toast.LENGTH_SHORT).show()
                            navController.navigate("login")
                        }
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
                Text("শিডিউল তালিকা", style = MaterialTheme.typography.titleMedium)
                if (schedules.isEmpty()) {
                    Text(
                        text = "কোনো শিডিউল পাওয়া যায়নি",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Ensures LazyColumn takes available space
                    ) {
                        items(schedules) { schedule ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("বাস: ${assignedBus?.name ?: "N/A"}")
                                    Text("তারিখ: ${schedule.date}")
                                    Text("শুরুর সময়: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(schedule.startTime))}")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        Log.d("ConductorDashboard", "Logging out")
                        Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
                        onLogout()
                    }
                ) {
                    Text("লগআউট")
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
                        value = selectedTime,
                        onValueChange = { selectedTime = it },
                        label = { Text("শুরুর সময় (HH:MM, 24-ঘন্টা ফরম্যাট)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedTime.isNotBlank() && !selectedTime.matches(Regex("\\d{2}:\\d{2}"))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!selectedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            scope.launch { snackbarHostState.showSnackbar("বৈধ তারিখ প্রয়োজন (YYYY-MM-DD)") }
                        } else if (!selectedTime.matches(Regex("\\d{2}:\\d{2}"))) {
                            scope.launch { snackbarHostState.showSnackbar("বৈধ সময় প্রয়োজন (HH:MM)") }
                        } else {
                            scope.launch {
                                try {
                                    val timeParts = selectedTime.split(":")
                                    val calendar = Calendar.getInstance().apply {
                                        set(
                                            selectedDate.substring(0, 4).toInt(),
                                            selectedDate.substring(5, 7).toInt() - 1,
                                            selectedDate.substring(8, 10).toInt(),
                                            timeParts[0].toInt(),
                                            timeParts[1].toInt()
                                        )
                                    }
                                    val result = AuthRepository().createSchedule(
                                        busId = assignedBus!!.busId,
                                        conductorId = user.uid,
                                        startTime = calendar.timeInMillis,
                                        date = selectedDate
                                    )
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "শিডিউল তৈরি সফল", Toast.LENGTH_SHORT).show()
                                        schedules = AuthRepository().getSchedulesForConductor(user.uid)
                                        showScheduleDialog = false
                                        selectedDate = ""
                                        selectedTime = ""
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
                TextButton(onClick = { showScheduleDialog = false }) { Text("বাতিল") }
            }
        )
    }
}