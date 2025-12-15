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
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/**
 * FIXED: Improved trip monitoring with better destination detection
 */
class TripMonitoringService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentRequestId: String? = null
    private var isMonitoring = false
    private var lastKnownLocation: Location? = null
    private var consecutiveDestinationHits = 0 // Track how many times we're near destination

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                lastKnownLocation = location
                Log.d("TripMonitoring", "📍 New location: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
                onLocationUpdate(location)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "trip_monitoring_channel"
        private const val NOTIFICATION_ID = 1001

        // FIXED: Increased radius to handle GPS inaccuracy
        private const val DESTINATION_RADIUS_METERS = 100.0 // Changed from 50m to 100m
        private const val STOP_PROXIMITY_METERS = 150.0 // Changed from 100m to 150m
        private const val MIN_HITS_FOR_ARRIVAL = 2 // Must be near destination 2 times in a row

        fun startMonitoring(context: Context, requestId: String) {
            val intent = Intent(context, TripMonitoringService::class.java).apply {
                putExtra("REQUEST_ID", requestId)
                action = "START_MONITORING"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Log.d("TripMonitoring", "🚀 Start monitoring requested for request: $requestId")
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, TripMonitoringService::class.java).apply {
                action = "STOP_MONITORING"
            }
            context.startService(intent)
            Log.d("TripMonitoring", "🛑 Stop monitoring requested")
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        Log.d("TripMonitoring", "✅ Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_MONITORING" -> {
                val requestId = intent.getStringExtra("REQUEST_ID")
                if (requestId != null) {
                    startMonitoringTrip(requestId)
                }
            }
            "STOP_MONITORING" -> {
                stopMonitoringTrip()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMonitoringTrip(requestId: String) {
        if (isMonitoring && currentRequestId == requestId) {
            Log.d("TripMonitoring", "⚠️ Already monitoring request $requestId")
            return
        }

        currentRequestId = requestId
        isMonitoring = true
        consecutiveDestinationHits = 0

        startForeground(NOTIFICATION_ID, createNotification("যাত্রা পর্যবেক্ষণ সক্রিয়"))
        startLocationUpdates()

        Log.d("TripMonitoring", "🎯 Started monitoring request $requestId")
    }

    private fun stopMonitoringTrip() {
        isMonitoring = false
        currentRequestId = null
        consecutiveDestinationHits = 0
        stopLocationUpdates()
        Log.d("TripMonitoring", "❌ Stopped monitoring")
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("TripMonitoring", "❌ Location permission not granted")
            stopSelf()
            return
        }

        // FIXED: More frequent updates for better accuracy
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // Update every 5 seconds (was 10)
        ).apply {
            setMinUpdateIntervalMillis(3000) // Fastest: 3 seconds (was 5)
            setWaitForAccurateLocation(false) // Don't wait for perfect accuracy
            setMaxUpdateDelayMillis(8000) // Force update after 8 seconds
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d("TripMonitoring", "📡 Location updates started (every 5s)")
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("TripMonitoring", "📡 Location updates stopped")
    }

    private fun onLocationUpdate(location: Location) {
        val requestId = currentRequestId ?: return

        scope.launch {
            try {
                // Get current request data
                val request = getRequest(requestId) ?: run {
                    Log.e("TripMonitoring", "❌ Could not fetch request $requestId")
                    return@launch
                }

                // Only monitor if rider is travelling and not completed
                if (request.rideStatus?.inBusTravelling != true) {
                    Log.d("TripMonitoring", "⚠️ Rider not in bus, stopping monitoring")
                    stopMonitoringTrip()
                    stopSelf()
                    return@launch
                }

                if (request.rideStatus.tripCompleted == true) {
                    Log.d("TripMonitoring", "✅ Trip already completed, stopping monitoring")
                    stopMonitoringTrip()
                    stopSelf()
                    return@launch
                }

                // Update notification
                updateNotification("যাত্রা চলছে - ${request.destination} এর দিকে")

                // PRIORITY 1: Check destination arrival FIRST
                val arrivedAtDestination = checkDestinationArrival(request, location)

                if (arrivedAtDestination) {
                    // Trip completed, stop here
                    return@launch
                }

                // PRIORITY 2: Check for early/late exit
                checkRouteChanges(request, location)

            } catch (e: Exception) {
                Log.e("TripMonitoring", "❌ Error processing location: ${e.message}", e)
            }
        }
    }

    private suspend fun checkDestinationArrival(request: Request, location: Location): Boolean {
        val destLat = request.destinationLatLng?.lat ?: return false
        val destLng = request.destinationLatLng?.lng ?: return false

        val distanceToDestination = calculateDistance(
            location.latitude,
            location.longitude,
            destLat,
            destLng
        )

        val distanceMeters = distanceToDestination * 1000

        Log.d("TripMonitoring", "📏 Distance to destination (${request.destination}): ${String.format("%.1f", distanceMeters)}m")

        // FIXED: Use consecutive hits to avoid false positives
        if (distanceMeters <= DESTINATION_RADIUS_METERS) {
            consecutiveDestinationHits++
            Log.d("TripMonitoring", "🎯 Near destination! Hit count: $consecutiveDestinationHits/$MIN_HITS_FOR_ARRIVAL")

            if (consecutiveDestinationHits >= MIN_HITS_FOR_ARRIVAL) {
                Log.d("TripMonitoring", "✅ Confirmed arrival at destination, auto-completing trip")
                autoCompleteTripAtDestination(request)
                return true
            }
        } else {
            // Reset counter if moved away
            if (consecutiveDestinationHits > 0) {
                Log.d("TripMonitoring", "⚠️ Moved away from destination, resetting counter")
            }
            consecutiveDestinationHits = 0
        }

        return false
    }

    private suspend fun checkRouteChanges(request: Request, location: Location) {
        val busId = request.busId ?: return

        // Don't check if already requested early/late exit
        if (request.rideStatus?.earlyExitRequested == true ||
            request.rideStatus?.lateExitRequested == true) {
            return
        }

        val bus = getBus(busId) ?: return
        val route = bus.route ?: return

        // Get rider's pickup and destination geohashes
        val pickupGeoHash = request.pickupLatLng?.let {
            GeoFireUtils.getGeoHashForLocation(GeoLocation(it.lat, it.lng), 5)
        }
        val destGeoHash = request.destinationLatLng?.let {
            GeoFireUtils.getGeoHashForLocation(GeoLocation(it.lat, it.lng), 5)
        }

        // Build route index
        val routeStops = mutableListOf<PointLocation>()
        route.originLoc?.let { routeStops.add(it) }
        routeStops.addAll(route.stopPointsLoc)
        route.destinationLoc?.let { routeStops.add(it) }

        // Find pickup and destination indices
        val pickupIndex = routeStops.indexOfFirst { it.geohash == pickupGeoHash }
        val destIndex = routeStops.indexOfFirst { it.geohash == destGeoHash }

        if (pickupIndex == -1 || destIndex == -1) return

        // Get distance to original destination
        val distanceToOriginalDest = calculateDistance(
            location.latitude,
            location.longitude,
            request.destinationLatLng!!.lat,
            request.destinationLatLng.lng
        )

        // Check if rider has passed destination (for late exit detection)
        val hasPassedDestination = distanceToOriginalDest * 1000 > 200

        if (!hasPassedDestination) {
            // EARLY EXIT: Check stops between pickup and destination
            var closestEarlyStop: PointLocation? = null
            var closestDistance = Double.MAX_VALUE

            for (i in pickupIndex + 1 until destIndex) {
                val stop = routeStops[i]
                val distanceToStop = calculateDistance(
                    location.latitude,
                    location.longitude,
                    stop.latitude,
                    stop.longitude
                )

                if (distanceToStop * 1000 <= STOP_PROXIMITY_METERS && distanceToStop < closestDistance) {
                    closestDistance = distanceToStop
                    closestEarlyStop = stop
                }
            }

            if (closestEarlyStop != null) {
                Log.d("TripMonitoring", "🔔 Early exit detected: ${closestEarlyStop.address}")
                handleEarlyExit(request, closestEarlyStop, bus)
                return
            }

        } else {
            // LATE EXIT: Rider passed destination
            var closestLateStop: PointLocation? = null
            var closestDistance = Double.MAX_VALUE

            for (i in destIndex + 1 until routeStops.size) {
                val stop = routeStops[i]
                val distanceToStop = calculateDistance(
                    location.latitude,
                    location.longitude,
                    stop.latitude,
                    stop.longitude
                )

                if (distanceToStop * 1000 <= STOP_PROXIMITY_METERS && distanceToStop < closestDistance) {
                    closestDistance = distanceToStop
                    closestLateStop = stop
                }
            }

            if (closestLateStop != null) {
                Log.d("TripMonitoring", "🔔 Late exit detected: ${closestLateStop.address}")
                handleLateExit(request, closestLateStop, bus)
                return
            }
        }
    }

    private suspend fun handleEarlyExit(request: Request, stop: PointLocation, bus: Bus) {
        try {
            Log.d("TripMonitoring", "⬅️ Processing early exit to: ${stop.address}")

            val newFare = calculateFareForRoute(bus, request.pickup, stop.address, request.seats)
            val stopLatLng = LatLngData(stop.latitude, stop.longitude)

            val updates = mapOf(
                "rideStatus/earlyExitRequested" to true,
                "rideStatus/earlyExitRequestedAt" to System.currentTimeMillis(),
                "rideStatus/earlyExitStop" to stop.address,
                "rideStatus/earlyExitLatLng" to stopLatLng,
                "destination" to stop.address,
                "destinationLatLng" to stopLatLng,
                "rideStatus/actualFare" to newFare
            )

            updateRequest(request.id, updates)
            updateNotification("আগাম নামার স্থান: ${stop.address} - ভাড়া: ৳$newFare")

            // Reset counter since destination changed
            consecutiveDestinationHits = 0

            Log.d("TripMonitoring", "✅ Early exit processed - New fare: ৳$newFare")

        } catch (e: Exception) {
            Log.e("TripMonitoring", "❌ Early exit failed: ${e.message}", e)
        }
    }

    private suspend fun handleLateExit(request: Request, stop: PointLocation, bus: Bus) {
        try {
            Log.d("TripMonitoring", "➡️ Processing late exit to: ${stop.address}")

            val newFare = calculateFareForRoute(bus, request.pickup, stop.address, request.seats)
            val stopLatLng = LatLngData(stop.latitude, stop.longitude)

            val updates = mapOf(
                "rideStatus/lateExitRequested" to true,
                "rideStatus/lateExitStop" to stop.address,
                "rideStatus/lateExitLatLng" to stopLatLng,
                "destination" to stop.address,
                "destinationLatLng" to stopLatLng,
                "rideStatus/actualFare" to newFare
            )

            updateRequest(request.id, updates)
            updateNotification("পরে নামার স্থান: ${stop.address} - ভাড়া: ৳$newFare")

            // Reset counter since destination changed
            consecutiveDestinationHits = 0

            Log.d("TripMonitoring", "✅ Late exit processed - New fare: ৳$newFare")

        } catch (e: Exception) {
            Log.e("TripMonitoring", "❌ Late exit failed: ${e.message}", e)
        }
    }

    private suspend fun autoCompleteTripAtDestination(request: Request) {
        try {
            Log.d("TripMonitoring", "🏁 Auto-completing trip at destination")

            val currentTime = System.currentTimeMillis()
            val updates = mapOf(
                "rideStatus/riderArrivedConfirmed" to true,
                "rideStatus/riderArrivedAt" to currentTime,
                "rideStatus/conductorArrivedConfirmed" to true,
                "rideStatus/conductorArrivedAt" to currentTime,
                "rideStatus/fareCollected" to true,
                "rideStatus/fareCollectedAt" to currentTime,
                "rideStatus/tripCompleted" to true,
                "rideStatus/tripCompletedAt" to currentTime,
                "status" to "Completed"
            )

            updateRequest(request.id, updates)

            updateNotification("✅ যাত্রা সম্পূর্ণ হয়েছে")

            Log.d("TripMonitoring", "✅ Trip auto-completed successfully")

            // Stop monitoring after 3 seconds
            delay(3000)
            stopMonitoringTrip()
            stopSelf()

        } catch (e: Exception) {
            Log.e("TripMonitoring", "❌ Auto-completion failed: ${e.message}", e)
        }
    }

    private fun calculateFareForRoute(bus: Bus, from: String, to: String, seats: Int): Int {
        val cleanFrom = from.replace(Regex("[^A-Za-z0-9 ]"), "")
        val cleanTo = to.replace(Regex("[^A-Za-z0-9 ]"), "")

        val registeredFare = bus.fares[cleanFrom]?.get(cleanTo)

        return if (registeredFare != null) {
            registeredFare * seats
        } else {
            val pickup = bus.route?.stopPointsLoc?.find {
                it.address.replace(Regex("[^A-Za-z0-9 ]"), "") == cleanFrom
            }
            val dest = bus.route?.stopPointsLoc?.find {
                it.address.replace(Regex("[^A-Za-z0-9 ]"), "") == cleanTo
            }

            if (pickup != null && dest != null) {
                val distance = calculateDistance(
                    pickup.latitude, pickup.longitude,
                    dest.latitude, dest.longitude
                )
                ((distance * 10).toInt().coerceAtLeast(20)) * seats
            } else {
                100 * seats
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private suspend fun getRequest(requestId: String): Request? = withContext(Dispatchers.IO) {
        try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("requests")
                .child(requestId)
                .get()
                .await()
            snapshot.getValue(Request::class.java)
        } catch (e: Exception) {
            Log.e("TripMonitoring", "Failed to get request: ${e.message}", e)
            null
        }
    }

    private suspend fun getBus(busId: String): Bus? = withContext(Dispatchers.IO) {
        try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("buses")
                .child(busId)
                .get()
                .await()
            snapshot.getValue(Bus::class.java)
        } catch (e: Exception) {
            Log.e("TripMonitoring", "Failed to get bus: ${e.message}", e)
            null
        }
    }

    private suspend fun updateRequest(requestId: String, updates: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            try {
                FirebaseDatabase.getInstance()
                    .getReference("requests")
                    .child(requestId)
                    .updateChildren(updates)
                    .await()
            } catch (e: Exception) {
                Log.e("TripMonitoring", "Failed to update request: ${e.message}", e)
                throw e
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "যাত্রা পর্যবেক্ষণ",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "স্বয়ংক্রিয় যাত্রা ট্র্যাকিং"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("মুরিটিন - যাত্রা পর্যবেক্ষণ")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        scope.cancel()
        Log.d("TripMonitoring", "🔚 Service destroyed")
    }
}