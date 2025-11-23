package com.example.muritin

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Userprofile_Update(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

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
            isLoading = false
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("তথ্য উদ্ধার সম্ভব হয়নি")
            }
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Error,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header with Gradient
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
                        onClick = { navController.navigate("show_account_info") },
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
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = Primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "তথ্য পরিবর্তন",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "আপনার প্রোফাইল আপডেট করুন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Form Content with proper spacing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-60).dp)
                ) {
                    if (isLoading) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
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
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "ব্যক্তিগত তথ্য",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )

                                // Name Field
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("নাম") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Person,
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
                                    isError = name.isBlank()
                                )

                                if (name.isBlank()) {
                                    Text(
                                        text = "নাম প্রয়োজন",
                                        color = Error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Phone Field
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text("ফোন নম্বর (+880)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Phone,
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
                                    isError = phone.isNotBlank() && !isValidPhone(phone)
                                )

                                if (phone.isNotBlank() && !isValidPhone(phone)) {
                                    Text(
                                        text = "বৈধ ফোন নম্বর প্রয়োজন (+8801XXXXXXXXX)",
                                        color = Error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Age Field
                                OutlinedTextField(
                                    value = age,
                                    onValueChange = { age = it },
                                    label = { Text("বয়স") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Cake,
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
                                    isError = age.isNotBlank() && !isValidAge(age)
                                )

                                if (age.isNotBlank() && !isValidAge(age)) {
                                    Text(
                                        text = "বয়স ১৮-১০০ এর মধ্যে হতে হবে",
                                        color = Error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Info Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Info.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = Info,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "ইমেইল পরিবর্তন করা যাবে না",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save Button
                        Button(
                            onClick = {
                                when {
                                    name.isBlank() -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("নাম প্রয়োজন")
                                        }
                                    }
                                    !isValidPhone(phone) -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("বৈধ ফোন নম্বর প্রয়োজন")
                                        }
                                    }
                                    !isValidAge(age) -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("বয়স ১৮-১০০ এর মধ্যে হতে হবে")
                                        }
                                    }
                                    else -> {
                                        isSaving = true
                                        scope.launch {
                                            val dbResult = AuthRepository().updateUserProfile(
                                                uid, name, phone, age.toIntOrNull() ?: 0, user?.email ?: ""
                                            )
                                            if (dbResult.isSuccess) {
                                                Toast.makeText(context, "পরিবর্তন সফল হয়েছে", Toast.LENGTH_SHORT).show()
                                                navController.navigate("show_account_info")
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("পরিবর্তন ব্যর্থ")
                                                }
                                            }
                                            isSaving = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Success
                            ),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "পরিবর্তন সংরক্ষণ করুন",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cancel Button
                        OutlinedButton(
                            onClick = { navController.navigate("show_account_info") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            )
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "পরিবর্তন বাতিল করুন",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // Add bottom spacing so content is fully scrollable
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}