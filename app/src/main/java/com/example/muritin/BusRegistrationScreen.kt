package com.example.muritin

import android.location.Geocoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.storage.FirebaseStorage
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
    var licenseDocumentUri by remember { mutableStateOf<Uri?>(null) }
    var licenseDocumentName by remember { mutableStateOf("কোনো ফাইল নির্বাচিত হয়নি") }
    var isUploadingLicense by remember { mutableStateOf(false) }
    var licenseDownloadUrl by remember { mutableStateOf<String?>(null) }

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
    val initialPosition = LatLng(23.8103, 90.4125)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 10f)
    }
    var screenForOrigin by remember { mutableStateOf(true) }
    var screenForDest by remember { mutableStateOf(false) }
    var screenForStops by remember { mutableStateOf(false) }
    var anotherStopButton by remember { mutableStateOf(false) }
    val mapProperties = MapProperties(isTrafficEnabled = true)
    val uiSettings = MapUiSettings(zoomGesturesEnabled = true, zoomControlsEnabled = false)
    val scrollState = rememberScrollState()

    // File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            licenseDocumentUri = it
            licenseDocumentName = it.lastPathSegment?.split("/")?.last() ?: "নথি নির্বাচিত"
        }
    }

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
                        label = { Text("বাস নম্বর") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null && number.isBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fitnessCertificate,
                        onValueChange = { fitnessCertificate = it },
                        label = { Text("ফিটনেস সার্টিফিকেট") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null && fitnessCertificate.isBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taxToken,
                        onValueChange = { taxToken = it },
                        label = { Text("ট্যাক্স টোকেন") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null && taxToken.isBlank()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // NEW: Bus License Upload
                    Text(
                        text = "বাস লাইসেন্স ডকুমেন্ট (PDF/ছবি)",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            enabled = !isUploadingLicense
                        ) {
                            if (isUploadingLicense) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("ফাইল নির্বাচন")
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = licenseDocumentName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (licenseDocumentUri != null) Color.Green else Color.Gray,
                            modifier = Modifier.weight(2f)
                        )
                    }

                    if (licenseDocumentUri != null && licenseDownloadUrl == null) {
                        Button(
                            onClick = {
                                isUploadingLicense = true
                                scope.launch {
                                    try {
                                        val mimeType = context.contentResolver.getType(licenseDocumentUri!!)
                                        val fileExt = when {
                                            mimeType?.contains("pdf") == true -> ".pdf"
                                            mimeType?.contains("image") == true -> ".jpg"
                                            else -> ".pdf"
                                        }
                                        val fileName = "bus_license_${number}_${System.currentTimeMillis()}$fileExt"
                                        val ref = FirebaseStorage.getInstance().reference
                                            .child("bus_licenses/$fileName")
                                        ref.putFile(licenseDocumentUri!!).await()
                                        val url = ref.downloadUrl.await().toString()
                                        licenseDownloadUrl = url
                                        Toast.makeText(context, "লাইসেন্স আপলোড সফল", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("আপলোড ব্যর্থ: ${e.message}")
                                        }
                                    } finally {
                                        isUploadingLicense = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploadingLicense
                        ) {
                            if (isUploadingLicense) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("আপলোড করুন")
                            }
                        }
                    }

                    if (licenseDownloadUrl != null) {
                        Text("আপলোড সফল", color = Color.Green)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Route Selection
                    Text("বাসের রুট নির্বাচন করুন", style = MaterialTheme.typography.headlineSmall)

                    if (screenForOrigin) {
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
                        val onMapClick: (LatLng) -> Unit = { latLng ->
                            originLatLng = latLng
                            val latLngStr = String.format("%.4f,%.4f", latLng.latitude, latLng.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                            coroutineScope.launch {
                                try {
                                    val geocoder = Geocoder(context)
                                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                    originAddress = addresses?.get(0)?.getAddressLine(0) ?: latLngStr
                                } catch (e: Exception) {
                                    originAddress = "লোকেশন পাওয়া যায়নি"
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GoogleMap(
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                            uiSettings = uiSettings,
                            onMapClick = onMapClick
                        ) {
                            originLatLng?.let {
                                Marker(state = MarkerState(it), title = "শুরু", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (originAddress.isBlank() || originLatLng == null) {
                                    error = "শুরুর অবস্থান নির্বাচন করুন"
                                } else {
                                    val geoLocation = GeoLocation(originLatLng!!.latitude, originLatLng!!.longitude)
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

                    if (screenForDest) {
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
                        val onMapClick: (LatLng) -> Unit = { latLng ->
                            destinationLatLng = latLng
                            val latLngStr = String.format("%.4f,%.4f", latLng.latitude, latLng.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                            coroutineScope.launch {
                                try {
                                    val geocoder = Geocoder(context)
                                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                    destinationAddress = addresses?.get(0)?.getAddressLine(0) ?: latLngStr
                                } catch (e: Exception) {
                                    destinationAddress = "লোকেশন পাওয়া যায়নি"
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GoogleMap(
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                            uiSettings = uiSettings,
                            onMapClick = onMapClick
                        ) {
                            Marker(state = MarkerState(originLatLng!!), title = "শুরু", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                            destinationLatLng?.let {
                                Marker(state = MarkerState(it), title = "গন্তব্য", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (destinationAddress.isBlank() || destinationLatLng == null) {
                                    error = "গন্তব্য নির্বাচন করুন"
                                } else {
                                    val geoLocation = GeoLocation(destinationLatLng!!.latitude, destinationLatLng!!.longitude)
                                    destGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
                                    BusRouteObject.destinationLoc = PointLocation(destinationAddress, destinationLatLng!!.latitude, destinationLatLng!!.longitude, destGeoHash)
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

                    if (screenForStops) {
                        SearchLocation(
                            label = "স্টপেজ লিখে খুজুন বা ম্যাপে মার্ক করুন",
                            apiKey = apiKey,
                            placesClient = placesClient,
                            geocodingApi = geocodingApi,
                            coroutineScope = coroutineScope,
                            context = context,
                            onLocationSelected = { addr, latlng ->
                                stopAddress = addr
                                stopLatLng = latlng
                            },
                            cameraPositionState = cameraPositionState
                        )
                        val onMapClick: (LatLng) -> Unit = { latLng ->
                            stopLatLng = latLng
                            val latLngStr = String.format("%.4f,%.4f", latLng.latitude, latLng.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, cameraPositionState.position.zoom)
                            coroutineScope.launch {
                                try {
                                    val geocoder = Geocoder(context)
                                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                    stopAddress = addresses?.get(0)?.getAddressLine(0) ?: latLngStr
                                } catch (e: Exception) {
                                    stopAddress = "লোকেশন পাওয়া যায়নি"
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        GoogleMap(
                            modifier = Modifier.fillMaxWidth().height(400.dp),
                            cameraPositionState = cameraPositionState,
                            properties = mapProperties,
                            uiSettings = uiSettings,
                            onMapClick = onMapClick
                        ) {
                            Marker(state = MarkerState(originLatLng!!), title = "শুরু")
                            Marker(state = MarkerState(destinationLatLng!!), title = "গন্তব্য")
                            stopLatLng?.let {
                                Marker(state = MarkerState(it), title = "স্টপ", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            }
                            stopsList.forEachIndexed { i, stop ->
                                Marker(state = MarkerState(stop.second), title = "স্টপ ${i+1}", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            }
                            if (polylinePoints.value.isNotEmpty()) {
                                Polyline(points = polylinePoints.value, color = Color.Blue, width = 10f)
                            }
                        }

                        if (stopLatLng != null) anotherStopButton = true
                        if (anotherStopButton) {
                            OutlinedButton(
                                onClick = {
                                    val geoLocation = GeoLocation(stopLatLng!!.latitude, stopLatLng!!.longitude)
                                    stopGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
                                    BusRouteObject.stopPointsLoc.add(PointLocation(stopAddress, stopLatLng!!.latitude, stopLatLng!!.longitude, stopGeoHash))
                                    stopsList.add(Pair(stopAddress, stopLatLng!!))
                                    stopAddress = ""
                                    stopLatLng = null
                                    anotherStopButton = false
                                },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text("আরও স্টপ যোগ করুন")
                            }
                        }

                        Row {
                            Button(
                                onClick = {
                                    if (originLatLng != null && destinationLatLng != null) {
                                        if (stopLatLng != null) {
                                            val geoLocation = GeoLocation(stopLatLng!!.latitude, stopLatLng!!.longitude)
                                            stopGeoHash = GeoFireUtils.getGeoHashForLocation(geoLocation, 5)
                                            BusRouteObject.stopPointsLoc.add(PointLocation(stopAddress, stopLatLng!!.latitude, stopLatLng!!.longitude, stopGeoHash))
                                            stopsList.add(Pair(stopAddress, stopLatLng!!))
                                        }
                                        coroutineScope.launch {
                                            val originStr = "${originLatLng!!.latitude},${originLatLng!!.longitude}"
                                            val destStr = "${destinationLatLng!!.latitude},${destinationLatLng!!.longitude}"
                                            val waypoints = stopsList.joinToString("|") { "${it.second.latitude},${it.second.longitude}" }
                                            try {
                                                val response = withContext(Dispatchers.IO) {
                                                    directionsApi.getRoute(originStr, destStr, waypoints, apiKey)
                                                }
                                                if (response.status == "OK" && response.routes.isNotEmpty()) {
                                                    returned_points_from_directionapi = response.routes[0].overview_polyline.points
                                                    val decoded = decodePolyline(returned_points_from_directionapi)
                                                    polylinePoints.value = decoded
                                                    if (decoded.isNotEmpty()) {
                                                        val bounds = LatLngBounds.builder().apply { decoded.forEach { include(it) } }.build()
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "রুট পাওয়া যায়নি", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Text("রুট যোগ করুন")
                            }
                            Button(
                                onClick = {
                                    originAddress = ""; originLatLng = null
                                    destinationAddress = ""; destinationLatLng = null
                                    stopAddress = ""; stopLatLng = null
                                    stopsList.clear(); BusRouteObject.clear(); polylinePoints.value = emptyList()
                                    screenForOrigin = true; screenForDest = false; screenForStops = false
                                },
                                modifier = Modifier.weight(1f).padding(start = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                            ) {
                                Text("রুট বাতিল")
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text("রুট:", style = MaterialTheme.typography.labelLarge)
                            if (originAddress.isNotEmpty()) Text("শুরু: $originAddress")
                            stopsList.forEach { Text("স্টপ: ${it.first}") }
                            if (stopAddress.isNotEmpty()) Text("স্টপ: $stopAddress")
                            if (destinationAddress.isNotEmpty()) Text("গন্তব্য: $destinationAddress")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("ভাড়ার তালিকা", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp)) {
                        items(fares.entries.toList()) { entry ->
                            entry.value.forEach { (dest, fare) ->
                                Text("${entry.key} থেকে $dest: $fare টাকা")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showFareDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("ভাড়া যোগ করুন")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || number.isBlank() || fitnessCertificate.isBlank() || taxToken.isBlank()) {
                                error = "সকল ক্ষেত্র পূরণ করুন"
                            } else if (BusRouteObject.originLoc == null || BusRouteObject.destinationLoc == null) {
                                error = "রুট নির্বাচন করুন"
                            } else if (fares.isEmpty()) {
                                error = "ভাড়া যোগ করুন"
                            } else if (licenseDownloadUrl == null) {
                                error = "লাইসেন্স ডকুমেন্ট আপলোড করুন"
                            } else {
                                isLoading = true
                                scope.launch {
                                    try {
                                        AuthRepository().ensureOwnerRole(user.uid)
                                        val stopsNameList = mutableListOf<String>().apply {
                                            if (originAddress.isNotEmpty()) add(originAddress)
                                            addAll(stopsList.map { it.first })
                                            if (destinationAddress.isNotEmpty()) add(destinationAddress)
                                        }
                                        val result = AuthRepository().registerBus(
                                            ownerId = user.uid,
                                            name = name,
                                            number = number,
                                            fitnessCertificate = fitnessCertificate,
                                            taxToken = taxToken,
                                            stops = stopsNameList,
                                            route = BusRouteObject,
                                            fares = fares,
                                            licenseDocumentUrl = licenseDownloadUrl!!
                                        )
                                        isLoading = false
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "রেজিস্ট্রেশন সফল", Toast.LENGTH_SHORT).show()
                                            navController.navigate("owner_dashboard") { popUpTo("register_bus") { inclusive = true } }
                                        } else {
                                            error = result.exceptionOrNull()?.message
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        error = e.message
                                    }
                                    error?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator() else Text("রেজিস্টার করুন")
                    }
                }
            }
        }
    }

    if (showFareDialog) {
        AlertDialog(
            onDismissRequest = { showFareDialog = false },
            title = { Text("ভাড়া যোগ করুন") },
            text = {
                Column {
                    var stopExpanded by remember { mutableStateOf(false) }
                    var destExpanded by remember { mutableStateOf(false) }
                    val allStops = mutableListOf<String>().apply {
                        if (originAddress.isNotEmpty()) add(originAddress)
                        addAll(stopsList.map { it.first })
                        if (destinationAddress.isNotEmpty()) add(destinationAddress)
                    }

                    ExposedDropdownMenuBox(expanded = stopExpanded, onExpandedChange = { stopExpanded = !stopExpanded }) {
                        OutlinedTextField(
                            value = currentStop,
                            onValueChange = { currentStop = it },
                            label = { Text("উৎস") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stopExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = stopExpanded, onDismissRequest = { stopExpanded = false }) {
                            allStops.forEach { stop ->
                                DropdownMenuItem(text = { Text(stop) }, onClick = { currentStop = stop; stopExpanded = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(expanded = destExpanded, onExpandedChange = { destExpanded = !destExpanded }) {
                        OutlinedTextField(
                            value = currentDestination,
                            onValueChange = { currentDestination = it },
                            label = { Text("গন্তব্য") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = destExpanded, onDismissRequest = { destExpanded = false }) {
                            allStops.filter { it != currentStop }.forEach { stop ->
                                DropdownMenuItem(text = { Text(stop) }, onClick = { currentDestination = stop; destExpanded = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = currentFare,
                        onValueChange = { currentFare = it },
                        label = { Text("ভাড়া (টাকা)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val fare = currentFare.toIntOrNull()
                    if (currentStop.isBlank() || currentDestination.isBlank() || fare == null || fare <= 0) {
                        scope.launch { snackbarHostState.showSnackbar("সঠিক তথ্য দিন") }
                    } else {
                        fares = fares.toMutableMap().apply {
                            this[currentStop] = (this[currentStop] ?: emptyMap()).toMutableMap().apply { this[currentDestination] = fare }
                        }
                        currentStop = ""; currentDestination = ""; currentFare = ""; showFareDialog = false
                    }
                }) { Text("যোগ করুন") }
            },
            dismissButton = { TextButton(onClick = { showFareDialog = false }) { Text("বাতিল") } }
        )
    }
}

