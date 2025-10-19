package com.example.muritin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(navController: NavHostController, user: FirebaseUser, requestId: String) {
    val scope = rememberCoroutineScope()
    var request by remember { mutableStateOf<Request?>(null) }
    var conductorLocation by remember { mutableStateOf<ConductorLocation?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(requestId) {
        try {
            val allRequests = AuthRepository().getRequestsForUser(user.uid)
            request = allRequests.find { it.id == requestId }
            request?.conductorId?.let { conductorId ->
                conductorLocation = AuthRepository().getConductorLocation(conductorId)
            }
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            Log.e("LiveTrackingScreen", "Error: ${e.message}")
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("লাইভ ট্র্যাকিং") }) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                request?.let { req ->
                    Text("বাস: ${req.busId}")
                    Text("কন্ডাক্টর: ${req.conductorId}")
                    Text("OTP: ${req.otp}")
                }

                val cameraPositionState = rememberCameraPositionState()
                conductorLocation?.let { loc ->
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(loc.lat, loc.lng), 15f)
                }

                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    conductorLocation?.let { loc ->
                        Marker(
                            state = MarkerState(position = LatLng(loc.lat, loc.lng)),
                            title = "বাস লোকেশন"
                        )
                    }
                    request?.pickupLatLng?.let { p ->
                        Marker(
                            state = MarkerState(position = LatLng(p.lat, p.lng)),
                            title = "পিকআপ"
                        )
                    }
                }

                Button(onClick = {
                    scope.launch {
                        request?.conductorId?.let { conductorId ->
                            conductorLocation = AuthRepository().getConductorLocation(conductorId)
                        }
                    }
                }) {
                    Text("রিফ্রেশ")
                }
            }
        }
    }
}