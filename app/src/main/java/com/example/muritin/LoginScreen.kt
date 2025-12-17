package com.example.muritin

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Help
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Animation values
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
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
            Spacer(modifier = Modifier.height(40.dp))

            // App Logo and Title
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsBus,
                    contentDescription = "মুড়ির টিন লোগো",
                    modifier = Modifier.size(60.dp),
                    tint = Primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "মুড়ির টিন",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "স্মার্ট বাস সেবা",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Login Card
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
                    Text(
                        text = "স্বাগতম!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "আপনার অ্যাকাউন্টে লগইন করুন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("ইমেইল") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Email,
                                contentDescription = null,
                                tint = Primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedLabelColor = Primary
                        ),
                        singleLine = true,
                        isError = email.isNotBlank() && !isValidEmail(email)
                    )

                    if (email.isNotBlank() && !isValidEmail(email)) {
                        Text(
                            text = "বৈধ ইমেইল প্রয়োজন",
                            color = Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("পাসওয়ার্ড") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Outlined.Visibility
                                    else
                                        Icons.Outlined.VisibilityOff,
                                    contentDescription = if (passwordVisible)
                                        "পাসওয়ার্ড লুকান"
                                    else
                                        "পাসওয়ার্ড দেখান",
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
                            unfocusedBorderColor = Border,
                            focusedLabelColor = Primary
                        ),
                        singleLine = true,
                        isError = password.isNotBlank() && password.length < 6
                    )

                    if (password.isNotBlank() && password.length < 6) {
                        Text(
                            text = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে",
                            color = Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot Password - Updated Style
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showResetDialog = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LockReset,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "পাসওয়ার্ড ভুলে গেছেন?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (!isValidEmail(email)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("বৈধ ইমেইল প্রয়োজন")
                                }
                            } else if (password.length < 6) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে")
                                }
                            } else {
                                isLoading = true
                                scope.launch {
                                    val result = AuthRepository().login(email, password)
                                    isLoading = false
                                    when {
                                        result.isSuccess -> {
                                            Toast.makeText(context, "লগইন সফল", Toast.LENGTH_SHORT).show()
                                            result.getOrNull()?.let { onLoginSuccess(it) }
                                        }
                                        else -> {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("ইমেইল অথবা পাসওয়ার্ড সঠিক নয়")
                                                Toast.makeText(context, "ইমেইল অথবা পাসওয়ার্ড সঠিক নয়", Toast.LENGTH_SHORT).show()
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
                                Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "লগইন করুন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Divider)
                        Text(
                            text = "অথবা",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Divider)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign Up Button
                    OutlinedButton(
                        onClick = { navController.navigate("signup") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Primary
                        ),
                        border = BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Primary, PrimaryLight)
                            )
                        )
                    ) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "নতুন অ্যাকাউন্ট তৈরি করুন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Help Button - Updated Style
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("help") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryLight.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Help,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    "সাহায্য প্রয়োজন?",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "লগইন ও নিবন্ধন সহায়তা",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
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

    // Password Reset Dialog - Updated Modern Style
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.2f),
                                    PrimaryLight.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.LockReset,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    "পাসওয়ার্ড রিসেট করুন",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "আপনার ইমেইল ঠিকানা দিন। আমরা পাসওয়ার্ড রিসেট লিংক পাঠাবো।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetError = null
                        },
                        label = { Text("ইমেইল") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Email,
                                contentDescription = null,
                                tint = Primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Border,
                            focusedLabelColor = Primary
                        ),
                        isError = resetEmail.isNotBlank() && !isValidEmail(resetEmail),
                        singleLine = true
                    )
                    if (resetError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Error.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = Error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = resetError ?: "",
                                    color = Error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isValidEmail(resetEmail)) {
                            resetError = "বৈধ ইমেইল প্রয়োজন"
                        } else {
                            resetLoading = true
                            resetError = null
                            scope.launch {
                                try {
                                    FirebaseAuth.getInstance()
                                        .sendPasswordResetEmail(resetEmail)
                                        .await()
                                    Toast.makeText(
                                        context,
                                        "পাসওয়ার্ড রিসেট ইমেইল পাঠানো হয়েছে",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showResetDialog = false
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "Password reset failed: ${e.message}", e)
                                    resetError = e.message ?: "রিসেট ব্যর্থ"
                                } finally {
                                    resetLoading = false
                                }
                            }
                        }
                    },
                    enabled = !resetLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.height(48.dp)
                ) {
                    if (resetLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পাঠান", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "বাতিল",
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
}