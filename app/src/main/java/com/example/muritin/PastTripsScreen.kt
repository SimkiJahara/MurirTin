package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
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
            error = "ট্রিপ পুনর্দ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
            scope.launch {
                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
            }
        }
    }

    LaunchedEffect(user.uid) {
        fetchPastTrips()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Top Bar with Gradient
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
                            text = "পূর্ববর্তী যাত্রাসমূহ",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${allRequests.size} টি সম্পন্ন যাত্রা",
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
                            tint = Error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            error ?: "অজানা ত্রুটি",
                            color = Error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    error = null
                                    fetchPastTrips()
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
            } else if (allRequests.isEmpty()) {
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
                            "কোনো পূর্ববর্তী যাত্রা নেই",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "আপনার সম্পন্ন যাত্রা এখানে দেখাবে",
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
                    items(allRequests) { request ->
                        PastTripCard(
                            request = request,
                            user = user,
                            navController = navController,
                            onRateTrip = {
                                selectedRequest = request
                                scope.launch {
                                    selectedBus = request.busId?.let { AuthRepository().getBus(it) }
                                    selectedConductor = AuthRepository().getUser(request.conductorId).getOrNull()
                                    showRatingDialog = true
                                }
                            },
                            onRefresh = {
                                scope.launch {
                                    isLoading = true
                                    fetchPastTrips()
                                }
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

@Composable
fun PastTripCard(
    request: Request,
    user: FirebaseUser,
    navController: NavHostController,
    onRateTrip: () -> Unit,
    onRefresh: () -> Unit
) {
    var bus by remember { mutableStateOf<Bus?>(null) }
    var conductor by remember { mutableStateOf<User?>(null) }
    var isChatEnabled by remember { mutableStateOf(false) }
    var scheduleEndTime by remember { mutableStateOf<Long?>(null) }
    var canRate by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with route
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
                        text = request.pickup,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = request.destination,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "কম দেখান" else "আরো দেখান",
                        tint = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    icon = Icons.Outlined.Payments,
                    label = "${request.fare} ৳",
                    color = Success
                )
                InfoChip(
                    icon = Icons.Outlined.EventSeat,
                    label = "${request.seats} সিট",
                    color = Info
                )
                request.otp?.let {
                    InfoChip(
                        icon = Icons.Outlined.Password,
                        label = it,
                        color = Warning
                    )
                }
            }

            // Expanded details
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Divider)
                Spacer(modifier = Modifier.height(16.dp))

                // Bus info
                bus?.let {
                    DetailRow(
                        icon = Icons.Outlined.DirectionsBus,
                        label = "বাস",
                        value = "${it.name} (${it.number})"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Conductor info
                conductor?.let {
                    DetailRow(
                        icon = Icons.Outlined.Person,
                        label = "কন্ডাক্টর",
                        value = it.name ?: "N/A"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(
                        icon = Icons.Outlined.Phone,
                        label = "ফোন",
                        value = it.phone ?: "N/A"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Schedule end time
                scheduleEndTime?.let { endTime ->
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    DetailRow(
                        icon = Icons.Outlined.Schedule,
                        label = "যাত্রা শেষ",
                        value = dateFormat.format(Date(endTime))
                    )

                    if (isChatEnabled) {
                        val chatExpiryTime = endTime + 432000000L
                        val now = System.currentTimeMillis()
                        val hoursLeft = ((chatExpiryTime - now) / (1000 * 60 * 60)).toInt()

                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(
                            icon = Icons.Outlined.Chat,
                            label = "চ্যাট উপলব্ধ",
                            value = "আর $hoursLeft ঘণ্টা"
                        )
                    }
                }
            }

            // Existing rating display
            request.rating?.let { rating ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Secondary.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "আপনার মূল্যায়ন",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RatingItem("কন্ডাক্টর", rating.conductorRating)
                            RatingItem("বাস", rating.busRating)
                            RatingItem("সামগ্রিক", rating.overallRating)
                        }
                        if (rating.comment.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "মন্তব্য: ${rating.comment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Action buttons
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rating button
                if (canRate) {
                    Button(
                        onClick = onRateTrip,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Secondary
                        )
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মূল্যায়ন করুন")
                    }
                }

                // Chat button
                if (isChatEnabled) {
                    Button(
                        onClick = {
                            navController.navigate("chat/${request.id}")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("চ্যাট")
                    }
                } else if (!canRate) {
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("চ্যাট সময় শেষ")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
fun RatingItem(label: String, rating: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        RatingDisplay(rating)
    }
}