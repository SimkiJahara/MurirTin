package com.example.muritin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
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
fun RiderHelpScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("ট্রিপ রিকোয়েস্ট", "আমার রিকোয়েস্ট", "লাইভ ট্র্যাকিং", "পূর্ববর্তী যাত্রা", "চ্যাট")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("রাইডার সহায়তা নির্দেশিকা") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
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
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> TripRequestHelpContent()
                1 -> MyRequestsHelpContent()
                2 -> LiveTrackingHelpContent()
                3 -> PastTripsHelpContent()
                4 -> ChatHelpContent()
            }
        }
    }
}

@Composable
fun TripRequestHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        RiderHelpSection(
            icon = Icons.Filled.DirectionsBus,
            title = "ট্রিপ রিকোয়েস্ট করার ধাপসমূহ",
            steps = listOf(
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

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "নিকটবর্তী স্টপ সম্পর্কে",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• ২.৫ কিমি ব্যাসার্ধের মধ্যে বাস স্টপ খুঁজে পাবেন\n• নীল মার্কার: সব উপলব্ধ স্টপ\n• সবুজ মার্কার: আপনার নির্বাচিত স্টপ\n• দূরত্ব কিলোমিটারে দেখানো হয়",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RiderHelpSection(
            icon = Icons.Filled.Warning,
            title = "গুরুত্বপূর্ণ নোট",
            steps = listOf(
                "• শুধুমাত্র বাস রুটের স্টপ থেকে স্টপে যেতে পারবেন",
                "• পিকআপ এবং গন্তব্য উভয়ই অবশ্যই নির্বাচন করতে হবে",
                "• ভাড়া দূরত্ব এবং সিট সংখ্যার উপর নির্ভর করে",
                "• কিছু বাসের জন্য কাস্টম ভাড়া থাকতে পারে",
                "• Location permission দিতে হবে"
            )
        )
    }
}

@Composable
fun MyRequestsHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        RiderHelpSection(
            icon = Icons.Filled.List,
            title = "আমার রিকোয়েস্ট পেজ",
            steps = listOf(
                "১. এখানে আপনার সব রিকোয়েস্ট দেখতে পাবেন",
                "২. প্রতি ৩ সেকেন্ডে স্বয়ংক্রিয়ভাবে রিফ্রেশ হয়",
                "৩. ডানদিকের রিফ্রেশ আইকনে ক্লিক করে অটো-রিফ্রেশ বন্ধ/চালু করুন",
                "৪. প্রতিটি রিকোয়েস্টের স্ট্যাটাস দেখতে পাবেন"
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
                Text(
                    text = "রিকোয়েস্ট স্ট্যাটাস",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pending: কন্ডাক্টর এখনো গ্রহণ করেনি\nAccepted: কন্ডাক্টর গ্রহণ করেছে\nCancelled: বাতিল করা হয়েছে",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RiderHelpSection(
            icon = Icons.Filled.Cancel,
            title = "রিকোয়েস্ট বাতিল করা",
            steps = listOf(
                "• শুধুমাত্র 'Pending' স্ট্যাটাসের রিকোয়েস্ট বাতিল করতে পারবেন",
                "• 'বাতিল করুন' বোতামে ক্লিক করুন",
                "• একবার বাতিল করলে আর ফিরিয়ে আনা যাবে না",
                "• Accepted রিকোয়েস্ট বাতিল করা যাবে না"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        RiderHelpSection(
            icon = Icons.Filled.Info,
            title = "Accepted রিকোয়েস্টে পাবেন",
            steps = listOf(
                "• বাসের নাম এবং নম্বর",
                "• কন্ডাক্টরের নাম এবং ফোন নম্বর",
                "• OTP কোড (বোর্ডিং এর সময় দিতে হবে)",
                "• লাইভ ট্র্যাকিং বোতাম",
                "• কন্ডাক্টরের সাথে চ্যাট করার অপশন"
            )
        )
    }
}

@Composable
fun LiveTrackingHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        RiderHelpSection(
            icon = Icons.Filled.Navigation,
            title = "লাইভ ট্র্যাকিং ব্যবহার করা",
            steps = listOf(
                "১. 'আমার রিকোয়েস্ট' থেকে Accepted রিকোয়েস্টে ক্লিক করুন",
                "২. 'লাইভ ট্র্যাকিং' বোতামে ক্লিক করুন",
                "৩. ম্যাপে আপনার পিকআপ এবং গন্তব্য দেখতে পাবেন",
                "৪. কন্ডাক্টরের বর্তমান অবস্থান দেখতে পাবেন",
                "৫. প্রতি ৫ সেকেন্ডে স্বয়ংক্রিয়ভাবে আপডেট হয়"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "ম্যাপ মার্কার",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• আপনার পিকআপ পয়েন্ট\n• আপনার গন্তব্য পয়েন্ট\n• কন্ডাক্টরের বর্তমান অবস্থান (চলমান)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RiderHelpSection(
            icon = Icons.Filled.PhoneAndroid,
            title = "OTP এর গুরুত্ব",
            steps = listOf(
                "• লাইভ ট্র্যাকিং পেজে OTP দেখানো হবে",
                "• বাসে উঠার সময় কন্ডাক্টরকে এই OTP বলুন",
                "• কন্ডাক্টর যাচাই করার পর আপনি বোর্ড করতে পারবেন",
                "• OTP কারো সাথে শেয়ার করবেন না",
                "• প্রতিটি রিকোয়েস্টের জন্য আলাদা OTP থাকে"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "কন্ডাক্টরের অবস্থান দেখতে না পেলে পেজ রিফ্রেশ করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun PastTripsHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        RiderHelpSection(
            icon = Icons.Filled.History,
            title = "পূর্ববর্তী যাত্রা দেখা",
            steps = listOf(
                "১. ড্যাশবোর্ড থেকে 'পূর্ববর্তী যাত্রাসমূহ' বোতামে ক্লিক করুন",
                "২. আপনার সব সম্পন্ন যাত্রা দেখতে পাবেন",
                "৩. প্রতিটি যাত্রার বিস্তারিত তথ্য দেখতে পাবেন",
                "৪. যাত্রা শেষের সময় দেখানো হবে"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "প্রতিটি যাত্রায় পাবেন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• পিকআপ এবং গন্তব্য\n• ভাড়া এবং সিট সংখ্যা\n• বাসের নাম ও নম্বর\n• কন্ডাক্টরের নাম ও ফোন\n• ব্যবহৃত OTP\n• যাত্রা শেষের সময়",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RiderHelpSection(
            icon = Icons.Filled.Schedule,
            title = "চ্যাট সময়সীমা",
            steps = listOf(
                "• যাত্রা শেষের পর ৩ দিন (৭২ ঘন্টা) চ্যাট করতে পারবেন",
                "• বাকি কত ঘন্টা আছে তা দেখানো হবে",
                "• ৩ দিন পরে চ্যাট বোতাম নিষ্ক্রিয় হয়ে যাবে",
                "• প্রয়োজনে এই সময়ের মধ্যে যোগাযোগ করুন"
            )
        )
    }
}

@Composable
fun ChatHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        RiderHelpSection(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "কন্ডাক্টরের সাথে চ্যাট",
            steps = listOf(
                "১. 'আমার রিকোয়েস্ট' বা 'পূর্ববর্তী যাত্রা' থেকে চ্যাট করুন",
                "২. 'Chat with Conductor' বোতামে ক্লিক করুন",
                "৩. নিচের টেক্সট বক্সে আপনার বার্তা লিখুন",
                "৪. 'Send' বোতামে ক্লিক করে পাঠান",
                "৫. কন্ডাক্টরের উত্তর স্বয়ংক্রিয়ভাবে দেখতে পাবেন"
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
                        Icons.Filled.QuestionAnswer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "চ্যাট কখন ব্যবহার করবেন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• বাসের অবস্থান জানতে\n• বিলম্বের কারণ জানতে\n• বিশেষ নির্দেশনা জানতে\n• ভুলে যাওয়া জিনিস সম্পর্কে\n• ভাড়া বা অন্যান্য প্রশ্ন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RiderHelpSection(
            icon = Icons.Filled.Info,
            title = "চ্যাট সম্পর্কে জানুন",
            steps = listOf(
                "• শুধুমাত্র Accepted রিকোয়েস্টে চ্যাট করতে পারবেন",
                "• রাইড চলাকালীন চ্যাট উপলব্ধ",
                "• রাইড শেষের ৩ দিন পর্যন্ত চ্যাট করতে পারবেন",
                "• আপনার বার্তা ডান দিকে দেখাবে",
                "• কন্ডাক্টরের বার্তা বাম দিকে দেখাবে"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "অপমানজনক ভাষা বা আচরণ করবেন না। সবার সাথে সম্মানের সাথে কথা বলুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun RiderHelpSection(
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