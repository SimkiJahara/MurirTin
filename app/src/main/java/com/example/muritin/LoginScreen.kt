package com.example.muritin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (User) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // This should now resolve
    val authRepo = remember { AuthRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title
        Text(
            text = "মুড়ির টিন",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("ইমেইল") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("পাসওয়ার্ড") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    scope.launch {
                        authRepo.login(email, password).fold(
                            onSuccess = { user ->
                                isLoading = false
                                Toast.makeText(context, "লগইন সফল!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(user)
                            },
                            onFailure = { error ->
                                isLoading = false
                                Toast.makeText(context, "লগইন ব্যর্থ: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                } else {
                    Toast.makeText(context, "ইমেইল এবং পাসওয়ার্ড পূরণ করুন", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("লগইন করুন")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Signup Link
        TextButton(
            onClick = { navController.navigate("signup") }
        ) {
            Text("নতুন অ্যাকাউন্ট তৈরি করুন")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    navController: NavController,
    onSignupSuccess: (User) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Rider") } // Default to Rider
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // This should now resolve
    val authRepo = remember { AuthRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "নতুন অ্যাকাউন্ট",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("ইমেইল") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("পাসওয়ার্ড") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Role Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = role == "Rider",
                    onClick = { role = "Rider" },
                    enabled = !isLoading
                )
                Text("Rider")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = role == "Conductor",
                    onClick = { role = "Conductor" },
                    enabled = !isLoading
                )
                Text("Conductor")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Signup Button
        Button(
            onClick = {
                if (email.isNotBlank() && password.length >= 6) {
                    isLoading = true
                    scope.launch {
                        authRepo.signup(email, password, role).fold(
                            onSuccess = { user ->
                                isLoading = false
                                Toast.makeText(context, "সাইন আপ সফল!", Toast.LENGTH_SHORT).show()
                                onSignupSuccess(user)
                            },
                            onFailure = { error ->
                                isLoading = false
                                Toast.makeText(context, "সাইন আপ ব্যর্থ: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                } else {
                    Toast.makeText(context, "ইমেইল এবং ৬+ অক্ষরের পাসওয়ার্ড দিন", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("সাইন আপ")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back to Login
        TextButton(
            onClick = { navController.navigate("login") }
        ) {
            Text("ইতিমধ্যে অ্যাকাউন্ট আছে?")
        }
    }
}