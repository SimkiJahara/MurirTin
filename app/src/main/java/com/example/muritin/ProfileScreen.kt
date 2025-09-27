package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    user: User,
    onProfileComplete: (User) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(user.name ?: "") }
    var phone by remember { mutableStateOf(user.phone ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Phone validation: Bangladeshi format (+8801XXXXXXXXX or 01XXXXXXXXX)
    fun isValidPhone(phone: String): Boolean {
        val cleanedPhone = phone.replace("\\s+".toRegex(), "")
        return cleanedPhone.matches(Regex("^(\\+8801|01)[3-9]\\d{8}$"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রোফাইল সম্পূর্ণ করুন") },
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "আপনার বিবরণ পূরণ করুন",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("নাম") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "নাম প্রবেশ করান" },
                        singleLine = true,
                        isError = name.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("ফোন নম্বর") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "ফোন নম্বর প্রবেশ করান" },
                        singleLine = true,
                        isError = !isValidPhone(phone) && error != null
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                error = "নাম প্রয়োজন"
                                scope.launch {
                                    snackbarHostState.showSnackbar(error!!)
                                }
                            } else if (!isValidPhone(phone)) {
                                error = "বৈধ ফোন নম্বর প্রয়োজন (+8801XXXXXXXXX)"
                                scope.launch {
                                    snackbarHostState.showSnackbar(error!!)
                                }
                            } else {
                                isLoading = true
                                scope.launch {
                                    Log.d("ProfileScreen", "Saving profile for user ${user.uid}, name: $name, phone: $phone")
                                    val updatedUser = user.copy(name = name, phone = phone)
                                    val result = AuthRepository().updateProfile(updatedUser)
                                    isLoading = false
                                    when {
                                        result.isSuccess -> {
                                            Log.d("ProfileScreen", "Profile updated successfully")
                                            Toast.makeText(context, "প্রোফাইল সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                                            onProfileComplete(updatedUser)
                                        }
                                        else -> {
                                            val message = result.exceptionOrNull()?.message ?: "অজানা ত্রুটি"
                                            Log.e("ProfileScreen", "Profile update failed: $message")
                                            error = "প্রোফাইল আপডেট ব্যর্থ: $message"
                                            scope.launch {
                                                snackbarHostState.showSnackbar(error!!)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { contentDescription = "প্রোফাইল সংরক্ষণ বাটন" },
                        enabled = !isLoading
                    ) {
                        Text("সংরক্ষণ করুন")
                    }
                }
            }
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = "লোড হচ্ছে" }
                )
            }
        }
    }
}