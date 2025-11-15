package com.example.muritin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorRatingsScreen(
    navController: NavHostController,
    conductorId: String
) {
    val scope = rememberCoroutineScope()
    var conductorRatings by remember { mutableStateOf<ConductorRatings?>(null) }
    var conductor by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(conductorId) {
        try {
            conductor = AuthRepository().getUser(conductorId).getOrNull()
            conductorRatings = AuthRepository().getConductorRatings(conductorId)
            isLoading = false
        } catch (e: Exception) {
            error = "তথ্য পুনরুদ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কন্ডাক্টর মূল্যায়ন") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(error ?: "অজানা ত্রুটি", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Conductor Info
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                conductor?.name ?: "কন্ডাক্টর",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                conductor?.phone ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Rating Summary
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "মূল্যায়ন সারাংশ",
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (conductorRatings == null || conductorRatings!!.totalRatings == 0) {
                                Text(
                                    "এখনও কোনো মূল্যায়ন নেই",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("গড় রেটিং", style = MaterialTheme.typography.bodySmall)
                                        RatingDisplay(
                                            conductorRatings!!.averageRating,
                                            conductorRatings!!.totalRatings
                                        )
                                    }
                                    Column {
                                        Text("মোট যাত্রা", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${conductorRatings!!.totalTrips}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Reviews
                conductorRatings?.reviews?.let { reviews ->
                    if (reviews.isNotEmpty()) {
                        item {
                            Text(
                                "সাম্প্রতিক মূল্যায়ন",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(reviews) { review ->
                            ReviewCard(review)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRatingsScreen(
    navController: NavHostController,
    busId: String
) {
    val scope = rememberCoroutineScope()
    var busRatings by remember { mutableStateOf<BusRatings?>(null) }
    var bus by remember { mutableStateOf<Bus?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(busId) {
        try {
            bus = AuthRepository().getBus(busId)
            busRatings = AuthRepository().getBusRatings(busId)
            isLoading = false
        } catch (e: Exception) {
            error = "তথ্য পুনরুদ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বাস মূল্যায়ন") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(error ?: "অজানা ত্রুটি", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bus Info
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                bus?.name ?: "বাস",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "নম্বর: ${bus?.number}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Rating Summary
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "মূল্যায়ন সারাংশ",
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (busRatings == null || busRatings!!.totalRatings == 0) {
                                Text(
                                    "এখনও কোনো মূল্যায়ন নেই",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("গড় রেটিং", style = MaterialTheme.typography.bodySmall)
                                        RatingDisplay(
                                            busRatings!!.averageRating,
                                            busRatings!!.totalRatings
                                        )
                                    }
                                    Column {
                                        Text("মোট যাত্রা", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${busRatings!!.totalTrips}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Reviews
                busRatings?.reviews?.let { reviews ->
                    if (reviews.isNotEmpty()) {
                        item {
                            Text(
                                "সাম্প্রতিক মূল্যায়ন",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(reviews) { review ->
                            ReviewCard(review)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: ReviewSummary) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    review.riderName,
                    style = MaterialTheme.typography.titleSmall
                )
                RatingDisplay(review.rating)
            }

            Text(
                review.route,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (review.comment.isNotEmpty()) {
                Text(
                    review.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            Text(
                dateFormat.format(Date(review.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}