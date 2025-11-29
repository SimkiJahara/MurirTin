package com.example.muritin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorSchedule(navController: NavHostController, user: FirebaseUser) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database =
        FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")

    var assignedBus by remember { mutableStateOf<Bus?>(null) }
    var schedules by remember { mutableStateOf<List<Schedule>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var isLoading by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }

    var selectedDate by remember { mutableStateOf("") }
    var selectedStartTime by remember { mutableStateOf("") }
    var selectedEndTime by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("going") }

    suspend fun refreshData() {
        isRefreshing = true
        try {
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
                isLoading = false
            } else {
                assignedBus = null
                schedules = emptyList()
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
            error = "রিফ্রেশ ব্যর্থ: ${e.message}"
            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
        } finally {
            isRefreshing = false
        }
    }

    LaunchedEffect(user.uid) {
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
            isLoading = false
        } else {
            assignedBus = null
            schedules = emptyList()
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Primary, PrimaryLight)
                        )
                    )
                    .padding(top = 30.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = { navController.navigate("conductor_dashboard") },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = Primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "যাত্রার শিডিউল",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp)
            ) {
                if (isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                } else if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Error.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error ?: "",
                                color = Error,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Spacer(Modifier.height(8.dp))
                            ModernActionButton(
                                icon = Icons.Outlined.Edit,
                                text = "শিডিউল তৈরি করুন",
                                onClick = {
                                    if (assignedBus != null) {
                                        showCreateDialog = true
                                    } else {
                                        Toast.makeText(context, "আপনাকে কোনো বাস অ্যাসাইন করা হয়নি", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                backgroundColor = Secondary,
                            )
                            Spacer(Modifier.height(8.dp))
                            ManagementSectionCard(
                                title = "আমার শিডিউল তালিকা",
                                icon = Icons.Filled.Schedule,
                                iconColor = RouteBlue
                            ) {
                                if (schedules.isEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = TextSecondary.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Outlined.Info,
                                                null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "কোনো শিডিউল নেই",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 260.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(schedules) { schedule ->
                                            var showDeleteDialog by remember { mutableStateOf(false) }
                                            var showEditDialogLocal by remember { mutableStateOf(false) }

                                            ScheduleItemCard(
                                                schedule = schedule,
                                                assignedBus = assignedBus,
                                                onEdit = {
                                                    editingSchedule = schedule
                                                    selectedDate = schedule.date
                                                    selectedStartTime =
                                                        SimpleDateFormat(
                                                            "HH:mm",
                                                            Locale.getDefault()
                                                        ).format(Date(schedule.startTime))
                                                    selectedEndTime =
                                                        SimpleDateFormat(
                                                            "HH:mm",
                                                            Locale.getDefault()
                                                        ).format(Date(schedule.endTime))
                                                    direction = schedule.direction
                                                    showEditDialog = true
                                                },
                                                onDelete = {
                                                    showDeleteDialog = true
                                                }
                                            )

                                            if (showDeleteDialog) {
                                                AlertDialog(
                                                    onDismissRequest = {
                                                        showDeleteDialog = false
                                                    },
                                                    title = { Text("শিডিউল মুছে ফেলবেন?") },
                                                    text = {
                                                        Text(
                                                            "তারিখ: ${schedule.date}"
                                                        )
                                                    },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    try {
                                                                        database.getReference("schedules")
                                                                            .child(schedule.scheduleId)
                                                                            .removeValue()
                                                                            .await()
                                                                        Toast.makeText(
                                                                            context,
                                                                            "মুছে ফেলা হয়েছে",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                        refreshData()
                                                                    } catch (_: Exception) {
                                                                    } finally {
                                                                        showDeleteDialog = false
                                                                    }
                                                                }
                                                            },
                                                            colors = ButtonDefaults.textButtonColors(
                                                                contentColor = MaterialTheme.colorScheme.error
                                                            )
                                                        ) { Text("মুছে ফেলুন") }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = {
                                                            showDeleteDialog = false
                                                        }) { Text("বাতিল") }
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
        }

        if (showCreateDialog ) {
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
                            scope.launch(Dispatchers.IO) {
                                val result = AuthRepository().createSchedule(
                                    busId = assignedBus!!.busId,
                                    conductorId = user.uid,
                                    startTime = startMs,
                                    endTime = endMs,
                                    date = date,
                                    direction = dir
                                )
                                withContext(Dispatchers.Main) {
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "তৈরি সফল", Toast.LENGTH_SHORT).show()
                                        refreshData()
                                    }
                                    showCreateDialog = false
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            showCreateDialog = false
                        }
                    }
                },
                onDismiss = { showCreateDialog = false }
            )
        }

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
                            database.getReference("schedules")
                                .child(editingSchedule!!.scheduleId)
                                .updateChildren(
                                    mapOf(
                                        "date" to date,
                                        "startTime" to startMs,
                                        "endTime" to endMs,
                                        "direction" to dir
                                    )
                                ).await()
                            Toast.makeText(context, "আপডেট সফল", Toast.LENGTH_SHORT).show()
                            refreshData()
                        } catch (_: Exception) {
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
}

@Composable
fun ScheduleItemCard(
    schedule: Schedule,
    assignedBus: Bus?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val start = sdf.format(Date(schedule.startTime))
    val end = sdf.format(Date(schedule.endTime))
    val canModify = schedule.startTime > System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            InfoLine(
                icon = Icons.Outlined.CalendarMonth,
                label = "তারিখ",
                value = schedule.date,
                tint = RouteGreen
            )

            Spacer(Modifier.height(6.dp))

            InfoLine(
                icon = Icons.Outlined.Schedule,
                label = "সময়",
                value = "$start - $end",
                tint = RouteOrange
            )

            Spacer(Modifier.height(6.dp))

            InfoLine(
                icon = Icons.Outlined.Route,
                label = "দিক",
                value = if (schedule.direction == "going") "যাচ্ছি" else "ফিরছি",
                tint = if (schedule.direction == "going") Primary else RoutePurple
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (canModify) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun InfoLine(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

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
        confirmButton = {
            Button(onClick = { onConfirm(date, start, end, direction) }) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("তারিখ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("শুরুর সময় (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("শেষের সময় (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("দিক:", style = MaterialTheme.typography.labelLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = direction == "going",
                        onClick = { direction = "going" },
                        label = { Text("যাচ্ছি") }
                    )
                    FilterChip(
                        selected = direction == "returning",
                        onClick = { direction = "returning" },
                        label = { Text("ফিরছি") }
                    )
                }
            }
        }
    )
}

fun parseTime(date: String, time: String): Long {
    val parts = time.split(":")
    val cal = Calendar.getInstance()
    cal.set(
        date.substring(0, 4).toInt(),
        date.substring(5, 7).toInt() - 1,
        date.substring(8, 10).toInt(),
        parts[0].toInt(),
        parts[1].toInt()
    )
    return cal.timeInMillis
}
