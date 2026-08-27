package com.autoguard.companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.autoguard.companion.data.entity.AlertEntity
import com.autoguard.companion.ui.theme.ModerateWarning
import com.autoguard.companion.ui.theme.PrimaryBlue
import com.autoguard.companion.ui.theme.SevereAlert
import com.autoguard.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: MainViewModel, navController: NavController) {
    val alerts by viewModel.alerts.collectAsState()
    
    // Filter only accidents
    val accidentAlerts = alerts.filter { it.type == "ACCIDENT" }.sortedByDescending { it.timestamp }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accident Alerts", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        if (accidentAlerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No accident alerts received.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(accidentAlerts) { alert ->
                    AlertItem(alert) {
                        viewModel.markAlertAsViewed(alert)
                        navController.navigate("alert_detail/${alert.id}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AlertItem(alert: AlertEntity, onClick: () -> Unit) {
    val color = when (alert.severity) {
        "SEVERE" -> SevereAlert
        "MODERATE" -> ModerateWarning
        else -> Color.Gray
    }
    
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
    val dateStr = dateFormat.format(Date(alert.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${alert.severity} ACCIDENT",
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (!alert.isViewed) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) { Text("NEW") }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = dateStr, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (alert.speed != null) {
                Text("Speed: ${alert.speed} km/h")
            }
            if (alert.latitude != null && alert.longitude != null) {
                Text("Location: ${alert.latitude}, ${alert.longitude}")
            }
        }
    }
}
