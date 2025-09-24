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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    onLoginSuccess: (User) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Rider") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsBus,
            contentDescription = "App Icon",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "মুরি টিন",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("ইমেইল") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("পাসওয়ার্ড") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

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
                Text("রাইডার")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = role == "Conductor",
                    onClick = { role = "Conductor" }
                )
                Text("কন্ডাক্টর")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    scope.launch {
                        Log.d("LoginScreen", "Attempting signup with email: $email, role: $role")
                        val result = AuthRepository().signup(email, password, role)
                        isLoading = false
                        when {
                            result.isSuccess -> {
                                Log.d("LoginScreen", "Signup successful for ${result.getOrNull()?.email}")
                                Toast.makeText(context, "সাইন আপ সফল!", Toast.LENGTH_SHORT).show()
                                result.getOrNull()?.let { onLoginSuccess(it) }
                            }
                            else -> {
                                val message = result.exceptionOrNull()?.message ?: "অজানা ত্রুটি"
                                Log.e("LoginScreen", "Signup failed: $message")
                                error = "সাইন আপ ব্যর্থ: $message"
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    error = "ইমেইল এবং পাসওয়ার্ড পূরণ করুন"
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("সাইন আপ")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    scope.launch {
                        Log.d("LoginScreen", "Attempting login with email: $email")
                        val result = AuthRepository().login(email, password)
                        isLoading = false
                        when {
                            result.isSuccess -> {
                                Log.d("LoginScreen", "Login successful for ${result.getOrNull()?.email}")
                                Toast.makeText(context, "লগইন সফল!", Toast.LENGTH_SHORT).show()
                                result.getOrNull()?.let { onLoginSuccess(it) }
                            }
                            else -> {
                                val message = result.exceptionOrNull()?.message ?: "অজানা ত্রুটি"
                                Log.e("LoginScreen", "Login failed: $message")
                                error = "লগইন ব্যর্থ: $message"
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    error = "ইমেইল এবং পাসওয়ার্ড পূরণ করুন"
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("লগইন")
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
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