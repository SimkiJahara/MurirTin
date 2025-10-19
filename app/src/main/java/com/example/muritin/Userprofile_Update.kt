package com.example.muritin

import android.util.Log
import android.util.Patterns
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Userprofile_Update(
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var error by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var role by remember { mutableStateOf("") }

    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: "No UID"

    fun isValidPhone(phone: String): Boolean {
        val cleanedPhone = phone.replace("\\s+".toRegex(), "")
        return cleanedPhone.matches(Regex("^(\\+8801|01)[3-9]\\d{8}$"))
    }

    fun isValidAge(age: String): Boolean {
        return age.toIntOrNull()?.let { it in 18..100 } ?: false
    }

    LaunchedEffect(uid) {
        val result = AuthRepository().getUser(uid)
        if (result.isSuccess) {
            val userData = result.getOrNull()
            name = userData?.name ?: ""
            phone = userData?.phone ?: ""
            age = userData?.age?.toString() ?: ""
            role = userData?.role ?: "Rider"
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
                title = { Text("তথ্য পরিবর্তন") },
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
                            text = "তথ্য পরিবর্তন করুন",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("নাম") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "নাম ক্ষেত্র" },
                            isError = name.isBlank() && error != null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("ফোন নম্বর (+880)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "ফোন নম্বর ক্ষেত্র" },
                            isError = !isValidPhone(phone) && phone.isNotBlank()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("বয়স") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "বয়স ক্ষেত্র" },
                            isError = !isValidAge(age) && age.isNotBlank()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    error = "নাম প্রয়োজন"
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                    }
                                } else if (!isValidPhone(phone)) {
                                    error = "বৈধ ফোন নম্বর প্রয়োজন (+8801Xxxxxxxxx)"
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                    }
                                } else if (!isValidAge(age)) {
                                    error = "বয়স ১৮-১০০ এর মধ্যে হতে হবে"
                                    scope.launch {
                                        snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                    }
                                } else {
                                    isLoading = true
                                    error = null
                                    scope.launch {
                                        val dbResult = AuthRepository().updateUserProfile(
                                            uid, name, phone, age.toIntOrNull() ?: 0, user?.email ?: ""
                                        )
                                        if (dbResult.isSuccess) {
                                            Toast.makeText(context, "পরিবর্তন সফল হয়েছে", Toast.LENGTH_SHORT).show()
                                            navController.navigate("show_account_info")
                                        } else {
                                            error = dbResult.exceptionOrNull()?.message ?: "পরিবর্তন ব্যর্থ"
                                            scope.launch {
                                                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
                                            }
                                        }
                                        isLoading = false
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
                                Text("পরিবর্তন সংরক্ষণ করুন")
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
                            Text("পরিবর্তন বাতিল করুন")
                        }
                    }
                }
            }
        }
    }
}
