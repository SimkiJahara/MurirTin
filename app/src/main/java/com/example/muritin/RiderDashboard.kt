package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboard(navController: NavHostController, user: FirebaseUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    Log.d("RiderDashboard", "Rendering RiderDashboard for ${user.email}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "রাইডার ড্যাশবোর্ড",
            style = MaterialTheme.typography.headlineMedium
        )
        Text("স্বাগতম, ${user.email}")

        Spacer(modifier = Modifier.height(24.dp))

        // NEW: Help Button
        OutlinedButton(
            onClick = {
                navController.navigate("rider_help")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Filled.Help,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("সহায়তা নির্দেশিকা")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button for viewing past/completed trips
        Button(
            onClick = {
                navController.navigate("past_trips")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("পূর্ববর্তী যাত্রাসমূহ")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button for account info
        Button(
            onClick = {
                navController.navigate("show_account_info")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("অ্যাকাউন্ট এর তথ্য")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // New: Request Trip Button
        Button(
            onClick = {
                navController.navigate("trip_request")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ট্রিপ রিকোয়েস্ট করুন")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // New: View My Requests
        Button(
            onClick = {
                navController.navigate("my_requests")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("আমার রিকোয়েস্টসমূহ")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                Log.d("RiderDashboard", "Logging out")
                Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
                onLogout()
            }
        ) {
            Text("লগআউট")
        }
    }
}


