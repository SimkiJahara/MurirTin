package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Rider_Account_Info is the entry UI for riders to easily access their dashboard
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Rider_Account_Info(
    navController: NavHostController, // Navigation controller to switch screens.
) {
    val context = LocalContext.current
    // Main layout: Centered column with padding for a clean look.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // User is logged in, go to account info page
                    Toast.makeText(context, "পূর্ববর্তী যাত্রা এর তথ্য আসবে", Toast.LENGTH_SHORT).show()
                    //navController.navigate("rider_account_info")
                } else {
                    // User not logged in, show a message or redirect to login
                    Toast.makeText(context, "দয়া করে লগইন করুন", Toast.LENGTH_SHORT).show()
                    navController.navigate("login")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("পূর্ববর্তী যাত্রাসমূহ") // Label of the button for past trip history
        }
        Button(
            onClick = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // User is logged in, go to account info page
                    Toast.makeText(context, "নিজের তথ্য আসবে", Toast.LENGTH_SHORT).show()
                    navController.navigate("show_account_info")
                } else {
                    // User not logged in, show a message or redirect to login
                    Toast.makeText(context, "দয়া করে লগইন করুন", Toast.LENGTH_SHORT).show()
                    navController.navigate("login")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("নিজের তথ্য") // Label of the button for showing info
        }
        OutlinedButton(
            onClick = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // User is logged in, go to account info page
                    Toast.makeText(context, "তথ্য পরিবর্তন এর পেজ আসবে ", Toast.LENGTH_SHORT).show()
                    //navController.navigate("rider_account_info")
                } else {

                    // User not logged in, show a message or redirect to login
                    Toast.makeText(context, "দয়া করে লগইন করুন", Toast.LENGTH_SHORT).show()
                    navController.navigate("login")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("নিজের তথ্য পরিবর্তন") // Label of the button for past trip history
        }
    }

}

