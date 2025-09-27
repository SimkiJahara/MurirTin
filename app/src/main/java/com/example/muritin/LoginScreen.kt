package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val roles = listOf("Rider" to "রাইডার", "Conductor" to "কন্ডাক্টর", "Owner" to "ওনার")
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
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
            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = "মুড়ি টিন লোগো",
                modifier = Modifier
                    .size(100.dp)
                    .semantics { contentDescription = "মুড়ি টিন লোগো" },
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "মুড়ির টিন",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

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
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ইমেইল") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "ইমেইল আইকন",
                                modifier = Modifier.semantics { contentDescription = "ইমেইল আইকন" }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "ইমেইল প্রবেশ করান" },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("পাসওয়ার্ড") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "পাসওয়ার্ড আইকন",
                                modifier = Modifier.semantics { contentDescription = "পাসওয়ার্ড আইকন" }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "পাসওয়ার্ড প্রবেশ করান" },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = roles.find { it.first == role }?.second ?: "রাইডার",
                            onValueChange = {},
                            label = { Text("রোল নির্বাচন করুন") },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .semantics { contentDescription = "রোল নির্বাচন" }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            roles.forEach { (key, value) ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        role = key
                                        expanded = false
                                    },
                                    modifier = Modifier.semantics { contentDescription = "রোল: $value" }
                                )
                            }
                        }
                    }
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
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error!!)
                                    }
                                }
                            }
                        }
                    } else {
                        error = "ইমেইল এবং পাসওয়ার্ড পূরণ করুন"
                        scope.launch {
                            snackbarHostState.showSnackbar(error!!)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .semantics { contentDescription = "সাইন আপ বাটন" },
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
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error!!)
                                    }
                                }
                            }
                        }
                    } else {
                        error = "ইমেইল এবং পাসওয়ার্ড পূরণ করুন"
                        scope.launch {
                            snackbarHostState.showSnackbar(error!!)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .semantics { contentDescription = "লগইন বাটন" },
                enabled = !isLoading
            ) {
                Text("লগইন")
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