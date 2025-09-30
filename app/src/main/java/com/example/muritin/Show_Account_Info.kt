package com.example.muritin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

// Show_Account_Info is the entry UI for all tye of users to easily access their account
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Show_Account_Info(
    navController: NavHostController, // Navigation controller to switch screens.
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf<String?>(null) }
    var age by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val user = FirebaseAuth.getInstance().currentUser   //Getting the current logged in user object
    val email = user?.email ?: "No email"
    val uid = user?.uid ?: "No UID"

    scope.launch {
        val result = AuthRepository().getUser(uid)
        if(result.isSuccess){
            val user = result.getOrNull() // The User object
            name = user?.name
            phone = user?.phone
            age = user?.age
            isLoading = false
        }
        else{
            Toast.makeText(context, "তথ্য উদ্ধার সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
            isLoading = false
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
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "আকাউন্ট এর তথ্য",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "নাম:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${name ?: "Loading..."}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "ইমেইল আড্রেস:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${email ?: "Loading..."}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "ফোন নম্বর:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${phone ?: "Loading..."}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "বয়স:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${age ?: "Loading..."}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // User is logged in, go to account info page
                    Toast.makeText(context, "তথ্য পরিবর্তন এর পেজ আসবে", Toast.LENGTH_SHORT).show()
                    navController.navigate("profile_update")
                } else {
                    // User not logged in, show a message or redirect to login
                    Toast.makeText(context, "দয়া করে লগইন করুন", Toast.LENGTH_SHORT).show()
                    navController.navigate("login")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("তথ্য পরিবর্তন করুন") // Label of the button for updating profile
        }
        OutlinedButton(
            onClick = {

            },
            modifier = Modifier.fillMaxWidth(),

        ) {
            Text("পাসওয়ার্ড পরিবর্তন করুন") // Label of the button for deleting account
        }
        Button(
            onClick = {

            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB71C1C),
                contentColor = Color.White
            )

        ) {
            Text("আকাউন্ট ডিলিট করুন") // Label of the button for deleting account
        }
    }

}





