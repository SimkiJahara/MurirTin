package com.example.muritin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muritin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        containerColor = BackgroundLight,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Primary, PrimaryLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "যাত্রার মূল্যায়ন করুন",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "আপনার অভিজ্ঞতা শেয়ার করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Trip Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsBus,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "যাত্রার বিবরণ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Divider(color = Divider, thickness = 1.dp)

                        TripDetailRow(
                            label = "রুট",
                            value = "${request.pickup} → ${request.destination}"
                        )

                        if (bus != null) {
                            TripDetailRow(
                                label = "বাস",
                                value = "${bus.name} (${bus.number})"
                            )
                        }

                        if (conductor != null) {
                            TripDetailRow(
                                label = "কন্ডাক্টর",
                                value = conductor.name ?: "কন্ডাক্টর"
                            )
                        }
                    }
                }

                // Conductor Rating Section
                RatingSection(
                    title = "কন্ডাক্টরকে মূল্যায়ন করুন",
                    subtitle = conductor?.name ?: "কন্ডাক্টর",
                    icon = Icons.Rounded.Person,
                    rating = conductorRating,
                    onRatingChanged = { conductorRating = it },
                    iconColor = Primary
                )

                // Bus Rating Section
                RatingSection(
                    title = "বাসকে মূল্যায়ন করুন",
                    subtitle = bus?.let { "${it.name} (${it.number})" } ?: "বাস",
                    icon = Icons.Rounded.DirectionsBus,
                    rating = busRating,
                    onRatingChanged = { busRating = it },
                    iconColor = Secondary
                )

                // Overall Rating Section
                RatingSection(
                    title = "সামগ্রিক অভিজ্ঞতা",
                    subtitle = "আপনার সম্পূর্ণ যাত্রার অভিজ্ঞতা",
                    icon = Icons.Rounded.TrendingUp,
                    rating = overallRating,
                    onRatingChanged = { overallRating = it },
                    iconColor = Success
                )

                // Comment Section
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = {
                        Text(
                            "মন্তব্য (ঐচ্ছিক)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border,
                        focusedLabelColor = Primary,
                        cursorColor = Primary
                    ),
                    placeholder = {
                        Text(
                            "আপনার অভিজ্ঞতা শেয়ার করুন...",
                            color = TextTertiary
                        )
                    }
                )

                // Error message
                error?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Error.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = it,
                            color = Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
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
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.6f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    "জমা দিন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "বাতিল",
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
        }
    )
}

@Composable
private fun TripDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun RatingSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Star Rating
            EnhancedStarRatingBar(
                rating = rating,
                onRatingChanged = onRatingChanged
            )

            // Rating Label
            if (rating > 0) {
                Text(
                    text = getRatingLabel(rating),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = getRatingColor(rating)
                )
            }
        }
    }
}

@Composable
private fun EnhancedStarRatingBar(
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
            val isSelected = i <= rating
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "star_scale"
            )

            IconButton(
                onClick = { onRatingChanged(i.toFloat()) },
                modifier = Modifier.scale(scale)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Star $i",
                    tint = if (isSelected) Color(0xFFFFD700) else TextTertiary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

private fun getRatingLabel(rating: Float): String {
    return when {
        rating >= 4.5f -> "অসাধারণ!"
        rating >= 3.5f -> "খুব ভালো"
        rating >= 2.5f -> "ভালো"
        rating >= 1.5f -> "মোটামুটি"
        else -> "উন্নতি প্রয়োজন"
    }
}

private fun getRatingColor(rating: Float): Color {
    return when {
        rating >= 4.5f -> Success
        rating >= 3.5f -> Info
        rating >= 2.5f -> Warning
        else -> Error
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (totalRatings > 0) {
            Text(
                text = "($totalRatings)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}