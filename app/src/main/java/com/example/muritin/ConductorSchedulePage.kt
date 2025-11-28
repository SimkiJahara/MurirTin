package com.example.muritin

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
            } else {
                assignedBus = null
                schedules = emptyList()
            }
        } catch (e: Exception) {
            error = "রিফ্রেশ ব্যর্থ: ${e.message}"
            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
            Log.e("ConductorDashboard", "Refresh failed: ${e.message}", e)
        } finally {
            isRefreshing = false
        }
    }

    LaunchedEffect(user.uid) {
        schedules = AuthRepository().getSchedulesForConductor(user.uid)
            .filter { it.endTime >= System.currentTimeMillis() }
    }
    // ── SCHEDULE LIST WITH EDIT & DELETE ─────────────────────────────
    Text("শিডিউল তালিকা", style = MaterialTheme.typography.titleMedium)
    if (schedules.isEmpty()) {
        Text("কোনো শিডিউল নেই", style = MaterialTheme.typography.bodyLarge)
    } else {
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = assignedBus != null
        ) { Text("শিডিউল তৈরি করুন") }

        Spacer(modifier = Modifier.height(16.dp))
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
                            AuthRepository()
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

