package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRegistrationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser ?: return
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var fitnessCertificate by remember { mutableStateOf("") }
    var taxToken by remember { mutableStateOf("") }
    var stops by remember { mutableStateOf("") }
    var fares by remember { mutableStateOf(mapOf<String, Map<String, Int>>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showFareDialog by remember { mutableStateOf(false) }
    var currentStop by remember { mutableStateOf("") }
    var currentDestination by remember { mutableStateOf("") }
    var currentFare by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বাস রেজিস্ট্রেশন") },
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
            verticalArrangement = Arrangement.Top,
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
                        text = "বাস রেজিস্ট্রেশন করুন",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("বাসের নাম") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = name.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("বাস নম্বর") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = number.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fitnessCertificate,
                        onValueChange = { fitnessCertificate = it },
                        label = { Text("ফিটনেস সার্টিফিকেট") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = fitnessCertificate.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taxToken,
                        onValueChange = { taxToken = it },
                        label = { Text("ট্যাক্স টোকেন") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = taxToken.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = stops,
                        onValueChange = { stops = it },
                        label = { Text("স্টপস (কমা দিয়ে আলাদা করুন)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = stops.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ভাড়ার তালিকা", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        items(fares.entries.toList()) { stopEntry ->
                            stopEntry.value.forEach { (dest, fare) ->
                                Text("${stopEntry.key} থেকে $dest: $fare টাকা")
                            }
                        }
                    }
                    Button(
                        onClick = { showFareDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ভাড়া যোগ করুন")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || number.isBlank() || fitnessCertificate.isBlank() || taxToken.isBlank() || stops.isBlank()) {
                                error = "সকল ক্ষেত্র পূরণ করুন"
                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                            } else if (fares.isEmpty()) {
                                error = "কমপক্ষে একটি ভাড়া যোগ করুন"
                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                            } else {
                                isLoading = true
                                error = null
                                scope.launch {
                                    val stopsList = stops.split(",").map { it.trim() }
                                    val result = AuthRepository().registerBus(
                                        ownerId = user.uid,
                                        name = name,
                                        number = number,
                                        fitnessCertificate = fitnessCertificate,
                                        taxToken = taxToken,
                                        stops = stopsList,
                                        fares = fares
                                    )
                                    isLoading = false
                                    when {
                                        result.isSuccess -> {
                                            Toast.makeText(context, "বাস রেজিস্ট্রেশন সফল", Toast.LENGTH_SHORT).show()
                                            navController.navigate("owner_dashboard") { popUpTo("register_bus") { inclusive = true } }
                                        }
                                        else -> {
                                            error = result.exceptionOrNull()?.message ?: "রেজিস্ট্রেশন ব্যর্থ"
                                            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("রেজিস্টার করুন")
                        }
                    }
                }
            }
        }
    }

    if (showFareDialog) {
        val stopsList = stops.split(",").map { it.trim() }.filter { it.isNotBlank() }
        AlertDialog(
            onDismissRequest = { showFareDialog = false },
            title = { Text("ভাড়া যোগ করুন") },
            text = {
                Column {
                    var stopExpanded by remember { mutableStateOf(false) }
                    var destExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = stopExpanded,
                        onExpandedChange = { stopExpanded = !stopExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentStop,
                            onValueChange = { currentStop = it },
                            label = { Text("উৎস স্টপ") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stopExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = stopExpanded,
                            onDismissRequest = { stopExpanded = false }
                        ) {
                            stopsList.forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text(stop) },
                                    onClick = {
                                        currentStop = stop
                                        stopExpanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = destExpanded,
                        onExpandedChange = { destExpanded = !destExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentDestination,
                            onValueChange = { currentDestination = it },
                            label = { Text("গন্তব্য স্টপ") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = destExpanded,
                            onDismissRequest = { destExpanded = false }
                        ) {
                            stopsList.filter { it != currentStop }.forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text(stop) },
                                    onClick = {
                                        currentDestination = stop
                                        destExpanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentFare,
                        onValueChange = { currentFare = it },
                        label = { Text("ভাড়া (টাকা)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = currentFare.toIntOrNull()?.let { it < 0 } ?: true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fareValue = currentFare.toIntOrNull()
                        if (currentStop.isBlank() || currentDestination.isBlank() || fareValue == null || fareValue < 0) {
                            scope.launch { snackbarHostState.showSnackbar("বৈধ স্টপ এবং ভাড়া প্রয়োজন") }
                        } else if (currentStop == currentDestination) {
                            scope.launch { snackbarHostState.showSnackbar("উৎস এবং গন্তব্য একই হতে পারে না") }
                        } else {
                            fares = fares.toMutableMap().apply {
                                this[currentStop] = (this[currentStop] ?: emptyMap()).toMutableMap().apply {
                                    this[currentDestination] = fareValue
                                }
                            }
                            currentStop = ""
                            currentDestination = ""
                            currentFare = ""
                            showFareDialog = false
                        }
                    }
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFareDialog = false }) { Text("বাতিল") }
            }
        )
    }
}