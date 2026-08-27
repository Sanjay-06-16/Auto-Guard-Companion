package com.autoguard.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.autoguard.companion.data.entity.AlertEntity
import com.autoguard.companion.ui.theme.PrimaryBlue
import com.autoguard.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel, navController: NavController) {
    val allAlerts by viewModel.alerts.collectAsState()
    
    // Simple filter state
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredAlerts = when(selectedFilter) {
        "ACCIDENTS" -> allAlerts.filter { it.type == "ACCIDENT" }
        "LOCATIONS" -> allAlerts.filter { it.type == "LOCATION" || it.type == "LOCATION_REQUEST" }
        else -> allAlerts
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert History", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter tabs
            TabRow(selectedTabIndex = when(selectedFilter) { "ALL" -> 0; "ACCIDENTS" -> 1; else -> 2 }) {
                Tab(selected = selectedFilter == "ALL", onClick = { selectedFilter = "ALL" }, text = { Text("All") })
                Tab(selected = selectedFilter == "ACCIDENTS", onClick = { selectedFilter = "ACCIDENTS" }, text = { Text("Accidents") })
                Tab(selected = selectedFilter == "LOCATIONS", onClick = { selectedFilter = "LOCATIONS" }, text = { Text("Location Requests") })
            }

            if (filteredAlerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No history available.")
                }
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    items(filteredAlerts) { alert ->
                        HistoryItem(alert)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(alert: AlertEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = if (alert.type == "ACCIDENT") "${alert.severity} ACCIDENT" else alert.type
            Text(title, fontWeight = FontWeight.Bold)
            val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date(alert.timestamp))
            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            if (alert.rawMessage.isNotBlank()) {
                Text(alert.rawMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
