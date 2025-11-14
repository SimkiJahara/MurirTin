package com.example.muritin

import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*

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
    var userData by remember { mutableStateOf<User?>(null) }

    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: ""

    // Load user data (including role)
    LaunchedEffect(uid) {
        val result = AuthRepository().getUser(uid)
        if (result.isSuccess) {
            userData = result.getOrNull()
            isLoading = false
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(busId) {
        isLoading = true
        try {
            bus = AuthRepository().getBus(busId)
            if(userData?.role == "Owner") {
                dailyReport = AuthRepository().getAnalyticForBus(busId)
            } else if (userData?.role == "Conductor"){
                dailyReport = AuthRepository().getAnalyticForBusConductor(busId)
            }
        } catch (e: Exception) {
            Log.e("Analytics", "Load report failed: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("পরিসংখ্যান রিপোর্ট", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "বাস: "+ bus?.name,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            if(userData?.role == "Conductor"){
                Text(
                    text = "গত ৩ দিনের রিপোর্ট",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (dailyReport.isEmpty()) {
                Text(
                    text = "কোনো তথ্য নেই",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                // Get sorted dates
                val sortedDates = dailyReport.keys.sorted()

                // Requests Bar Chart
                val reqEntries = sortedDates.mapIndexed { index, date ->
                    BarEntry(index.toFloat(), dailyReport[date]?.first?.toFloat() ?: 0f)
                }
                SimpleBarChart(
                    entries = reqEntries,
                    label = "রিকোয়েস্ট",
                    color = Color.MAGENTA,
                    dates = sortedDates
                )

                Spacer(Modifier.height(16.dp))

                // Income Bar Chart
                val incEntries = sortedDates.mapIndexed { index, date ->
                    BarEntry(index.toFloat(), dailyReport[date]?.second?.toFloat() ?: 0f)
                }
                SimpleBarChart(
                    entries = incEntries,
                    label = "উপার্জন (টাকা)",
                    color = Color.BLUE,
                    dates = sortedDates
                )
            }
        }
    }
}

// Simple Bar Chart with X-axis dates
@Composable
fun SimpleBarChart(
    entries: List<BarEntry>,
    label: String,
    color: Int,
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
            val dataSet = BarDataSet(entries, label).apply {
                this.color = color
                valueTextColor = Color.MAGENTA
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