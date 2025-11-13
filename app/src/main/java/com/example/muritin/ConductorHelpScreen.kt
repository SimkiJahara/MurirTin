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
fun ConductorHelpScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("শিডিউল", "রিকোয়েস্ট", "লোকেশন আপডেট", "চ্যাট", "OTP যাচাই")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কন্ডাক্টর সহায়তা নির্দেশিকা") },
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
                0 -> ScheduleHelpContent()
                1 -> RequestManagementHelpContent()
                2 -> LocationUpdateHelpContent()
                3 -> ConductorChatHelpContent()
                4 -> OTPVerificationHelpContent()
            }
        }
    }
}

@Composable
fun ScheduleHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ConductorHelpSection(
            icon = Icons.Filled.Schedule,
            title = "শিডিউল তৈরি করা",
            steps = listOf(
                "১. ড্যাশবোর্ডে 'শিডিউল তৈরি করুন' বোতামে ক্লিক করুন",
                "২. তারিখ লিখুন (YYYY-MM-DD ফরম্যাটে)",
                "৩. শুরুর সময় লিখুন (HH:MM ফরম্যাটে, ২৪-ঘণ্টা)",
                "৪. শেষের সময় লিখুন (HH:MM ফরম্যাটে)",
                "৫. দিক নির্বাচন করুন: 'যাচ্ছি' বা 'ফিরছি'",
                "৬. 'সংরক্ষণ' বোতামে ক্লিক করুন"
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
                        Icons.Filled.DirectionsBus,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "দিক সম্পর্কে",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 'যাচ্ছি': শুরুর স্থান থেকে গন্তব্যের দিকে\n• 'ফিরছি': গন্তব্য থেকে শুরুর স্থানের দিকে\n• দিক অনুযায়ী রাইডারদের রিকোয়েস্ট দেখাবে\n• সঠিক দিক নির্বাচন করা গুরুত্বপূর্ণ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.Edit,
            title = "শিডিউল সম্পাদনা করা",
            steps = listOf(
                "• শিডিউল তালিকায় এডিট আইকনে ক্লিক করুন",
                "• শুধুমাত্র আসন্ন শিডিউল এডিট করতে পারবেন",
                "• শুরু হয়ে যাওয়া শিডিউল এডিট করা যাবে না",
                "• তারিখ, সময় এবং দিক পরিবর্তন করতে পারবেন",
                "• 'সংরক্ষণ' বোতামে ক্লিক করে আপডেট করুন"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.Delete,
            title = "শিডিউল মুছে ফেলা",
            steps = listOf(
                "• শিডিউল তালিকায় ডিলিট আইকনে ক্লিক করুন",
                "• শুধুমাত্র আসন্ন শিডিউল মুছতে পারবেন",
                "• নিশ্চিতকরণ ডায়ালগে 'মুছে ফেলুন' বোতামে ক্লিক করুন",
                "• মুছে ফেলা শিডিউল পুনরুদ্ধার করা যাবে না"
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
                    text = "শিডিউল তৈরির আগে অবশ্যই একটি বাস অ্যাসাইন করা থাকতে হবে",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun RequestManagementHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ConductorHelpSection(
            icon = Icons.Filled.NotificationsActive,
            title = "পেন্ডিং রিকোয়েস্ট দেখা",
            steps = listOf(
                "১. ড্যাশবোর্ড স্বয়ংক্রিয়ভাবে প্রতি ১৫ সেকেন্ডে রিফ্রেশ হয়",
                "২. পেন্ডিং রিকোয়েস্ট সেকশনে যাত্রীদের রিকোয়েস্ট দেখুন",
                "৩. প্রতিটি রিকোয়েস্টে পিকআপ, গন্তব্য, সিট এবং ভাড়া দেখানো হবে",
                "৪. শুধুমাত্র আপনার বর্তমান রুটের রিকোয়েস্ট দেখাবে",
                "৫. ৩০ মিনিটের মধ্যে পৌঁছানো সম্ভব এমন রিকোয়েস্ট দেখাবে"
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
                    text = "রিকোয়েস্ট ফিল্টারিং",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• শুধুমাত্র আপনার সক্রিয় শিডিউলের রিকোয়েস্ট দেখাবে\n• দিক অনুযায়ী ফিল্টার হয় (যাচ্ছি/ফিরছি)\n• পিকআপ পয়েন্ট ৩০ মিনিটের দূরত্বে হতে হবে\n• আপনার বাসের রুটের স্টপে থাকতে হবে",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.CheckCircle,
            title = "রিকোয়েস্ট গ্রহণ করা",
            steps = listOf(
                "১. পেন্ডিং রিকোয়েস্ট থেকে একটি নির্বাচন করুন",
                "২. বিস্তারিত তথ্য চেক করুন",
                "৩. 'অ্যাকসেপ্ট করুন' বোতামে ক্লিক করুন",
                "৪. স্বয়ংক্রিয়ভাবে একটি OTP তৈরি হবে",
                "৫. রিকোয়েস্ট 'অ্যাকসেপ্টেড' সেকশনে চলে যাবে"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.List,
            title = "অ্যাকসেপ্টেড রিকোয়েস্ট দেখা",
            steps = listOf(
                "• 'অ্যাকসেপ্টেড রিকোয়েস্টসমূহ' সেকশনে দেখুন",
                "• রাইডারের নাম এবং ফোন নম্বর দেখতে পাবেন",
                "• পিকআপ, গন্তব্য, সিট এবং ভাড়া দেখতে পাবেন",
                "• OTP কোড দেখতে পাবেন",
                "• রাইডারের সাথে চ্যাট করার অপশন পাবেন"
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
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "ম্যানুয়ালি রিফ্রেশ করতে উপরের রিফ্রেশ আইকনে ক্লিক করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun LocationUpdateHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ConductorHelpSection(
            icon = Icons.Filled.MyLocation,
            title = "লোকেশন আপডেট করা",
            steps = listOf(
                "১. Location permission দিতে হবে",
                "২. ড্যাশবোর্ডে 'লোকেশন আপডেট করুন' বোতামে ক্লিক করুন",
                "৩. আপনার বর্তমান অবস্থান সিস্টেমে সেভ হবে",
                "৪. রাইডাররা আপনার অবস্থান ট্র্যাক করতে পারবে",
                "৫. নিয়মিত আপডেট করুন (প্রতি ৫-১০ মিনিটে)"
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
                        text = "লোকেশনের গুরুত্ব",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• রাইডাররা লাইভ ট্র্যাকিং করতে পারে\n• পেন্ডিং রিকোয়েস্ট দেখার জন্য প্রয়োজন\n• নিকটবর্তী রিকোয়েস্ট খুঁজে পেতে সাহায্য করে\n• রাইডারদের আপনার আগমনের সময় জানায়",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.Security,
            title = "Location Permission দেওয়া",
            steps = listOf(
                "১. প্রথমবার ব্যবহারের সময় permission চাইবে",
                "২. 'Allow' বা 'অনুমতি দিন' বোতামে ক্লিক করুন",
                "৩. Settings থেকেও permission দিতে পারেন",
                "৪. 'While using the app' নির্বাচন করুন",
                "৫. Permission না দিলে অনেক ফিচার কাজ করবে না"
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
                    text = "Location permission ছাড়া পেন্ডিং রিকোয়েস্ট দেখতে পারবেন না",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.BatteryAlert,
            title = "ব্যাটারি সাশ্রয়ী টিপস",
            steps = listOf(
                "• প্রয়োজন অনুযায়ী লোকেশন আপডেট করুন",
                "• অহেতুক বার বার আপডেট করবেন না",
                "• যাত্রী পিকআপের আগে অবশ্যই আপডেট করুন",
                "• রাইড শেষে একবার আপডেট করুন",
                "• শিডিউল শেষ হলে আর আপডেট করার দরকার নেই"
            )
        )
    }
}

@Composable
fun ConductorChatHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ConductorHelpSection(
            icon = Icons.Filled.Chat,
            title = "রাইডারের সাথে চ্যাট করা",
            steps = listOf(
                "১. ড্যাশবোর্ড থেকে 'রাইডারদের সাথে চ্যাট করুন' বোতামে ক্লিক করুন",
                "২. সক্রিয় চ্যাটের তালিকা দেখতে পাবেন",
                "৩. একটি রাইডার নির্বাচন করুন",
                "৪. 'চ্যাট করুন' বোতামে ক্লিক করুন",
                "৫. টেক্সট বক্সে বার্তা লিখুন এবং 'Send' বোতামে ক্লিক করুন"
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
                    text = "চ্যাট কখন করবেন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• রাইডারের পিকআপ পয়েন্ট খুঁজতে সমস্যা হলে\n• বিলম্ব হলে জানাতে\n• পিকআপ স্থান পরিবর্তন করতে\n• ভাড়া বা অন্যান্য প্রশ্নের উত্তর দিতে\n• রাইডার কোথায় আছে জানতে",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.Schedule,
            title = "চ্যাট সময়সীমা",
            steps = listOf(
                "• শুধুমাত্র Accepted রিকোয়েস্টে চ্যাট করতে পারবেন",
                "• রাইড চলাকালীন চ্যাট উপলব্ধ",
                "• রাইড শেষের ৩ দিন (৭২ ঘণ্টা) পর্যন্ত চ্যাট করতে পারবেন",
                "• বাকি কত ঘণ্টা আছে তা দেখানো হবে",
                "• ৩ দিন পরে চ্যাট নিষ্ক্রিয় হয়ে যাবে"
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.List,
            title = "চ্যাট তালিকা দেখা",
            steps = listOf(
                "• 'রাইডারদের সাথে চ্যাট করুন' বোতামে ক্লিক করুন",
                "• সব সক্রিয় চ্যাট দেখতে পাবেন",
                "• রাইডারের নাম, ফোন এবং ট্রিপ তথ্য দেখাবে",
                "• চ্যাট উপলব্ধ থাকার সময় দেখাবে",
                "• রিফ্রেশ আইকনে ক্লিক করে আপডেট করুন"
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
                    text = "রাইডারদের সাথে সম্মানের সাথে কথা বলুন। অপমানজনক ভাষা ব্যবহার করবেন না।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun OTPVerificationHelpContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ConductorHelpSection(
            icon = Icons.Filled.Password,
            title = "OTP কি এবং কেন প্রয়োজন",
            steps = listOf(
                "• OTP = One Time Password",
                "• রিকোয়েস্ট Accept করলে স্বয়ংক্রিয়ভাবে তৈরি হয়",
                "• রাইডার যাচাই করার জন্য ব্যবহৃত হয়",
                "• প্রতিটি রিকোয়েস্টের জন্য আলাদা OTP থাকে",
                "• ৪ সংখ্যার কোড (যেমন: 1234)"
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
                        Icons.Filled.HowToReg,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "OTP যাচাই প্রক্রিয়া",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "১. রাইডার পিকআপ পয়েন্টে পৌঁছাবে\n২. রাইডার আপনাকে OTP বলবে\n৩. অ্যাকসেপ্টেড রিকোয়েস্টে OTP চেক করুন\n৪. OTP মিললে রাইডারকে বাসে উঠতে দিন\n৫. OTP না মিললে রাইডারকে সঠিক OTP জিজ্ঞাসা করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.Visibility,
            title = "OTP কোথায় পাবেন",
            steps = listOf(
                "• ড্যাশবোর্ডে 'অ্যাকসেপ্টেড রিকোয়েস্টসমূহ' সেকশনে",
                "• প্রতিটি রিকোয়েস্ট কার্ডে 'OTP: XXXX' দেখানো হবে",
                "• চ্যাট লিস্টেও OTP দেখতে পাবেন",
                "• লাইভ ট্র্যাকিং পেজে রাইডার OTP দেখতে পায়"
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
                    text = "নিরাপত্তা টিপস",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• OTP কারো সাথে শেয়ার করবেন না\n• শুধুমাত্র পিকআপ পয়েন্টে যাচাই করুন\n• ভুল OTP দিলে বাসে উঠতে দেবেন না\n• OTP যাচাই না করে টিকিট কাটবেন না\n• সন্দেহ হলে রাইডারের নাম এবং ফোন নম্বর চেক করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ConductorHelpSection(
            icon = Icons.Filled.Error,
            title = "সাধারণ সমস্যা এবং সমাধান",
            steps = listOf(
                "সমস্যা: রাইডার OTP মনে করতে পারছে না",
                "সমাধান: রাইডারকে তার ফোনে 'আমার রিকোয়েস্ট' বা 'লাইভ ট্র্যাকিং' দেখতে বলুন",
                "",
                "সমস্যা: OTP মিলছে না",
                "সমাধান: নাম এবং পিকআপ/গন্তব্য চেক করুন, অন্য রিকোয়েস্ট হতে পারে",
                "",
                "সমস্যা: একাধিক রাইডারের একই পিকআপ পয়েন্ট",
                "সমাধান: প্রতিটি রাইডারের আলাদা OTP চেক করুন"
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
                    text = "OTP যাচাই না করে যাত্রী উঠালে আপনি দায়ী থাকবেন এবং সমস্যা হতে পারে",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ConductorHelpSection(
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