package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
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

        // Button for past trips (placeholder for Week 5)
        Button(
            onClick = {
                Toast.makeText(context, "পূর্ববর্তী যাত্রা এর তথ্য আসবে", Toast.LENGTH_SHORT).show()
                // TODO: Implement PastTripsScreen in Week 5
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

        // Button for booking tickets
        Button(
            onClick = {
                Toast.makeText(context, "টিকিট বুক করার পর্দা আসবে", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("টিকিট বুক করুন")
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


