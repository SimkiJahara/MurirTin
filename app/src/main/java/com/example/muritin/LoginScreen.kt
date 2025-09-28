package com.example.muritin

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("লগইন") },
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
            // App logo and title
            Image(
                imageVector = Icons.Filled.DirectionsBus,
                contentDescription = "মুড়ির টিন লোগো",
                modifier = Modifier
                    .size(80.dp)
                    .semantics { contentDescription = "মুড়ির টিন লোগো" }
            )
            Text(
                text = "মুড়ির টিন",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

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
                        text = "লগইন করুন",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ইমেইল") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "ইমেইল ক্ষেত্র" },
                        isError = !isValidEmail(email) && email.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("পাসওয়ার্ড") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "পাসওয়ার্ড ক্ষেত্র" },
                        isError = password.length < 6 && password.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!isValidEmail(email)) {
                                error = "বৈধ ইমেইল প্রয়োজন"
                                scope.launch {
                                    snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                }
                            } else if (password.length < 6) {
                                error = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে"
                                scope.launch {
                                    snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                }
                            } else {
                                isLoading = true
                                error = null
                                scope.launch {
                                    val result = AuthRepository().login(email, password)
                                    isLoading = false
                                    when {
                                        result.isSuccess -> {
                                            Toast.makeText(context, "লগইন সফল", Toast.LENGTH_SHORT).show()
                                            result.getOrNull()?.let { onLoginSuccess(it) }
                                        }
                                        else -> {
                                            error = result.exceptionOrNull()?.message ?: "লগইন ব্যর্থ"
                                            scope.launch {
                                                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "লগইন বোতাম" },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("লগইন")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "অ্যাকাউন্ট নেই? এখানে একটি তৈরি করুন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                navController.navigate("signup")
                            }
                            .semantics { contentDescription = "নিবন্ধন লিঙ্ক" },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}