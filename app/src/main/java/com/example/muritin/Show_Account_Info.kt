package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Show_Account_Info(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showDelAccDialog by remember { mutableStateOf(false) }
    var showDelAccDialogOwner by remember { mutableStateOf(false) }
    var showDelAccDialogConductor by remember { mutableStateOf(false) }
    var showPasswordChangeDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordChangeLoading by remember { mutableStateOf(false) }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "No email"
    val uid = user?.uid ?: ""

    LaunchedEffect(uid) {
        val result = AuthRepository().getUser(uid)
        if (result.isSuccess) {
            userData = result.getOrNull()
            isLoading = false
        } else {
            error = "তথ্য উদ্ধার সম্ভব হয়নি"
            isLoading = false
            scope.launch {
                snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি")
            }
        }
    }

    fun dashboardRoute(): String = when (userData?.role) {
        "Conductor" -> "conductor_dashboard"
        "Owner" -> "owner_dashboard"
        else -> "rider_dashboard"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Custom Top Bar with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Primary, PrimaryLight)
                        )
                    )
                    .padding(top = 40.dp, bottom = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = { navController.navigate(dashboardRoute()) },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userData?.name ?: "লোড হচ্ছে...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = when (userData?.role) {
                            "Conductor" -> "কন্ডাক্টর"
                            "Owner" -> "ওনার"
                            else -> "রাইডার"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .offset(y = (-60).dp)
            ) {
                if (isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                } else if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Error.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error ?: "অজানা ত্রুটি",
                                color = Error,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Account Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "অ্যাকাউন্ট তথ্য",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            InfoItem(
                                icon = Icons.Outlined.Person,
                                label = "নাম",
                                value = userData?.name ?: "N/A",
                                iconColor = RouteBlue
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)

                            InfoItem(
                                icon = Icons.Outlined.Email,
                                label = "ইমেইল",
                                value = email,
                                iconColor = RouteOrange
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)

                            InfoItem(
                                icon = Icons.Outlined.Phone,
                                label = "ফোন নম্বর",
                                value = userData?.phone ?: "N/A",
                                iconColor = RouteGreen
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)

                            InfoItem(
                                icon = Icons.Outlined.Cake,
                                label = "বয়স",
                                value = "${userData?.age ?: "N/A"} বছর",
                                iconColor = RoutePurple
                            )

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Divider)

                            InfoItem(
                                icon = Icons.Outlined.Badge,
                                label = "এন আইডি",
                                value = userData?.nid ?: "N/A",
                                iconColor = Primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    ModernActionButton(
                        icon = Icons.Outlined.Edit,
                        text = "তথ্য পরিবর্তন করুন",
                        onClick = { navController.navigate("profile_update") },
                        backgroundColor = Primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ModernActionButton(
                        icon = Icons.Outlined.Dashboard,
                        text = "ড্যাশবোর্ডে ফিরুন",
                        onClick = {
                            navController.navigate(dashboardRoute()) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            }
                        },
                        backgroundColor = Secondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ModernActionButton(
                        icon = Icons.Outlined.Lock,
                        text = "পাসওয়ার্ড পরিবর্তন করুন",
                        onClick = { showPasswordChangeDialog = true },
                        backgroundColor = Info
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delete Account Button
                    ModernActionButton(
                        icon = Icons.Outlined.Delete,
                        text = "অ্যাকাউন্ট ডিলিট করুন",
                        onClick = {
                            if (userData?.role == "Owner") {
                                showDelAccDialogOwner = true
//                            } else if (userData?.role == "Conductor"){
//                                showDelAccDialogConductor = true
                            } else {
                                showDelAccDialog = true
                            }
                        },
                        backgroundColor = Error,
                        isDestructive = true
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Delete Account Dialogs
    if (showDelAccDialog) {
        ModernAlertDialog(
            icon = Icons.Outlined.Delete,
            iconColor = Error,
            title = "অ্যাকাউন্ট ডিলিট করুন",
            message = "আপনি কি নিশ্চিতভাবে আপনার অ্যাকাউন্ট ডিলিট করতে চান? এই ক্রিয়াটি পূর্বাবস্থায় ফেরানো যাবে না।",
            confirmText = "হ্যাঁ",
            dismissText = "না",
            onConfirm = {
                scope.launch {
                    try {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val dbResult = AuthRepository().deleteUserData(currentUser.uid)
                            if (dbResult.isSuccess) {
                                currentUser.delete().await()
                                Toast.makeText(context, "অ্যাকাউন্ট ডিলিট সফল", Toast.LENGTH_SHORT).show()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("ডিলিট ব্যর্থ") }
                            }
                        }
                    } catch (e: Exception) {
                        scope.launch { snackbarHostState.showSnackbar("ডিলিট ব্যর্থ: ${e.message}") }
                    }
                }
                showDelAccDialog = false
            },
            onDismiss = { showDelAccDialog = false }
        )
    }

    if (showDelAccDialogOwner) {
        ModernAlertDialog(
            icon = Icons.Outlined.Delete,
            iconColor = Error,
            title = "অ্যাকাউন্ট ডিলিট করুন",
            message = "আপনি কি নিশ্চিতভাবে আপনার অ্যাকাউন্ট ডিলিট করতে চান? আপনার সব বাসের তথ্য, শিডিউল ডিলিট হয়ে যাবে। এই ক্রিয়াটি পূর্বাবস্থায় ফেরানো যাবে না।",
            confirmText = "হ্যাঁ",
            dismissText = "না",
            onConfirm = {
                scope.launch {
                    try {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val dbResult = AuthRepository().deleteOwnerData(currentUser.uid)
                            if (dbResult.isSuccess) {
                                currentUser.delete().await()
                                Toast.makeText(context, "অ্যাকাউন্ট ডিলিট সফল", Toast.LENGTH_SHORT).show()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("ডিলিট ব্যর্থ") }
                            }
                        }
                    } catch (e: Exception) {
                        scope.launch { snackbarHostState.showSnackbar("ডিলিট ব্যর্থ: ${e.message}") }
                    }
                }
                showDelAccDialogOwner = false
            },
            onDismiss = { showDelAccDialogOwner = false }
        )
    }

    if (showDelAccDialogConductor) {
        ModernAlertDialog(
            icon = Icons.Outlined.Delete,
            iconColor = Error,
            title = "অ্যাকাউন্ট ডিলিট করুন",
            message = "আপনি কি নিশ্চিতভাবে আপনার অ্যাকাউন্ট ডিলিট করতে চান? আপনার বাসের শিডিউল, অ্যাক্সেপ্ট করা রিকোয়েস্ট ডিলিট হয়ে যাবে। এই ক্রিয়াটি পূর্বাবস্থায় ফেরানো যাবে না।",
            confirmText = "হ্যাঁ",
            dismissText = "না",
            onConfirm = {
                scope.launch {
                    try {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val dbResult = AuthRepository().deleteConductorData(currentUser.uid)
                            if (dbResult.isSuccess) {
                                currentUser.delete().await()
                                Toast.makeText(context, "অ্যাকাউন্ট ডিলিট সফল", Toast.LENGTH_SHORT).show()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("ডিলিট ব্যর্থ") }
                            }
                        }
                    } catch (e: Exception) {
                        scope.launch { snackbarHostState.showSnackbar("ডিলিট ব্যর্থ: ${e.message}") }
                    }
                }
                showDelAccDialogConductor = false
            },
            onDismiss = { showDelAccDialogConductor = false }
        )
    }

    // Password Change Dialog
    if (showPasswordChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordChangeDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Info.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Info,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    "পাসওয়ার্ড পরিবর্তন করুন",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("বর্তমান পাসওয়ার্ড") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("নতুন পাসওয়ার্ড") },
                        leadingIcon = {
                            Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = Primary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    if (newPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        },
                        visualTransformation = if (newPasswordVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = newPassword.isNotBlank() && newPassword.length < 6
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("নতুন পাসওয়ার্ড নিশ্চিত করুন") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Primary)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = confirmNewPassword.isNotBlank() && confirmNewPassword != newPassword
                    )
                    if (passwordChangeError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = passwordChangeError!!,
                            color = Error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword != confirmNewPassword) {
                            passwordChangeError = "পাসওয়ার্ড মিলছে না"
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            passwordChangeError = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষর হতে হবে"
                            return@Button
                        }
                        passwordChangeLoading = true
                        scope.launch {
                            try {
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                if (currentUser != null) {
                                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
                                    currentUser.reauthenticate(credential).await()
                                    currentUser.updatePassword(newPassword).await()
                                    Toast.makeText(context, "পাসওয়ার্ড পরিবর্তন সফল", Toast.LENGTH_SHORT).show()
                                    showPasswordChangeDialog = false
                                }
                            } catch (e: Exception) {
                                passwordChangeError = "পাসওয়ার্ড পরিবর্তন ব্যর্থ: ${e.message}"
                            } finally {
                                passwordChangeLoading = false
                            }
                        }
                    },
                    enabled = !passwordChangeLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (passwordChangeLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("পরিবর্তন করুন")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPasswordChangeDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("বাতিল", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun ModernActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    isDestructive: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ModernAlertDialog(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iconColor
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(dismissText, color = TextSecondary)
            }
        }
    )
}