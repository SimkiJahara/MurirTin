package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorBusInfo(navController: NavHostController, user: FirebaseUser) {
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

    LaunchedEffect(user.uid) {
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
            }
        } catch (e: Exception) {
            error = "বাস পাওয়া ব্যর্থ: ${e.message}"
            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
            Log.e("ConductorBusInfo", "Bus Fetching failed: ${e.message}", e)
        }
    }

    Column {
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
    }
}

