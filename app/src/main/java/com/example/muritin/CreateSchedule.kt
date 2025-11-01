package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Create_Schedule_Of_Bus(
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(true) }
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "No email"
    val uid = user?.uid ?: ""
    val scrollState = rememberScrollState()

    var goingRouteId by remember { mutableStateOf("") }
    var comingRouteId by remember { mutableStateOf("") }
    var selectedDateForGoing by remember { mutableStateOf("") }
    var selectedStartTimeForGoing by remember { mutableStateOf("") }
    var selectedEndTimeForGoing by remember { mutableStateOf("") }
    var selectedDateForComing by remember { mutableStateOf("") }
    var selectedStartTimeForComing by remember { mutableStateOf("") }
    var selectedEndTimeForComing by remember { mutableStateOf("") }
    var assignedBus by remember { mutableStateOf<Bus?>(null) }

    LaunchedEffect(uid) {
        try {
            val snapshot = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("busAssignments")
                .orderByChild("conductorId")
                .equalTo(user?.uid)
                .get()
                .await()
            val busId = snapshot.children.firstOrNull()?.key
            if (busId != null) {
                assignedBus = AuthRepository().getBus(busId)
                //Getting 2 routeIds for going and coming route
                val snapshot2 = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("busRoutes")
                    .orderByChild("busId")
                    .equalTo(busId)
                    .get()
                    .await()

                for (child in snapshot2.children) {
                    val status = child.child("status").getValue(String::class.java)
                    val routeId = child.child("routeId").getValue(String::class.java)

                    when (status) {
                        "Going" -> goingRouteId = routeId.toString()
                        "Coming" -> comingRouteId = routeId.toString()
                    }
                }
            } else {
                error = "কোনো বাস অ্যাসাইন করা হয়নি"
            }
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
                title = { Text("বাসের শিডিউল") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "শিডিউল তৈরি করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    //Schedule for going
                    Text(
                        text = "যাওয়ার শিডিউল তৈরি করুন",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = selectedDateForGoing,
                        onValueChange = { selectedDateForGoing = it },
                        label = { Text("তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedDateForGoing.isNotBlank() && !selectedDateForGoing.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedStartTimeForGoing,
                        onValueChange = { selectedStartTimeForGoing = it },
                        label = { Text("শুরুর সময় (HH:MM, 24-ঘন্টা ফরম্যাট)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedStartTimeForGoing.isNotBlank() && !selectedStartTimeForGoing.matches(Regex("\\d{2}:\\d{2}"))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedEndTimeForGoing,
                        onValueChange = { selectedEndTimeForGoing = it },
                        label = { Text("শেষের সময় (HH:MM, 24-ঘন্টা ফরম্যাট)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedEndTimeForGoing.isNotBlank() && !selectedEndTimeForGoing.matches(Regex("\\d{2}:\\d{2}"))
                    )
                    //Schedule for coming
                    Text(
                        text = "আসার শিডিউল তৈরি করুন",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = selectedDateForComing,
                        onValueChange = { selectedDateForComing = it },
                        label = { Text("তারিখ (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedDateForComing.isNotBlank() && !selectedDateForComing.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedStartTimeForComing,
                        onValueChange = { selectedStartTimeForComing = it },
                        label = { Text("শুরুর সময় (HH:MM, 24-ঘন্টা ফরম্যাট)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedStartTimeForComing.isNotBlank() && !selectedStartTimeForComing.matches(Regex("\\d{2}:\\d{2}"))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = selectedEndTimeForComing,
                        onValueChange = { selectedEndTimeForComing = it },
                        label = { Text("শেষের সময় (HH:MM, 24-ঘন্টা ফরম্যাট)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = selectedEndTimeForComing.isNotBlank() && !selectedEndTimeForComing.matches(Regex("\\d{2}:\\d{2}"))
                    )
                    Text(goingRouteId)
                    Text(comingRouteId)
                    Button(
                        onClick = {
                            if (!selectedDateForGoing.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                scope.launch { snackbarHostState.showSnackbar("বৈধ তারিখ প্রয়োজন (YYYY-MM-DD)") }
                            } else if (!selectedStartTimeForGoing.matches(Regex("\\d{2}:\\d{2}"))) {
                                scope.launch { snackbarHostState.showSnackbar("বৈধ শুরুর সময় প্রয়োজন (HH:MM)") }
                            } else if (!selectedEndTimeForGoing.matches(Regex("\\d{2}:\\d{2}"))) {
                                scope.launch { snackbarHostState.showSnackbar("বৈধ শেষের সময় প্রয়োজন (HH:MM)") }
                            } else if (!selectedDateForComing.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                scope.launch { snackbarHostState.showSnackbar("বৈধ তারিখ প্রয়োজন (YYYY-MM-DD)") }
                            } else if (!selectedStartTimeForComing.matches(Regex("\\d{2}:\\d{2}"))) {
                                scope.launch { snackbarHostState.showSnackbar("বৈধ শুরুর সময় প্রয়োজন (HH:MM)") }
                            } else if (!selectedEndTimeForComing.matches(Regex("\\d{2}:\\d{2}"))) {
                                scope.launch { snackbarHostState.showSnackbar("বৈধ শেষের সময় প্রয়োজন (HH:MM)") }
                            } else {
                                scope.launch {
                                    try {
                                        //Going Trip Validation
                                        val startTimeParts = selectedStartTimeForGoing.split(":")
                                        val endTimeParts = selectedEndTimeForGoing.split(":")
                                        val calendar = Calendar.getInstance()
                                        calendar.set(
                                            selectedDateForGoing.substring(0, 4).toInt(),
                                            selectedDateForGoing.substring(5, 7).toInt() - 1,
                                            selectedDateForGoing.substring(8, 10).toInt(),
                                            startTimeParts[0].toInt(),
                                            startTimeParts[1].toInt()
                                        )
                                        val startTimeForGoing = calendar.timeInMillis
                                        calendar.set(
                                            selectedDateForGoing.substring(0, 4).toInt(),
                                            selectedDateForGoing.substring(5, 7).toInt() - 1,
                                            selectedDateForGoing.substring(8, 10).toInt(),
                                            endTimeParts[0].toInt(),
                                            endTimeParts[1].toInt()
                                        )
                                        val endTimeForGoing = calendar.timeInMillis
                                        if (endTimeForGoing <= startTimeForGoing) {
                                            scope.launch { snackbarHostState.showSnackbar("শেষের সময় শুরুর সময়ের পরে হতে হবে") }
                                            return@launch
                                        }
                                        //Coming Trip Validation
                                        val startTimePartsComing = selectedStartTimeForComing.split(":")
                                        val endTimePartsComing = selectedEndTimeForComing.split(":")
                                        val calendarComing = Calendar.getInstance()

                                        calendarComing.set(
                                            selectedDateForComing.substring(0, 4).toInt(),
                                            selectedDateForComing.substring(5, 7).toInt() - 1,
                                            selectedDateForComing.substring(8, 10).toInt(),
                                            startTimePartsComing[0].toInt(),
                                            startTimePartsComing[1].toInt()
                                        )
                                        val startTimeForComing = calendarComing.timeInMillis

                                        calendarComing.set(
                                            selectedDateForComing.substring(0, 4).toInt(),
                                            selectedDateForComing.substring(5, 7).toInt() - 1,
                                            selectedDateForComing.substring(8, 10).toInt(),
                                            endTimePartsComing[0].toInt(),
                                            endTimePartsComing[1].toInt()
                                        )
                                        val endTimeForComing = calendarComing.timeInMillis

                                        if (startTimeForComing <= endTimeForGoing) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("ফেরার শুরুর সময় আসার শেষের সময়ের পরে হতে হবে")
                                            }
                                            return@launch
                                        }
                                        if (endTimeForComing <= startTimeForComing) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("ফেরার শেষের সময় শুরুর সময়ের পরে হতে হবে")
                                            }
                                            return@launch
                                        }
                                        // Date Checking
                                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        val goingDate = dateFormat.parse(selectedDateForGoing)
                                        val comingDate = dateFormat.parse(selectedDateForComing)

                                        val today = Calendar.getInstance()
                                        today.set(Calendar.HOUR_OF_DAY, 0)
                                        today.set(Calendar.MINUTE, 0)
                                        today.set(Calendar.SECOND, 0)
                                        today.set(Calendar.MILLISECOND, 0)

                                        if (goingDate == null || goingDate.before(today.time)) {
                                            scope.launch { snackbarHostState.showSnackbar("যাওয়ার তারিখ আজ বা তার পরে হতে হবে") }
                                            return@launch
                                        }

                                        if (comingDate == null || comingDate.before(today.time)) {
                                            scope.launch { snackbarHostState.showSnackbar("ফেরার তারিখ আজ বা তার পরে হতে হবে") }
                                            return@launch
                                        }

                                        if (goingDate != null && comingDate != null && comingDate.before(goingDate)) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("ফেরার তারিখ যাওয়ার তারিখের দিন বা পরে হতে হবে")
                                            }
                                            return@launch
                                        }
                                        val result1 = AuthRepository().createSchedule(
                                            busId = assignedBus!!.busId,
                                            routeId = goingRouteId,
                                            conductorId = user?.uid ?: "",
                                            startTime = startTimeForGoing,
                                            endTime = endTimeForGoing,
                                            date = selectedDateForGoing
                                        )
                                        val result2 = AuthRepository().createSchedule(
                                            busId = assignedBus!!.busId,
                                            routeId = comingRouteId,
                                            conductorId = user?.uid ?: "",
                                            startTime = startTimeForComing,
                                            endTime = endTimeForComing,
                                            date = selectedDateForComing
                                        )
                                        if (result1.isSuccess && result2.isSuccess) {
                                            Toast.makeText(context, "শিডিউল তৈরি সফল", Toast.LENGTH_SHORT).show()
                                        } else {
                                            error = result1.exceptionOrNull()?.message ?: "শিডিউল তৈরি ব্যর্থ"
                                            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                        }
                                        navController.navigate("conductor_dashboard")
                                    } catch (e: Exception) {
                                        error = "শিডিউল তৈরি ব্যর্থ: ${e.message}"
                                        scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সংরক্ষণ করুন")
                    }
                    Button(
                        onClick = {navController.navigate("conductor_dashboard")},
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White
                        )
                    )
                    {
                        Text("বাতিল করুন")
                    }
                }
            }
        }
    }
}