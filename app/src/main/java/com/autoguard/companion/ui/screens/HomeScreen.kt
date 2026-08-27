package com.autoguard.companion.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.autoguard.companion.bluetooth.BluetoothState
import com.autoguard.companion.ui.theme.*
import com.autoguard.companion.ui.viewmodel.MainViewModel
import com.google.accompanist.permissions.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, navController: NavController) {
    val alerts by viewModel.alerts.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val latestLocation by viewModel.latestLocation.collectAsState()
    val bluetoothState by viewModel.bluetoothState.collectAsState()
    val bluetoothError by viewModel.bluetoothErrorMessage.collectAsState()
    val esp32GpsAvailable by viewModel.esp32GpsAvailable.collectAsState()
    val context = LocalContext.current

    // Permission Handling for Bluetooth (Android 12+)
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberMultiplePermissionsState(
            permissions = listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        )
    } else null

    // ================= LOCATION PERMISSION =================

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Show Bluetooth errors
    LaunchedEffect(bluetoothError) {
        bluetoothError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val unreadAlertsCount = alerts.count { it.type == "ACCIDENT" && !it.isViewed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Guard Dashboard", color = TextWhite) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTeal)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Status Summary
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("System Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
                        
                        // Bluetooth Quick Toggle
                        TextButton(
                            onClick = { 
                                if (bluetoothState == BluetoothState.CONNECTED) {
                                    viewModel.disconnectBluetooth()
                                } else {
                                    if (bluetoothPermissions != null && !bluetoothPermissions.allPermissionsGranted) {
                                        bluetoothPermissions.launchMultiplePermissionRequest()
                                    } else {
                                        viewModel.connectBluetooth()
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = if (bluetoothState == BluetoothState.CONNECTED) "Disconnect BT" else "Connect BT",
                                color = PrimaryTeal
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val btStatusText = when(bluetoothState) {
                        BluetoothState.CONNECTED -> "CONNECTED"
                        BluetoothState.CONNECTING -> "CONNECTING..."
                        BluetoothState.ERROR -> "ERROR"
                        BluetoothState.UNAVAILABLE -> "UNAVAILABLE"
                        else -> "DISCONNECTED"
                    }
                    Text("Bluetooth: $btStatusText", color = if(bluetoothState == BluetoothState.CONNECTED) AccentGreen else TextMuted)
                    
                    val gpsStatus = if (esp32GpsAvailable) "Available (ESP32)"
                                   else if (bluetoothState == BluetoothState.CONNECTED) "Searching..." 
                                   else "Unknown"
                    Text("ESP32 GPS: $gpsStatus", color = TextMuted)
                    
                    Text("Unread Accident Alerts: $unreadAlertsCount", color = TextMuted)
                    
                    val locStr = latestLocation?.let { 
                        if (it.latitude != null && it.longitude != null) 
                            "%.6f, %.6f".format(it.latitude, it.longitude)
                        else "Unknown"
                    } ?: "Unknown"
                    Text("Last Known Location: $locStr", color = TextMuted)
                }
            }

            // Dashboard Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardCard(
                        title = "Accident Alerts", 
                        icon = Icons.Default.Warning, 
                        accentColor = AccentAmber,
                        badgeCount = unreadAlertsCount
                    ) {
                        navController.navigate("alerts")
                    }
                }
                item {
                    DashboardCard(
                        title = "Find My Bike", 
                        icon = Icons.Default.LocationOn,
                        accentColor = PrimaryTeal
                    ) {
                        navController.navigate("find_bike")
                    }
                }
                item {
                    DashboardCard(
                        title = "Alert History", 
                        icon = Icons.Default.List,
                        accentColor = PrimaryTeal
                    ) {
                        navController.navigate("history")
                    }
                }
                item {
                    DashboardCard(
                        title = "Rider Profile", 
                        icon = Icons.Default.Person,
                        accentColor = PrimaryTeal
                    ) {
                        navController.navigate("profile")
                    }
                }
                item {
                    DashboardCard(
                        title = "Call Rider", 
                        icon = Icons.Default.Phone,
                        accentColor = AccentGreen
                    ) {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${profile?.riderPhoneNumber ?: ""}")
                        }
                        context.startActivity(intent)
                    }
                }
                item {
                    DashboardCard(
                        title = "Call 108", 
                        icon = Icons.Default.Call,
                        accentColor = AccentRed,
                        isEmergency = true
                    ) {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:108")
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String, 
    icon: ImageVector, 
    accentColor: Color,
    badgeCount: Int = 0, 
    isEmergency: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (isEmergency) BorderStroke(2.dp, accentColor) else null
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = title, 
                    modifier = Modifier.size(48.dp), 
                    tint = accentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title, 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = FontWeight.Medium,
                    color = TextWhite
                )
            }
            if (badgeCount > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = AccentRed
                ) {
                    Text(badgeCount.toString(), color = TextWhite)
                }
            }
        }
    }
}
