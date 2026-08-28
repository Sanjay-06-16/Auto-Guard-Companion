package com.autoguard.companion.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.autoguard.companion.ui.theme.PrimaryBlue
import com.autoguard.companion.ui.viewmodel.MainViewModel
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FindBikeScreen(viewModel: MainViewModel, navController: NavController) {
    val profile by viewModel.profile.collectAsState()
    val latestLocation by viewModel.latestLocation.collectAsState()
    val context = LocalContext.current
    
    // SMS Permissions state
    val smsPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
    )
    
    var isRequestSent by remember { mutableStateOf(false) }
    var requestTime by remember { mutableLongStateOf(0L) }
    var showTimeoutError by remember { mutableStateOf(false) }

    // Logic to clear "Waiting" state when a new location arrives OR timeout occurs
    LaunchedEffect(latestLocation) {
        latestLocation?.let {
            if (it.timestamp > requestTime) {
                isRequestSent = false
                showTimeoutError = false
            }
        }
    }

    LaunchedEffect(isRequestSent) {
        if (isRequestSent) {
            showTimeoutError = false
            delay(60000) // 60 seconds timeout
            if (isRequestSent) {
                isRequestSent = false
                showTimeoutError = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find My Bike", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bikeGsm = profile?.bikeGsmNumber
            
            if (bikeGsm.isNullOrBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "Please save the ESP32 GSM SIM number in Rider Profile.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.navigate("profile") }) {
                    Text("Go to Profile")
                }
            } else {
                Text(
                    "Configured Bike Number: $bikeGsm",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                if (!smsPermissionsState.allPermissionsGranted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("SMS permission is required to receive bike location.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { smsPermissionsState.launchMultiplePermissionRequest() }) {
                                Text("Grant Permissions")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        if (smsPermissionsState.allPermissionsGranted) {
                            viewModel.sendLocationRequest()
                            isRequestSent = true
                            requestTime = System.currentTimeMillis()
                            showTimeoutError = false
                            Toast.makeText(context, "Location request sent to $bikeGsm", Toast.LENGTH_SHORT).show()
                        } else {
                            smsPermissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isRequestSent
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRequestSent) "Requesting..." else "Send LOC Request")
                }
                
                if (isRequestSent) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for bike location...", color = Color.Gray)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }

                if (showTimeoutError) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Bike location response not received.", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { 
                        if (smsPermissionsState.allPermissionsGranted) {
                            viewModel.sendLocationRequest()
                            isRequestSent = true
                            requestTime = System.currentTimeMillis()
                            showTimeoutError = false
                        } else {
                            smsPermissionsState.launchMultiplePermissionRequest()
                        }
                    }) {
                        Text("Try Again")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Divider()
                Spacer(modifier = Modifier.height(32.dp))

                if (latestLocation != null) {
                    val loc = latestLocation!!
                    Text("Bike Location Found", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val dateStr = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date(loc.timestamp))
                    Text("Timestamp: $dateStr")
                    Text("Latitude: ${loc.latitude}")
                    Text("Longitude: ${loc.longitude}")

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (loc.latitude != null && loc.longitude != null) {
                        Button(
                            onClick = {
                                val uri = "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("View on Map")
                        }
                    }
                } else {
                    Text("No location data received yet.")
                }
            }
        }
    }
}
