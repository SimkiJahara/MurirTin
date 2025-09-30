package com.example.muritin

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
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
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

// Userprofile_Update is the entry UI for all tye of users to easily update their account
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Userprofile_Update(
    navController: NavHostController, // Navigation controller to switch screens.
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var error by remember { mutableStateOf<String?>(null) }

    var old_name: String? by remember { mutableStateOf("") }
    var old_phone: String? by remember { mutableStateOf("") }
    var old_age: String? by remember { mutableStateOf("") }
    var old_email by remember { mutableStateOf("") }

    var new_name by remember { mutableStateOf("") }
    var new_phone by remember { mutableStateOf("") }
    var new_age by remember { mutableStateOf("") }
    var new_email by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }

    val user = FirebaseAuth.getInstance().currentUser   //Getting the current logged in user object
    old_email = user?.email ?: "No email"
    val uid = user?.uid ?: "No UID"

    // Validation functions
    fun isValidPhone(phone: String): Boolean {
        val cleanedPhone = phone.replace("\\s+".toRegex(), "")
        return cleanedPhone.matches(Regex("^(\\+8801|01)[3-9]\\d{8}$"))
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidAge(age: String): Boolean {
        return age.toIntOrNull()?.let { it in 18..100 } ?: false
    }

    //Fetching existing information about
    scope.launch {
        val result = AuthRepository().getUser(uid)
        if(result.isSuccess){
            val user = result.getOrNull() // The User object
            old_name = user?.name
            old_phone = user?.phone
            old_age = user?.age?.toString() ?: ""

            new_name = old_name ?: ""
            new_phone = old_phone ?: ""
            new_age = old_age ?: ""
            new_email = old_email ?: ""

            isLoading = false
        }
        else{
            Toast.makeText(context, "তথ্য উদ্ধার সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    //Update info form
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("নিবন্ধন করুন") },
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
                        text = "আপনার অ্যাকাউন্ট তৈরি করুন",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = new_name,
                        onValueChange = { new_name = it },
                        label = { Text("নাম") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "নাম ক্ষেত্র" },
                        isError = new_name.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = new_phone,
                        onValueChange = { new_phone = it },
                        label = { Text("ফোন নম্বর (+880)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "ফোন নম্বর ক্ষেত্র" },
                        isError = !isValidPhone(new_phone) && new_phone.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = new_age,
                        onValueChange = { new_age = it },
                        label = { Text("বয়স") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "বয়স ক্ষেত্র" },
                        isError = !isValidAge(new_age) && new_age.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = new_email,
                        onValueChange = { new_email = it },
                        label = { Text("ইমেইল") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "ইমেইল ক্ষেত্র" },
                        isError = !isValidEmail(new_email) && new_email.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (new_name.isBlank()) {
                                error = "নাম প্রয়োজন"
                                scope.launch {
                                    snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                }
                            } else if (!isValidPhone(new_phone)) {
                                error = "বৈধ ফোন নম্বর প্রয়োজন (+8801Xxxxxxxxx)"
                                scope.launch {
                                    snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                }
                            } else if (!isValidAge(new_age)) {
                                error = "বয়স ১৮-১০০ এর মধ্যে হতে হবে"
                                scope.launch {
                                    snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                }
                            } else if (!isValidEmail(new_email)) {
                                error = "বৈধ ইমেইল প্রয়োজন"
                                scope.launch {
                                    snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                }
                            }
                            else {
                                isLoading = true
                                error = null
                                scope.launch {
                                    val result = AuthRepository().updateUserProfile(
                                        uid,
                                        new_name,
                                        new_phone,
                                        new_age.toIntOrNull() ?: 0,
                                        new_email
                                    )
                                    isLoading = false
                                    when {
                                        result.isSuccess -> {
                                            Toast.makeText(context, "পরিবর্তন সফল হয়েছে", Toast.LENGTH_SHORT).show()
                                            navController.navigate("show_account_info")
                                        }
                                        else -> {
                                            error = result.exceptionOrNull()?.message ?: "পরিবর্তন ব্যর্থ"
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
                            .semantics { contentDescription = "পরিবর্তন বোতাম" },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("পরিবর্তন করুন") //Button to save the changes
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            navController.navigate("show_account_info")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White
                        )

                    ) {
                        Text("পরিবর্তন বাতিল করুন") // Cancel changes button
                    }

                }
            }
        }
    }

}

