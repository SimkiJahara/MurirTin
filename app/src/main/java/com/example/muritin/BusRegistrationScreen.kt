 package com.example.muritin

import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
    var fares by remember { mutableStateOf(mapOf<String, Map<String, Int>>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showFareDialog by remember { mutableStateOf(false) }
    var currentStop by remember { mutableStateOf("") }
    var currentDestination by remember { mutableStateOf("") }
    var currentFare by remember { mutableStateOf("") }

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
    var originGeoHash by remember { mutableStateOf("") }
    var originLatLng by remember { mutableStateOf<LatLng?>(null) }
    var destinationAddress by remember { mutableStateOf("") }
    var destGeoHash by remember { mutableStateOf("") }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) }
    var stopAddress by remember { mutableStateOf("") }
    var stopGeoHash by remember { mutableStateOf("") }
    var stopLatLng by remember { mutableStateOf<LatLng?>(null) }
    var BusRouteObject = remember { BusRoute() }

    val stopsList = remember { mutableStateListOf<Pair<String, LatLng>>() }
    val polylinePoints = remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var returned_points_from_directionapi by remember { mutableStateOf("") }
    val initialPosition = LatLng(23.8103, 90.4125) // Dhaka
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }
    var screenForOrigin by remember { mutableStateOf(true) }
    var screenForDest by remember { mutableStateOf(false) }
    var screenForStops by remember { mutableStateOf(false) }
    var anotherStopButton by remember { mutableStateOf(false)}
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
                        isError = error != null && name.isBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("লাইসেন্স নম্বর") },
                        placeholder = {Text("Gha-1111-22")},
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null && number.isBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fitnessCertificate,
                        onValueChange = { fitnessCertificate = it },
                        label = { Text("ফিটনেস সনদ এর নম্বর") },
                        placeholder = {Text("1223545555")},
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null && fitnessCertificate.isBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taxToken,
                        onValueChange = { taxToken = it },
                        label = { Text("ট্যাক্স টোকেন এর নম্বর") },
                        placeholder = {Text("11-22223333")},
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null && taxToken.isBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "বাসের রুট নির্বাচন করুন",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    //Section for selecting origin location
                    if(screenForOrigin) {
                        SearchLocation(
                            label = "যাত্রা শুরুর অবস্থান লিখে খুজুন বা ম্যাপে মার্ক করুন",
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
                        // On map click, update the clicked location
                        val onMapClick: (LatLng) -> Unit = { latLng ->
                            originLatLng = latLng
                            val latLngStr = String.format("%.4f,%.4f", latLng.latitude, latLng.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                            coroutineScope.launch {
                                try {
                                    // Use Geocoder to get address
                                    val geocoder = Geocoder(context)
                                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) // Get 1 result
                                    originAddress = if (addresses != null && addresses.isNotEmpty()) {
                                        addresses[0].getAddressLine(0) ?: latLngStr // Use first address line or go back to coordinates
                                    } else {
                                        "লোকেশন পাওয়া যায়নি: $latLngStr"
                                    }
                                } catch (e: Exception) {
                                    originAddress = "লোকেশন পাওয়া যায়নি: ${e.message}"
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                            uiSettings = uiSettings,
                            onMapClick = onMapClick
                        ) {
                            if (originLatLng != null) {
                                Marker(
                                    state = MarkerState(position = originLatLng!!),
                                    title = "শুরু",
                                    snippet = originAddress,
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_VIOLET
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (originAddress.isBlank() || originLatLng == null) {
                                    error = "যাত্রা শুরুর অবস্থান নির্বাচন করুন"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else {
                                    val geoLocation = GeoLocation(originLatLng?.latitude ?: 0.00 , originLatLng?.longitude ?: 0.00)
                                    originGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
                                    BusRouteObject.originLoc = PointLocation(originAddress, originLatLng!!.latitude, originLatLng!!.longitude, originGeoHash)
                                    screenForOrigin = false
                                    screenForDest = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Text("গন্তব্য নির্বাচন করুন")
                        }
                    }
                    //Section for selecting destination
                    if(screenForDest) {
                        SearchLocation(
                            label = "গন্তব্যস্থল লিখে খুজুন বা ম্যাপে মার্ক করুন",
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
                        // On map click, update the clicked location
                        val onMapClick: (LatLng) -> Unit = { latLng ->
                            destinationLatLng = latLng
                            val latLngStr = String.format("%.4f,%.4f", latLng.latitude, latLng.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                            coroutineScope.launch {
                                try {
                                    // Use Geocoder to get address
                                    val geocoder = Geocoder(context)
                                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) // Get 1 result
                                    destinationAddress = if (addresses != null && addresses.isNotEmpty()) {
                                        addresses[0].getAddressLine(0) ?: latLngStr
                                    } else {
                                        "লোকেশন পাওয়া যায়নি: $latLngStr"
                                    }
                                } catch (e: Exception) {
                                    destinationAddress = "লোকেশন পাওয়া যায়নি: ${e.message}"
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                            uiSettings = uiSettings,
                            onMapClick = onMapClick
                        ) {
                            Marker(
                                state = MarkerState(position = originLatLng!!),
                                title = "শুরু",
                                snippet = originAddress,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_VIOLET
                                )
                            )
                            if (destinationLatLng != null) {
                                Marker(
                                    state = MarkerState(position = destinationLatLng!!),
                                    title = "গন্তব্য",
                                    snippet = destinationAddress,
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_VIOLET
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (destinationAddress.isBlank() || destinationLatLng == null) {
                                    error = "গন্তব্য নির্বাচন করুন"
                                    scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                                } else {
                                    val geoLocation = GeoLocation(destinationLatLng?.latitude ?: 0.00 , destinationLatLng?.longitude ?: 0.00)
                                    destGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
                                    BusRouteObject.destinationLoc = PointLocation (destinationAddress, destinationLatLng!!.latitude, destinationLatLng!!.longitude, destGeoHash)
                                    screenForOrigin = false
                                    screenForDest = false
                                    screenForStops = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Text("স্টপেজ নির্বাচন করুন")
                        }
                    }
                    //Section for selecting stops
                    if(screenForStops) {
                        SearchLocation(
                            label = "স্টপেজ লিখে খুজুন বা ম্যাপে মার্ক করুন",
                            apiKey = apiKey,
                            placesClient = placesClient,
                            geocodingApi = geocodingApi,
                            coroutineScope = coroutineScope,
                            context = context,
                            onLocationSelected = { addr, latlng ->
                                stopAddress = addr         //stops.add(Pair(addr, latlng))
                                stopLatLng = latlng
                            },
                            cameraPositionState = cameraPositionState
                        )
                        // On map click, update the clicked location
                        val onMapClick: (LatLng) -> Unit = { latLng ->
                            stopLatLng = latLng
                            val latLngStr = String.format("%.4f,%.4f", latLng.latitude, latLng.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                            coroutineScope.launch {
                                try {
                                    // Use Geocoder to get address
                                    val geocoder = Geocoder(context)
                                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) // Get 1 result
                                    stopAddress = if (addresses != null && addresses.isNotEmpty()) {
                                        addresses[0].getAddressLine(0) ?: latLngStr
                                    } else {
                                        "লোকেশন পাওয়া যায়নি: $latLngStr"
                                    }
                                } catch (e: Exception) {
                                    stopAddress = "লোকেশন পাওয়া যায়নি: ${e.message}"
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                            uiSettings = uiSettings,
                            onMapClick = onMapClick
                        ) {
                            Marker(
                                state = MarkerState(position = originLatLng!!),
                                title = "শুরু",
                                snippet = originAddress,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_VIOLET
                                )
                            )
                            Marker(
                                state = MarkerState(position = destinationLatLng!!),
                                title = "গন্তব্য",
                                snippet = destinationAddress,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_VIOLET
                                )
                            )
                            //Current stop marker
                            if(stopLatLng != null) {
                                Marker(
                                    state = MarkerState(position = stopLatLng!!),
                                    title = "গন্তব্য",
                                    snippet = destinationAddress,
                                    icon = BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED
                                    )
                                )
                            }
                            //Previous stop markers
                            if(stopsList != null) {
                                stopsList.forEachIndexed { index, stop ->
                                    Marker(
                                        state = MarkerState(position = stop.second),
                                        title = "স্টপ ${index + 1}",
                                        icon = BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_RED
                                        )
                                    )
                                }
                            }
                            if (polylinePoints.value.isNotEmpty()) {
                                Polyline(
                                    points = polylinePoints.value,
                                    color = Color.Blue,
                                    width = 10f
                                )
                            }
                        }
                        if(stopLatLng != null) {
                            anotherStopButton = true}
                        if(anotherStopButton){
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val geoLocation = GeoLocation(stopLatLng?.latitude ?: 0.00 , stopLatLng?.longitude ?: 0.00)
                                    stopGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
                                    BusRouteObject.stopPointsLoc.add(PointLocation(stopAddress, stopLatLng!!.latitude, stopLatLng!!.longitude, stopGeoHash))
                                    stopsList.add(Pair(stopAddress, stopLatLng) as Pair<String, LatLng>)
                                    stopAddress = ""
                                    stopLatLng = null
                                    screenForDest = false
                                    screenForStops = false
                                    screenForOrigin = false
                                    screenForStops = true //Show the screen again
                                },
                                modifier = Modifier
                                    //.weight(1f)
                                    .padding(start = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("আরও একটি স্টপেজ যোগ করুন")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Button(
                                onClick = {
                                    anotherStopButton = false
                                    if (originLatLng != null && destinationLatLng != null) {
                                        if(stopLatLng!=null){
                                            //Adding the current selected stop location
                                            val geoLocation = GeoLocation(stopLatLng?.latitude ?: 0.00 , stopLatLng?.longitude ?: 0.00)
                                            stopGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
                                            BusRouteObject.stopPointsLoc.add(PointLocation(stopAddress, stopLatLng!!.latitude, stopLatLng!!.longitude, stopGeoHash))
                                        }
                                        coroutineScope.launch {
                                            val originStr = "${originLatLng!!.latitude},${originLatLng!!.longitude}"
                                            val destStr = "${destinationLatLng!!.latitude},${destinationLatLng!!.longitude}"
                                            if(stopLatLng != null){  //The last stop needs to be added
                                                stopsList.add(Pair(stopAddress, stopLatLng) as Pair<String, LatLng>)
                                            }
                                            val waypoints = if (stopsList.isNotEmpty()) {
                                                stopsList.joinToString("|") { "${it.second.latitude},${it.second.longitude}" }
                                            } else if (stopLatLng != null){   //when only selecting one stop
                                                "${stopLatLng!!.latitude},${stopLatLng!!.longitude}"
                                            } else { null }
                                            try {
                                                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    directionsApi.getRoute(
                                                        origin = originStr,
                                                        destination = destStr,
                                                        waypoints = waypoints,
                                                        apiKey = apiKey
                                                    )
                                                }
                                                if (response.status == "OK" && response.routes.isNotEmpty()) {
                                                    returned_points_from_directionapi = response.routes[0].overview_polyline.points
                                                    val decoded = decodePolyline(returned_points_from_directionapi)
                                                    polylinePoints.value = decoded
                                                    if (decoded.isNotEmpty()) {
                                                        val boundsBuilder = LatLngBounds.Builder()
                                                        decoded.forEach { point -> boundsBuilder.include(point) }
                                                        val bounds = boundsBuilder.build()
                                                        cameraPositionState.animate(
                                                            CameraUpdateFactory.newLatLngBounds(bounds, 100)
                                                        )
                                                    }
                                                } else {
                                                    Toast.makeText(context, "রুট পাওয়া যায়নি", Toast.LENGTH_LONG).show()
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
                                    originAddress = ""
                                    originLatLng = null
                                    destinationAddress = ""
                                    destinationLatLng = null
                                    stopAddress = ""
                                    stopLatLng = null
                                    stopsList.clear()
                                    BusRouteObject.clear()
                                    polylinePoints.value = emptyList()
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.fromLatLngZoom(initialPosition, 10f)
                                            )
                                        )
                                    }
                                    screenForDest = false
                                    screenForStops = false
                                    screenForOrigin = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB71C1C),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("রুট বাতিল করুন")
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "রুট:", style = MaterialTheme.typography.labelLarge)
                            if (originAddress.isNotEmpty()) {
                                Text(text = "যাত্রা শুরু: $originAddress", style = MaterialTheme.typography.bodySmall)
                            }
                            if (stopsList.isNotEmpty()) {
                                stopsList.forEachIndexed { index, stop ->
                                    Text(text = "স্টপ: ${stop.first}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (stopAddress.isNotEmpty()) {  //Currently selected stop, that is not in the list yet
                                Text(text = "স্টপ: $stopAddress", style = MaterialTheme.typography.bodySmall)
                            }
                            if (destinationAddress.isNotEmpty()) {
                                Text(text = "গন্তব্য: $destinationAddress", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "ভাড়ার তালিকা", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 150.dp)
                    ) {
                        items(fares.entries.toList()) { stopEntry: Map.Entry<String, Map<String, Int>> ->
                            stopEntry.value.forEach { (dest, fare) ->
                                Text(text = "${stopEntry.key} থেকে $dest: $fare টাকা")
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
                            if (name.isBlank() || number.isBlank() || fitnessCertificate.isBlank() || taxToken.isBlank()) {
                                error = "সকল ক্ষেত্র পূরণ করুন"
                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                            } else if (BusRouteObject.originLoc == null || BusRouteObject.destinationLoc == null){
                                error = "যাত্রা শুরুর স্থান এবং গন্তব্য যোগ করুন"
                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                            }
                            else if (fares.isEmpty()) {
                                error = "কমপক্ষে একটি ভাড়া যোগ করুন"
                                scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
                            } else {
                                isLoading = true
                                error = null
                                scope.launch {
                                    Log.d("BusRegistrationScreen", "Registering bus for user: ${user.uid}")
                                    try {
                                        AuthRepository().ensureOwnerRole(user.uid)
                                        val stopsNameList = mutableListOf<String>()
                                        if (originAddress.isNotEmpty()) stopsNameList.add(originAddress)
                                        stopsNameList.addAll(stopsList.map { it.first })
                                        if (destinationAddress.isNotEmpty()) stopsNameList.add(destinationAddress)
                                        Log.d("BusRegistrationScreen", "Stops: $stopsList, Fares: $fares")
                                        Log.d("BusRegistrationScreen", "Route: $BusRouteObject, originLoc: ${BusRouteObject.originLoc}, destinationLoc: ${BusRouteObject.destinationLoc}, stopPointsLoc: ${BusRouteObject.stopPointsLoc}")
                                        val result = AuthRepository().registerBus(
                                            ownerId = user.uid,
                                            name = name,
                                            number = number,
                                            fitnessCertificate = fitnessCertificate,
                                            taxToken = taxToken,
                                            stops = stopsNameList,
                                            route = BusRouteObject,
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
                                    } catch (e: Exception) {
                                        isLoading = false
                                        error = "Role verification failed: ${e.message}"
                                        scope.launch { snackbarHostState.showSnackbar(error ?: "Unknown error") }
                                        Log.e("BusRegistrationScreen", "Registration error: ${e.message}", e)
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
                } // End of Card Column
            } // End of Card
        } // End of Scaffold Column
    } // End of Scaffold

    if (showFareDialog) {
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
                            val stopsNameList = mutableListOf<String>()
                            if (originAddress.isNotEmpty()) stopsNameList.add(originAddress)
                            stopsNameList.addAll(stopsList.map { it.first })
                            if (destinationAddress.isNotEmpty()) stopsNameList.add(destinationAddress)
                            stopsNameList.forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text(stop) },
                                    onClick = {
                                        currentStop = stop.replace(Regex("[^A-Za-z0-9 ]"), "")
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
                            val stopsNameList = mutableListOf<String>()
                            if (originAddress.isNotEmpty()) stopsNameList.add(originAddress)
                            stopsNameList.addAll(stopsList.map { it.first })
                            if (destinationAddress.isNotEmpty()) stopsNameList.add(destinationAddress)
                            stopsNameList.filter { it != currentStop }.forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text(stop) },
                                    onClick = {
                                        currentDestination = stop.replace(Regex("[^A-Za-z0-9 ]"), "")
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
                                this[currentStop] = (this[currentStop] ?: emptyMap()).toMutableMap().apply {
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
                TextButton(onClick = { showFareDialog = false }) {
                    Text("বাতিল")
                }
            }
        ) // End of AlertDialog
    } // End of if (showFareDialog)
} // End of BusRegistrationScreen

