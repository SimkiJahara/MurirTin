package com.example.muritin

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

/**
 * Foreground service that monitors rider location during active trips
 * Automatically detects:
 * - When rider reaches destination (auto-completes trip)
 * - When rider approaches early/late exit stops (auto-recalculates fare)
 */
class TripMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var currentRequestId: String? = null
    private var isMonitoring = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "trip_monitoring_channel"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
        private const val LOCATION_FASTEST_INTERVAL = 5000L // 5 seconds

        fun startMonitoring(context: Context, requestId: String) {
            val intent = Intent(context, TripMonitoringService::class.java).apply {
                action = "START_MONITORING"
                putExtra("REQUEST_ID", requestId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, TripMonitoringService::class.java).apply {
                action = "STOP_MONITORING"
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("TripMonitorService", "Service created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Starting trip monitoring..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MONITORING" -> {
                val requestId = intent.getStringExtra("REQUEST_ID")
                if (requestId != null && !isMonitoring) {
                    startLocationMonitoring(requestId)
                }
            }
            "STOP_MONITORING" -> {
                stopLocationMonitoring()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startLocationMonitoring(requestId: String) {
        currentRequestId = requestId
        isMonitoring = true

        Log.d("TripMonitorService", "Starting location monitoring for request: $requestId")

        // Create location request
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setWaitForAccurateLocation(false)
        }.build()

        // Create location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("TripMonitorService", "Location permission not granted")
            stopSelf()
            return
        }

        // Start location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        updateNotification("Monitoring trip progress...")
    }

    private fun handleLocationUpdate(location: Location) {
        val requestId = currentRequestId ?: return
        val latLng = LatLng(location.latitude, location.longitude)

        Log.d("TripMonitorService", "Location update: ${location.latitude}, ${location.longitude}")

        // Process location in background
        serviceScope.launch {
            try {
                val result = AuthRepository().monitorRiderTrip(requestId, latLng)

                if (result.isSuccess) {
                    when (val status = result.getOrNull()) {
                        "completed" -> {
                            Log.d("TripMonitorService", "Trip auto-completed")
                            updateNotification("Trip completed! Thank you for riding.")
                            // Stop monitoring after 5 seconds
                            delay(5000)
                            stopLocationMonitoring()
                            stopSelf()
                        }
                        "exit_changed_early" -> {
                            Log.d("TripMonitorService", "Early exit detected, fare recalculated")
                            updateNotification("Exit location updated. Fare recalculated.")
                        }
                        "exit_changed_late" -> {
                            Log.d("TripMonitorService", "Late exit detected, fare recalculated")
                            updateNotification("Exit location updated. Fare recalculated.")
                        }
                        "monitoring" -> {
                            // Normal monitoring, update progress
                            updateNotification("Trip in progress...")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TripMonitorService", "Error processing location: ${e.message}", e)
            }
        }
    }

    private fun stopLocationMonitoring() {
        if (isMonitoring) {
            Log.d("TripMonitorService", "Stopping location monitoring")
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isMonitoring = false
            currentRequestId = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trip Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors your trip progress and automatically completes rides"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Muritin - Active Trip")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationMonitoring()
        serviceScope.cancel()
        Log.d("TripMonitorService", "Service destroyed")
    }
}