package com.example.muritin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permission states for location access
 */
enum class PermissionState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    NOT_REQUESTED
}

/**
 * Check if location permissions are granted
 */
fun Context.hasLocationPermissions(): Boolean {
    val fineLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseLocation = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fineLocation || coarseLocation
}

/**
 * Check if background location permission is granted (Android 10+)
 */
fun Context.hasBackgroundLocationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Not needed on older versions
    }
}

/**
 * Check if notification permission is granted (Android 13+)
 */
fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Not needed on older versions
    }
}

/**
 * Composable to request location permissions
 */
@Composable
fun rememberLocationPermissionState(
    onPermissionResult: (granted: Boolean) -> Unit = {}
): Pair<() -> Unit, Boolean> {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(context.hasLocationPermissions())
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        permissionGranted = granted
        onPermissionResult(granted)
    }

    val requestPermission = {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    return Pair(requestPermission, permissionGranted)
}

/**
 * Composable to request background location permission (Android 10+)
 */
@Composable
fun rememberBackgroundLocationPermissionState(
    onPermissionResult: (granted: Boolean) -> Unit = {}
): Pair<() -> Unit, Boolean> {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(context.hasBackgroundLocationPermission())
    }

    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        onPermissionResult(granted)
    }

    val requestPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationPermissionLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            // Not needed on older versions
            onPermissionResult(true)
        }
    }

    return Pair(requestPermission, permissionGranted)
}

/**
 * Composable to request notification permission (Android 13+)
 */
@Composable
fun rememberNotificationPermissionState(
    onPermissionResult: (granted: Boolean) -> Unit = {}
): Pair<() -> Unit, Boolean> {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(context.hasNotificationPermission())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        onPermissionResult(granted)
    }

    val requestPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            // Not needed on older versions
            onPermissionResult(true)
        }
    }

    return Pair(requestPermission, permissionGranted)
}

/**
 * Request all necessary permissions for trip monitoring
 */
@Composable
fun rememberTripMonitoringPermissions(
    onAllPermissionsGranted: () -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    var locationGranted by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(false) }

    val (requestLocation, hasLocation) = rememberLocationPermissionState { granted ->
        locationGranted = granted
    }

    val (requestNotification, hasNotification) = rememberNotificationPermissionState { granted ->
        notificationGranted = granted
    }

    LaunchedEffect(locationGranted, notificationGranted) {
        if (locationGranted && notificationGranted) {
            onAllPermissionsGranted()
        }
    }

    val requestAllPermissions = {
        // First request location
        if (!context.hasLocationPermissions()) {
            requestLocation()
        } else {
            locationGranted = true
        }

        // Then request notification
        if (!context.hasNotificationPermission()) {
            requestNotification()
        } else {
            notificationGranted = true
        }
    }

    return requestAllPermissions
}