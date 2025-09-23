package com.example.muritin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MuriTinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentUser = FirebaseAuth.getInstance().currentUser

                    if (currentUser != null) {
                        // User is logged in - show dashboard based on role
                        UserDashboard(navController, currentUser)
                    } else {
                        // Show auth screens
                        AuthNavHost(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun AuthNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = { user ->
                    when (user.role) {
                        "Conductor" -> navController.navigate("conductor_dashboard")
                        else -> navController.navigate("rider_dashboard")
                    }
                }
            )
        }
        composable("signup") {
            SignupScreen(
                navController = navController,
                onSignupSuccess = { user ->
                    when (user.role) {
                        "Conductor" -> navController.navigate("conductor_dashboard")
                        else -> navController.navigate("rider_dashboard")
                    }
                }
            )
        }
        composable("rider_dashboard") {
            RiderDashboard(FirebaseAuth.getInstance().currentUser!!)
        }
        composable("conductor_dashboard") {
            ConductorDashboard(FirebaseAuth.getInstance().currentUser!!)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboard(navController: NavHostController, user: FirebaseUser) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Automatically redirect based on role
    LaunchedEffect(user.uid) {
        AuthRepository().getUserRole(user.uid) { role ->
            if (currentRoute != "rider_dashboard" && currentRoute != "conductor_dashboard") {
                if (role == "Conductor") {
                    navController.navigate("conductor_dashboard")
                } else {
                    navController.navigate("rider_dashboard")
                }
            }
        }
    }

    // Show loading while determining role
    if (currentRoute != "rider_dashboard" && currentRoute != "conductor_dashboard") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboard(user: FirebaseUser) {
    val context = LocalContext.current

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

        Button(
            onClick = {
                // TODO: Navigate to booking screen
                Toast.makeText(context, "টিকিট বুক করার পর্দা আসবে", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("টিকিট বুক করুন")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("লগআউট")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorDashboard(user: FirebaseUser) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "কন্ডাক্টর ড্যাশবোর্ড",
            style = MaterialTheme.typography.headlineMedium
        )
        Text("স্বাগতম, ${user.email}")

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // TODO: Navigate to schedule update screen
                Toast.makeText(context, "শিডিউল আপডেট পর্দা আসবে", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("শিডিউল আপডেট করুন")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("লগআউট")
        }
    }
}

@Composable
fun MuriTinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}