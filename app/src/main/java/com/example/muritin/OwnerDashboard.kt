package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboard(navController: NavHostController, user: FirebaseUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    var userData by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.uid) {
        val result = AuthRepository().getUser(user.uid)
        if (result.isSuccess) {
            userData = result.getOrNull()
            isLoading = false
        } else {
            isLoading = false
            Toast.makeText(context, "ব্যবহারকারীর তথ্য পাওয়া যায়নি", Toast.LENGTH_LONG).show()
            navController.navigate("login") { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (userData?.role == "Owner") {
            Text(
                text = "ওনার ড্যাশবোর্ড",
                style = MaterialTheme.typography.headlineMedium
            )
            Text("স্বাগতম, ${userData?.email ?: user.email}")
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (user != null) {
                        Toast.makeText(context, "অ্যাকাউন্ট এর তথ্য আসবে", Toast.LENGTH_SHORT).show()
                        navController.navigate("show_account_info")
                    } else {
                        Toast.makeText(context, "দয়া করে লগইন করুন", Toast.LENGTH_SHORT).show()
                        navController.navigate("login")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "অ্যাকাউন্ট তথ্য বোতাম" }
            ) {
                Text("অ্যাকাউন্ট এর তথ্য")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("register_bus") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "বাস রেজিস্টার বোতাম" }
            ) {
                Text("বাস রেজিস্টার করুন")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("bus_list") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "বাস তালিকা বোতাম" }
            ) {
                Text("আমার বাসসমূহ দেখুন")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("signup_conductor") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "কন্ডাক্টর নিবন্ধন বোতাম" }
            ) {
                Text("কন্ডাক্টর নিবন্ধন করুন")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("conductor_list") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "কন্ডাক্টর তালিকা বোতাম" }
            ) {
                Text("কন্ডাক্টর তালিকা দেখুন")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    Log.d("OwnerDashboard", "Logging out")
                    Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
                    onLogout()
                },
                modifier = Modifier.semantics { contentDescription = "লগআউট বোতাম" }
            ) {
                Text("লগআউট")
            }
        } else {
            Text("অননুমোদিত: শুধুমাত্র ওনাররা এই পৃষ্ঠা দেখতে পারেন", color = MaterialTheme.colorScheme.error)
            LaunchedEffect(Unit) {
                navController.navigate("login") { popUpTo(navController.graph.id) { inclusive = true } }
            }
        }
    }
}
