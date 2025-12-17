package com.example.muritin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorHelpScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    var showContent by remember { mutableStateOf(false) }

    val tabs = listOf(
        HelpTab("শিডিউল", Icons.Filled.Schedule, Primary),
        HelpTab("রিকোয়েস্ট", Icons.Filled.NotificationsActive, Secondary),
        HelpTab("লোকেশন", Icons.Filled.MyLocation, Success),
        HelpTab("চ্যাট", Icons.Filled.Chat, Info),
        HelpTab("OTP", Icons.Filled.Password, Warning)
    )

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Primary, PrimaryLight)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "কন্ডাক্টর সহায়তা",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "সম্পূর্ণ নির্দেশিকা",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Help,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
        ) {
            // Modern Tab Row with Icons
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.White,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(4.dp)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(tabs[selectedTab].color)
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedTab == index)
                                            tab.color.copy(alpha = 0.15f)
                                        else
                                            Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    tab.icon,
                                    contentDescription = null,
                                    tint = if (selectedTab == index) tab.color else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                tab.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) tab.color else TextSecondary
                            )
                        }
                    }
                }
            }

            // Content with Animation
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
            ) {
                when (selectedTab) {
                    0 -> ScheduleHelpContent()
                    1 -> RequestManagementHelpContent()
                    2 -> LocationUpdateHelpContent()
                    3 -> ConductorChatHelpContent()
                    4 -> OTPVerificationHelpContent()
                }
            }
        }
    }
}

data class HelpTab(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

// Enhanced Help Section Component
@Composable
fun EnhancedHelpSection(
    icon: ImageVector,
    title: String,
    description: String? = null,
    steps: List<String>,
    color: Color = Primary,
    tips: List<String>? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.1f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Steps
            steps.forEachIndexed { index, step ->
                if (step.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (step.startsWith("•")) {
                                Icon(
                                    Icons.Filled.Circle,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(8.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = step.removePrefix("• ").removePrefix("${index + 1}. "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // Tips Section
            tips?.let { tipsList ->
                if (tipsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Success.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = Success,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "সহায়ক টিপস",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Success
                                )
                            }

                            tipsList.forEach { tip ->
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Info Card Component
@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    content: String,
    color: Color = Info,
    backgroundColor: Color = Info.copy(alpha = 0.1f)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// Warning Card Component
@Composable
fun WarningCard(
    icon: ImageVector = Icons.Filled.Warning,
    message: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Error.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}


@Composable
fun ScheduleHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Create Schedule Section
        EnhancedHelpSection(
            icon = Icons.Filled.Schedule,
            title = "শিডিউল তৈরি করা",
            description = "নতুন ট্রিপ শিডিউল তৈরি করুন",
            steps = listOf(
                "ড্যাশবোর্ডে 'শিডিউল তৈরি করুন' বোতামে ক্লিক করুন",
                "তারিখ লিখুন (YYYY-MM-DD ফরম্যাটে)",
                "শুরু সময় লিখুন (HH:MM ফরম্যাটে, ২৪-ঘণ্টা)",
                "শেষের সময় লিখুন (HH:MM ফরম্যাটে)",
                "দিক নির্বাচন করুন: 'যাচ্ছি' বা 'ফিরছি'",
                "'সংরক্ষণ' বোতামে ক্লিক করুন"
            ),
            color = Primary,
            tips = listOf(
                "• শিডিউল তৈরির আগে অবশ্যই একটি বাস অ্যাসাইন করা থাকতে হবে",
                "• সময় ওভারল্যাপ করলে শিডিউল তৈরি হবে না",
                "• ভবিষ্যতের তারিখের জন্য শিডিউল তৈরি করুন"
            )
        )

        // Direction Info
        InfoCard(
            icon = Icons.Filled.DirectionsBus,
            title = "দিক সম্পর্কে",
            content = "• 'যাচ্ছি': শুরু স্থান থেকে গন্তব্যের দিকে\n• 'ফিরছি': গন্তব্য থেকে শুরু স্থানের দিকে\n• দিক অনুযায়ী রাইডারদের রিকোয়েস্ট দেখাবে\n• সঠিক দিক নির্বাচন করা গুরুত্বপূর্ণ",
            color = RouteBlue
        )

        // Edit Schedule Section
        EnhancedHelpSection(
            icon = Icons.Filled.Edit,
            title = "শিডিউল সম্পাদনা",
            description = "বিদ্যমান শিডিউল পরিবর্তন করুন",
            steps = listOf(
                "• শিডিউল তালিকায় এডিট আইকনে ক্লিক করুন",
                "• শুধুমাত্র আসন্ন শিডিউল এডিট করতে পারবেন",
                "• শুরু হয়ে যাওয়া শিডিউল এডিট করা যাবে না",
                "• তারিখ, সময় এবং দিক পরিবর্তন করতে পারবেন",
                "• 'সংরক্ষণ' বোতামে ক্লিক করে আপডেট করুন"
            ),
            color = Secondary
        )

        // Delete Schedule Section
        EnhancedHelpSection(
            icon = Icons.Filled.Delete,
            title = "শিডিউল মুছে ফেলা",
            description = "অপ্রয়োজনীয় শিডিউল ডিলিট করুন",
            steps = listOf(
                "• শিডিউল তালিকায় ডিলিট আইকনে ক্লিক করুন",
                "• শুধুমাত্র আসন্ন শিডিউল মুছতে পারবেন",
                "• নিশ্চিতকরণ ডায়ালগে 'মুছে ফেলুন' বোতামে ক্লিক করুন",
                "• মুছে ফেলা শিডিউল পুনরুদ্ধার করা যাবে না"
            ),
            color = Error
        )

        // Warning
        WarningCard(
            message = "শিডিউল তৈরির আগে অবশ্যই একটি বাস অ্যাসাইন করা থাকতে হবে। বাস ছাড়া শিডিউল তৈরি করতে পারবেন না।"
        )
    }
}

@Composable
fun RequestManagementHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // View Pending Requests
        EnhancedHelpSection(
            icon = Icons.Filled.NotificationsActive,
            title = "পেন্ডিং রিকোয়েস্ট দেখা",
            description = "যাত্রীদের নতুন রিকোয়েস্ট চেক করুন",
            steps = listOf(
                "ড্যাশবোর্ড স্বয়ংক্রিয়ভাবে প্রতি ১৫ সেকেন্ডে রিফ্রেশ হয়",
                "পেন্ডিং রিকোয়েস্ট সেকশনে যাত্রীদের রিকোয়েস্ট দেখুন",
                "প্রতিটি রিকোয়েস্টে পিকআপ, গন্তব্য, সিট এবং ভাড়া দেখানো হবে",
                "শুধুমাত্র আপনার বর্তমান রুটের রিকোয়েস্ট দেখাবে",
                "৩০ মিনিটের মধ্যে পৌঁছানো সম্ভব এমন রিকোয়েস্ট দেখাবে"
            ),
            color = Primary,
            tips = listOf(
                "• নিয়মিত রিফ্রেশ করুন নতুন রিকোয়েস্ট দেখতে",
                "• লোকেশন আপডেট করুন সঠিক দূরত্ব জানতে"
            )
        )

        // Request Filtering Info
        InfoCard(
            icon = Icons.Filled.FilterList,
            title = "রিকোয়েস্ট ফিল্টারিং",
            content = "• শুধুমাত্র আপনার সক্রিয় শিডিউলের রিকোয়েস্ট দেখাবে\n• দিক অনুযায়ী ফিল্টার হয় (যাচ্ছি/ফিরছি)\n• পিকআপ পয়েন্ট ৩০ মিনিটের দূরত্বে হতে হবে\n• আপনার বাসের রুটের স্টপে থাকতে হবে",
            color = Info
        )

        // Accept Request
        EnhancedHelpSection(
            icon = Icons.Filled.CheckCircle,
            title = "রিকোয়েস্ট গ্রহণ করা",
            description = "যাত্রীর রিকোয়েস্ট অ্যাকসেপ্ট করুন",
            steps = listOf(
                "পেন্ডিং রিকোয়েস্ট থেকে একটি নির্বাচন করুন",
                "বিস্তারিত তথ্য চেক করুন (নাম, ফোন, রুট, ভাড়া)",
                "'অ্যাকসেপ্ট করুন' বোতামে ক্লিক করুন",
                "স্বয়ংক্রিয়ভাবে একটি OTP তৈরি হবে",
                "রিকোয়েস্ট 'অ্যাকসেপ্টেড' সেকশনে চলে যাবে"
            ),
            color = Success,
            tips = listOf(
                "• OTP যাত্রীকে দেখানো হবে",
                "• পিকআপ পয়েন্টে পৌঁছানোর আগে অ্যাকসেপ্ট করুন",
                "• একসাথে একাধিক রিকোয়েস্ট অ্যাকসেপ্ট করতে পারবেন"
            )
        )

        // View Accepted Requests
        EnhancedHelpSection(
            icon = Icons.Filled.List,
            title = "অ্যাকসেপ্টেড রিকোয়েস্ট দেখা",
            description = "গ্রহণ করা রিকোয়েস্টের তালিকা",
            steps = listOf(
                "• 'অ্যাকসেপ্টেড রিকোয়েস্টসমূহ' সেকশনে দেখুন",
                "• রাইডারের নাম এবং ফোন নম্বর দেখতে পাবেন",
                "• পিকআপ, গন্তব্য, সিট এবং ভাড়া দেখতে পাবেন",
                "• OTP কোড দেখতে পাবেন",
                "• রাইডারের সাথে চ্যাট করার অপশন পাবেন"
            ),
            color = Secondary
        )

        // Manual Refresh Info
        InfoCard(
            icon = Icons.Filled.Refresh,
            title = "ম্যানুয়াল রিফ্রেশ",
            content = "ম্যানুয়ালি রিফ্রেশ করতে উপরের রিফ্রেশ আইকনে ক্লিক করুন। স্বয়ংক্রিয় রিফ্রেশের পাশাপাশি আপনি যেকোনো সময় ম্যানুয়ালি রিফ্রেশ করতে পারবেন।",
            color = Primary
        )

        // Important Note
        WarningCard(
            icon = Icons.Filled.Info,
            message = "রিকোয়েস্ট অ্যাকসেপ্ট করার আগে অবশ্যই যাত্রীর পিকআপ পয়েন্ট এবং গন্তব্য যাচাই করুন। ভুল রিকোয়েস্ট অ্যাকসেপ্ট করলে সমস্যা হতে পারে।"
        )
    }
}


@Composable
fun LocationUpdateHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Update Location
        EnhancedHelpSection(
            icon = Icons.Filled.MyLocation,
            title = "লোকেশন আপডেট করা",
            description = "আপনার বর্তমান অবস্থান শেয়ার করুন",
            steps = listOf(
                "Location permission দিতে হবে",
                "ড্যাশবোর্ডে 'লোকেশন আপডেট করুন' বোতামে ক্লিক করুন",
                "আপনার বর্তমান অবস্থান সিস্টেমে সেভ হবে",
                "রাইডাররা আপনার অবস্থান ট্র্যাক করতে পারবে",
                "নিয়মিত আপডেট করুন (প্রতি ৫-১০ মিনিটে)"
            ),
            color = Success,
            tips = listOf(
                "• যাত্রী পিকআপের আগে অবশ্যই আপডেট করুন",
                "• ট্রিপ চলাকালীন নিয়মিত আপডেট করুন",
                "• রাইডাররা লাইভ ট্র্যাকিং করতে পারবে"
            )
        )

        // Location Importance
        InfoCard(
            icon = Icons.Filled.LocationOn,
            title = "লোকেশনের গুরুত্ব",
            content = "• রাইডাররা লাইভ ট্র্যাকিং করতে পারে\n• পেন্ডিং রিকোয়েস্ট দেখার জন্য প্রয়োজন\n• নিকটবর্তী রিকোয়েস্ট খুঁজে পেতে সাহায্য করে\n• রাইডারদের আপনার আগমনের সময় জানায়",
            color = Info
        )

        // Location Permission
        EnhancedHelpSection(
            icon = Icons.Filled.Security,
            title = "Location Permission দেওয়া",
            description = "অ্যাপকে লোকেশন অ্যাক্সেস করার অনুমতি দিন",
            steps = listOf(
                "প্রথমবার ব্যবহারের সময় permission চাইবে",
                "'Allow' বা 'অনুমতি দিন' বোতামে ক্লিক করুন",
                "Settings থেকেও permission দিতে পারেন",
                "'While using the app' নির্বাচন করুন",
                "Permission না দিলে অনেক ফিচার কাজ করবে না"
            ),
            color = Warning
        )

        // Battery Saving Tips
        InfoCard(
            icon = Icons.Filled.BatteryChargingFull,
            title = "ব্যাটারি সাশ্রয়ী টিপস",
            content = "• প্রয়োজন অনুযায়ী লোকেশন আপডেট করুন\n• অহেতুক বার বার আপডেট করবেন না\n• যাত্রী পিকআপের আগে অবশ্যই আপডেট করুন\n• রাইড শেষে একবার আপডেট করুন\n• শিডিউল শেষ হলে আর আপডেট করার দরকার নেই",
            color = Success,
            backgroundColor = Success.copy(alpha = 0.1f)
        )

        // Location Error
        WarningCard(
            message = "Location permission ছাড়া পেন্ডিং রিকোয়েস্ট দেখতে পারবেন না এবং অনেক ফিচার কাজ করবে না। অবশ্যই permission দিন।"
        )
    }
}

@Composable
fun ConductorChatHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Start Chat
        EnhancedHelpSection(
            icon = Icons.Filled.Chat,
            title = "রাইডারের সাথে চ্যাট করা",
            description = "যাত্রীর সাথে সরাসরি যোগাযোগ করুন",
            steps = listOf(
                "ড্যাশবোর্ড থেকে 'রাইডারদের সাথে চ্যাট করুন' বোতামে ক্লিক করুন",
                "সক্রিয় চ্যাটের তালিকা দেখতে পাবেন",
                "একটি রাইডার নির্বাচন করুন",
                "'চ্যাট করুন' বোতামে ক্লিক করুন",
                "টেক্সট বক্সে বার্তা লিখুন এবং 'Send' বোতামে ক্লিক করুন"
            ),
            color = Info,
            tips = listOf(
                "• পিকআপ পয়েন্ট খুঁজতে সমস্যা হলে চ্যাট করুন",
                "• বিলম্ব হলে রাইডারকে জানান",
                "• সমস্যা হলে দ্রুত যোগাযোগ করুন"
            )
        )

        // When to Chat
        InfoCard(
            icon = Icons.Filled.QuestionAnswer,
            title = "চ্যাট কখন করবেন",
            content = "• রাইডারের পিকআপ পয়েন্ট খুঁজতে সমস্যা হলে\n• বিলম্ব হলে জানাতে\n• পিকআপ স্থান পরিবর্তন করতে\n• ভাড়া বা অন্যান্য প্রশ্নের উত্তর দিতে\n• রাইডার কোথায় আছে জানতে",
            color = Primary
        )

        // Chat Time Limit
        EnhancedHelpSection(
            icon = Icons.Filled.Schedule,
            title = "চ্যাট সময়সীমা",
            description = "চ্যাট কতক্ষণ উপলব্ধ থাকে",
            steps = listOf(
                "• শুধুমাত্র Accepted রিকোয়েস্টে চ্যাট করতে পারবেন",
                "• রাইড চলাকালীন চ্যাট উপলব্ধ",
                "• রাইড শেষের ৫ দিন (১২০ ঘণ্টা) পর্যন্ত চ্যাট করতে পারবেন",
                "• বাকি কত ঘণ্টা আছে তা দেখানো হবে",
                "• ৫ দিন পরে চ্যাট নিষ্ক্রিয় হয়ে যাবে"
            ),
            color = Warning
        )

        // Chat List
        EnhancedHelpSection(
            icon = Icons.Filled.List,
            title = "চ্যাট তালিকা দেখা",
            description = "সকল সক্রিয় চ্যাট একসাথে দেখুন",
            steps = listOf(
                "• 'রাইডারদের সাথে চ্যাট করুন' বোতামে ক্লিক করুন",
                "• সব সক্রিয় চ্যাট দেখতে পাবেন",
                "• রাইডারের নাম, ফোন এবং ট্রিপ তথ্য দেখাবে",
                "• চ্যাট উপলব্ধ থাকার সময় দেখাবে",
                "• রিফ্রেশ আইকনে ক্লিক করে আপডেট করুন"
            ),
            color = Secondary
        )

        // Chat Etiquette
        InfoCard(
            icon = Icons.Filled.Psychology,
            title = "চ্যাট শিষ্টাচার",
            content = "• রাইডারদের সাথে সম্মানের সাথে কথা বলুন\n• দ্রুত উত্তর দেওয়ার চেষ্টা করুন\n• স্পষ্ট এবং সংক্ষিপ্ত বার্তা পাঠান\n• গালি বা অশোভন ভাষা ব্যবহার করবেন না\n• পেশাদার আচরণ বজায় রাখুন",
            color = Success,
            backgroundColor = Success.copy(alpha = 0.1f)
        )

        // Warning
        WarningCard(
            icon = Icons.Filled.Block,
            message = "রাইডারদের সাথে সম্মানের সাথে কথা বলুন। অপমানজনক ভাষা ব্যবহার করলে আপনার অ্যাকাউন্ট স্থগিত হতে পারে।"
        )
    }
}

@Composable
fun OTPVerificationHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // What is OTP
        EnhancedHelpSection(
            icon = Icons.Filled.Password,
            title = "OTP কি এবং কেন প্রয়োজন",
            description = "One Time Password সম্পর্কে জানুন",
            steps = listOf(
                "• OTP = One Time Password",
                "• রিকোয়েস্ট Accept করলে স্বয়ংক্রিয়ভাবে তৈরি হয়",
                "• রাইডার যাচাই করার জন্য ব্যবহৃত হয়",
                "• প্রতিটি রিকোয়েস্টের জন্য আলাদা OTP থাকে",
                "• ৪ সংখ্যার কোড (যেমন: 1234)",
                "• নিরাপত্তার জন্য অত্যন্ত গুরুত্বপূর্ণ"
            ),
            color = Warning,
            tips = listOf(
                "• OTP কাউকে শেয়ার করবেন না",
                "• শুধুমাত্র যাত্রী যাচাইয়ের জন্য ব্যবহার করুন"
            )
        )

        // OTP Verification Process
        InfoCard(
            icon = Icons.Filled.HowToReg,
            title = "OTP যাচাই প্রক্রিয়া",
            content = "১. রাইডার পিকআপ পয়েন্টে পৌঁছাবে\n২. রাইডার আপনাকে OTP বলবে\n৩. অ্যাকসেপ্টেড রিকোয়েস্টে OTP চেক করুন\n৪. OTP মিললে রাইডারকে বাসে উঠতে দিন\n৫. OTP না মিললে রাইডারকে সঠিক OTP জিজ্ঞাসা করুন",
            color = Success,
            backgroundColor = Success.copy(alpha = 0.1f)
        )

        // Where to Find OTP
        EnhancedHelpSection(
            icon = Icons.Filled.Visibility,
            title = "OTP কোথায় পাবেন",
            description = "আপনার OTP খুঁজে বের করুন",
            steps = listOf(
                "• ড্যাশবোর্ডে 'অ্যাকসেপ্টেড রিকোয়েস্টসমূহ' সেকশনে",
                "• প্রতিটি রিকোয়েস্ট কার্ডে 'OTP: XXXX' দেখানো হবে",
                "• চ্যাট লিস্টেও OTP দেখতে পাবেন",
                "• লাইভ ট্র্যাকিং পেজেও OTP প্রদর্শিত হয়",
                "• রাইডারও তার ফোনে OTP দেখতে পায়"
            ),
            color = Primary
        )

        // Security Tips
        InfoCard(
            icon = Icons.Filled.Security,
            title = "নিরাপত্তা টিপস",
            content = "• OTP কারো সাথে শেয়ার করবেন না\n• শুধুমাত্র পিকআপ পয়েন্টে যাচাই করুন\n• ভুল OTP দিলে বাসে উঠতে দেবেন না\n• OTP যাচাই না করে টিকিট কাটবেন না\n• সন্দেহ হলে রাইডারের নাম এবং ফোন নম্বর চেক করুন",
            color = Error,
            backgroundColor = Error.copy(alpha = 0.1f)
        )

        // Common Problems
        EnhancedHelpSection(
            icon = Icons.Filled.Error,
            title = "সাধারণ সমস্যা এবং সমাধান",
            description = "OTP সংক্রান্ত সমস্যার সমাধান",
            steps = listOf(
                "সমস্যা: রাইডার OTP মনে করতে পারছে না",
                "সমাধান: রাইডারকে তার ফোনে 'আমার রিকোয়েস্ট' বা 'লাইভ ট্র্যাকিং' দেখতে বলুন",
                "",
                "সমস্যা: OTP মিলছে না",
                "সমাধান: নাম এবং পিকআপ/গন্তব্য চেক করুন, অন্য রিকোয়েস্ট হতে পারে",
                "",
                "সমস্যা: একাধিক রাইডারের একই পিকআপ পয়েন্ট",
                "সমাধান: প্রতিটি রাইডারের আলাদা OTP চেক করুন",
                "",
                "সমস্যা: OTP দেখাচ্ছে না",
                "সমাধান: পেজ রিফ্রেশ করুন বা অ্যাপ রিস্টার্ট করুন"
            ),
            color = Secondary
        )

        // Critical Warning
        WarningCard(
            message = "OTP যাচাই না করে যাত্রী উঠালে আপনি দায়ী থাকবেন এবং সমস্যা হতে পারে। প্রতিবার অবশ্যই OTP যাচাই করুন।"
        )

        // Best Practices
        InfoCard(
            icon = Icons.Filled.Lightbulb,
            title = "সর্বোত্তম অনুশীলন",
            content = "• পিকআপের আগে OTP প্রস্তুত রাখুন\n• রাইডারকে OTP বলতে বলুন, আপনি বলবেন না\n• প্রতিটি যাত্রীর OTP আলাদাভাবে যাচাই করুন\n• OTP স্ক্রিনশট নেবেন না\n• যাচাই করার পর রাইডারকে আসন দিন",
            color = Success,
            backgroundColor = Success.copy(alpha = 0.1f)
        )
    }
}