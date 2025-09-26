package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

// LoginScreen is the entry UI for users to sign up or log in to মুড়ির টিন.
//  Provides a user-friendly interface for email-based authentication and role selection.
//  Uses Jetpack Compose to render input fields and buttons, calls AuthRepository for backend logic.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController, // Navigation controller to switch screens.
    onLoginSuccess: (User) -> Unit // Callback to handle successful login/signup.
) {
    // Get Android context for showing toasts.
    val context = LocalContext.current
    // Coroutine scope for async Firebase calls.
    val scope = rememberCoroutineScope()
    // State for email input field.
    var email by remember { mutableStateOf("") }
    // State for password input field.
    var password by remember { mutableStateOf("") }
    // State for role selection (Rider or Conductor).
    var role by remember { mutableStateOf("Rider") }
    // State to show loading indicator during Firebase calls.
    var isLoading by remember { mutableStateOf(false) }
    // State to display error messages.
    var error by remember { mutableStateOf<String?>(null) }

    // Main layout: Centered column with padding for a clean look.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App logo (bus icon) for branding.
        Icon(
            imageVector = Icons.Default.DirectionsBus,
            contentDescription = "App Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        // App title in Bangla for localization.
        Text(
            text = "মুড়ির টিন",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Email input field.
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("ইমেইল") }, // Bangla label for email.
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Password input field.
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("পাসওয়ার্ড") }, // Bangla label for password.
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Role selection radio buttons for Rider or Conductor.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = role == "Rider",
                    onClick = { role = "Rider" }
                )
                Text("রাইডার") // Bangla label for Rider role.
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = role == "Conductor",
                    onClick = { role = "Conductor" }
                )
                Text("কন্ডাক্টর") // Bangla label for Conductor role.
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Signup button to create a new user account.
        Button(
            onClick = {
                // Validate inputs before calling signup.
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    scope.launch {
                        Log.d("LoginScreen", "Attempting signup with email: $email, role: $role")
                        // Call AuthRepository to perform signup.
                        val result = AuthRepository().signup(email, password, role)
                        isLoading = false
                        when {
                            result.isSuccess -> {
                                Log.d("LoginScreen", "Signup successful for ${result.getOrNull()?.email}")
                                Toast.makeText(context, "সাইন আপ সফল!", Toast.LENGTH_SHORT).show()
                                result.getOrNull()?.let { onLoginSuccess(it) } // Navigate to dashboard.
                            }
                            else -> {
                                // Handle signup failure and show error.
                                val message = result.exceptionOrNull()?.message ?: "অজানা ত্রুটি"
                                Log.e("LoginScreen", "Signup failed: $message")
                                error = "সাইন আপ ব্যর্থ: $message"
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    // Show error if inputs are empty.
                    error = "ইমেইল এবং পাসওয়ার্ড পূরণ করুন"
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Disable button during loading.
        ) {
            Text("সাইন আপ") // Bangla label for signup.
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Login button for existing users.
        OutlinedButton(
            onClick = {
                // Validate inputs before calling login.
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    scope.launch {
                        Log.d("LoginScreen", "Attempting login with email: $email")
                        // Call AuthRepository to perform login.
                        val result = AuthRepository().login(email, password)
                        isLoading = false
                        when {
                            result.isSuccess -> {
                                Log.d("LoginScreen", "Login successful for ${result.getOrNull()?.email}")
                                Toast.makeText(context, "লগইন সফল!", Toast.LENGTH_SHORT).show()
                                result.getOrNull()?.let { onLoginSuccess(it) } // Navigate to dashboard.
                            }
                            else -> {
                                // Handle login failure and show error.
                                val message = result.exceptionOrNull()?.message ?: "অজানা ত্রুটি"
                                Log.e("LoginScreen", "Login failed: $message")
                                error = "লগইন ব্যর্থ: $message"
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    // Show error if inputs are empty.
                    error = "ইমেইল এবং পাসওয়ার্ড পূরণ করুন"
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Disable button during loading.
        ) {
            Text("লগইন") // Bangla label for login.
        }

        // Show loading indicator during Firebase operations.
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        // Display error messages if any.
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
