package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Show_Account_Info(
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "No email"
    val uid = user?.uid ?: "No UID"

    LaunchedEffect(uid) {
        val result = AuthRepository().getUser(uid)
        if (result.isSuccess) {
            userData = result.getOrNull()
            isLoading = false
        } else {
            error = "তথ্য উদ্ধার সম্ভব হয়নি"
            isLoading = false
            scope.launch {
                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("অ্যাকাউন্ট তথ্য") },
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
            verticalArrangement = Arrangement.Center,
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "অ্যাকাউন্ট তথ্য",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "নাম:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = userData?.name ?: "Loading...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "ইমেইল আড্রেস:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "ফোন নম্বর:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = userData?.phone ?: "Loading...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "বয়স:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = userData?.age?.toString() ?: "Loading...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        // Role-specific fields
                        if (userData?.role == "Conductor") {
                            Text(
                                text = "লাইসেন্স স্থিতি:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "লাইসেন্স তথ্য আসবে", // Placeholder for Week 3
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else if (userData?.role == "Owner") {
                            Text(
                                text = "বাসের সংখ্যা:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "বাস তথ্য আসবে", // Placeholder for Week 4
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    navController.navigate("profile_update")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("তথ্য পরিবর্তন করুন")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                // TODO: Prompt user for new password
                                currentUser.updatePassword("newPassword123").await() // Replace with user input
                                Toast.makeText(context, "পাসওয়ার্ড পরিবর্তন সফল", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "দয়া করে লগইন করুন", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                            }
                        } catch (e: Exception) {
                            error = "পাসওয়ার্ড পরিবর্তন ব্যর্থ: ${e.message}"
                            scope.launch {
                                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("পাসওয়ার্ড পরিবর্তন করুন")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                // Delete from Realtime Database
                                val dbResult = AuthRepository().deleteUserData(currentUser.uid)
                                if (dbResult.isSuccess) {
                                    // Delete Firebase Authentication account
                                    currentUser.delete().await()
                                    Toast.makeText(context, "অ্যাকাউন্ট ডিলিট সফল", Toast.LENGTH_SHORT).show()
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                } else {
                                    error = "ডাটাবেস থেকে ডিলিট ব্যর্থ: ${dbResult.exceptionOrNull()?.message}"
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                    }
                                }
                            } else {
                                Toast.makeText(context, "দয়া করে লগইন করুন", Toast.LENGTH_SHORT).show()
                                navController.navigate("login")
                            }
                        } catch (e: Exception) {
                            error = "অ্যাকাউন্ট ডিলিট ব্যর্থ: ${e.message}"
                            scope.launch {
                                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB71C1C),
                    contentColor = Color.White
                )
            ) {
                Text("অ্যাকাউন্ট ডিলিট করুন")
            }
        }
    }
}





