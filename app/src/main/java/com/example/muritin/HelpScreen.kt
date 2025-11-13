package com.example.muritin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("লগইন", "নিবন্ধন", "পাসওয়ার্ড রিসেট")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সহায়তা নির্দেশিকা") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "ফিরে যান")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> LoginHelpContent()
                1 -> SignupHelpContent()
                2 -> PasswordResetHelpContent()
            }
        }
    }
}

@Composable
fun LoginHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HelpSection(
            icon = Icons.Filled.Login,
            title = "লগইন কিভাবে করবেন",
            steps = listOf(
                "১. লগইন স্ক্রিনে যান",
                "২. আপনার নিবন্ধিত ইমেইল ঠিকানা লিখুন",
                "৩. আপনার পাসওয়ার্ড লিখুন (কমপক্ষে ৬ অক্ষর)",
                "৪. 'লগইন' বোতামে ক্লিক করুন"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        HelpSection(
            icon = Icons.Filled.Warning,
            title = "গুরুত্বপূর্ণ তথ্য",
            steps = listOf(
                "• লগইন করার আগে আপনার ইমেইল যাচাই করতে হবে",
                "• নিবন্ধনের পর আপনার ইমেইলে একটি যাচাই লিঙ্ক পাঠানো হয়",
                "• যাচাই না করে লগইন করলে ত্রুটি দেখাবে",
                "• ইমেইল বা পাসওয়ার্ড ভুল হলে পুনরায় চেষ্টা করুন"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "প্রথমবার? নিচে 'অ্যাকাউন্ট নেই? এখানে একটি তৈরি করুন' লিঙ্কে ক্লিক করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun SignupHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HelpSection(
            icon = Icons.Filled.PersonAdd,
            title = "নতুন অ্যাকাউন্ট তৈরি করুন",
            steps = listOf(
                "১. লগইন স্ক্রিনে 'অ্যাকাউন্ট নেই? এখানে একটি তৈরি করুন' ক্লিক করুন",
                "২. আপনার পুরো নাম লিখুন",
                "৩. বৈধ বাংলাদেশী ফোন নম্বর দিন (+8801XXXXXXXXX)",
                "৪. আপনার বয়স লিখুন (১৮-১০০)",
                "৫. বৈধ ইমেইল ঠিকানা দিন",
                "৬. একটি শক্তিশালী পাসওয়ার্ড তৈরি করুন (কমপক্ষে ৬ অক্ষর)",
                "৭. আপনার ভূমিকা নির্বাচন করুন (রাইডার/কন্ডাক্টর/ওনার)",
                "৮. 'নিবন্ধন করুন' বোতামে ক্লিক করুন"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        HelpSection(
            icon = Icons.Filled.Groups,
            title = "ভূমিকা সম্পর্কে",
            steps = listOf(
                "রাইডার: বাস সেবা ব্যবহারকারী যাত্রী",
                "কন্ডাক্টর: বাসে টিকিট পরিচালনাকারী (শুধুমাত্র ওনার নিবন্ধন করতে পারেন)",
                "ওনার: বাসের মালিক যিনি কন্ডাক্টর নিয়োগ করতে পারেন"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "নিবন্ধনের পর কি করবেন?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "১. আপনার ইমেইল চেক করুন\n২. যাচাই ইমেইলের লিঙ্কে ক্লিক করুন\n৩. তারপর লগইন করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HelpSection(
            icon = Icons.Filled.Error,
            title = "সাধারণ সমস্যা",
            steps = listOf(
                "• ফোন নম্বর অবশ্যই +880 দিয়ে শুরু হতে হবে",
                "• ইমেইল বৈধ ফরম্যাটে হতে হবে (example@domain.com)",
                "• পাসওয়ার্ড কমপক্ষে ৬ অক্ষর লম্বা হতে হবে",
                "• বয়স ১৮ থেকে ১০০ এর মধ্যে হতে হবে"
            )
        )
    }
}

@Composable
fun PasswordResetHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HelpSection(
            icon = Icons.Filled.LockReset,
            title = "পাসওয়ার্ড ভুলে গেলে",
            steps = listOf(
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

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "গুরুত্বপূর্ণ সতর্কতা",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• রিসেট লিঙ্ক সীমিত সময়ের জন্য বৈধ\n• লিঙ্ক শুধুমাত্র একবার ব্যবহার করা যাবে\n• ইমেইল না পেলে স্প্যাম ফোল্ডার চেক করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HelpSection(
            icon = Icons.Filled.Security,
            title = "নিরাপত্তা টিপস",
            steps = listOf(
                "• শক্তিশালী পাসওয়ার্ড ব্যবহার করুন",
                "• পাসওয়ার্ড কারো সাথে শেয়ার করবেন না",
                "• নিয়মিত পাসওয়ার্ড পরিবর্তন করুন",
                "• বিভিন্ন সেবায় ভিন্ন পাসওয়ার্ড ব্যবহার করুন"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "এখনও সমস্যা হচ্ছে? সাপোর্ট টিমের সাথে যোগাযোগ করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun HelpSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    steps: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            steps.forEach { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}