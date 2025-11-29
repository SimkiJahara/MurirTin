package com.example.muritin

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Money
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.BackgroundLight
import com.example.muritin.ui.theme.Divider
import com.example.muritin.ui.theme.Error
import com.example.muritin.ui.theme.Primary
import com.example.muritin.ui.theme.PrimaryLight
import com.example.muritin.ui.theme.RouteBlue
import com.example.muritin.ui.theme.RouteGreen
import com.example.muritin.ui.theme.RouteOrange
import com.example.muritin.ui.theme.RoutePurple
import com.example.muritin.ui.theme.Secondary
import com.example.muritin.ui.theme.TextPrimary
import com.example.muritin.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorBusInfo(navController: NavHostController, user: FirebaseUser) {

    val scope = rememberCoroutineScope()
    val database =
        FirebaseDatabase.getInstance("https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/")
    var assignedBus by remember { mutableStateOf<Bus?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.uid) {
        try {
            // 1. Get assigned bus
            val snapshot = database.getReference("busAssignments")
                .orderByChild("conductorId")
                .equalTo(user.uid)
                .get()
                .await()

            val busId = snapshot.children.firstOrNull()?.key
            if (busId != null) {
                assignedBus = AuthRepository().getBus(busId)
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
            error = "বাস পাওয়া ব্যর্থ: ${e.message}"
            scope.launch { snackbarHostState.showSnackbar(error ?: "অজানা ত্রুটি") }
            Log.e("ConductorBusInfo", "Bus Fetching failed: ${e.message}", e)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Custom Top Bar with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Primary, PrimaryLight)
                        )
                    )
                    .padding(top = 30.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = { navController.navigate("conductor_dashboard") },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsBus,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "বাসের তথ্য",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bus Information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
                    .padding(top = 20.dp)
            ) {
                if (isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                } else if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Error.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error ?: "অজানা ত্রুটি",
                                color = Error,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // Account Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {

                            InfoItem(
                                icon = Icons.Outlined.DirectionsBus,
                                label = "বাসের নাম",
                                value = assignedBus!!.name,
                                iconColor = RouteBlue
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                thickness = DividerDefaults.Thickness,
                                color = Divider
                            )

                            InfoItem(
                                icon = Icons.Outlined.DocumentScanner,
                                label = "বাসের লাইসেন্স নম্বর",
                                value = assignedBus!!.number,
                                iconColor = RouteOrange
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                thickness = DividerDefaults.Thickness,
                                color = Divider
                            )

                            InfoItem(
                                icon = Icons.Outlined.StopCircle,
                                label = "বাসের স্টপস",
                                value = assignedBus!!.stops.joinToString(", "),
                                iconColor = RouteGreen
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                thickness = DividerDefaults.Thickness,
                                color = Divider
                            )

                            InfoItem(
                                icon = Icons.Outlined.Money,
                                label = "ভাড়ার তালিকা",
                                value = "স্টপ ভিত্তিক ভাড়া",
                                iconColor = RoutePurple
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            assignedBus!!.fares.forEach { (fromStop, destMap) ->

                                // Origin Stop Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(RoutePurple.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Route,
                                            contentDescription = null,
                                            tint = RoutePurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "$fromStop থেকে:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }

                                // Destination → Fare Items
                                Column(
                                    modifier = Modifier.padding(start = 46.dp)  // aligns with icon
                                ) {
                                    destMap.forEach { (dest, fare) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            Icon(
                                                imageVector = Icons.Outlined.ArrowForward,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = "$dest — $fare টাকা",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Action Buttons
                    ModernActionButton(
                        icon = Icons.Outlined.Edit,
                        text = "পরিসংখ্যান রিপোর্ট দেখুন",
                        onClick = { navController.navigate("analytics_report/${assignedBus!!.busId}") },
                        backgroundColor = Secondary
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

