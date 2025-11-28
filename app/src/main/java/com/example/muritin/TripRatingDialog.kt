package com.example.muritin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun TripRatingDialog(
    request: Request,
    bus: Bus?,
    conductor: User?,
    onDismiss: () -> Unit,
    onRatingSubmitted: () -> Unit
) {
    var conductorRating by remember { mutableFloatStateOf(0f) }
    var busRating by remember { mutableFloatStateOf(0f) }
    var overallRating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("যাত্রার মূল্যায়ন করুন") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trip details
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "যাত্রা: ${request.pickup} → ${request.destination}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (bus != null) {
                            Text("বাস: ${bus.name} (${bus.number})")
                        }
                        if (conductor != null) {
                            Text("কন্ডাক্টর: ${conductor.name}")
                        }
                    }
                }

                // Conductor Rating - ALWAYS SHOW
                Column {
                    Text(
                        "কন্ডাক্টরকে মূল্যায়ন করুন",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (conductor != null) {
                        Text(
                            conductor.name ?: "কন্ডাক্টর",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    StarRatingBar(
                        rating = conductorRating,
                        onRatingChanged = { conductorRating = it }
                    )
                }

                // Bus Rating - ALWAYS SHOW
                Column {
                    Text(
                        "বাসকে মূল্যায়ন করুন",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (bus != null) {
                        Text(
                            "${bus.name} (${bus.number})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    StarRatingBar(
                        rating = busRating,
                        onRatingChanged = { busRating = it }
                    )
                }

                // Overall Rating - ALWAYS SHOW
                Column {
                    Text(
                        "সামগ্রিক অভিজ্ঞতা",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "আপনার সম্পূর্ণ যাত্রার অভিজ্ঞতা",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    StarRatingBar(
                        rating = overallRating,
                        onRatingChanged = { overallRating = it }
                    )
                }

                // Comment
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("মন্তব্য (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("আপনার অভিজ্ঞতা শেয়ার করুন...") }
                )

                // Error message
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        // Validate ratings
                        if (conductorRating == 0f || busRating == 0f || overallRating == 0f) {
                            error = "অনুগ্রহ করে সকল রেটিং প্রদান করুন"
                            return@launch
                        }

                        isSubmitting = true
                        error = null

                        val result = AuthRepository().submitTripRating(
                            requestId = request.id,
                            riderId = request.riderId,
                            conductorRating = conductorRating,
                            busRating = busRating,
                            overallRating = overallRating,
                            comment = comment.trim()
                        )

                        isSubmitting = false

                        if (result.isSuccess) {
                            onRatingSubmitted()
                        } else {
                            error = result.exceptionOrNull()?.message ?: "রেটিং জমা দিতে ব্যর্থ"
                        }
                    }
                },
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("জমা দিন")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun StarRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    maxStars: Int = 5
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            IconButton(
                onClick = { onRatingChanged(i.toFloat()) }
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Star $i",
                    tint = if (i <= rating) Color(0xFFFFD700) else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun RatingDisplay(rating: Float, totalRatings: Int = 0) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.bodyMedium
        )
        if (totalRatings > 0) {
            Text(
                text = "($totalRatings)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}