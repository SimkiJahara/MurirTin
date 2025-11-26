package com.example.muritin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHelpScreen(navController: NavHostController) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        Triple("বাস রেজিস্ট্রেশন", Icons.Outlined.DirectionsBus, RoutePurple),
        Triple("বাস পরিচালনা", Icons.Outlined.Settings, RouteGreen),
        Triple("কন্ডাক্টর", Icons.Outlined.PersonAdd, RouteBlue),
        Triple("ভাড়া", Icons.Outlined.AttachMoney, Warning)
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
                                "ওনার সহায়তা কেন্দ্র",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "আপনার বাস ব্যবসা সফল করতে",
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
                                Icons.Filled.Business,
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
                                        color = if (isSelected) color else Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        fontSize = 11.sp
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
                    0 -> BusRegistrationHelpContent()
                    1 -> BusManagementHelpContent()
                    2 -> ConductorManagementHelpContent()
                    3 -> FareManagementHelpContent()
                }
            }
        }
    }
}
@Composable
fun BusRegistrationHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Registration Steps
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.AppRegistration,
                iconColor = RoutePurple,
                title = "বাস রেজিস্ট্রেশন ধাপসমূহ",
                items = listOf(
                    "১. ড্যাশবোর্ড থেকে 'বাস রেজিস্টার করুন' বোতামে ক্লিক করুন",
                    "২. বাসের নাম লিখুন (যেমন: 'ঢাকা এক্সপ্রেস')",
                    "৩. বাসের নম্বর লিখুন (যেমন: 'ঢাকা-মেট্রো-গ-১১-১২৩৪')",
                    "৪. ফিটনেস সার্টিফিকেট নম্বর লিখুন",
                    "৫. ট্যাক্স টোকেন নম্বর লিখুন",
                    "৬. যাত্রা শুরুর অবস্থান নির্বাচন করুন",
                    "৭. গন্তব্যস্থল নির্বাচন করুন",
                    "৮. স্টপেজ যোগ করুন (ঐচ্ছিক)",
                    "৯. রুট দেখুন এবং নিশ্চিত করুন",
                    "১০. প্রতিটি স্টপের জন্য ভাড়া সেট করুন",
                    "১১. 'রেজিস্টার করুন' বোতামে ক্লিক করুন"
                )
            )
        }

        // Route Setup Guide
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
                                Icons.Filled.Map,
                                contentDescription = null,
                                tint = RouteBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "রুট সেটআপ সম্পর্কে",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "🔍 সার্চ বক্সে টাইপ করে অথবা ম্যাপে ক্লিক করে স্থান নির্বাচন করুন\n📍 প্রতিটি স্টপ আলাদাভাবে যোগ করতে হবে\n➕ 'আরও একটি স্টপেজ যোগ করুন' বোতাম দিয়ে একাধিক স্টপ যোগ করুন\n🗺️ রুট সম্পূর্ণ হলে 'রুট যোগ করুন' বোতামে ক্লিক করুন\n🔵 নীল লাইন আপনার রুট দেখাবে\n🟣 ভায়োলেট মার্কার শুরু এবং গন্তব্য দেখায়\n🔴 লাল মার্কার স্টপেজ দেখায়",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Map Controls
        item {
            GradientOwnerInfoCard(
                icon = Icons.Filled.TouchApp,
                title = "ম্যাপ কন্ট্রোল",
                message = "• জুম ইন/আউট করতে পিঞ্চ করুন\n• ড্র্যাগ করে ম্যাপ ঘুরান\n• মার্কারে ক্লিক করে তথ্য দেখুন\n• সার্চ বক্স ব্যবহার করে দ্রুত খুঁজুন",
                gradientColors = listOf(RoutePurple.copy(alpha = 0.8f), Primary.copy(alpha = 0.6f))
            )
        }

        // Important Notes
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.Warning,
                iconColor = Error,
                title = "গুরুত্বপূর্ণ তথ্য",
                items = listOf(
                    "• সব ফিল্ড অবশ্যই পূরণ করতে হবে",
                    "• কমপক্ষে একটি ভাড়া সেট করতেই হবে",
                    "• রুট বাতিল করতে 'রুট বাতিল করুন' বোতামে ক্লিক করুন",
                    "• Location permission প্রয়োজন",
                    "• বাস নম্বর অনন্য হতে হবে",
                    "• ফিটনেস ও ট্যাক্স সার্টিফিকেট বৈধ রাখুন"
                )
            )
        }

        // After Registration
        item {
            GradientOwnerInfoCard(
                icon = Icons.Filled.CheckCircle,
                title = "রেজিস্ট্রেশনের পর",
                message = "✅ বাস তালিকায় দেখতে পাবেন\n👤 কন্ডাক্টর অ্যাসাইন করতে পারবেন\n📊 এনালিটিক্স দেখতে পারবেন\n🚌 বাস চালু করতে পারবেন",
                gradientColors = listOf(Success.copy(alpha = 0.8f), RouteGreen.copy(alpha = 0.6f))
            )
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
            OwnerFAQExpandableCard(
                question = "কয়টি বাস রেজিস্টার করতে পারব?",
                answer = "আপনি সীমাহীন সংখ্যক বাস রেজিস্টার করতে পারবেন। প্রতিটি বাসের জন্য আলাদা রুট এবং কন্ডাক্টর অ্যাসাইন করতে পারবেন।"
            )
        }

        item {
            OwnerFAQExpandableCard(
                question = "রেজিস্ট্রেশন ব্যর্থ হলে কি করব?",
                answer = "নিশ্চিত করুন যে সব ফিল্ড সঠিকভাবে পূরণ করেছেন এবং কমপক্ষে একটি ভাড়া সেট করেছেন। ইন্টারনেট সংযোগ চেক করুন। সমস্যা চলতে থাকলে সাপোর্টে যোগাযোগ করুন।"
            )
        }

        item {
            OwnerFAQExpandableCard(
                question = "রুট পরে পরিবর্তন করা যাবে?",
                answer = "বর্তমানে রুট পরিবর্তন সরাসরি সম্ভব নয়। নতুন রুট দিয়ে বাস পুনরায় রেজিস্টার করতে হবে। আমরা শীঘ্রই এডিট ফিচার যোগ করব।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
@Composable
fun BusManagementHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.List,
                iconColor = RouteGreen,
                title = "বাস তালিকা ব্যবস্থাপনা",
                items = listOf(
                    "১. ড্যাশবোর্ড থেকে 'আমার বাসসমূহ দেখুন' বোতামে ক্লিক করুন",
                    "২. আপনার সব রেজিস্টার করা বাস দেখতে পাবেন",
                    "৩. প্রতিটি বাসের বিস্তারিত তথ্য দেখতে পাবেন",
                    "৪. অ্যাসাইনড কন্ডাক্টর দেখতে পাবেন",
                    "৫. চলমান এবং আসন্ন শিডিউল দেখতে পাবেন",
                    "৬. বাস পারফরম্যান্স মনিটর করতে পারবেন"
                )
            )
        }

        // Bus Information Card
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
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = Info,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "প্রতিটি বাসে পাবেন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "🚌 বাসের নাম এবং নম্বর\n📜 ফিটনেস সার্টিফিকেট ও ট্যাক্স টোকেন\n📍 সব স্টপেজের তালিকা\n💰 সব রুটের ভাড়া তালিকা\n👤 অ্যাসাইনড কন্ডাক্টরের নাম\n📅 শিডিউল তালিকা\n📊 বাস পারফরম্যান্স ডেটা",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Bus Actions
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.Settings,
                iconColor = RoutePurple,
                title = "বাস সেটিংস ও অ্যাকশন",
                items = listOf(
                    "• 'কন্ডাক্টর অ্যাসাইন করুন' - নতুন কন্ডাক্টর নিয়োগ দিন",
                    "• ড্রপডাউন থেকে কন্ডাক্টর নির্বাচন করুন",
                    "• 'কোনোটি নেই' নির্বাচন করে কন্ডাক্টর সরাতে পারেন",
                    "• 'এনালিটিক্স রিপোর্ট' - বাসের পারফরম্যান্স দেখুন",
                    "• 'মুছুন' বোতামে ক্লিক করে বাস ডিলিট করুন",
                    "• ডিলিট করলে সব শিডিউল এবং অ্যাসাইনমেন্ট মুছে যাবে"
                )
            )
        }

        // Analytics Features
        item {
            GradientOwnerInfoCard(
                icon = Icons.Filled.Analytics,
                title = "এনালিটিক্স রিপোর্ট",
                message = "📈 মোট রাইড সংখ্যা\n💵 মোট আয়\n👥 মোট যাত্রী\n⭐ গড় রেটিং\n📊 মাসিক পারফরম্যান্স\n🎯 জনপ্রিয় রুট",
                gradientColors = listOf(RouteGreen.copy(alpha = 0.8f), Success.copy(alpha = 0.6f))
            )
        }

        // Conductor Assignment
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
                                Icons.Filled.Assignment,
                                contentDescription = null,
                                tint = RouteBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "কন্ডাক্টর অ্যাসাইনমেন্ট নিয়ম",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "✓ একজন কন্ডাক্টর একসময়ে শুধুমাত্র একটি বাসে কাজ করতে পারে\n✓ নতুন কন্ডাক্টর নির্বাচন করলে পুরানো অ্যাসাইনমেন্ট সরে যাবে\n✓ কন্ডাক্টর ছাড়া বাস চালু রাখতে পারবেন\n✓ যেকোনো সময় কন্ডাক্টর পরিবর্তন করতে পারবেন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
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
                            "সতর্কতা",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "বাস ডিলিট করলে তা পুনরুদ্ধার করা যাবে না। সব শিডিউল, রাইড হিস্ট্রি এবং অ্যাসাইনমেন্ট স্থায়ীভাবে মুছে যাবে। সতর্কতার সাথে ডিলিট করুন।",
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
            OwnerFAQExpandableCard(
                question = "একটি বাসে কয়জন কন্ডাক্টর থাকতে পারে?",
                answer = "একটি বাসে একসময়ে শুধুমাত্র একজন কন্ডাক্টর থাকতে পারে। তবে আপনি যেকোনো সময় কন্ডাক্টর পরিবর্তন করতে পারবেন।"
            )
        }

        item {
            OwnerFAQExpandableCard(
                question = "বাস ডিলিট করলে কি হবে?",
                answer = "বাস ডিলিট করলে সব শিডিউল, রাইড হিস্ট্রি, ভাড়া সেটিংস এবং কন্ডাক্টর অ্যাসাইনমেন্ট স্থায়ীভাবে মুছে যাবে। এটি পুনরুদ্ধার করা যাবে না।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
@Composable
fun ConductorManagementHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Registration Steps
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.PersonAdd,
                iconColor = RouteBlue,
                title = "কন্ডাক্টর নিবন্ধন করা",
                items = listOf(
                    "১. ড্যাশবোর্ড থেকে 'কন্ডাক্টর নিবন্ধন করুন' বোতামে ক্লিক করুন",
                    "২. কন্ডাক্টরের নাম লিখুন",
                    "৩. বৈধ ফোন নম্বর দিন (+8801XXXXXXXXX)",
                    "৪. কন্ডাক্টরের বয়স লিখুন (১৮-১০০)",
                    "৫. কন্ডাক্টরের ইমেইল ঠিকানা দিন",
                    "৬. কন্ডাক্টরের জন্য পাসওয়ার্ড তৈরি করুন",
                    "৭. আপনার নিজের পাসওয়ার্ড দিয়ে নিশ্চিত করুন",
                    "৮. 'নিবন্ধন করুন' বোতামে ক্লিক করুন"
                )
            )
        }

        // Security Info
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
                                .background(Success.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Security,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "নিরাপত্তা ও যাচাইকরণ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "🔒 শুধুমাত্র ওনাররা কন্ডাক্টর নিবন্ধন করতে পারেন\n🔐 আপনার পাসওয়ার্ড দিয়ে যাচাই করা হবে\n📧 কন্ডাক্টর যাচাই ইমেইল পাবে\n✅ কন্ডাক্টর স্বয়ংক্রিয়ভাবে আপনার সাথে সংযুক্ত হবে\n👤 কন্ডাক্টর তার নিজস্ব ড্যাশবোর্ড পাবে",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Conductor List Management
        item {
            GradientOwnerInfoCard(
                icon = Icons.Filled.Group,
                title = "কন্ডাক্টর তালিকা দেখা",
                message = "📋 'কন্ডাক্টর তালিকা দেখুন' থেকে সব কন্ডাক্টর দেখুন\n👤 প্রতিটি কন্ডাক্টরের নাম, ইমেইল, ফোন দেখুন\n🚌 কোন বাসে অ্যাসাইনড তা দেখুন\n📊 পারফরম্যান্স ডেটা দেখুন",
                gradientColors = listOf(RouteBlue.copy(alpha = 0.8f), Primary.copy(alpha = 0.6f))
            )
        }

        // Conductor Permissions
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.VerifiedUser,
                iconColor = Info,
                title = "কন্ডাক্টরের অনুমতি",
                items = listOf(
                    "✅ বাসের রাইড রিকোয়েস্ট দেখতে পারে",
                    "✅ রিকোয়েস্ট গ্রহণ বা প্রত্যাখ্যান করতে পারে",
                    "✅ যাত্রীদের সাথে চ্যাট করতে পারে",
                    "✅ OTP যাচাই করতে পারে",
                    "✅ লাইভ লোকেশন শেয়ার করে",
                    "✅ রাইড হিস্ট্রি দেখতে পারে",
                    "❌ বাস সেটিংস পরিবর্তন করতে পারে না",
                    "❌ ভাড়া পরিবর্তন করতে পারে না"
                )
            )
        }

        // Assignment Rules
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
                                .background(Warning.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Assignment,
                                contentDescription = null,
                                tint = Warning,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "অ্যাসাইনমেন্ট নিয়ম",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "🔹 একজন কন্ডাক্টর একসময়ে শুধুমাত্র একটি বাসে কাজ করতে পারে\n🔹 বাস তালিকা থেকে 'কন্ডাক্টর অ্যাসাইন করুন' বোতামে ক্লিক করুন\n🔹 নতুন কন্ডাক্টর নির্বাচন করলে পুরানো অ্যাসাইনমেন্ট সরে যাবে\n🔹 কন্ডাক্টর সরাতে 'কোনোটি নেই' নির্বাচন করুন\n🔹 অ্যাসাইনমেন্ট তাৎক্ষণিক কার্যকর হয়",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Performance Monitoring
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.Leaderboard,
                iconColor = RoutePurple,
                title = "পারফরম্যান্স মনিটরিং",
                items = listOf(
                    "• মোট রাইড সংখ্যা দেখুন",
                    "• গড় রেটিং চেক করুন",
                    "• যাত্রী সন্তুষ্টি মনিটর করুন",
                    "• রেসপন্স টাইম ট্র্যাক করুন",
                    "• মাসিক পারফরম্যান্স রিপোর্ট",
                    "• সেরা পারফরমার শনাক্ত করুন"
                )
            )
        }

        // Tips Card
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
                            "💡 বিশ্বস্ত ব্যক্তিদের কন্ডাক্টর করুন\n💡 নিয়মিত পারফরম্যান্স চেক করুন\n💡 যাত্রীর অভিযোগ গুরুত্বের সাথে নিন\n💡 ভালো পারফরমারদের উৎসাহিত করুন",
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
            OwnerFAQExpandableCard(
                question = "কন্ডাক্টর ডিলিট করা যাবে?",
                answer = "বর্তমানে কন্ডাক্টর ডিলিট করার অপশন নেই। তবে আপনি তাকে কোনো বাস থেকে অ্যাসাইন না করে রাখতে পারেন। আমরা শীঘ্রই এই ফিচার যোগ করব।"
            )
        }

        item {
            OwnerFAQExpandableCard(
                question = "কন্ডাক্টর অভিযোগ করলে কি করব?",
                answer = "কন্ডাক্টরের অভিযোগ গুরুত্বের সাথে নিন। প্রথমে যাচাই করুন, তারপর প্রয়োজনীয় ব্যবস্থা নিন। গুরুতর ক্ষেত্রে সাপোর্ট টিমে যোগাযোগ করুন।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
@Composable
fun FareManagementHelpContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fare Setting Steps
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.AttachMoney,
                iconColor = Warning,
                title = "ভাড়া সেট করার নিয়ম",
                items = listOf(
                    "১. বাস রেজিস্ট্রেশনের সময় 'ভাড়া যোগ করুন' বোতামে ক্লিক করুন",
                    "২. উৎস স্টপ নির্বাচন করুন (যেখান থেকে যাত্রী উঠবে)",
                    "৩. গন্তব্য স্টপ নির্বাচন করুন (যেখানে যাত্রী নামবে)",
                    "৪. ভাড়ার পরিমাণ টাকায় লিখুন",
                    "৫. 'যোগ করুন' বোতামে ক্লিক করুন",
                    "৬. প্রতিটি রুটের জন্য আলাদা ভাড়া সেট করুন"
                )
            )
        }

        // Fare Calculation
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
                                Icons.Filled.Calculate,
                                contentDescription = null,
                                tint = Info,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "ভাড়া কিভাবে কাজ করে",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        "💰 আপনার সেট করা ভাড়া রাইডারদের দেখানো হবে\n📊 যদি কোনো রুটের জন্য ভাড়া সেট না থাকে, ডিফল্ট ভাড়া গণনা হবে\n📏 ডিফল্ট: প্রতি কিলোমিটারে ১০ টাকা\n✖️ একাধিক সিটের জন্য ভাড়া গুণ করা হয়\n📋 ভাড়া তালিকা বাস তালিকায় দেখতে পাবেন\n🔄 যেকোনো সময় ভাড়া আপডেট করতে পারবেন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Fare Examples
        item {
            GradientOwnerInfoCard(
                icon = Icons.Filled.Receipt,
                title = "ভাড়া গণনার উদাহরণ",
                message = "📌 উদাহরণ ১: মিরপুর → মতিঝিল\n   • আপনি সেট করেছেন: ৫০ টাকা\n   • যাত্রী ২টি সিট চায়\n   • মোট ভাড়া: ৫০ × ২ = ১০০ টাকা\n\n📌 উদাহরণ ২: ভাড়া সেট নেই\n   • দূরত্ব: ১৫ কিমি\n   • ১টি সিট\n   • মোট ভাড়া: ১৫ × ১০ = ১৫০ টাকা",
                gradientColors = listOf(Warning.copy(alpha = 0.8f), Secondary.copy(alpha = 0.6f))
            )
        }

        // Fare Tips
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.TipsAndUpdates,
                iconColor = Success,
                title = "ভাড়া সেটিং টিপস",
                items = listOf(
                    "• দূরত্ব অনুযায়ী ভাড়া নির্ধারণ করুন",
                    "• সব সম্ভাব্য রুটের জন্য ভাড়া সেট করুন",
                    "• উৎস এবং গন্তব্য একই হতে পারবে না",
                    "• ভাড়া অবশ্যই ০ বা ০-এর বেশি হতে হবে",
                    "• প্রতিদ্বন্দ্বী বাসের ভাড়া চেক করুন",
                    "• যুক্তিসঙ্গত ভাড়া নির্ধারণ করুন"
                )
            )
        }

        // Important Notes
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Error.copy(alpha = 0.1f))
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
                            "গুরুত্বপূর্ণ নোট",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ কমপক্ষে একটি ভাড়া সেট না করলে বাস রেজিস্টার করতে পারবেন না\n⚠️ ভাড়া যথাসম্ভব নির্ভুল রাখুন\n⚠️ ঘন ঘন ভাড়া পরিবর্তন যাত্রীদের বিভ্রান্ত করতে পারে",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }

        // Pricing Strategy
        item {
            ModernOwnerHelpCard(
                icon = Icons.Filled.Psychology,
                iconColor = RoutePurple,
                title = "মূল্য নির্ধারণ কৌশল",
                items = listOf(
                    "📈 চাহিদা অনুযায়ী দাম সেট করুন",
                    "🚦 পিক আওয়ারে সামান্য বেশি চার্জ করতে পারেন",
                    "🎯 জনপ্রিয় রুটে প্রতিযোগিতামূলক মূল্য দিন",
                    "💎 প্রিমিয়াম সার্ভিসের জন্য বেশি চার্জ করুন",
                    "🎁 নিয়মিত যাত্রীদের ছাড় দিন (শীঘ্রই)",
                    "📊 মাসিক আয় বিশ্লেষণ করে দাম সমন্বয় করুন"
                )
            )
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
            OwnerFAQExpandableCard(
                question = "ভাড়া পরে পরিবর্তন করা যাবে?",
                answer = "হ্যাঁ, আপনি যেকোনো সময় বাস সেটিংস থেকে ভাড়া আপডেট করতে পারবেন। নতুন ভাড়া তাৎক্ষণিক কার্যকর হবে এবং পরবর্তী সব রাইডে প্রযোজ্য হবে।"
            )
        }

        item {
            OwnerFAQExpandableCard(
                question = "ডিফল্ট ভাড়া কিভাবে কাজ করে?",
                answer = "যদি আপনি নির্দিষ্ট রুটের জন্য ভাড়া সেট না করেন, তাহলে সিস্টেম স্বয়ংক্রিয়ভাবে দূরত্বের উপর ভিত্তি করে প্রতি কিলোমিটার ১০ টাকা হিসাবে ভাড়া গণনা করবে।"
            )
        }

        item {
            OwnerFAQExpandableCard(
                question = "একই রুটের জন্য ভিন্ন ভাড়া সেট করা যাবে?",
                answer = "না, একই উৎস এবং গন্তব্যের জন্য শুধুমাত্র একটি ভাড়া সেট করা যায়। নতুন ভাড়া সেট করলে পুরানো ভাড়া প্রতিস্থাপিত হবে।"
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ============= REUSABLE COMPONENTS =============

@Composable
fun ModernOwnerHelpCard(
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
                        !item.startsWith("১০") && !item.startsWith("১১") &&
                        !item.startsWith("✅") && !item.startsWith("❌") &&
                        !item.startsWith("✓") && !item.startsWith("🔹") &&
                        !item.startsWith("📈") && !item.startsWith("🚦") &&
                        !item.startsWith("🎯") && !item.startsWith("💎") &&
                        !item.startsWith("🎁") && !item.startsWith("📊") &&
                        !item.startsWith("📌")) {
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
fun GradientOwnerInfoCard(
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
fun OwnerFAQExpandableCard(
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
                        .background(RoutePurple.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.QuestionMark,
                        contentDescription = null,
                        tint = RoutePurple,
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
                    tint = RoutePurple
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