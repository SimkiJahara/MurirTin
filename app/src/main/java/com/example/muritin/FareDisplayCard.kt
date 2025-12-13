package com.example.muritin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muritin.ui.theme.*

/**
 * Real-time fare display card that shows:
 * - Original fare
 * - Current calculated fare (if route changed)
 * - Fare breakdown (pickup → destination)
 * - Visual indicator if fare changed
 */
@Composable
fun FareDisplayCard(
    request: Request,
    modifier: Modifier = Modifier
) {
    val originalFare = request.fare
    val actualFare = request.rideStatus?.actualFare ?: 0
    val currentFare = if (actualFare > 0) actualFare else originalFare
    val fareChanged = actualFare > 0 && actualFare != originalFare

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (fareChanged) RouteOrange.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Payment,
                        contentDescription = null,
                        tint = if (fareChanged) RouteOrange else Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (fareChanged) "আপডেট করা ভাড়া" else "ভাড়া",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (fareChanged) RouteOrange else Primary
                    )
                }

                if (fareChanged) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RouteOrange
                    ) {
                        Text(
                            text = "পরিবর্তিত",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Fare (large display)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "৳",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (fareChanged) RouteOrange else RouteGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentFare.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (fareChanged) RouteOrange else RouteGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            // Route info
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.pickup,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1
                )

                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                )

                Text(
                    text = request.destination,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
            }

            // Seats info
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.EventSeat,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${request.seats} আসন",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Original fare (if changed)
            if (fareChanged) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Border.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মূল ভাড়া:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Text(
                        text = "৳$originalFare",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (actualFare < originalFare) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (actualFare < originalFare) RouteGreen else Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (actualFare < originalFare) "কম" else "বেশি",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (actualFare < originalFare) RouteGreen else Error
                        )
                    }

                    Text(
                        text = "৳${Math.abs(actualFare - originalFare)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (actualFare < originalFare) RouteGreen else Error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Exit reason (if applicable)
            if (request.rideStatus?.earlyExitRequested == true) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RouteOrange.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = RouteOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "আগাম নামা",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = RouteOrange
                            )
                            Text(
                                text = request.rideStatus.earlyExitStop ?: "নতুন স্থান",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (request.rideStatus?.lateExitRequested == true) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RouteBlue.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = RouteBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "পরে নামা",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = RouteBlue
                            )
                            Text(
                                text = request.rideStatus.lateExitStop ?: "নতুন স্থান",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact version for list items
 */
@Composable
fun CompactFareDisplay(
    request: Request,
    modifier: Modifier = Modifier
) {
    val originalFare = request.fare
    val actualFare = request.rideStatus?.actualFare ?: 0
    val currentFare = if (actualFare > 0) actualFare else originalFare
    val fareChanged = actualFare > 0 && actualFare != originalFare

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (fareChanged) RouteOrange.copy(alpha = 0.1f) else Primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Payment,
                    contentDescription = null,
                    tint = if (fareChanged) RouteOrange else Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ভাড়া",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "৳$currentFare",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (fareChanged) RouteOrange else Primary
                        )
                        if (fareChanged) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(৳$originalFare)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (fareChanged) {
                Icon(
                    if (actualFare < originalFare) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = if (actualFare < originalFare) RouteGreen else Error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}