package com.example.muritin

import android.util.Log
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কন্ডাক্টর তালিকা") },
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
            } else if (conductors.isEmpty()) {
                Text("কোনো কন্ডাক্টর পাওয়া যায়নি")
            } else {
                LazyColumn {
                    items(conductors) { conductor ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
//                            Column(
//                                modifier = Modifier.padding(16.dp)
//                            ) {
//                                Text("নাম: ${conductor.name ?: "N/A"}")
//                                Text("ইমেইল: ${conductor.email}")
//                                Text("ফোন: ${conductor.phone ?: "N/A"}")
//                                Text("বয়স: ${conductor.age ?: "N/A"}")
//                            }
                            var conductorRatings by remember(conductor.uid) { mutableStateOf<ConductorRatings?>(null) }

                            LaunchedEffect(conductor.uid) {
                                conductorRatings = AuthRepository().getConductorRatings(conductor.uid)
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("নাম: ${conductor.name ?: "N/A"}")
                                Text("ইমেইল: ${conductor.email}")
                                Text("ফোন: ${conductor.phone ?: "N/A"}")
                                Text("বয়স: ${conductor.age ?: "N/A"}")

                                conductorRatings?.let { ratings ->
                                    if (ratings.totalRatings > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Text("রেটিং: ")
                                            RatingDisplay(ratings.averageRating, ratings.totalRatings)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        navController.navigate("conductor_ratings/${conductor.uid}")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("বিস্তারিত মূল্যায়ন দেখুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}