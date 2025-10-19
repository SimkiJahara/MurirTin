package com.example.muritin

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavHostController,
    onSignUpSuccess: (User) -> Unit,
    preSelectedRole: String = "Rider"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ownerPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(preSelectedRole) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var currentUserRole by remember { mutableStateOf<String?>(null) }
    var roleLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val result = AuthRepository().getUserRole(currentUser.uid)
            currentUserRole = result
            roleLoading = false
        } else {
            currentUserRole = null
            roleLoading = false
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (preSelectedRole == "Conductor") "কন্ডাক্টর নিবন্ধন" else "নিবন্ধন করুন") },
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
            if (roleLoading) {
                CircularProgressIndicator()
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
                            text = if (preSelectedRole == "Conductor") "কন্ডাক্টর নিবন্ধন করুন" else "আপনার অ্যাকাউন্ট তৈরি করুন",
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
                        Spacer(modifier = Modifier.height(8.dp))
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
                        if (preSelectedRole == "Conductor") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = ownerPassword,
                                onValueChange = { ownerPassword = it },
                                label = { Text("ওনারের পাসওয়ার্ড") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "ওনারের পাসওয়ার্ড ক্ষেত্র" },
                                isError = ownerPassword.length < 6 && ownerPassword.isNotBlank()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (preSelectedRole == "Conductor") {
                            OutlinedTextField(
                                value = "কন্ডাক্টর",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("ভূমিকা") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "ভূমিকা নির্বাচন" },
                                enabled = false
                            )
                        } else {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = when (role) {
                                        "Rider" -> "রাইডার"
                                        "Conductor" -> "কন্ডাক্টর"
                                        "Owner" -> "ওনার"
                                        else -> "রাইডার"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("ভূমিকা") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .semantics { contentDescription = "ভূমিকা নির্বাচন" }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("রাইডার") },
                                        onClick = {
                                            role = "Rider"
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("কন্ডাক্টর") },
                                        onClick = {
                                            if (currentUserRole == "Owner") {
                                                role = "Conductor"
                                                expanded = false
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("শুধুমাত্র ওনাররা কন্ডাক্টর নিবন্ধন করতে পারেন")
                                                }
                                            }
                                        },
                                        enabled = currentUserRole == "Owner"
                                    )
                                    DropdownMenuItem(
                                        text = { Text("ওনার") },
                                        onClick = {
                                            role = "Owner"
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    error = "নাম প্রয়োজন"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else if (!isValidPhone(phone)) {
                                    error = "বৈধ ফোন নম্বর প্রয়োজন (+8801Xxxxxxxxx)"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else if (!isValidAge(age)) {
                                    error = "বয়স ১৮-১০০ এর মধ্যে হতে হবে"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else if (!isValidEmail(email)) {
                                    error = "বৈধ ইমেইল প্রয়োজন"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else if (password.length < 6) {
                                    error = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else if (preSelectedRole == "Conductor" && ownerPassword.length < 6) {
                                    error = "ওনারের পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else if (preSelectedRole == "Conductor" && currentUserRole != "Owner") {
                                    error = "শুধুমাত্র ওনাররা কন্ডাক্টর নিবন্ধন করতে পারেন"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else {
                                    isLoading = true
                                    error = null
                                    scope.launch {
                                        val result = AuthRepository().signup(
                                            email = email,
                                            password = password,
                                            role = role,
                                            name = name,
                                            phone = phone,
                                            age = age.toIntOrNull() ?: 0,
                                            ownerPassword = if (preSelectedRole == "Conductor") ownerPassword else null
                                        )
                                        isLoading = false
                                        when {
                                            result.isSuccess -> {
                                                Toast.makeText(context, "নিবন্ধন সফল", Toast.LENGTH_SHORT).show()
                                                result.getOrNull()?.let { onSignUpSuccess(it) }
                                            }
                                            else -> {
                                                error = result.exceptionOrNull()?.message ?: "নিবন্ধন ব্যর্থ"
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
                                .semantics { contentDescription = "নিবন্ধন বোতাম" },
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("নিবন্ধন করুন")
                            }
                        }
                    }
                }
            }
        }
    }
}