package com.example.muritin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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

data class FAQItem(
    val question: String,
    val answer: String,
    val icon: ImageVector
)

data class HelpCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val items: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavHostController) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Legacy HelpSection composable - kept for compatibility with other screens
    @Composable
    fun HelpSection(
        icon: ImageVector,
        title: String,
        steps: List<String>
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                steps.forEach { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 4.dp),
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
    val tabs = listOf(
        Triple("লগইন", Icons.AutoMirrored.Outlined.Login, Primary),
        Triple("নিবন্ধন", Icons.Outlined.PersonAdd, Secondary),
        Triple("রিসেট", Icons.Outlined.LockReset, Success)
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
                            colors = listOf(Primary, PrimaryLight)
                        )
                    )
            ) {
                Column {
                    // Status bar spacer
                    Spacer(modifier = Modifier.height(8.dp))

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
                                "সহায়তা কেন্দ্র",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "আমরা আপনাকে সাহায্য করতে এখানে",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Help Icon Animation
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.SupportAgent,
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
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (isSelected) color else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) color else Color.White
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
                    0 -> LoginHelpContent()
                    1 -> SignupHelpContent()
                    2 -> PasswordResetHelpContent()
                }
            }
        }
    }
}

@Composable
fun LoginHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Steps Card
        item {
            ModernHelpCard(
                icon = Icons.Filled.PlayCircleOutline,
                iconColor = Primary,
                title = "লগইন করার ধাপসমূহ",
                items = listOf(
                    "১. লগইন স্ক্রিনে যান",
                    "২. আপনার নিবন্ধিত ইমেইল ঠিকানা লিখুন",
                    "৩. আপনার পাসওয়ার্ড লিখুন (কমপক্ষে ৬ অক্ষর)",
                    "৪. 'লগইন' বোতামে ক্লিক করুন"
                )
            )
        }

        // Important Notes
        item {
            ModernHelpCard(
                icon = Icons.Filled.Lightbulb,
                iconColor = Warning,
                title = "গুরুত্বপূর্ণ তথ্য",
                items = listOf(
                    "• লগইন করার আগে আপনার ইমেইল যাচাই করতে হবে",
                    "• নিবন্ধনের পর আপনার ইমেইলে একটি যাচাই লিঙ্ক পাঠানো হয়",
                    "• যাচাই না করে লগইন করলে ত্রুটি দেখাবে",
                    "• ইমেইল বা পাসওয়ার্ড ভুল হলে পুনরায় চেষ্টা করুন"
                )
            )
        }

        // First Time User
        item {
            GradientInfoCard(
                icon = Icons.Filled.Info,
                title = "প্রথমবার?",
                message = "নিচে 'নতুন অ্যাকাউন্ট তৈরি করুন' লিঙ্কে ক্লিক করুন",
                gradientColors = listOf(Info.copy(alpha = 0.8f), Primary.copy(alpha = 0.6f))
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
            FAQExpandableCard(
                question = "লগইন করতে পারছি না কেন?",
                answer = "আপনার ইমেইল যাচাই করা আছে কিনা দেখুন। স্প্যাম ফোল্ডারও চেক করুন।"
            )
        }

        item {
            FAQExpandableCard(
                question = "পাসওয়ার্ড ভুলে গেছি?",
                answer = "'পাসওয়ার্ড ভুলে গেছেন?' লিঙ্কে ক্লিক করে রিসেট করুন।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun SignupHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Registration Steps
        item {
            ModernHelpCard(
                icon = Icons.Filled.HowToReg,
                iconColor = Secondary,
                title = "নতুন অ্যাকাউন্ট তৈরি করুন",
                items = listOf(
                    "১. লগইন স্ক্রিনে 'নতুন অ্যাকাউন্ট তৈরি করুন' ক্লিক করুন",
                    "২. আপনার পুরো নাম লিখুন",
                    "৩. বৈধ বাংলাদেশী ফোন নম্বর দিন (+8801XXXXXXXXX)",
                    "৪. আপনার বয়স লিখুন (১৮-১০০)",
                    "৫. বৈধ ইমেইল ঠিকানা দিন",
                    "৬. একটি শক্তিশালী পাসওয়ার্ড তৈরি করুন (কমপক্ষে ৬ অক্ষর)",
                    "৭. আপনার ভূমিকা নির্বাচন করুন",
                    "৮. 'নিবন্ধন করুন' বোতামে ক্লিক করুন"
                )
            )
        }

        // Role Selection Cards
        item {
            Text(
                "ভূমিকা সম্পর্কে",
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
                RoleCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.PersonPin,
                    title = "রাইডার",
                    description = "বাস সেবা ব্যবহারকারী যাত্রী",
                    color = RouteBlue
                )
                RoleCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Badge,
                    title = "কন্ডাক্টর",
                    description = "টিকিট পরিচালনাকারী",
                    color = RouteGreen
                )
            }
        }

        item {
            RoleCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Business,
                title = "ওনার",
                description = "বাসের মালিক যিনি কন্ডাক্টর নিয়োগ করতে পারেন",
                color = RoutePurple
            )
        }

        // After Registration
        item {
            GradientInfoCard(
                icon = Icons.Filled.MarkEmailRead,
                title = "নিবন্ধনের পর কি করবেন?",
                message = "১. আপনার ইমেইল চেক করুন\n২. যাচাই ইমেইলের লিঙ্কে ক্লিক করুন\n৩. তারপর লগইন করুন",
                gradientColors = listOf(Success.copy(alpha = 0.8f), RouteGreen.copy(alpha = 0.6f))
            )
        }

        // Common Issues
        item {
            ModernHelpCard(
                icon = Icons.Filled.ReportProblem,
                iconColor = Error,
                title = "সাধারণ সমস্যা",
                items = listOf(
                    "• ফোন নম্বর অবশ্যই +880 দিয়ে শুরু হতে হবে",
                    "• ইমেইল বৈধ ফরম্যাটে হতে হবে (example@domain.com)",
                    "• পাসওয়ার্ড কমপক্ষে ৬ অক্ষর লম্বা হতে হবে",
                    "• বয়স ১৮ থেকে ১০০ এর মধ্যে হতে হবে"
                )
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun PasswordResetHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Reset Steps
        item {
            ModernHelpCard(
                icon = Icons.Filled.LockReset,
                iconColor = Success,
                title = "পাসওয়ার্ড ভুলে গেলে",
                items = listOf(
                    "১. লগইন স্ক্রিনে যান",
                    "২. 'পাসওয়ার্ড ভুলে গেছেন?' লিঙ্কে ক্লিক করুন",
                    "৩. আপনার নিবন্ধিত ইমেইল ঠিকানা লিখুন",
                    "৪. 'পাঠান' বোতামে ক্লিক করুন",
                    "৫. আপনার ইমেইল চেক করুন",
                    "৬. পাসওয়ার্ড রিসেট লিঙ্কে ক্লিক করুন",
                    "৭. নতুন পাসওয়ার্ড সেট করুন",
                    "৮. নতুন পাসওয়ার্ড দিয়ে লগইন করুন"
                )
            )
        }

        // Warning Card
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
                            "গুরুত্বপূর্ণ সতর্কতা",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• রিসেট লিঙ্ক সীমিত সময়ের জন্য বৈধ\n• লিঙ্ক শুধুমাত্র একবার ব্যবহার করা যাবে\n• ইমেইল না পেলে স্প্যাম ফোল্ডার চেক করুন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // Security Tips
        item {
            ModernHelpCard(
                icon = Icons.Filled.Security,
                iconColor = Primary,
                title = "নিরাপত্তা টিপস",
                items = listOf(
                    "• শক্তিশালী পাসওয়ার্ড ব্যবহার করুন",
                    "• পাসওয়ার্ড কারো সাথে শেয়ার করবেন না",
                    "• নিয়মিত পাসওয়ার্ড পরিবর্তন করুন",
                    "• বিভিন্ন সেবায় ভিন্ন পাসওয়ার্ড ব্যবহার করুন"
                )
            )
        }

        // Still Need Help
        item {
            GradientInfoCard(
                icon = Icons.Filled.Headphones,
                title = "এখনও সমস্যা হচ্ছে?",
                message = "সাপোর্ট টিমের সাথে যোগাযোগ করুন",
                gradientColors = listOf(Primary, PrimaryLight)
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ModernHelpCard(
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
                        !item.startsWith("৮")) {
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
fun GradientInfoCard(
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
fun RoleCard(
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
fun FAQExpandableCard(
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
                        .background(Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.QuestionMark,
                        contentDescription = null,
                        tint = Primary,
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
                    tint = Primary
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