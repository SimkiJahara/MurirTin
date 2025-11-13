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
fun OwnerHelpScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("বাস রেজিস্ট্রেশন", "বাস পরিচালনা", "কন্ডাক্টর", "ভাড়া ব্যবস্থাপনা")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ওনার সহায়তা নির্দেশিকা") },
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
                0 -> BusRegistrationHelpContent()
                1 -> BusManagementHelpContent()
                2 -> ConductorManagementHelpContent()
                3 -> FareManagementHelpContent()
            }
        }
    }
}

@Composable
fun BusRegistrationHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OwnerHelpSection(
            icon = Icons.Filled.DirectionsBus,
            title = "বাস রেজিস্ট্রেশন করার ধাপসমূহ",
            steps = listOf(
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
                        Icons.Filled.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "রুট সেটআপ সম্পর্কে",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• সার্চ বক্সে টাইপ করে অথবা ম্যাপে ক্লিক করে স্থান নির্বাচন করুন\n• প্রতিটি স্টপ আলাদাভাবে যোগ করতে হবে\n• 'আরও একটি স্টপেজ যোগ করুন' বোতাম দিয়ে একাধিক স্টপ যোগ করুন\n• রুট সম্পূর্ণ হলে 'রুট যোগ করুন' বোতামে ক্লিক করুন\n• নীল লাইন আপনার রুট দেখাবে",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OwnerHelpSection(
            icon = Icons.Filled.Warning,
            title = "গুরুত্বপূর্ণ তথ্য",
            steps = listOf(
                "• সব ফিল্ড অবশ্যই পূরণ করতে হবে",
                "• কমপক্ষে একটি ভাড়া সেট করতেই হবে",
                "• রুট বাতিল করতে 'রুট বাতিল করুন' বোতামে ক্লিক করুন",
                "• ম্যাপে ভায়োলেট মার্কার শুরু এবং গন্তব্য দেখায়",
                "• লাল মার্কার স্টপেজ দেখায়",
                "• Location permission প্রয়োজন"
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
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "রেজিস্ট্রেশনের পর বাস তালিকায় দেখতে পাবেন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun BusManagementHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OwnerHelpSection(
            icon = Icons.Filled.List,
            title = "বাস তালিকা ব্যবস্থাপনা",
            steps = listOf(
                "১. ড্যাশবোর্ড থেকে 'আমার বাসসমূহ দেখুন' বোতামে ক্লিক করুন",
                "২. আপনার সব রেজিস্টার করা বাস দেখতে পাবেন",
                "৩. প্রতিটি বাসের বিস্তারিত তথ্য দেখতে পাবেন",
                "৪. অ্যাসাইনড কন্ডাক্টর দেখতে পাবেন",
                "৫. চলমান এবং আসন্ন শিডিউল দেখতে পাবেন"
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
                    text = "প্রতিটি বাসে পাবেন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• বাসের নাম এবং নম্বর\n• ফিটনেস সার্টিফিকেট ও ট্যাক্স টোকেন\n• সব স্টপেজের তালিকা\n• সব রুটের ভাড়া তালিকা\n• অ্যাসাইনড কন্ডাক্টরের নাম\n• শিডিউল তালিকা",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OwnerHelpSection(
            icon = Icons.Filled.Settings,
            title = "বাস সেটিংস",
            steps = listOf(
                "• 'কন্ডাক্টর অ্যাসাইন করুন' - নতুন কন্ডাক্টর নিয়োগ দিন",
                "• ড্রপডাউন থেকে কন্ডাক্টর নির্বাচন করুন",
                "• 'কোনোটি নেই' নির্বাচন করে কন্ডাক্টর সরাতে পারেন",
                "• 'মুছুন' বোতামে ক্লিক করে বাস ডিলিট করুন",
                "• ডিলিট করলে সব শিডিউল এবং অ্যাসাইনমেন্ট মুছে যাবে"
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
                    text = "বাস ডিলিট করলে তা পুনরুদ্ধার করা যাবে না। সতর্কতার সাথে ডিলিট করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun ConductorManagementHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OwnerHelpSection(
            icon = Icons.Filled.PersonAdd,
            title = "কন্ডাক্টর নিবন্ধন করা",
            steps = listOf(
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
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "নিরাপত্তা",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• শুধুমাত্র ওনাররা কন্ডাক্টর নিবন্ধন করতে পারেন\n• আপনার পাসওয়ার্ড দিয়ে যাচাই করা হবে\n• কন্ডাক্টর যাচাই ইমেইল পাবে\n• কন্ডাক্টর স্বয়ংক্রিয়ভাবে আপনার সাথে সংযুক্ত হবে",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OwnerHelpSection(
            icon = Icons.Filled.Group,
            title = "কন্ডাক্টর তালিকা দেখা",
            steps = listOf(
                "১. ড্যাশবোর্ড থেকে 'কন্ডাক্টর তালিকা দেখুন' বোতামে ক্লিক করুন",
                "২. আপনার সব নিবন্ধিত কন্ডাক্টর দেখতে পাবেন",
                "৩. প্রতিটি কন্ডাক্টরের নাম, ইমেইল, ফোন এবং বয়স দেখতে পাবেন",
                "৪. এই তালিকা থেকে বাসে অ্যাসাইন করতে পারবেন"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OwnerHelpSection(
            icon = Icons.Filled.Assignment,
            title = "কন্ডাক্টর অ্যাসাইনমেন্ট",
            steps = listOf(
                "• একজন কন্ডাক্টর একসময়ে শুধুমাত্র একটি বাসে কাজ করতে পারে",
                "• বাস তালিকা থেকে 'কন্ডাক্টর অ্যাসাইন করুন' বোতামে ক্লিক করুন",
                "• নতুন কন্ডাক্টর নির্বাচন করলে পুরানো অ্যাসাইনমেন্ট সরে যাবে",
                "• কন্ডাক্টর সরাতে 'কোনোটি নেই' নির্বাচন করুন"
            )
        )
    }
}

@Composable
fun FareManagementHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OwnerHelpSection(
            icon = Icons.Filled.AttachMoney,
            title = "ভাড়া সেট করার নিয়ম",
            steps = listOf(
                "১. বাস রেজিস্ট্রেশনের সময় 'ভাড়া যোগ করুন' বোতামে ক্লিক করুন",
                "২. উৎস স্টপ নির্বাচন করুন (যেখান থেকে যাত্রী উঠবে)",
                "৩. গন্তব্য স্টপ নির্বাচন করুন (যেখানে যাত্রী নামবে)",
                "৪. ভাড়ার পরিমাণ টাকায় লিখুন",
                "৫. 'যোগ করুন' বোতামে ক্লিক করুন",
                "৬. প্রতিটি রুটের জন্য আলাদা ভাড়া সেট করুন"
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
                    text = "ভাড়া সেটিং টিপস",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• দূরত্ব অনুযায়ী ভাড়া নির্ধারণ করুন\n• সব সম্ভাব্য রুটের জন্য ভাড়া সেট করুন\n• উৎস এবং গন্তব্য একই হতে পারবে না\n• ভাড়া অবশ্যই ০ বা ০-এর বেশি হতে হবে",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OwnerHelpSection(
            icon = Icons.Filled.Info,
            title = "ভাড়া কিভাবে কাজ করে",
            steps = listOf(
                "• আপনার সেট করা ভাড়া রাইডারদের দেখানো হবে",
                "• যদি কোনো রুটের জন্য ভাড়া সেট না থাকে, দূরত্ব অনুযায়ী ডিফল্ট ভাড়া গণনা হবে",
                "• ডিফল্ট: প্রতি কিলোমিটারে ১০ টাকা",
                "• একাধিক সিটের জন্য ভাড়া গুণ করা হয়",
                "• ভাড়া তালিকা বাস তালিকায় দেখতে পাবেন"
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
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "কমপক্ষে একটি ভাড়া সেট না করলে বাস রেজিস্টার করতে পারবেন না",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OwnerHelpSection(
            icon = Icons.Filled.Calculate,
            title = "ভাড়া গণনার উদাহরণ",
            steps = listOf(
                "উদাহরণ ১: মিরপুর থেকে মতিঝিল",
                "• আপনি সেট করেছেন: ৫০ টাকা",
                "• যাত্রী ২টি সিট চায়",
                "• মোট ভাড়া: ৫০ × ২ = ১০০ টাকা",
                "",
                "উদাহরণ ২: ভাড়া সেট নেই",
                "• দূরত্ব: ১৫ কিমি",
                "• ১টি সিট",
                "• মোট ভাড়া: ১৫ × ১০ = ১৫০ টাকা"
            )
        )
    }
}

@Composable
private fun OwnerHelpSection(
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
                if (step.isNotEmpty()) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}