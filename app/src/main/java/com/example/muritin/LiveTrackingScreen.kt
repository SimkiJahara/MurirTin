package com.example.muritin

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseUser
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
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
    val pickAndDestLLofBus = remember { mutableStateListOf<PointLocation>() }

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

    LaunchedEffect (Unit) {
        scope.launch {
            val busLoc = AuthRepository().getLLofPickupDestofBusForRider(requestId)
            pickAndDestLLofBus.clear()
            pickAndDestLLofBus.addAll(busLoc)
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
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomGesturesEnabled = true, zoomControlsEnabled = false),
                    properties = MapProperties(isTrafficEnabled = true)
                ) {
                    conductorLocation?.let { loc ->
                        Marker(
                            state = MarkerState(position = LatLng(loc.lat, loc.lng)),
                            title = "বাস লোকেশন",
                            icon = BitmapDescriptorFactory.fromResource(R.drawable.bus_marker_icon)
                        )
                    }
                    pickAndDestLLofBus.first().let { p ->
                        Marker(
                            state = MarkerState(position = LatLng(pickAndDestLLofBus.first().latitude, pickAndDestLLofBus.first().longitude)),
                            title = "পিকআপ",
                            snippet = pickAndDestLLofBus.first().address,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
                    pickAndDestLLofBus.lastOrNull().let { p ->
                        Marker(
                            state = MarkerState(position = LatLng(pickAndDestLLofBus.last().latitude, pickAndDestLLofBus.last().longitude)),
                            title = "গন্তব্য",
                            snippet = pickAndDestLLofBus.last().address,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            )
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