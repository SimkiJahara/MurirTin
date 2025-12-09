package com.example.muritin

import android.R
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.BackgroundLight
import com.example.muritin.ui.theme.Border
import com.example.muritin.ui.theme.Divider
import com.example.muritin.ui.theme.Info
import com.example.muritin.ui.theme.Primary
import com.example.muritin.ui.theme.PrimaryLight
import com.example.muritin.ui.theme.Secondary
import com.example.muritin.ui.theme.TextPrimary
import com.example.muritin.ui.theme.TextSecondary
import com.example.muritin.ui.theme.Warning
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var deleteTargetBus by remember { mutableStateOf<Bus?>(null) }

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
            val snapshot =
                FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
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
                    .padding(top = 40.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() }
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আমার বাসসমূহ",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${buses.size} টি বাস",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "লোড হচ্ছে...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = com.example.muritin.ui.theme.Error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            error ?: "অজানা ত্রুটি",
                            color = com.example.muritin.ui.theme.Error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    error = null
                                    navController.navigate("bus_list")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary
                            )
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("আবার চেষ্টা করুন")
                        }
                    }
                }
            } else if (buses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DirectionsBus,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "কোনো বাস নেই",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "আপনার সব রেজিস্টার করা বাস এখানে দেখাবে",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(buses) { bus ->
                        var assignedConductorId by remember(bus.busId) { mutableStateOf<String?>(null) }

                        LaunchedEffect(bus.busId) {
                            assignedConductorId = AuthRepository().getAssignedConductorForBus(bus.busId)
                        }
                        BusInfoCard(
                            bus = bus,
                            conductor = conductors.find { it.uid == assignedConductorId },
                            schedules = schedules[bus.busId] ?: emptyList(),
                            navController = navController,
                            onAssignConductor = {
                                selectedBus = bus
                                showAssignDialog = true
                            },
                            onDelete = {
                                deleteTargetBus = bus
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        if (showDeleteDialog && deleteTargetBus != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("বাস মুছে ফেলুন") },
                containerColor = BackgroundLight,
                text = { Text("আপনি কি নিশ্চিতভাবে এই বাসটি মুছে ফেলতে চান?") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val result = AuthRepository().deleteBus(deleteTargetBus!!.busId)
                            if (result.isSuccess) {
                                buses = AuthRepository().getBusesForOwner(user.uid)
                            } else {
                                snackbarHostState.showSnackbar("মুছতে ব্যর্থ")
                            }
                        }
                        showDeleteDialog = false
                    }) {
                        Text("হ্যাঁ", color = com.example.muritin.ui.theme.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("না", color = Primary)
                    }
                }
            )
        }

        if (showAssignDialog) {
            AlertDialog(
                onDismissRequest = { showAssignDialog = false },
                containerColor = BackgroundLight,
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedLabelColor = Primary,
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Border
                                ),
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
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("সংরক্ষণ করুন")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAssignDialog = false }) { Text("বাতিল", color = com.example.muritin.ui.theme.Error) }
                }
            )
        }
    }
}

@Composable
fun BusInfoCard(
    bus: Bus,
    conductor: User?,
    schedules: List<Schedule>,
    navController: NavHostController,
    onAssignConductor: () -> Unit,
    onDelete: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.DirectionsBus,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bus.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "লাইসেন্স নম্বর: ${bus.number}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick info chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InfoChip(Icons.Outlined.Badge, "ফিটনেস: ${bus.fitnessCertificate}", Info)
                Spacer(modifier = Modifier.height(6.dp))
                InfoChip(Icons.Outlined.FileCopy, "ট্যাক্স: ${bus.taxToken}", Warning)
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Divider)
                Spacer(modifier = Modifier.height(16.dp))

                Column{
                    Text(
                        "বাসের রুট",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text("${bus.route?.originLoc?.address} ->", color = Primary)
                    bus.route?.stopPointsLoc?.forEach { stop ->
                        Text("${stop.address} ->")
                    }
                    Text("${bus.route?.destinationLoc?.address}", color = Primary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column {
                    Text(
                        "ভাড়া তালিকা",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    bus.fares.forEach { (origin, dests) ->
                        dests.forEach { (dest, fare) ->
                            Text(
                                buildAnnotatedString {
                                    append("$origin → $dest: ")
                                    withStyle(style = SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {   // your blue color
                                        append("$fare টাকা")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(
                                color = Divider,
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Divider)
                Spacer(modifier = Modifier.height(16.dp))

                // Conductor info
                DetailRow(
                    icon = Icons.Outlined.Person,
                    label = "অ্যাসাইনড কন্ডাক্টর",
                    value = conductor?.name ?: "কোনোটি নেই"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(
                        "চলমান ও আসন্ন শিডিউল",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))

                    if (schedules.isEmpty()) {
                        Text("কোনো শিডিউল নেই", color = TextSecondary)
                    } else {
                        schedules.forEach { schedule ->
                            val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            Text("তারিখ: ${schedule.date}", fontWeight = FontWeight.Bold, color = Primary)
                            Text(
                                "শুরু: ${fmt.format(Date(schedule.startTime))}, শেষ: ${
                                    fmt.format(
                                        Date(schedule.endTime)
                                    )
                                }",
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider( color = Divider, thickness = 1.dp )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                val busRatings by produceState<BusRatings?>(initialValue = null, bus.busId) {
                    value = AuthRepository().getBusRatings(bus.busId)
                }

                Button(
                    onClick = {
                        navController.navigate("bus_ratings/${bus.busId}")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight)
                ) {
                    Icon(Icons.Outlined.Star, contentDescription = null)
                    Spacer(Modifier.width(6.dp))

                    val buttonText = buildString {
                        append("মূল্যায়ন দেখুন")
                        busRatings?.let { ratings ->
                            if (ratings.totalRatings > 0) {
                                val avg = "%.2f".format(ratings.averageRating)
                                append(" (${avg} ★ | ${ratings.totalRatings})")
                            }
                        }
                    }

                    Text(buttonText)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        navController.navigate("analytics_report/${bus.busId}")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Outlined.BarChart, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("পরিসংখ্যান রিপোর্ট")
                }

                Button(
                    onClick = onAssignConductor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Secondary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("কন্ডাক্টর অ্যাসাইন করুন")
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.muritin.ui.theme.Error,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("বাস ডিলিট ")
                }
            }
        }
    }
}




//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("আমার বাসসমূহ") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    titleContentColor = MaterialTheme.colorScheme.onPrimary
//                )
//            )
//        },
//        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(horizontal = 16.dp),
//            verticalArrangement = Arrangement.Top,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            if (isLoading) {
//                CircularProgressIndicator()
//            } else if (error != null) {
//                Text(
//                    text = error ?: "অজানা ত্রুটি",
//                    color = MaterialTheme.colorScheme.error,
//                    style = MaterialTheme.typography.bodyLarge
//                )
//            } else if (buses.isEmpty()) {
//                Text("কোনো বাস পাওয়া যায়নি")
//            } else {
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .weight(1f)
//                ) {
//                    items(buses) { bus ->
//                        var assignedConductorId by remember(bus.busId) { mutableStateOf<String?>(null) }
//
//                        LaunchedEffect(bus.busId) {
//                            assignedConductorId = AuthRepository().getAssignedConductorForBus(bus.busId)
//                        }
//
//                        Card(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 8.dp),
//                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//                        ) {
//                            Column(modifier = Modifier.padding(16.dp)) {
//                                Text("নাম: ${bus.name}")
//                                Text("নম্বর: ${bus.number}")
//                                Text("ফিটনেস সার্টিফিকেট: ${bus.fitnessCertificate}")
//                                Text("ট্যাক্স টোকেন: ${bus.taxToken}")
//                                Text("স্টপস: ${bus.stops.joinToString(", ")}")
//                                Text("ভাড়া তালিকা:")
//                                bus.fares.forEach { (stop, dests) ->
//                                    dests.forEach { (dest, fare) ->
//                                        Text("$stop থেকে $dest: $fare টাকা")
//                                    }
//                                }
//                                Text("অ্যাসাইনড কন্ডাক্টর: ${conductors.find { it.uid == assignedConductorId }?.name ?: assignedConductorId ?: "কোনোটি নেই"}")
//                                Text("শিডিউল তালিকা (চলমান এবং আসন্ন):")
//                                schedules[bus.busId]?.forEach { schedule ->
//                                    Text(
//                                        "তারিখ: ${schedule.date}, শুরুর সময়: ${
//                                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(schedule.startTime))
//                                        }, শেষের সময়: ${
//                                            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(schedule.endTime))
//                                        }"
//                                    )
//                                } ?: Text("কোনো শিডিউল নেই")
//
//                                Column(modifier = Modifier.fillMaxWidth()) {
//                                    // Check if bus has active schedule
//                                    val hasActiveSchedule = schedules[bus.busId]?.any { schedule ->
//                                        val now = System.currentTimeMillis()
//                                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
//                                        schedule.date == today && schedule.startTime <= now && schedule.endTime >= now
//                                    } ?: false
//
//                                    // Live Location Button (only shown if schedule is active)
//                                    if (hasActiveSchedule) {
//                                        Button(
//                                            onClick = {
//                                                navController.navigate("bus_live_tracking/${bus.busId}")
//                                            },
//                                            modifier = Modifier.fillMaxWidth(),
//                                            colors = ButtonDefaults.buttonColors(
//                                                containerColor = MaterialTheme.colorScheme.tertiary
//                                            )
//                                        ) {
//                                            Text("🚌 লাইভ লোকেশন দেখুন")
//                                        }
//                                        Spacer(modifier = Modifier.height(8.dp))
//                                    }
//
//                                    Button(
//                                        onClick = {
//                                            navController.navigate("analytics_report/${bus.busId}")
//                                        },
//                                        modifier = Modifier.fillMaxWidth()
//                                    ) {
//                                        Text("পরিসংখ্যান রিপোর্ট দেখুন")
//                                    }
//                                    Spacer(modifier = Modifier.height(8.dp))
//
//                                    Button(
//                                        onClick = {
//                                            selectedBus = bus
//                                            selectedConductorId = assignedConductorId
//                                            showAssignDialog = true
//                                        },
//                                        modifier = Modifier.fillMaxWidth()
//                                    ) {
//                                        Text("কন্ডাক্টর অ্যাসাইন করুন")
//                                    }
//                                    Spacer(modifier = Modifier.height(8.dp))
//
//                                    Button(
//                                        onClick = {
//                                            navController.navigate("bus_ratings/${bus.busId}")
//                                        },
//                                        modifier = Modifier.fillMaxWidth()
//                                    ) {
//                                        Text("মূল্যায়ন দেখুন")
//                                    }
//
//                                    var busRatings by remember(bus.busId) { mutableStateOf<BusRatings?>(null) }
//
//                                    LaunchedEffect(bus.busId) {
//                                        busRatings = AuthRepository().getBusRatings(bus.busId)
//                                    }
//
//                                    busRatings?.let { ratings ->
//                                        if (ratings.totalRatings > 0) {
//                                            Row(
//                                                verticalAlignment = Alignment.CenterVertically,
//                                                modifier = Modifier.padding(vertical = 4.dp)
//                                            ) {
//                                                Text("রেটিং: ")
//                                                RatingDisplay(ratings.averageRating, ratings.totalRatings)
//                                            }
//                                        }
//                                    }
//
//                                    Spacer(modifier = Modifier.height(8.dp))
//
//                                    Button(
//                                        onClick = { showDeleteBusDialog = true },
//                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
//                                        modifier = Modifier.fillMaxWidth()
//                                    ) {
//                                        Text("মুছুন")
//                                    }
//
//                                    if (showDeleteBusDialog) {
//                                        AlertDialog(
//                                            onDismissRequest = { showDeleteBusDialog = false },
//                                            title = { Text("বাস মুছে ফেলুন") },
//                                            text = { Text("আপনি কি নিশ্চিতভাবে এই বাস মুছে ফেলতে চান? এই ক্রিয়াটি পূর্বাবস্থায় ফেরানো যাবে না।") },
//                                            confirmButton = {
//                                                TextButton(
//                                                    onClick = {
//                                                        scope.launch {
//                                                            val result = AuthRepository().deleteBus(bus.busId)
//                                                            if (result.isSuccess) {
//                                                                Toast.makeText(context, "বাস মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
//                                                                buses = AuthRepository().getBusesForOwner(user.uid)
//                                                                schedules = buses.associate { bus ->
//                                                                    bus.busId to AuthRepository().getSchedulesForBus(bus.busId)
//                                                                }
//                                                            } else {
//                                                                error = "বাস মুছে ফেলতে ব্যর্থ: ${result.exceptionOrNull()?.message}"
//                                                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
//                                                            }
//                                                        }
//                                                        showDeleteBusDialog = false
//                                                    }
//                                                ) {
//                                                    Text("হ্যাঁ")
//                                                }
//                                            },
//                                            dismissButton = {
//                                                TextButton(
//                                                    onClick = { showDeleteBusDialog = false }
//                                                ) {
//                                                    Text("না")
//                                                }
//                                            }
//                                        )
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    if (showAssignDialog) {
//        AlertDialog(
//            onDismissRequest = { showAssignDialog = false },
//            title = { Text("কন্ডাক্টর অ্যাসাইন করুন") },
//            text = {
//                Column {
//                    var expanded by remember { mutableStateOf(false) }
//                    ExposedDropdownMenuBox(
//                        expanded = expanded,
//                        onExpandedChange = { expanded = !expanded }
//                    ) {
//                        OutlinedTextField(
//                            value = selectedConductorId?.let { id ->
//                                conductors.find { it.uid == id }?.name ?: id
//                            } ?: "কোনোটি নির্বাচন করুন",
//                            onValueChange = {},
//                            readOnly = true,
//                            label = { Text("কন্ডাক্টর নির্বাচন করুন") },
//                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .menuAnchor()
//                        )
//                        ExposedDropdownMenu(
//                            expanded = expanded,
//                            onDismissRequest = { expanded = false }
//                        ) {
//                            DropdownMenuItem(
//                                text = { Text("কোনোটি নেই") },
//                                onClick = {
//                                    selectedConductorId = null
//                                    expanded = false
//                                },
//                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
//                            )
//                            conductors.forEach { conductor ->
//                                DropdownMenuItem(
//                                    text = { Text(conductor.name ?: conductor.email) },
//                                    onClick = {
//                                        selectedConductorId = conductor.uid
//                                        expanded = false
//                                    },
//                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
//                                )
//                            }
//                        }
//                    }
//                }
//            },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        scope.launch {
//                            selectedBus?.busId?.let { busId ->
//                                if (selectedConductorId != null) {
//                                    val result = AuthRepository().assignConductorToBus(busId, selectedConductorId!!)
//                                    if (result.isSuccess) {
//                                        Toast.makeText(context, "কন্ডাক্টর অ্যাসাইন হয়েছে", Toast.LENGTH_SHORT).show()
//                                    } else {
//                                        error = result.exceptionOrNull()?.message ?: "কন্ডাক্টর অ্যাসাইন ব্যর্থ"
//                                        scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
//                                    }
//                                } else {
//                                    val result = AuthRepository().removeConductorFromBus(busId)
//                                    if (result.isSuccess) {
//                                        Toast.makeText(context, "কন্ডাক্টর সরানো হয়েছে", Toast.LENGTH_SHORT).show()
//                                    } else {
//                                        error = result.exceptionOrNull()?.message ?: "কন্ডাক্টর সরাতে ব্যর্থ"
//                                        scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
//                                    }
//                                }
//                                buses = AuthRepository().getBusesForOwner(user.uid)
//                                schedules = buses.associate { bus ->
//                                    bus.busId to AuthRepository().getSchedulesForBus(bus.busId)
//                                }
//                            }
//                            showAssignDialog = false
//                        }
//                    }
//                ) {
//                    Text("সংরক্ষণ করুন")
//                }
//            },
//            dismissButton = {
//                TextButton(onClick = { showAssignDialog = false }) { Text("বাতিল") }
//            }
//        )
//    }
