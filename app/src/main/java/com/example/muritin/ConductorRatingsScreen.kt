package com.example.muritin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import kotlinx.coroutines.delay
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
    var showStatistics by remember { mutableStateOf(false) }

    // Animated loading state
    var loadingProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (isLoading) {
            loadingProgress = (loadingProgress + 0.1f) % 1f
            delay(50)
        }
    }

    LaunchedEffect(conductorId) {
        try {
            conductor = AuthRepository().getUser(conductorId).getOrNull()
            conductorRatings = AuthRepository().getConductorRatings(conductorId)
            delay(300) // Smooth transition
            isLoading = false
            showStatistics = true
        } catch (e: Exception) {
            error = "তথ্য পুনরুদ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "কন্ডাক্টর মূল্যায়ন",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.shadow(8.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
        ) {
            when {
                isLoading -> {
                    LoadingScreen(progress = loadingProgress)
                }
                error != null -> {
                    ErrorScreen(error = error!!, onRetry = {
                        scope.launch {
                            isLoading = true
                            error = null
                            try {
                                conductor = AuthRepository().getUser(conductorId).getOrNull()
                                conductorRatings = AuthRepository().getConductorRatings(conductorId)
                                isLoading = false
                            } catch (e: Exception) {
                                error = "তথ্য পুনরুদ্ধারে ত্রুটি: ${e.message}"
                                isLoading = false
                            }
                        }
                    })
                }
                else -> {
                    ConductorRatingsContent(
                        padding = padding,
                        conductor = conductor,
                        conductorRatings = conductorRatings,
                        showStatistics = showStatistics
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen(progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated circular progress
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(80.dp),
                    color = Primary,
                    strokeWidth = 6.dp,
                    trackColor = Primary.copy(alpha = 0.2f)
                )
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "তথ্য লোড হচ্ছে...",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "অনুগ্রহ করে অপেক্ষা করুন",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorScreen(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Error icon with animation
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "কিছু ভুল হয়েছে!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "পুনরায় চেষ্টা করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConductorRatingsContent(
    padding: PaddingValues,
    conductor: User?,
    conductorRatings: ConductorRatings?,
    showStatistics: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Conductor Profile Header
        item {
            AnimatedVisibility(
                visible = showStatistics,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                ConductorProfileCard(conductor = conductor)
            }
        }

        // Rating Summary Card
        item {
            AnimatedVisibility(
                visible = showStatistics,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                exit = fadeOut()
            ) {
                RatingSummaryCard(conductorRatings = conductorRatings)
            }
        }

        // Rating Distribution
        if (conductorRatings != null && conductorRatings.totalRatings > 0) {
            item {
                AnimatedVisibility(
                    visible = showStatistics,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
                ) {
                    RatingDistributionCard(conductorRatings = conductorRatings)
                }
            }
        }

        // Reviews Section Header
        conductorRatings?.reviews?.let { reviews ->
            if (reviews.isNotEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = showStatistics,
                        enter = fadeIn()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "সাম্প্রতিক মূল্যায়ন",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "${reviews.size} টি রিভিউ",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                items(reviews) { review ->
                    AnimatedVisibility(
                        visible = showStatistics,
                        enter = fadeIn() + expandVertically()
                    ) {
                        EnhancedReviewCard(review = review)
                    }
                }
            }
        }

        // Empty state
        if (conductorRatings == null || conductorRatings.totalRatings == 0) {
            item {
                AnimatedVisibility(
                    visible = showStatistics,
                    enter = fadeIn()
                ) {
                    EmptyReviewsCard()
                }
            }
        }
    }
}


@Composable
private fun ConductorProfileCard(conductor: User?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Primary, PrimaryLight)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            conductor?.name ?: "কন্ডাক্টর",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                conductor?.phone ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "পেশাদার কন্ডাক্টর",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingSummaryCard(conductorRatings: ConductorRatings?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "মূল্যায়ন সারাংশ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (conductorRatings == null || conductorRatings.totalRatings == 0) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(BackgroundLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "এখনও কোনো মূল্যায়ন নেই",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "প্রথম যাত্রা সম্পন্ন হলে রেটিং দেখা যাবে",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Average Rating
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Warning.copy(alpha = 0.2f),
                                            Warning.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    String.format("%.1f", conductorRatings.averageRating),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(5) { index ->
                                        Icon(
                                            if (index < conductorRatings.averageRating.toInt())
                                                Icons.Default.Star
                                            else
                                                Icons.Default.StarBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Warning
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "গড় রেটিং",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            "${conductorRatings.totalRatings} টি মূল্যায়ন",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(120.dp)
                            .background(Divider)
                    )

                    // Total Trips
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Success.copy(alpha = 0.2f),
                                            Success.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Success
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${conductorRatings.totalTrips}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "মোট যাত্রা",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            "সম্পন্ন হয়েছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingDistributionCard(conductorRatings: ConductorRatings) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "রেটিং বিতরণ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calculate distribution
            val distribution = calculateRatingDistribution(conductorRatings.reviews)

            (5 downTo 1).forEach { rating ->
                val count = distribution[rating] ?: 0
                val percentage = if (conductorRatings.totalRatings > 0) {
                    (count.toFloat() / conductorRatings.totalRatings.toFloat())
                } else 0f

                RatingBar(
                    rating = rating,
                    count = count,
                    percentage = percentage
                )

                if (rating > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun RatingBar(rating: Int, count: Int, percentage: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(60.dp)
        ) {
            Text(
                "$rating",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Warning
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(BackgroundLight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Warning, Warning.copy(alpha = 0.7f))
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            "$count",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

private fun calculateRatingDistribution(reviews: List<ReviewSummary>): Map<Int, Int> {
    val distribution = mutableMapOf<Int, Int>()
    for (i in 1..5) {
        distribution[i] = 0
    }

    reviews.forEach { review ->
        val rating = review.rating.toInt()
        distribution[rating] = (distribution[rating] ?: 0) + 1
    }

    return distribution
}


@Composable
private fun EnhancedReviewCard(review: ReviewSummary) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryLight, Primary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            review.riderName.firstOrNull()?.toString()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            review.riderName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            formatTimeAgo(review.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }

                // Rating Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = getRatingColor(review.rating).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = getRatingColor(review.rating)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            String.format("%.1f", review.rating),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = getRatingColor(review.rating)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route Info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BackgroundLight
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        review.route,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Comment
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                val displayText = if (isExpanded || review.comment.length <= 100) {
                    review.comment
                } else {
                    review.comment.take(100) + "..."
                }

                Text(
                    displayText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                if (review.comment.length > 100) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            if (isExpanded) "কম দেখুন" else "আরও দেখুন",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Primary
                        )
                    }
                }
            }

            // Star Rating Display
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { index ->
                    Icon(
                        if (index < review.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (index < review.rating.toInt()) Warning else TextTertiary
                    )
                    if (index < 4) Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyReviewsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(BackgroundLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.RateReview,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = TextSecondary.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "কোনো রিভিউ পাওয়া যায়নি",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "এই কন্ডাক্টরের এখনও কোনো মূল্যায়ন নেই।\nপ্রথম যাত্রী হিসেবে রিভিউ দিন!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// Helper function for rating color
private fun getRatingColor(rating: Float): Color {
    return when {
        rating >= 4.5f -> Success
        rating >= 3.5f -> Color(0xFF66BB6A)
        rating >= 2.5f -> Warning
        rating >= 1.5f -> Color(0xFFFF9800)
        else -> Error
    }
}

// Helper function to format time ago
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365

    return when {
        years > 0 -> "$years বছর আগে"
        months > 0 -> "$months মাস আগে"
        weeks > 0 -> "$weeks সপ্তাহ আগে"
        days > 0 -> "$days দিন আগে"
        hours > 0 -> "$hours ঘণ্টা আগে"
        minutes > 0 -> "$minutes মিনিট আগে"
        else -> "এইমাত্র"
    }
}

// Rating Display Component (reusable)
@Composable
fun RatingDisplay1(rating: Float, totalRatings: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Warning
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            String.format("%.1f", rating),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        totalRatings?.let {
            Text(
                " ($it)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

// Review Card Component (for compatibility)
@Composable
fun ReviewCard(review: ReviewSummary) {
    EnhancedReviewCard(review = review)
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
    var showStatistics by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (isLoading) {
            loadingProgress = (loadingProgress + 0.1f) % 1f
            delay(50)
        }
    }

    LaunchedEffect(busId) {
        try {
            bus = AuthRepository().getBus(busId)
            busRatings = AuthRepository().getBusRatings(busId)
            delay(300)
            isLoading = false
            showStatistics = true
        } catch (e: Exception) {
            error = "তথ্য পুনরুদ্ধারে ত্রুটি: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "বাস মূল্যায়ন",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.shadow(8.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
        ) {
            when {
                isLoading -> {
                    BusLoadingScreen(progress = loadingProgress)
                }
                error != null -> {
                    ErrorScreen(error = error!!, onRetry = {
                        scope.launch {
                            isLoading = true
                            error = null
                            try {
                                bus = AuthRepository().getBus(busId)
                                busRatings = AuthRepository().getBusRatings(busId)
                                isLoading = false
                            } catch (e: Exception) {
                                error = "তথ্য পুনরুদ্ধারে ত্রুটি: ${e.message}"
                                isLoading = false
                            }
                        }
                    })
                }
                else -> {
                    BusRatingsContent(
                        padding = padding,
                        bus = bus,
                        busRatings = busRatings,
                        showStatistics = showStatistics
                    )
                }
            }
        }
    }
}

@Composable
private fun BusLoadingScreen(progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(80.dp),
                    color = Primary,
                    strokeWidth = 6.dp,
                    trackColor = Primary.copy(alpha = 0.2f)
                )
                Icon(
                    Icons.Default.DirectionsBus,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "বাসের তথ্য লোড হচ্ছে...",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "অনুগ্রহ করে অপেক্ষা করুন",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun BusRatingsContent(
    padding: PaddingValues,
    bus: Bus?,
    busRatings: BusRatings?,
    showStatistics: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bus Profile Header
        item {
            AnimatedVisibility(
                visible = showStatistics,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                BusProfileCard(bus = bus)
            }
        }

        // Rating Summary Card
        item {
            AnimatedVisibility(
                visible = showStatistics,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                BusRatingSummaryCard(busRatings = busRatings)
            }
        }

        // Rating Distribution
        if (busRatings != null && busRatings.totalRatings > 0) {
            item {
                AnimatedVisibility(
                    visible = showStatistics,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
                ) {
                    BusRatingDistributionCard(busRatings = busRatings)
                }
            }
        }

        // Reviews Section Header
        busRatings?.reviews?.let { reviews ->
            if (reviews.isNotEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = showStatistics,
                        enter = fadeIn()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "সাম্প্রতিক মূল্যায়ন",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    "${reviews.size} টি রিভিউ",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                items(reviews) { review ->
                    AnimatedVisibility(
                        visible = showStatistics,
                        enter = fadeIn() + expandVertically()
                    ) {
                        EnhancedReviewCard(review = review)
                    }
                }
            }
        }

        // Empty state
        if (busRatings == null || busRatings.totalRatings == 0) {
            item {
                AnimatedVisibility(
                    visible = showStatistics,
                    enter = fadeIn()
                ) {
                    EmptyBusReviewsCard()
                }
            }
        }
    }
}

@Composable
private fun BusProfileCard(bus: Bus?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(RouteBlue, RouteGreen)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bus Icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(4.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsBus,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = RouteBlue
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            bus?.name ?: "বাস",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ConfirmationNumber,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    bus?.number ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Route info
                        bus?.route?.let { route ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${route.originLoc?.address ?: ""} - ${route.destinationLoc?.address ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusRatingSummaryCard(busRatings: BusRatings?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "মূল্যায়ন সারাংশ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (busRatings == null || busRatings.totalRatings == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(BackgroundLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "এখনও কোনো মূল্যায়ন নেই",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "প্রথম যাত্রা সম্পন্ন হলে রেটিং দেখা যাবে",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Warning.copy(alpha = 0.2f),
                                            Warning.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    String.format("%.1f", busRatings.averageRating),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(5) { index ->
                                        Icon(
                                            if (index < busRatings.averageRating.toInt())
                                                Icons.Default.Star
                                            else
                                                Icons.Default.StarBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Warning
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "গড় রেটিং",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            "${busRatings.totalRatings} টি মূল্যায়ন",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(120.dp)
                            .background(Divider)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Success.copy(alpha = 0.2f),
                                            Success.copy(alpha = 0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Success
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${busRatings.totalTrips}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "মোট যাত্রা",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            "সম্পন্ন হয়েছে",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BusRatingDistributionCard(busRatings: BusRatings) {
    RatingDistributionCard(
        conductorRatings = ConductorRatings(
            conductorId = busRatings.busId,
            totalRatings = busRatings.totalRatings,
            averageRating = busRatings.averageRating,
            totalTrips = busRatings.totalTrips,
            reviews = busRatings.reviews
        )
    )
}

@Composable
private fun EmptyBusReviewsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(BackgroundLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsBus,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = TextSecondary.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "কোনো রিভিউ পাওয়া যায়নি",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "এই বাসের এখনও কোনো মূল্যায়ন নেই।\nপ্রথম যাত্রী হিসেবে রিভিউ দিন!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}