package com.example.muritin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*

// Data classes for structured content
data class RiderFAQItem(
    val question: String,
    val answer: String,
    val icon: ImageVector
)

data class RiderHelpCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderHelpScreen(navController: NavHostController) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple("ট্রিপ রিকোয়েস্ট", Icons.Outlined.DirectionsBus, RoutePurple),
        Triple("আমার রিকোয়েস্ট", Icons.Outlined.List, RouteGreen),
        Triple("লাইভ ট্র্যাকিং", Icons.Outlined.Navigation, RouteBlue),
        Triple("পূর্ববর্তী যাত্রা", Icons.Outlined.History, Warning),
        Triple("চ্যাট", Icons.Outlined.Chat, Info)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Top Bar with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(RoutePurple, RoutePurple.copy(alpha = 0.8f))
                        )
                    )
                    .statusBarsPadding()
            ) {
                Column {
                    // Status bar spacer
                    Spacer(modifier = Modifier.height(4.dp))

                    // Top Bar Content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "রাইডার সহায়তা কেন্দ্র",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "আপনার যাত্রা সহজ করতে আমরা এখানে",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Help Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.DirectionsBus,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Bar (Decorative)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "কিভাবে সাহায্য করতে পারি?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Custom Tab Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEachIndexed { index, (title, icon, color) ->
                            val isSelected = selectedTab == index
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        Color.White
                                    else
                                        Color.White.copy(alpha = 0.3f)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isSelected) 4.dp else 0.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) color else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) color else Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Content Area
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    0 -> TripRequestHelpContent()
                    1 -> MyRequestsHelpContent()
                    2 -> LiveTrackingHelpContent()
                    3 -> PastTripsHelpContent()
                    4 -> ChatHelpContent()
                }
            }
        }
    }
}
@Composable
fun TripRequestHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Steps Card
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.PlayCircleOutline,
                iconColor = RouteBlue,
                title = "ট্রিপ রিকোয়েস্ট করার ধাপসমূহ",
                items = listOf(
                    "১. ড্যাশবোর্ড থেকে 'ট্রিপ রিকোয়েস্ট করুন' বোতামে ক্লিক করুন",
                    "২. আপনার বর্তমান অবস্থান স্বয়ংক্রিয়ভাবে পিকআপ পয়েন্ট হিসেবে সেট হবে",
                    "৩. ম্যাপে ক্লিক করে বা সার্চ বক্সে টাইপ করে পিকআপ স্থান নির্বাচন করুন",
                    "৪. 'নিকটবর্তী পিকআপ স্টপ খুঁজুন' বোতামে ক্লিক করুন",
                    "৫. লিস্ট থেকে একটি স্টপ নির্বাচন করুন অথবা 'নিকটতম স্টপ নির্বাচন করুন'",
                    "৬. 'গন্তব্য নির্বাচন করুন' বোতামে ক্লিক করুন",
                    "৭. একইভাবে গন্তব্য স্টপ নির্বাচন করুন",
                    "৮. সিটের সংখ্যা নির্ধারণ করুন",
                    "৯. আনুমানিক ভাড়া চেক করুন",
                    "১০. 'রিকোয়েস্ট জমা দিন' বোতামে ক্লিক করুন"
                )
            )
        }

        // Map Features Card
        item {
            GradientRiderInfoCard(
                icon = Icons.Filled.Map,
                title = "ম্যাপ ফিচারসমূহ",
                message = "• আপনার বর্তমান অবস্থান নীল বৃত্ত দিয়ে দেখানো হয়\n• বাস স্টপ মার্কারে ক্লিক করে দূরত্ব দেখুন\n• জুম ইন/আউট করে এলাকা দেখুন\n• ড্র্যাগ করে ম্যাপ ঘুরান",
                gradientColors = listOf(RouteBlue.copy(alpha = 0.8f), Primary.copy(alpha = 0.6f))
            )
        }

        // Stop Selection Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Secondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Secondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "নিকটবর্তী স্টপ সম্পর্কে",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "• ২.৫ কিমি ব্যাসার্ধের মধ্যে বাস স্টপ খুঁজে পাবেন\n• নীল মার্কার: সব উপলব্ধ স্টপ\n• সবুজ মার্কার: আপনার নির্বাচিত স্টপ\n• দূরত্ব কিলোমিটারে দেখানো হয়\n• নিকটতম স্টপ স্বয়ংক্রিয়ভাবে হাইলাইট থাকে",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Important Notes
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.Warning,
                iconColor = Error,
                title = "গুরুত্বপূর্ণ নোট",
                items = listOf(
                    "• শুধুমাত্র বাস রুটের স্টপ থেকে স্টপে যেতে পারবেন",
                    "• পিকআপ এবং গন্তব্য উভয়ই অবশ্যই নির্বাচন করতে হবে",
                    "• ভাড়া দূরত্ব এবং সিট সংখ্যার উপর নির্ভর করে",
                    "• কিছু বাসের জন্য কাস্টম ভাড়া থাকতে পারে",
                    "• Location permission দিতে হবে",
                    "• ইন্টারনেট সংযোগ প্রয়োজন"
                )
            )
        }

        // FAQ Section
        item {
            Text(
                "সাধারণ প্রশ্নাবলী",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "বাস স্টপ খুঁজে পাচ্ছি না কেন?",
                answer = "নিশ্চিত করুন যে আপনার Location permission চালু আছে এবং আপনি সঠিক এলাকায় আছেন। যদি সমস্যা চলতে থাকে, পেজ রিফ্রেশ করুন।"
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "ভাড়া কিভাবে নির্ধারণ হয়?",
                answer = "ভাড়া পিকআপ এবং গন্তব্যের দূরত্ব এবং আপনার নির্বাচিত সিট সংখ্যার উপর ভিত্তি করে হিসাব করা হয়। কিছু বাসের জন্য বিশেষ রেট থাকতে পারে।"
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "রিকোয়েস্ট জমা দেওয়ার পর কি হবে?",
                answer = "রিকোয়েস্ট জমা দেওয়ার পর কন্ডাক্টর তা দেখতে পাবেন এবং গ্রহণ বা প্রত্যাখ্যান করতে পারবেন। আপনি 'আমার রিকোয়েস্ট' পেজে স্ট্যাটাস দেখতে পারবেন।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
@Composable
fun MyRequestsHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview Card
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.List,
                iconColor = RouteGreen,
                title = "আমার রিকোয়েস্ট পেজ",
                items = listOf(
                    "১. এখানে আপনার সব রিকোয়েস্ট দেখতে পাবেন",
                    "২. প্রতি ৩ সেকেন্ডে স্বয়ংক্রিয়ভাবে রিফ্রেশ হয়",
                    "৩. ডানদিকের রিফ্রেশ আইকনে ক্লিক করে অটো-রিফ্রেশ বন্ধ/চালু করুন",
                    "৪. প্রতিটি রিকোয়েস্টের স্ট্যাটাস দেখতে পাবেন",
                    "৫. রিকোয়েস্টে ক্লিক করে বিস্তারিত দেখুন"
                )
            )
        }

        // Status Cards
        item {
            Text(
                "রিকোয়েস্ট স্ট্যাটাস",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Schedule,
                    title = "Pending",
                    description = "অপেক্ষমাণ",
                    color = Warning
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CheckCircle,
                    title = "Accepted",
                    description = "গৃহীত",
                    color = Success
                )
            }
        }

        item {
            StatusCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Cancel,
                title = "Cancelled",
                description = "বাতিল করা",
                color = Error
            )
        }

        // Cancellation Guide
        item {
            GradientRiderInfoCard(
                icon = Icons.Filled.RemoveCircleOutline,
                title = "রিকোয়েস্ট বাতিল করা",
                message = "• শুধুমাত্র 'Pending' স্ট্যাটাসের রিকোয়েস্ট বাতিল করতে পারবেন\n• 'বাতিল করুন' বোতামে ক্লিক করুন\n• একবার বাতিল করলে আর ফিরিয়ে আনা যাবে না\n• Accepted রিকোয়েস্ট বাতিল করা যাবে না",
                gradientColors = listOf(Error.copy(alpha = 0.7f), Warning.copy(alpha = 0.6f))
            )
        }

        // Accepted Request Features
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.Info,
                iconColor = Success,
                title = "Accepted রিকোয়েস্টে পাবেন",
                items = listOf(
                    "• বাসের নাম এবং নম্বর",
                    "• কন্ডাক্টরের নাম এবং ফোন নম্বর",
                    "• OTP কোড (বোর্ডিং এর সময় দিতে হবে)",
                    "• লাইভ ট্র্যাকিং বোতাম",
                    "• কন্ডাক্টরের সাথে চ্যাট করার অপশন",
                    "• রিকোয়েস্টের সম্পূর্ণ বিবরণ",
                    "• ভাড়া এবং সিট সংখ্যা"
                )
            )
        }

        // Action Buttons Guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RoutePurple.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = RoutePurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "দ্রুত অ্যাকশন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "📍 লাইভ ট্র্যাকিং: কন্ডাক্টরের রিয়েল-টাইম অবস্থান দেখুন\n💬 চ্যাট: কন্ডাক্টরের সাথে সরাসরি যোগাযোগ করুন\n❌ বাতিল: Pending রিকোয়েস্ট বাতিল করুন\n🔄 রিফ্রেশ: সর্বশেষ স্ট্যাটাস আপডেট করুন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // FAQ
        item {
            Text(
                "সাধারণ প্রশ্নাবলী",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "কন্ডাক্টর রিকোয়েস্ট গ্রহণ করতে কতক্ষণ লাগে?",
                answer = "এটি কন্ডাক্টরের উপলব্ধতার উপর নির্ভর করে। সাধারণত ৫-১৫ মিনিটের মধ্যে আপডেট পাবেন। যদি দীর্ঘ সময় হয়, নতুন রিকোয়েস্ট পাঠাতে পারেন।"
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "Accepted রিকোয়েস্ট বাতিল করতে পারব?",
                answer = "না, একবার কন্ডাক্টর রিকোয়েস্ট গ্রহণ করলে আপনি এটি বাতিল করতে পারবেন না। জরুরি কিছু হলে সরাসরি কন্ডাক্টরের সাথে চ্যাটে যোগাযোগ করুন।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
@Composable
fun LiveTrackingHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Guide
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.Navigation,
                iconColor = RoutePurple,
                title = "লাইভ ট্র্যাকিং ব্যবহার করা",
                items = listOf(
                    "১. 'আমার রিকোয়েস্ট' থেকে Accepted রিকোয়েস্টে ক্লিক করুন",
                    "২. 'লাইভ ট্র্যাকিং' বোতামে ক্লিক করুন",
                    "৩. ম্যাপে আপনার পিকআপ এবং গন্তব্য দেখতে পাবেন",
                    "৪. কন্ডাক্টরের বর্তমান অবস্থান দেখতে পাবেন",
                    "৫. প্রতি ৫ সেকেন্ডে স্বয়ংক্রিয়ভাবে আপডেট হয়",
                    "৬. আনুমানিক আগমনের সময় দেখা যায়",
                    "৭. OTP কোড সবসময় দৃশ্যমান থাকে"
                )
            )
        }

        // Map Markers
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RouteBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.MyLocation,
                                contentDescription = null,
                                tint = RouteBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "ম্যাপ মার্কার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "🟢 সবুজ মার্কার: আপনার পিকআপ পয়েন্ট\n🔴 লাল মার্কার: আপনার গন্তব্য পয়েন্ট\n🚌 বাস আইকন: কন্ডাক্টরের বর্তমান অবস্থান (চলমান)\n📍 নীল বিন্দু: আপনার বর্তমান অবস্থান",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // OTP Information
        item {
            GradientRiderInfoCard(
                icon = Icons.Filled.Pin,
                title = "OTP এর গুরুত্ব",
                message = "• বাসে উঠার সময় কন্ডাক্টরকে OTP বলুন\n• কন্ডাক্টর যাচাই করার পর আপনি বোর্ড করতে পারবেন\n• OTP কারো সাথে শেয়ার করবেন না\n• প্রতিটি রিকোয়েস্টের জন্য আলাদা OTP থাকে\n• OTP লাইভ ট্র্যাকিং পেজে সবসময় দৃশ্যমান",
                gradientColors = listOf(Success.copy(alpha = 0.8f), RouteGreen.copy(alpha = 0.6f))
            )
        }

        // Features
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.TipsAndUpdates,
                iconColor = Warning,
                title = "দরকারি ফিচার",
                items = listOf(
                    "• রিয়েল-টাইম অবস্থান আপডেট",
                    "• আনুমানিক আগমনের সময়",
                    "• দূরত্ব ক্যালকুলেশন",
                    "• রুট ভিজুয়ালাইজেশন",
                    "• স্বয়ংক্রিয় ম্যাপ জুম",
                    "• কন্ডাক্টর তথ্য",
                    "• জরুরি যোগাযোগ অপশন"
                )
            )
        }

        // Troubleshooting
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Error.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Error.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "সমস্যা সমাধান",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• কন্ডাক্টরের অবস্থান দেখতে না পেলে পেজ রিফ্রেশ করুন\n• ইন্টারনেট সংযোগ চেক করুন\n• Location permission চালু আছে কিনা দেখুন\n• অ্যাপ পুনরায় চালু করে দেখুন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // FAQ
        item {
            Text(
                "সাধারণ প্রশ্নাবলী",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "আনুমানিক সময় কতটা সঠিক?",
                answer = "আনুমানিক সময় ট্রাফিক এবং রুটের উপর নির্ভর করে। এটি একটি প্রাক্কলন মাত্র। বাস্তব সময় ভিন্ন হতে পারে।"
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "কন্ডাক্টর OTP গ্রহণ না করলে?",
                answer = "যদি কন্ডাক্টর OTP গ্রহণ না করেন, চ্যাটে যোগাযোগ করুন অথবা সরাসরি ফোন করুন। সমস্যা অব্যাহত থাকলে সাপোর্টে যোগাযোগ করুন।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun PastTripsHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.History,
                iconColor = Warning,
                title = "পূর্ববর্তী যাত্রা দেখা",
                items = listOf(
                    "১. ড্যাশবোর্ড থেকে 'পূর্ববর্তী যাত্রাসমূহ' বোতামে ক্লিক করুন",
                    "২. আপনার সব সম্পন্ন যাত্রা দেখতে পাবেন",
                    "৩. প্রতিটি যাত্রার বিস্তারিত তথ্য দেখতে পাবেন",
                    "৪. যাত্রা শেষের সময় দেখানো হবে",
                    "৫. ভাড়া এবং অন্যান্য তথ্য উপলব্ধ"
                )
            )
        }

        // Trip Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Info.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Receipt,
                                contentDescription = null,
                                tint = Info,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "প্রতিটি যাত্রায় পাবেন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "📍 পিকআপ এবং গন্তব্য স্টপ\n💰 ভাড়া এবং সিট সংখ্যা\n🚌 বাসের নাম ও নম্বর\n👤 কন্ডাক্টরের নাম ও ফোন\n🔢 ব্যবহৃত OTP\n⏰ যাত্রা শেষের সময়\n📊 যাত্রার দূরত্ব",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Chat Availability
        item {
            GradientRiderInfoCard(
                icon = Icons.Filled.Schedule,
                title = "চ্যাট সময়সীমা",
                message = "• যাত্রা শেষের পর ৩ দিন (৭২ ঘন্টা) চ্যাট করতে পারবেন\n• বাকি কত ঘন্টা আছে তা দেখানো হবে\n• ৩ দিন পরে চ্যাট বোতাম নিষ্ক্রিয় হয়ে যাবে\n• প্রয়োজনে এই সময়ের মধ্যে যোগাযোগ করুন",
                gradientColors = listOf(Warning.copy(alpha = 0.8f), Secondary.copy(alpha = 0.6f))
            )
        }

        // Useful Actions
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.TouchApp,
                iconColor = Success,
                title = "কি করতে পারবেন",
                items = listOf(
                    "• সম্পূর্ণ যাত্রার ইতিহাস দেখুন",
                    "• ভাড়া এবং রুট যাচাই করুন",
                    "• কন্ডাক্টরের তথ্য সংরক্ষণ করুন",
                    "• ৩ দিনের মধ্যে কন্ডাক্টরের সাথে চ্যাট করুন",
                    "• যাত্রার রিপোর্ট ডাউনলোড করুন (শীঘ্রই)",
                    "• বাস ও কন্ডাক্টর রেটিং দিন (শীঘ্রই)"
                )
            )
        }

        // Tips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Success.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Success.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "দরকারি টিপস",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Success
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• নিয়মিত আপনার যাত্রা ইতিহাস চেক করুন\n• ভুল ভাড়া দেখলে ৩ দিনের মধ্যে রিপোর্ট করুন\n• OTP সংরক্ষণ করুন বিবাদের ক্ষেত্রে\n• সময়মতো চ্যাটে সমস্যা জানান",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
@Composable
fun ChatHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Chat Guide
        item {
            ModernRiderHelpCard(
                icon = Icons.AutoMirrored.Filled.Chat,
                iconColor = Info,
                title = "কন্ডাক্টরের সাথে চ্যাট",
                items = listOf(
                    "১. 'আমার রিকোয়েস্ট' বা 'পূর্ববর্তী যাত্রা' থেকে চ্যাট করুন",
                    "২. 'Chat with Conductor' বোতামে ক্লিক করুন",
                    "৩. নিচের টেক্সট বক্সে আপনার বার্তা লিখুন",
                    "৪. 'Send' বোতামে ক্লিক করে পাঠান",
                    "৫. কন্ডাক্টরের উত্তর স্বয়ংক্রিয়ভাবে দেখতে পাবেন",
                    "৬. রিয়েল-টাইম মেসেজিং সুবিধা"
                )
            )
        }

        // When to Chat
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RouteGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.QuestionAnswer,
                                contentDescription = null,
                                tint = RouteGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "চ্যাট কখন ব্যবহার করবেন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "🚌 বাসের অবস্থান জানতে\n⏰ বিলম্বের কারণ জানতে\n📍 বিশেষ নির্দেশনা জানতে\n🎒 ভুলে যাওয়া জিনিস সম্পর্কে\n💰 ভাড়া বা অন্যান্য প্রশ্ন\n🆘 জরুরি সাহায্যের জন্য",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Chat Features
        item {
            GradientRiderInfoCard(
                icon = Icons.Filled.Stars,
                title = "চ্যাট ফিচার",
                message = "• রিয়েল-টাইম মেসেজিং\n• মেসেজ পড়া হয়েছে কিনা দেখুন\n• টাইমস্ট্যাম্প সহ বার্তা\n• সহজ ইউজার ইন্টারফেস\n• দ্রুত প্রতিক্রিয়া",
                gradientColors = listOf(Info.copy(alpha = 0.8f), RouteBlue.copy(alpha = 0.6f))
            )
        }

        // Chat Availability
        item {
            ModernRiderHelpCard(
                icon = Icons.Filled.Info,
                iconColor = Warning,
                title = "চ্যাট সম্পর্কে জানুন",
                items = listOf(
                    "• শুধুমাত্র Accepted রিকোয়েস্টে চ্যাট করতে পারবেন",
                    "• রাইড চলাকালীন চ্যাট উপলব্ধ",
                    "• রাইড শেষের ৩ দিন পর্যন্ত চ্যাট করতে পারবেন",
                    "• আপনার বার্তা ডান দিকে দেখাবে",
                    "• কন্ডাক্টরের বার্তা বাম দিকে দেখাবে",
                    "• মেসেজ ডিলিট করা যাবে না"
                )
            )
        }

        // Etiquette
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Error.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Error.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Block,
                            contentDescription = null,
                            tint = Error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "চ্যাট শিষ্টাচার",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• অপমানজনক ভাষা বা আচরণ করবেন না\n• সবার সাথে সম্মানের সাথে কথা বলুন\n• অপ্রয়োজনীয় মেসেজ পাঠাবেন না\n• ব্যক্তিগত তথ্য শেয়ার করবেন না\n• ধৈর্য ধরে উত্তরের জন্য অপেক্ষা করুন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // Tips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Success.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Success.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.EmojiObjects,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "চ্যাট টিপস",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Success
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "✅ সংক্ষিপ্ত ও স্পষ্ট বার্তা লিখুন\n✅ জরুরি বিষয় প্রথমে উল্লেখ করুন\n✅ ধন্যবাদ বলতে ভুলবেন না\n✅ কন্ডাক্টর ব্যস্ত থাকতে পারে, ধৈর্য ধরুন\n✅ সমস্যা হলে স্ক্রিনশট নিন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // FAQ
        item {
            Text(
                "সাধারণ প্রশ্নাবলী",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "কন্ডাক্টর উত্তর দিচ্ছেন না কেন?",
                answer = "কন্ডাক্টর হয়তো গাড়ি চালাচ্ছেন বা অন্য যাত্রীদের সাথে ব্যস্ত আছেন। অনুগ্রহ করে কিছুক্ষণ অপেক্ষা করুন। জরুরি হলে ফোন করুন।"
            )
        }

        item {
            RiderFAQExpandableCard(
                question = "চ্যাট ইতিহাস কতদিন থাকে?",
                answer = "যাত্রা শেষের পর ৩ দিন পর্যন্ত চ্যাট ইতিহাস দেখতে ও মেসেজ পাঠাতে পারবেন। এর পরে শুধু দেখতে পারবেন, নতুন মেসেজ পাঠাতে পারবেন না।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ============= REUSABLE COMPONENTS =============

@Composable
fun ModernRiderHelpCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (!item.startsWith("•") && !item.startsWith("১") &&
                        !item.startsWith("২") && !item.startsWith("৩") &&
                        !item.startsWith("৪") && !item.startsWith("৫") &&
                        !item.startsWith("৬") && !item.startsWith("৭") &&
                        !item.startsWith("৮") && !item.startsWith("৯") &&
                        !item.startsWith("১০")) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(iconColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GradientRiderInfoCard(
    icon: ImageVector,
    title: String,
    message: String,
    gradientColors: List<Color>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(colors = gradientColors)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RiderFAQExpandableCard(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RouteBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.QuestionMark,
                        contentDescription = null,
                        tint = RouteBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = RouteBlue
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Divider)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}


