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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.EmailAuthProvider
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

    // State for password change dialog
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordChangeLoading by remember { mutableStateOf(false) }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }

    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "No email"
    val uid = user?.uid ?: "No UID"

    LaunchedEffect(uid) {
        val result = AuthRepository().getUser(uid)
        if (result.isSuccess) {
            userData = result.getOrNull()
            isLoading = false
        } else {
            error = "তথ্য উদ্ধার সম্ভব হয়নি"
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
                            text = "বয়স:",
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

            Button(
                onClick = { showPasswordChangeDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("পাসওয়ার্ড পরিবর্তন করুন")
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

    // Password Change Dialog
    if (showPasswordChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordChangeDialog = false },
            title = { Text("পাসওয়ার্ড পরিবর্তন করুন") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("বর্তমান পাসওয়ার্ড") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "বর্তমান পাসওয়ার্ড ক্ষেত্র" },
                        isError = currentPassword.length < 6 && currentPassword.isNotBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("নতুন পাসওয়ার্ড") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "নতুন পাসওয়ার্ড ক্ষেত্র" },
                        isError = newPassword.length < 6 && newPassword.isNotBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("নতুন পাসওয়ার্ড নিশ্চিত করুন") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "নতুন পাসওয়ার্ড নিশ্চিতকরণ ক্ষেত্র" },
                        isError = (confirmNewPassword != newPassword || confirmNewPassword.length < 6) && confirmNewPassword.isNotBlank()
                    )
                    if (passwordChangeError != null) {
                        Text(
                            text = passwordChangeError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPassword.length < 6) {
                            passwordChangeError = "বর্তমান পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে"
                        } else if (newPassword.length < 6) {
                            passwordChangeError = "নতুন পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে"
                        } else if (newPassword != confirmNewPassword) {
                            passwordChangeError = "নতুন পাসওয়ার্ড মিলছে না"
                        } else {
                            passwordChangeLoading = true
                            passwordChangeError = null
                            scope.launch {
                                try {
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser != null && currentUser.email != null) {
                                        // Re-authenticate the user
                                        val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                                        currentUser.reauthenticate(credential).await()
                                        // Update password
                                        currentUser.updatePassword(newPassword).await()
                                        Toast.makeText(context, "পাসওয়ার্ড পরিবর্তন সফল", Toast.LENGTH_SHORT).show()
                                        showPasswordChangeDialog = false
                                        // Clear fields
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmNewPassword = ""
                                    } else {
                                        passwordChangeError = "ব্যবহারকারী লগইন নেই"
                                        scope.launch {
                                            snackbarHostState.showSnackbar(passwordChangeError ?: "অজানা ত্রুটি")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ShowAccountInfo", "Password change failed: ${e.message}", e)
                                    passwordChangeError = when {
                                        e.message?.contains("wrong password") == true -> "বর্তমান পাসওয়ার্ড ভুল"
                                        e.message?.contains("too many requests") == true -> "অনেকগুলো অনুরোধ, পরে চেষ্টা করুন"
                                        else -> e.message ?: "পাসওয়ার্ড পরিবর্তন ব্যর্থ"
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(passwordChangeError ?: "অজানা ত্রুটি")
                                    }
                                } finally {
                                    passwordChangeLoading = false
                                }
                            }
                        }
                    },
                    enabled = !passwordChangeLoading
                ) {
                    if (passwordChangeLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("পরিবর্তন করুন")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordChangeDialog = false
                    currentPassword = ""
                    newPassword = ""
                    confirmNewPassword = ""
                    passwordChangeError = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }
}






































