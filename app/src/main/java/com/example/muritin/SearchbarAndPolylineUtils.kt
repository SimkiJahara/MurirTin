package com.example.muritin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.muritin.ui.theme.BackgroundLight
import com.example.muritin.ui.theme.Border
import com.example.muritin.ui.theme.Primary
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SearchLocation(
    label: String,
    apiKey: String,
    placesClient: com.google.android.libraries.places.api.net.PlacesClient,
    geocodingApi: GeocodingApi,
    coroutineScope: CoroutineScope,
    context: Context,
    onLocationSelected: (String, LatLng) -> Unit,
    cameraPositionState: CameraPositionState
) {
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { newQuery ->
            searchQuery = newQuery
            if (newQuery.length > 2) {
                coroutineScope.launch {
                    val request = com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
                        .builder()
                        .setQuery(newQuery)
                        .setCountries("BD")
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response ->
                            suggestions = response.autocompletePredictions.map { prediction ->
                                prediction.getFullText(null).toString()
                            }
                        }
                        .addOnFailureListener { suggestions = emptyList() }
                }
            } else {
                suggestions = emptyList()
            }
        },
        label = { Text(text = label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = Border,
            focusedLabelColor = Primary
        ),
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            }
        }
    )

    if (suggestions.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .heightIn(max = 180.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(suggestions) { suggestion ->
                    // Rounded row “option” look
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                                            suggestions = emptyList()
                                        } else {
                                            Toast.makeText(context, "লোকেশন পাওয়া যায়নি", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSearching = false
                                    }
                                }
                            },
                        color = Color(0xFFF7F7F7), // light grey for better contrast inside card
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

fun decodePolyline(encoded: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0

    while (index < encoded.length) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
            index++
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) -(result shr 1) else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
            index++
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) -(result shr 1) else result shr 1
        lng += dlng

        poly.add(LatLng(lat / 1e5, lng / 1e5))
    }
    return poly
}
