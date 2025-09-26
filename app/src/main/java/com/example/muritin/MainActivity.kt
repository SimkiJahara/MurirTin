package com.example.muritin

import android.os.Bundle
import android.os.StrictMode
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
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// MainActivity is the entry point of the মুড়ির টিন app.
//  Sets up Firebase, navigation, and the main UI with Jetpack Compose.
//  Initializes Firebase, defines navigation routes, and renders role-based dashboards.
class MainActivity : ComponentActivity() {
    // Coroutine scope for background tasks like Firebase operations.
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable strict mode to detect threading issues during development.
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        super.onCreate(savedInstanceState)

        // Initialize Firebase and save test data for debugging.
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MainActivity", "Firebase initialized successfully")
            scope.launch {
                // Save test user data (remove in production).
                AuthRepository().debugSaveTestData()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed: ${e.message}", e)
        }

        // Set up the main UI with Jetpack Compose.
        setContent {
            // Apply the app's custom theme.
            MuriTinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create navigation controller for screen transitions.
                    val navController = rememberNavController()
                    AppNavHost(navController)
                }
            }
        }
    }
}

// the navigation structure for the app.
//  Routes users to login or role-based dashboards based on authentication status.
//  Uses NavHost to define routes and navigate between screens.
@Composable
fun AppNavHost(navController: NavHostController) {
    // Check if a user is logged in.
    val currentUser = FirebaseAuth.getInstance().currentUser
    Log.d("AppNavHost", "Current user: ${currentUser?.email ?: "null"}, uid: ${currentUser?.uid ?: "null"}")
    // Set starting screen: login if no user, dashboard if logged in.
    val startDestination = if (currentUser != null) "user_dashboard" else "login"

    // Navigation host with defined routes.
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Login screen route.
        composable("login") {
            Log.d("AppNavHost", "Navigating to LoginScreen")
            LoginScreen(
                navController = navController,
                onLoginSuccess = { user ->
                    // Navigate to appropriate dashboard based on user role.
                    Log.d("AppNavHost", "Login success, navigating to ${user.role} dashboard")
                    when (user.role) {
                        "Conductor" -> navController.navigate("conductor_dashboard") {
                            popUpTo("login") { inclusive = true } // Clear login from back stack.
                        }
                        else -> navController.navigate("rider_dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }
        // User dashboard route to determine role.
        composable("user_dashboard") {
            Log.d("AppNavHost", "Navigating to UserDashboard")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                UserDashboard(navController, user)
            } else {
                // Redirect to login if no user is found.
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        // Rider dashboard route.
        composable("rider_dashboard") {
            Log.d("AppNavHost", "Navigating to RiderDashboard")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                RiderDashboard(
                    user = user,
                    onLogout = {
                        // Handle logout and redirect to login.
                        Log.d("AppNavHost", "Logout triggered, navigating to login")
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        // Conductor dashboard route.
        composable("conductor_dashboard") {
            Log.d("AppNavHost", "Navigating to ConductorDashboard")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                ConductorDashboard(
                    user = user,
                    onLogout = {
                        // Handle logout and redirect to login.
                        Log.d("AppNavHost", "Logout triggered, navigating to login")
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
    }
}

// UserDashboard determines the user's role and redirects to the appropriate dashboard.
//  Ensures users see the correct dashboard (Rider/Conductor) based on their role.
// Fetches role from Firebase and navigates accordingly, with timeout handling.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboard(navController: NavHostController, user: FirebaseUser) {
    // State to store the user's role.
    var role by remember { mutableStateOf<String?>(null) }
    // State to display errors.
    var error by remember { mutableStateOf<String?>(null) }
    // Coroutine scope for Firebase calls.
    val scope = rememberCoroutineScope()
    // Android context for toasts.
    val context = LocalContext.current

    // Fetch role when the user ID changes.
    LaunchedEffect(user.uid) {
        Log.d("UserDashboard", "Starting role fetch for user ${user.uid}, email: ${user.email}")
        // Timeout after 3 seconds to prevent hanging.
        withTimeoutOrNull(3000L) {
            try {
                // Fetch role from AuthRepository.
                role = AuthRepository().getUserRole(user.uid)
                Log.d("UserDashboard", "Role fetched successfully: $role")
                // Navigate based on role.
                when (role) {
                    "Conductor" -> {
                        Log.d("UserDashboard", "Navigating to conductor_dashboard")
                        navController.navigate("conductor_dashboard") {
                            popUpTo("user_dashboard") { inclusive = true }
                        }
                    }
                    else -> {
                        Log.d("UserDashboard", "Navigating to rider_dashboard")
                        navController.navigate("rider_dashboard") {
                            popUpTo("user_dashboard") { inclusive = true }
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle errors and fallback to Rider dashboard.
                Log.e("UserDashboard", "Error fetching role: ${e.message}", e)
                error = "রোল পুনরুদ্ধারে ত্রুটি: ${e.message}"
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                Log.d("UserDashboard", "Falling back to rider_dashboard")
                navController.navigate("rider_dashboard") {
                    popUpTo("user_dashboard") { inclusive = true }
                }
            }
        } ?: run {
            // Handle timeout and fallback to Rider dashboard.
            Log.w("UserDashboard", "Timeout fetching role for user ${user.uid}")
            error = "রোল পুনরুদ্ধারে সময় শেষ"
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            Log.d("UserDashboard", "Falling back to rider_dashboard due to timeout")
            navController.navigate("rider_dashboard") {
                popUpTo("user_dashboard") { inclusive = true }
            }
        }
    }

    // Display loading or error state while fetching role.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (role == null && error == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("লোড হচ্ছে...") // Bangla loading message.
            }
        } else if (error != null) {
            Text(
                text = error ?: "অজানা ত্রুটি",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// RiderDashboard is the main screen for Riders to book tickets.
//  Placeholder UI for Riders to interact with schedules (to be implemented).
//  Displays a welcome message and buttons, will add booking features later.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboard(user: FirebaseUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    Log.d("RiderDashboard", "Rendering RiderDashboard for ${user.email}")

    // Centered column layout for Rider dashboard.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome title in Bangla.
        Text(
            text = "রাইডার ড্যাশবোর্ড",
            style = MaterialTheme.typography.headlineMedium
        )
        // Display user's email.
        Text("স্বাগতম, ${user.email}")

        Spacer(modifier = Modifier.height(24.dp))

        // Placeholder button for future booking feature.
        Button(
            onClick = {
                Toast.makeText(context, "টিকিট বুক করার পর্দা আসবে", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("টিকিট বুক করুন") // Bangla label for booking.
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout button.
        OutlinedButton(
            onClick = {
                Log.d("RiderDashboard", "Logging out")
                Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
                onLogout()
            }
        ) {
            Text("লগআউট") // Bangla label for logout.
        }
    }
}

// ConductorDashboard is the main screen for Conductors to manage schedules.
// Placeholder UI for Conductors to update bus schedules (to be implemented).
// Displays a welcome message and buttons, will add schedule features later.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorDashboard(user: FirebaseUser, onLogout: () -> Unit) {
    val context = LocalContext.current
    Log.d("ConductorDashboard", "Rendering ConductorDashboard for ${user.email}")

    // Centered column layout for Conductor dashboard.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome title in Bangla.
        Text(
            text = "কন্ডাক্টর ড্যাশবোর্ড",
            style = MaterialTheme.typography.headlineMedium
        )
        // Display user's email.
        Text("স্বাগতম, ${user.email}")

        Spacer(modifier = Modifier.height(24.dp))

        // Placeholder button for future schedule update feature.
        Button(
            onClick = {
                Toast.makeText(context, "শিডিউল আপডেট পর্দা আসবে", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("শিডিউল আপডেট করুন") // Bangla label for schedule update.
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout button.
        OutlinedButton(
            onClick = {
                Log.d("ConductorDashboard", "Logging out")
                Toast.makeText(context, "লগআউট সফল", Toast.LENGTH_SHORT).show()
                onLogout()
            }
        ) {
            Text("লগআউট") // Bangla label for logout.
        }
    }
}

// MuriTinTheme applies the app's custom theme.
//  Ensures consistent styling across all screens.
// Uses MaterialTheme from Jetpack Compose (to be customized later).
@Composable
fun MuriTinTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
