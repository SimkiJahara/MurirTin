package com.example.muritin

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(Dispatchers.IO)
    lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
            Log.d("MainActivity", "Firebase initialized successfully")
            scope.launch {
                AuthRepository().debugSaveTestData() // Remove after testing
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed: ${e.message}", e)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize Places SDK with API key
        val apiKey = getString(R.string.map_api_key)
        Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)

        setContent {
            MuriTinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController)
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    Log.d("AppNavHost", "Current user: ${currentUser?.email ?: "null"}, uid: ${currentUser?.uid ?: "null"}")
    val startDestination = if (currentUser != null) "user_dashboard" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            Log.d("AppNavHost", "Navigating to LoginScreen")
            LoginScreen(
                navController = navController,
                onLoginSuccess = { user ->
                    Log.d("AppNavHost", "Login success, navigating to user_dashboard")
                    navController.navigate("user_dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("signup") {
            Log.d("AppNavHost", "Navigating to SignUpScreen")
            SignUpScreen(
                navController = navController,
                onSignUpSuccess = { user ->
                    Log.d("AppNavHost", "Signup success, navigating to login")
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }
        composable("signup_conductor") {
            Log.d("AppNavHost", "Navigating to SignUpScreen for Conductor")
            SignUpScreen(
                navController = navController,
                onSignUpSuccess = { user ->
                    Log.d("AppNavHost", "Conductor signup success, navigating to owner_dashboard")
                    navController.navigate("owner_dashboard") {
                        popUpTo("signup_conductor") { inclusive = true }
                    }
                },
                preSelectedRole = "Conductor"
            )
        }
        composable("help") {
            HelpScreen(navController = navController)
        }
        composable("user_dashboard") {
            Log.d("AppNavHost", "Navigating to UserDashboard")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                UserDashboard(navController, user)
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        composable("rider_dashboard") {
            Log.d("AppNavHost", "Navigating to RiderDashboard")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                RiderDashboard(
                    navController = navController,
                    user = user,
                    onLogout = {
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
        composable("conductor_dashboard") {
            Log.d("AppNavHost", "Navigating to ConductorDashboard")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                ConductorDashboard(
                    navController = navController,
                    user = user,
                    onLogout = {
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
        composable("owner_dashboard") {
            Log.d("AppNavHost", "Navigating to OwnerDashboard")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                OwnerDashboard(
                    navController = navController,
                    user = user,
                    onLogout = {
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
        composable("show_account_info") {
            Show_Account_Info(navController = navController)
        }
        composable("profile_update") {
            Userprofile_Update(navController = navController)
        }
        // Add these composable routes to your NavHost in MainActivity.kt

        composable("past_trips") {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                PastTripsScreen(navController = navController, user = user)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        composable("rider_help") {
            RiderHelpScreen(navController = navController)
        }
        composable("owner_help") {
            OwnerHelpScreen(navController = navController)
        }
        composable("conductor_help") {
            ConductorHelpScreen(navController = navController)
        }

        composable("conductor_chat_list") {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                ConductorChatListScreen(navController = navController, user = user)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        composable("conductor_list") {
            Log.d("AppNavHost", "Navigating to ConductorListScreen")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                ConductorListScreen(navController = navController, user = user)
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        composable("register_bus") {
            Log.d("AppNavHost", "Navigating to BusRegistrationScreen")
            BusRegistrationScreen(navController = navController)
        }
        composable("bus_list") {
            Log.d("AppNavHost", "Navigating to BusListScreen")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                BusListScreen(navController = navController, user = user)
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        composable("trip_request") {
            Log.d("AppNavHost", "Navigating to TripRequestScreen")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                TripRequestScreen(navController = navController, user = user)
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        composable("my_requests") {
            Log.d("AppNavHost", "Navigating to MyRequestsScreen")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                MyRequestsScreen(navController = navController, user = user)
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        composable("live_tracking/{requestId}") { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            Log.d("AppNavHost", "Navigating to LiveTrackingScreen for $requestId")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                LiveTrackingScreen(navController = navController, user = user, requestId = requestId)
            } else {
                LaunchedEffect(Unit) {
                    Log.d("AppNavHost", "No user, navigating to login")
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
        composable("chat/{requestId}") { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            Log.d("AppNavHost", "Opening ChatScreen for requestId = $requestId")
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                ChatScreen(
                    navController = navController,
                    requestId = requestId,
                    user = user
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboard(navController: NavHostController, user: FirebaseUser) {
    var userData by remember { mutableStateOf<User?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(user.uid) {
        Log.d("UserDashboard", "Fetching user data for ${user.uid}, email: ${user.email}")
        withTimeoutOrNull(3000L) {
            try {
                val result = AuthRepository().getUser(user.uid)
                userData = result.getOrNull()
                if (userData == null) {
                    Log.e("UserDashboard", "No user data found for ${user.uid}")
                    error = "ব্যবহারকারীর তথ্য পাওয়া যায়নি"
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    return@withTimeoutOrNull
                }
                Log.d("UserDashboard", "User data fetched: role=${userData?.role}, name=${userData?.name}, phone=${userData?.phone}, age=${userData?.age}")

                // Navigate directly to appropriate dashboard based on role
                when (userData?.role) {
                    "Conductor" -> {
                        Log.d("UserDashboard", "Navigating to conductor_dashboard")
                        navController.navigate("conductor_dashboard") {
                            popUpTo("user_dashboard") { inclusive = true }
                        }
                    }
                    "Owner" -> {
                        Log.d("UserDashboard", "Navigating to owner_dashboard")
                        navController.navigate("owner_dashboard") {
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
                Log.e("UserDashboard", "Error fetching user data: ${e.message}", e)
                error = "তথ্য পুনরুদ্ধারে ত্রুটি: ${e.message}"
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                navController.navigate("rider_dashboard") {
                    popUpTo("user_dashboard") { inclusive = true }
                }
            }
        } ?: run {
            Log.w("UserDashboard", "Timeout fetching user data for ${user.uid}")
            error = "তথ্য পুনরুদ্ধারে সময় শেষ"
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            navController.navigate("rider_dashboard") {
                popUpTo("user_dashboard") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (userData == null && error == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("লোড হচ্ছে...")
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

