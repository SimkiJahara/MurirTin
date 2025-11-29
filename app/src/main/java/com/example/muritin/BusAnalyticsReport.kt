package com.example.muritin

import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.muritin.ui.theme.BackgroundLight
import com.example.muritin.ui.theme.Error
import com.example.muritin.ui.theme.Primary
import com.example.muritin.ui.theme.PrimaryLight
import com.example.muritin.ui.theme.PrimaryVariant
import com.example.muritin.ui.theme.SecondaryLight
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAnalyticsReport(
    navController: NavHostController,
    busId: String
) {
    var dailyReport by remember { mutableStateOf<Map<Date, Pair<Int, Double>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    var bus: Bus? by remember { mutableStateOf(Bus()) }
    var error by remember { mutableStateOf<String?>(null) }
    var userData by remember { mutableStateOf<User?>(null) }

    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: ""
    val scrollState = rememberScrollState()

    // Load user data (including role)
    LaunchedEffect(uid) {
        val result = AuthRepository().getUser(uid)
        if (result.isSuccess) {
            userData = result.getOrNull()
            isLoading = false
        } else {
            error = "তথ্য উদ্ধার সম্ভব হয়নি"
            isLoading = false
        }
    }

    LaunchedEffect(busId) {
        isLoading = true
        try {
            bus = AuthRepository().getBus(busId)
            if (userData?.role == "Owner") {
                dailyReport = AuthRepository().getAnalyticForBus(busId)
            } else if (userData?.role == "Conductor") {
                dailyReport = AuthRepository().getAnalyticForBusConductor(busId)
            }
        } catch (e: Exception) {
            isLoading = false
            error = "তথ্য উদ্ধার সম্ভব হয়নি"
            Log.e("Analytics", "Load report failed: ${e.message}")
        } finally {
            isLoading = false
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
                if (userData?.role == "Conductor"){
                    IconButton(
                        onClick = { navController.navigate("conductor_assignedbus_info") },
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                } else if (userData?.role == "Owner") {
                    IconButton(
                        onClick = { navController.navigate("bus_list") },
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = Primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "পরিসংখ্যান রিপোর্ট",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }

            // Content
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
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
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
                    // Analytics barchart
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "বাস: ${bus?.name ?: "অজানা"}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (userData?.role == "Conductor") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "গত ৩ দিনের রিপোর্ট",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (dailyReport.isEmpty()) {

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.3f
                                        )
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(40.dp)
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "কোনো তথ্য নেই",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            } else {

                                val sortedDates = dailyReport.keys.sorted()

                                // Requests Chart
                                val reqEntries = sortedDates.mapIndexed { index, date ->
                                    BarEntry(
                                        index.toFloat(),
                                        dailyReport[date]?.first?.toFloat() ?: 0f
                                    )
                                }

                                SimpleBarChart(
                                    entries = reqEntries,
                                    label = "রিকোয়েস্ট",
                                    color = PrimaryVariant,
                                    dates = sortedDates
                                )

                                Spacer(Modifier.height(16.dp))

                                // Income Chart
                                val incEntries = sortedDates.mapIndexed { index, date ->
                                    BarEntry(
                                        index.toFloat(),
                                        dailyReport[date]?.second?.toFloat() ?: 0f
                                    )
                                }

                                SimpleBarChart(
                                    entries = incEntries,
                                    label = "উপার্জন (টাকা)",
                                    color = SecondaryLight,
                                    dates = sortedDates
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
// Simple Bar Chart with X-axis dates
@Composable
fun SimpleBarChart(
    entries: List<BarEntry>,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    dates: List<Date>
) {
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setDrawGridBackground(false)

                // X-axis with dates
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                xAxis.valueFormatter = object : ValueFormatter() {
                    private val sdf = java.text.SimpleDateFormat("MMM d", Locale.getDefault())
                    override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                        val index = value.toInt()
                        return dates.getOrNull(index)?.let { sdf.format(it) } ?: ""
                    }
                }

                axisLeft.setDrawGridLines(false)
                axisRight.isEnabled = false
                legend.isEnabled = true
            }
        },
        update = { chart ->
            val androidColor = color.toArgb()

            val dataSet = BarDataSet(entries, label).apply {
                this.color = androidColor
                valueTextColor = androidColor
                valueTextSize = 12f
            }

            val barData = BarData(dataSet).apply {
                barWidth = 0.6f
            }

            chart.data = barData
            chart.invalidate()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    )
}