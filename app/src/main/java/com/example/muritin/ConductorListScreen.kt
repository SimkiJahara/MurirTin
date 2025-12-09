package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.muritin.ui.theme.SecondaryVariant
import com.example.muritin.ui.theme.TextPrimary
import com.example.muritin.ui.theme.TextSecondary
import com.example.muritin.ui.theme.Warning
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorListScreen(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var conductors by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.uid) {
        try {
            Log.d("ConductorListScreen", "Fetching conductors for ownerId: ${user.uid}, email: ${user.email}")
            val ownerRole = AuthRepository().getUserRole(user.uid)
            Log.d("ConductorListScreen", "Owner role: $ownerRole")
            if (ownerRole != "Owner") {
                error = "শুধুমাত্র ওনাররা কন্ডাক্টর তালিকা দেখতে পারেন"
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                }
                return@LaunchedEffect
            }
            val snapshot = FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users")
                .orderByChild("ownerId")
                .equalTo(user.uid)
                .get()
                .await()
            Log.d("ConductorListScreen", "Snapshot received: ${snapshot.childrenCount} children")
            conductors = snapshot.children.mapNotNull { child ->
                val conductor = child.getValue(User::class.java)
                if (conductor?.role == "Conductor") conductor else null
            }
            isLoading = false
            Log.d("ConductorListScreen", "Fetched ${conductors.size} conductors")
        } catch (e: Exception) {
            error = "কন্ডাক্টর তালিকা পুনরুদ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
            scope.launch {
                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
            }
            Log.e("ConductorListScreen", "Error fetching conductors: ${e.message}", e)
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
                            text = "আমার কন্ডাক্টরসমূহ",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${conductors.size} জন কন্ডাক্টর",
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
            } else if (conductors.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.People,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "কোনো কন্ডাক্টর নেই",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "আপনার অধীনের সকল কন্ডাক্টর এখানে দেখাবে",
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
                    items(conductors) { conductor ->
                        ConductorInfoCard(
                            conductor = conductor,
                            navController = navController
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
}

@Composable
fun ConductorInfoCard(
    conductor: User?,
    navController: NavHostController
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
                        Icons.Filled.People,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conductor?.name.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "ইমেইল: ${conductor?.email}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conductor info
            DetailRow(
                icon = Icons.Outlined.Phone,
                label = "ফোন নম্বর",
                value = conductor?.phone!!
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(
                icon = Icons.Outlined.CalendarMonth,
                label = "বয়স",
                value = conductor.age.toString()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            var conductorRatings by remember(conductor?.uid) {
                mutableStateOf<ConductorRatings?>(
                    null
                )
            }

            LaunchedEffect(conductor?.uid) {
                if (conductor != null) {
                    conductorRatings = AuthRepository().getConductorRatings(conductor.uid )
                }
            }

            Button(
                onClick = {
                    if (conductor != null) {
                        navController.navigate("conductor_ratings/${conductor.uid}")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryVariant)
            ) {
                Icon(Icons.Outlined.Star, contentDescription = null)
                Spacer(Modifier.width(6.dp))

                val buttonText = buildString {
                    append("মূল্যায়ন দেখুন")
                    conductorRatings?.let { ratings ->
                        if (ratings.totalRatings > 0) {
                            val avg = "%.2f".format(ratings.averageRating)
                            append(" (${avg} ★ | ${ratings.totalRatings})")
                        }
                    }
                }

                Text(buttonText)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


//Scaffold(
//topBar = {
//    TopAppBar(
//        title = { Text("কন্ডাক্টর তালিকা") },
//        colors = TopAppBarDefaults.topAppBarColors(
//            containerColor = MaterialTheme.colorScheme.primary,
//            titleContentColor = MaterialTheme.colorScheme.onPrimary
//        )
//    )
//},
//snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
//) { padding ->
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(padding)
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Top,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        if (isLoading) {
//            CircularProgressIndicator()
//        } else if (error != null) {
//            Text(
//                text = error ?: "অজানা ত্রুটি",
//                color = MaterialTheme.colorScheme.error,
//                style = MaterialTheme.typography.bodyLarge
//            )
//        } else if (conductors.isEmpty()) {
//            Text("কোনো কন্ডাক্টর পাওয়া যায়নি")
//        } else {
//            LazyColumn {
//                items(conductors) { conductor ->
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(vertical = 8.dp),
//                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//                    ) {
////                            Column(
////                                modifier = Modifier.padding(16.dp)
////                            ) {
////                                Text("নাম: ${conductor.name ?: "N/A"}")
////                                Text("ইমেইল: ${conductor.email}")
////                                Text("ফোন: ${conductor.phone ?: "N/A"}")
////                                Text("বয়স: ${conductor.age ?: "N/A"}")
////                            }
//                        var conductorRatings by remember(conductor.uid) { mutableStateOf<ConductorRatings?>(null) }
//
//                        LaunchedEffect(conductor.uid) {
//                            conductorRatings = AuthRepository().getConductorRatings(conductor.uid)
//                        }
//
//                        Column(modifier = Modifier.padding(16.dp)) {
//                            Text("নাম: ${conductor.name ?: "N/A"}")
//                            Text("ইমেইল: ${conductor.email}")
//                            Text("ফোন: ${conductor.phone ?: "N/A"}")
//                            Text("বয়স: ${conductor.age ?: "N/A"}")
//
//                            conductorRatings?.let { ratings ->
//                                if (ratings.totalRatings > 0) {
//                                    Row(
//                                        verticalAlignment = Alignment.CenterVertically,
//                                        modifier = Modifier.padding(top = 8.dp)
//                                    ) {
//                                        Text("রেটিং: ")
//                                        RatingDisplay(ratings.averageRating, ratings.totalRatings)
//                                    }
//                                }
//                            }
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            Button(
//                                onClick = {
//                                    navController.navigate("conductor_ratings/${conductor.uid}")
//                                },
//                                modifier = Modifier.fillMaxWidth()
//                            ) {
//                                Text("বিস্তারিত মূল্যায়ন দেখুন")
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}