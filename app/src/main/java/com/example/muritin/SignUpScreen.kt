package com.example.muritin

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
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
    var nid by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ownerPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(preSelectedRole) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var ownerPasswordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    var currentUserRole by remember { mutableStateOf<String?>(null) }
    var roleLoading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserRole = AuthRepository().getUserRole(currentUser.uid)
        }
        roleLoading = false
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

    fun isValidNID(nid: String): Boolean {
        val cleanNid = nid.filter { it.isDigit() }
        if (cleanNid.length != 10 && cleanNid.length != 17) return false
        return cleanNid.toLongOrNull() != null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Primary,
                        PrimaryLight,
                        BackgroundLight
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsBus,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (preSelectedRole == "Conductor") "কন্ডাক্টর নিবন্ধন" else "নতুন অ্যাকাউন্ট",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "মুড়ির টিন বাস সেবায় যোগ দিন",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Registration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    if (roleLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    } else {
                        Text(
                            text = "আপনার তথ্য দিন",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Name Field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("নাম") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = Primary)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Phone Field
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("ফোন নম্বর (+880)") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Phone, contentDescription = null, tint = Primary)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border
                            ),
                            singleLine = true,
                            isError = phone.isNotBlank() && !isValidPhone(phone)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Age Field
                            OutlinedTextField(
                                value = age,
                                onValueChange = { age = it },
                                label = { Text("বয়স") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Cake, contentDescription = null, tint = Primary)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Border
                                ),
                                singleLine = true,
                                isError = age.isNotBlank() && !isValidAge(age)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // NID Field
                        OutlinedTextField(
                            value = nid,
                            onValueChange = { nid = it },
                            label = { Text("জাতীয় পরিচয়পত্র নম্বর") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Badge, contentDescription = null, tint = Primary)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border
                            ),
                            singleLine = true,
                            isError = nid.isNotBlank() && !isValidNID(nid)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("ইমেইল") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null, tint = Primary)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border
                            ),
                            singleLine = true,
                            isError = email.isNotBlank() && !isValidEmail(email)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("পাসওয়ার্ড") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible)
                                            Icons.Outlined.Visibility
                                        else
                                            Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        tint = Primary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border
                            ),
                            singleLine = true,
                            isError = password.isNotBlank() && password.length < 6
                        )

                        if (preSelectedRole == "Conductor") {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = ownerPassword,
                                onValueChange = { ownerPassword = it },
                                label = { Text("ওনারের পাসওয়ার্ড") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = Primary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { ownerPasswordVisible = !ownerPasswordVisible }) {
                                        Icon(
                                            imageVector = if (ownerPasswordVisible)
                                                Icons.Outlined.Visibility
                                            else
                                                Icons.Outlined.VisibilityOff,
                                            contentDescription = null,
                                            tint = Primary
                                        )
                                    }
                                },
                                visualTransformation = if (ownerPasswordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Border
                                ),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Role Selection
                        if (preSelectedRole != "Conductor") {
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
                                    label = { Text("ভূমিকা নির্বাচন করুন") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Group, contentDescription = null, tint = Primary)
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        unfocusedBorderColor = Border
                                    )
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Outlined.DirectionsBus,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = RouteBlue
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("রাইডার", fontWeight = FontWeight.Medium)
                                                    Text(
                                                        "বাসে যাত্রা করুন",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            role = "Rider"
                                            expanded = false
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Outlined.Business,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = RouteGreen
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("ওনার", fontWeight = FontWeight.Medium)
                                                    Text(
                                                        "বাস পরিচালনা করুন",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            role = "Owner"
                                            expanded = false
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Sign Up Button
                        Button(
                            onClick = {
                                when {
                                    name.isBlank() -> {
                                        scope.launch { snackbarHostState.showSnackbar("নাম প্রয়োজন") }
                                    }
                                    !isValidPhone(phone) -> {
                                        scope.launch { snackbarHostState.showSnackbar("বৈধ ফোন নম্বর প্রয়োজন") }
                                    }
                                    !isValidNID(nid) -> {
                                        scope.launch { snackbarHostState.showSnackbar("এন আইডি ১০ অথবা ১৭ সংখ্যার হতে হবে") }
                                    }
                                    !isValidAge(age) -> {
                                        scope.launch { snackbarHostState.showSnackbar("বয়স ১৮-১০০ এর মধ্যে হতে হবে") }
                                    }
                                    !isValidEmail(email) -> {
                                        scope.launch { snackbarHostState.showSnackbar("বৈধ ইমেইল প্রয়োজন") }
                                    }
                                    password.length < 6 -> {
                                        scope.launch { snackbarHostState.showSnackbar("পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে") }
                                    }
                                    preSelectedRole == "Conductor" && ownerPassword.length < 6 -> {
                                        scope.launch { snackbarHostState.showSnackbar("ওনারের পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে") }
                                    }
                                    else -> {
                                        isLoading = true
                                        scope.launch {
                                            val result = AuthRepository().signup(
                                                email = email,
                                                password = password,
                                                role = role,
                                                name = name,
                                                phone = phone,
                                                nid = nid,
                                                age = age.toIntOrNull() ?: 0,
                                                ownerPassword = if (preSelectedRole == "Conductor") ownerPassword else null
                                            )
                                            isLoading = false
                                            when {
                                                result.isSuccess -> {
                                                    Toast.makeText(
                                                        context,
                                                        "নিবন্ধন সফল, যাচাই ইমেইল পাঠানো হয়েছে",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    result.getOrNull()?.let { onSignUpSuccess(it) }
                                                }
                                                else -> {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            result.exceptionOrNull()?.message ?: "নিবন্ধন ব্যর্থ"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary
                            ),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "নিবন্ধন করুন",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Login Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ইতিমধ্যে অ্যাকাউন্ট আছে?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "লগইন করুন",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    navController.navigate("login") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Error,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}