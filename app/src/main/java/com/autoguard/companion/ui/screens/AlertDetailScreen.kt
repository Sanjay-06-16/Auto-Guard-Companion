package com.autoguard.companion.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.autoguard.companion.data.entity.AlertEntity
import com.autoguard.companion.ui.theme.ModerateWarning
import com.autoguard.companion.ui.theme.PrimaryTeal
import com.autoguard.companion.ui.theme.SevereAlert
import com.autoguard.companion.ui.theme.TextWhite
import com.autoguard.companion.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(alertId: Int?, viewModel: MainViewModel, navController: NavController) {
    var alert by remember { mutableStateOf<AlertEntity?>(null) }
    val context = LocalContext.current

    LaunchedEffect(alertId) {
        if (alertId != null) {
            alert = viewModel.getAlertById(alertId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert Details", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTeal)
            )
        }
    ) { padding ->
        val currentAlert = alert
        if (currentAlert == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                val color = when (currentAlert.severity) {
                    "SEVERE" -> SevereAlert
                    "MODERATE" -> ModerateWarning
                    else -> Color.Gray
                }
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${currentAlert.severity} ACCIDENT",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date(currentAlert.timestamp))
                        Text("Time: $dateStr")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Speed: ${currentAlert.speed ?: "Unknown"} km/h")
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Requirement: Do not show 0.0 coordinates, show "Unknown" instead.
                        val latStr = if (currentAlert.latitude != null && currentAlert.latitude != 0.0) currentAlert.latitude.toString() else "Unknown"
                        val lonStr = if (currentAlert.longitude != null && currentAlert.longitude != 0.0) currentAlert.longitude.toString() else "Unknown"
                        
                        Text("Latitude: $latStr")
                        Text("Longitude: $lonStr")
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Event Information:", fontWeight = FontWeight.SemiBold)
                        Text(currentAlert.rawMessage, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Requirement: If location is available, show clickable "Open Location in Maps"
                if (currentAlert.latitude != null && currentAlert.longitude != null && 
                    currentAlert.latitude != 0.0 && currentAlert.longitude != 0.0) {
                    Button(
                        onClick = {
                            val uri = "geo:${currentAlert.latitude},${currentAlert.longitude}?q=${currentAlert.latitude},${currentAlert.longitude}(Accident+Location)"
                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                            mapIntent.setPackage("com.google.android.apps.maps")
                            try {
                                context.startActivity(mapIntent)
                            } catch (e: Exception) {
                                // Fallback if Google Maps app not available
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Location in Maps")
                    }
                }
                
                // Requirement 8: REMOVE the button "Call Emergency Contacts". Do not replace it.
            }
        }
    }
}
