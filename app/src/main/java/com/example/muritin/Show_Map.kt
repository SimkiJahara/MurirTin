package com.example.muritin

import android.widget.Toast
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
fun Show_Map(navController: NavHostController) {
    val context = LocalContext.current

    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val singapore = LatLng(1.35, 103.87)
    val currentMarkerState = rememberMarkerState(position = singapore)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }

    val mapProperties = MapProperties(
        isTrafficEnabled = true,
        isMyLocationEnabled = true
    )

    val uiSettings = MapUiSettings(
        zoomGesturesEnabled = true,
        zoomControlsEnabled = false,
    )

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun getLastKnownLocation(
        fusedLocationClient: FusedLocationProviderClient,
        cameraPositionState: CameraPositionState
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(context, "আপনার লোকেশন পাওয়া যায়নি", Toast.LENGTH_LONG).show()
        }
    }

    val requestPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastKnownLocation(fusedLocationClient, cameraPositionState)
        } else {Toast.makeText(context, "লোকেশন এর অনুমতি দেওয়া জরুরি", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(lifecycleOwner) {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getLastKnownLocation(fusedLocationClient, cameraPositionState)
            }
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true -> {
            }
            else -> {
                requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
    // State to hold the clicked location
    var clickedLatLng by remember { mutableStateOf<LatLng?>(null) }
    val markerState = rememberMarkerState(position = clickedLatLng ?: singapore)

    // Update marker state when clickedLatLng changes
    LaunchedEffect(clickedLatLng) {
        markerState.position = clickedLatLng ?: singapore
    }

    // On map click, update the clicked location
    val onMapClick: (LatLng) -> Unit = { latLng ->
        clickedLatLng = latLng
        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapClick = onMapClick,
    ) {

            Marker(
                state = markerState,
                title = "Clicked Location",
            )

    }
}