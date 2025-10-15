package com.example.muritin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.*
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data classes for Geocoding API response
data class GeocodeResponse(val results: List<GeocodeResult>, val status: String)
data class GeocodeResult(val geometry: Geometry)
data class Geometry(val location: Location)
data class Location(val lat: Double, val lng: Double)

// Data classes for Directions API response
data class DirectionsResponse(val routes: List<Route>, val status: String)
data class Route(val overview_polyline: OverviewPolyline)
data class OverviewPolyline(val points: String)

// So that the search bars can be cleared from outside the function
object GlobalSearchState {
    var origin by mutableStateOf("")
    var destination by mutableStateOf("")
    var stop by mutableStateOf("")

    fun clearAll() {
        origin = ""
        destination = ""
        stop = ""
    }
}

// Retrofit interfaces
interface GeocodingApi {
    @GET("maps/api/geocode/json")
    suspend fun getLatLng(
        @Query("address") address: String,
        @Query("key") key: String
    ): GeocodeResponse
}

interface DirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun getRoute(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String? = null,
        @Query("key") key: String
    ): DirectionsResponse
}

// Function to decode the string (coordinates) returned by direction api request
fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val length = encoded.length
    var lat = 0
    var lng = 0
    while (index < length) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        val p = LatLng(lat * 1e-5, lng * 1e-5)
        poly.add(p)
    }
    return poly
}

@Composable
fun SearchLocation(
    label: String,
    apiKey: String,
    placesClient: com.google.android.libraries.places.api.net.PlacesClient,
    geocodingApi: GeocodingApi,
    coroutineScope: CoroutineScope,
    context: android.content.Context,
    onLocationSelected: (String, LatLng) -> Unit,
    cameraPositionState: CameraPositionState
) {
    // Pick global based on label
    val globalQueryRef = when (label) {
        "যাত্রা শুরুর অবস্থান" -> GlobalSearchState::origin
        "গন্তব্যস্থল" -> GlobalSearchState::destination
        "স্টপেজ (যদি থাকে)" -> GlobalSearchState::stop
        else -> null  // Fallback, though labels are fixed
    }

    var searchQuery by remember { mutableStateOf(globalQueryRef?.get() ?: "") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    globalQueryRef?.let {
        LaunchedEffect(it.get()) {
            searchQuery = it.get()
            suggestions = emptyList()  // Clear suggestions on reset
        }
    }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { newQuery ->
            searchQuery = newQuery
            globalQueryRef?.set(newQuery)
            if (newQuery.length > 2) {
                coroutineScope.launch {
                    val request = com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
                        .builder()
                        .setQuery(newQuery)
                        .setCountries("BD") // Bias to Bangladesh
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            suggestions = response.autocompletePredictions.map {
                                it.getFullText(null).toString()
                            }
                        }
                        .addOnFailureListener { suggestions = emptyList() }
                }
            } else {
                suggestions = emptyList()
            }
        },
        label = { Text(label) },
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(android.graphics.Color.parseColor("#6650a4"))
        ),
        trailingIcon = { if (isSearching) CircularProgressIndicator(modifier = Modifier.size(16.dp)) }
    )

    if (suggestions.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 150.dp)
                .padding(horizontal = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn {
                items(suggestions) { suggestion ->
                    ListItem(
                        headlineContent = { Text(suggestion, style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    isSearching = true
                                    try {
                                        val response = withContext(Dispatchers.IO) {
                                            geocodingApi.getLatLng(suggestion, apiKey)
                                        }
                                        if (response.status == "OK" && response.results.isNotEmpty()) {
                                            val latLng = LatLng(
                                                response.results[0].geometry.location.lat,
                                                response.results[0].geometry.location.lng
                                            )
                                            onLocationSelected(suggestion, latLng)
                                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                                latLng,
                                                cameraPositionState.position.zoom
                                            )
                                            searchQuery = suggestion
                                            globalQueryRef?.set(suggestion)
                                            suggestions = emptyList()
                                        } else {
                                            Toast.makeText(context, "লোকেশন পাওয়া যায়নি", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Divider(color = Color.LightGray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRegistrationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser ?: return
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var fitnessCertificate by remember { mutableStateOf("") }
    var taxToken by remember { mutableStateOf("") }
    //var stops by remember { mutableStateOf("") }
    var fares by remember { mutableStateOf(mapOf<String, Map<String, Int>>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showFareDialog by remember { mutableStateOf(false) }
    var currentStop by remember { mutableStateOf("") }
    var currentDestination by remember { mutableStateOf("") }
    var currentFare by remember { mutableStateOf("") }

    //For map
    val apiKey = context.getString(R.string.map_api_key)
    val placesClient = remember { com.google.android.libraries.places.api.Places.createClient(context) }
    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val geocodingApi = remember { retrofit.create(GeocodingApi::class.java) }
    val directionsApi = remember { retrofit.create(DirectionsApi::class.java) }
    val coroutineScope = rememberCoroutineScope()

    var originAddress by remember { mutableStateOf("") }
    var originLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationAddress by remember { mutableStateOf("") }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    val stops = remember { mutableStateListOf<Pair<String, LatLng>>() }
    val polylinePoints = remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var returned_points_from_directionapi =""
    val initialPosition = LatLng(23.8103, 90.4125) // Dhaka
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }
    val mapProperties = MapProperties(isTrafficEnabled = true)
    val uiSettings = MapUiSettings(zoomGesturesEnabled = true, zoomControlsEnabled = false)
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বাস রেজিস্ট্রেশন") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(2.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "বাস রেজিস্ট্রেশন করুন",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("বাসের নাম") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = name.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("বাস নম্বর") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = number.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fitnessCertificate,
                        onValueChange = { fitnessCertificate = it },
                        label = { Text("ফিটনেস সার্টিফিকেট") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = fitnessCertificate.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taxToken,
                        onValueChange = { taxToken = it },
                        label = { Text("ট্যাক্স টোকেন") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = taxToken.isBlank() && error != null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "বাসের রুট নির্বাচন করুন",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Origin Search
                    SearchLocation(
                        label = "যাত্রা শুরুর অবস্থান",
                        apiKey = apiKey,
                        placesClient = placesClient,
                        geocodingApi = geocodingApi,
                        coroutineScope = coroutineScope,
                        context = context,
                        onLocationSelected = { addr, latlng ->
                            originAddress = addr
                            originLatLng = latlng
                        },
                        cameraPositionState = cameraPositionState
                    )

                    // Destination Search
                    SearchLocation(
                        label = "গন্তব্যস্থল",
                        apiKey = apiKey,
                        placesClient = placesClient,
                        geocodingApi = geocodingApi,
                        coroutineScope = coroutineScope,
                        context = context,
                        onLocationSelected = { addr, latlng ->
                            destinationAddress = addr
                            destinationLatLng = latlng
                        },
                        cameraPositionState = cameraPositionState
                    )

                    // Add Stop Search
                    SearchLocation(
                        label = "স্টপেজ (একাধিক হলে একটার পর একটা মুছে লিখুন)",
                        apiKey = apiKey,
                        placesClient = placesClient,
                        geocodingApi = geocodingApi,
                        coroutineScope = coroutineScope,
                        context = context,
                        onLocationSelected = { addr, latlng ->
                            stops.add(addr to latlng)
                        },
                        cameraPositionState = cameraPositionState
                    )

                    // Button to ad Route
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Button(
                            onClick = {
                                if (originLatLng != null && destinationLatLng != null) {
                                    coroutineScope.launch {
                                        val originStr = "${originLatLng!!.latitude},${originLatLng!!.longitude}"
                                        val destStr = "${destinationLatLng!!.latitude},${destinationLatLng!!.longitude}"
                                        val waypoints = stops.joinToString("|") { "${it.second.latitude},${it.second.longitude}" }
                                        try {
                                            val response = withContext(Dispatchers.IO) {
                                                directionsApi.getRoute(
                                                    originStr,
                                                    destStr,
                                                    if (waypoints.isNotEmpty()) waypoints else null,
                                                    apiKey
                                                )
                                            }
                                            if (response.status == "OK" && response.routes.isNotEmpty()) {
                                                returned_points_from_directionapi = response.routes[0].overview_polyline.points
                                                val decoded = decodePolyline(returned_points_from_directionapi)
                                                polylinePoints.value = decoded

                                                // Adjust camera to show the entire route
                                                if (decoded.isNotEmpty()) {
                                                    val boundsBuilder = LatLngBounds.Builder()
                                                    decoded.forEach { boundsBuilder.include(it) }
                                                    val bounds = boundsBuilder.build()
                                                    cameraPositionState.animate(
                                                        CameraUpdateFactory.newLatLngBounds(bounds, 100)
                                                    )
                                                }
                                            } else {
                                                Toast.makeText(context, "রুট পাওয়া যায়নি", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "শুরুর এবং গন্তব্য স্থল যোগ করুন", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("রুট যোগ করুন")
                        }

                        Button(
                            onClick = {
                                // Clear all inputs, markers, and polyline
                                originAddress = ""
                                originLatLng = null
                                destinationAddress = ""
                                destinationLatLng = null
                                GlobalSearchState.clearAll()
                                stops.clear()
                                polylinePoints.value = emptyList()
                                // Reset camera to initial position
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.fromLatLngZoom(initialPosition, 10f)
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),  // Small gap between buttons
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB71C1C),
                                contentColor = Color.White
                            )
                        ) {
                            Text("রুট বাতিল করুন")
                        }
                    }
                    // Display route
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("রুট:", style = MaterialTheme.typography.labelLarge)
                            if (originAddress.isNotEmpty()) {
                                Text("যাত্রা শুরু: $originAddress ", style = MaterialTheme.typography.bodySmall)
                            }
                            if (stops.isNotEmpty()) {
                                stops.forEachIndexed { index, stop ->
                                    Text("স্টপ: ${stop.first} ", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (destinationAddress.isNotEmpty()) {
                                Text("গন্তব্য: $destinationAddress ", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Map (increased weight to take more space)
                    GoogleMap(
                        modifier = Modifier.fillMaxSize().height(400.dp),
                        cameraPositionState = cameraPositionState,
                        properties = mapProperties,
                        uiSettings = uiSettings
                    ) {
                        if (originLatLng != null) {
                            Marker(
                                state = MarkerState(position = originLatLng!!),
                                title = "শুরু",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                            )
                        }
                        if (destinationLatLng != null) {
                            Marker(
                                state = MarkerState(position = destinationLatLng!!),
                                title = "গন্তব্য",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                            )
                        }
                        stops.forEachIndexed { index, stop ->
                            Marker(
                                state = MarkerState(position = stop.second),
                                title = "স্টপ ${index + 1}",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                        if (polylinePoints.value.isNotEmpty()) {
                            Polyline(
                                points = polylinePoints.value,
                                color = Color.Blue,
                                width = 10f
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ভাড়ার তালিকা", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        items(fares.entries.toList()) { stopEntry ->
                            stopEntry.value.forEach { (dest, fare) ->
                                Text("${stopEntry.key} থেকে $dest: $fare টাকা")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showFareDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ভাড়া যোগ করুন")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || number.isBlank() || fitnessCertificate.isBlank() || taxToken.isBlank()  ) {  //stops.isBlank()
                                error = "সকল ক্ষেত্র পূরণ করুন"
                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                            } else if (fares.isEmpty()) {
                                error = "কমপক্ষে একটি ভাড়া যোগ করুন"
                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                            } else {
                                isLoading = true
                                error = null
                                scope.launch {
                                    //val stopsList = stops.split(",").map { it.trim() }
                                    val result = AuthRepository().registerBus(
                                        ownerId = user.uid,
                                        name = name,
                                        number = number,
                                        fitnessCertificate = fitnessCertificate,
                                        taxToken = taxToken,
                                        stops = emptyList(),
                                        fares = fares
                                    )
                                    isLoading = false
                                    when {
                                        result.isSuccess -> {
                                            Toast.makeText(context, "বাস রেজিস্ট্রেশন সফল", Toast.LENGTH_SHORT).show()
                                            navController.navigate("owner_dashboard") { popUpTo("register_bus") { inclusive = true } }
                                        }
                                        else -> {
                                            error = result.exceptionOrNull()?.message ?: "রেজিস্ট্রেশন ব্যর্থ"
                                            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("রেজিস্টার করুন")
                        }
                    }
                }
            }
        }
    }

    if (showFareDialog) {
        val stopsList = mutableListOf<String>()
        stopsList.add(originAddress)
        if(stops != null) {
            for (stop in stops) {
                stopsList.add(stop.first)
            }
        }
        stopsList.add(destinationAddress)
        AlertDialog(
            onDismissRequest = { showFareDialog = false },
            title = { Text("ভাড়া যোগ করুন") },
            text = {
                Column {
                    var stopExpanded by remember { mutableStateOf(false) }
                    var destExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = stopExpanded,
                        onExpandedChange = { stopExpanded = !stopExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentStop,
                            onValueChange = { currentStop = it },
                            label = { Text("উৎস স্টপ") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stopExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = stopExpanded,
                            onDismissRequest = { stopExpanded = false }
                        ) {
                            stopsList.forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text(stop) },
                                    onClick = {
                                        currentStop = stop
                                        stopExpanded = false
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = destExpanded,
                        onExpandedChange = { destExpanded = !destExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentDestination,
                            onValueChange = { currentDestination = it },
                            label = { Text("গন্তব্য স্টপ") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = destExpanded,
                            onDismissRequest = { destExpanded = false }
                        ) {
                            stopsList.filter { it != currentStop }.forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text(stop) },
                                    onClick = {
                                        currentDestination = stop
                                        destExpanded = false
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentFare,
                        onValueChange = { currentFare = it },
                        label = { Text("ভাড়া (টাকা)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = currentFare.toIntOrNull()?.let { it < 0 } ?: true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fareValue = currentFare.toIntOrNull()
                        if (currentStop.isBlank() || currentDestination.isBlank() || fareValue == null || fareValue < 0) {
                            scope.launch { snackbarHostState.showSnackbar("বৈধ স্টপ এবং ভাড়া প্রয়োজন") }
                        } else if (currentStop == currentDestination) {
                            scope.launch { snackbarHostState.showSnackbar("উৎস এবং গন্তব্য একই হতে পারে না") }
                        } else {
                            fares = fares.toMutableMap().apply {
                                this[currentStop] =
                                    (this[currentStop] ?: emptyMap()).toMutableMap().apply {
                                        this[currentDestination] = fareValue
                                    }
                            }
                            currentStop = ""
                            currentDestination = ""
                            currentFare = ""
                            showFareDialog = false
                        }
                    }
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFareDialog = false }) { Text("বাতিল") }
            }
        )
    }

    //Location details to store in Firebase
    val encoded_directionapi_points = returned_points_from_directionapi
    val Points = remember { mutableStateListOf<RoutePoint>() }
    Points.add(RoutePoint("Starting Location",originAddress, originLatLng?.latitude ?:0.00 ,originLatLng?.longitude ?: 0.00))
    if(stops != null) {
        for (index in stops) {
            Points.add(RoutePoint("Stoppage",index.first, index.second.latitude, index.second.longitude))
        }
    }
    Points.add(RoutePoint("Destination Location",destinationAddress, destinationLatLng?.latitude ?:0.00 ,destinationLatLng?.longitude ?: 0.00))
}
