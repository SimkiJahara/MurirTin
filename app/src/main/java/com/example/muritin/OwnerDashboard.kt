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
import androidx.navigation.NavHostController // Added import
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboard(navController: NavHostController, user: FirebaseUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    Log.d("OwnerDashboard", "Rendering OwnerDashboard for ${user.email}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ওনার ড্যাশবোর্ড",
            style = MaterialTheme.typography.headlineMedium
        )
        Text("স্বাগতম, ${user.email}")

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
            onClick = {
                Toast.makeText(context, "বাস রেজিস্ট্রেশন পর্দা আসবে", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "বাস রেজিস্টার বোতাম" }
        ) {
            Text("বাস রেজিস্টার করুন")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.navigate("signup_conductor")
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "কন্ডাক্টর নিবন্ধন বোতাম" }
        ) {
            Text("কন্ডাক্টর নিবন্ধন করুন")
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
    }
}
