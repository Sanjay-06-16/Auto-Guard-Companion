package com.autoguard.companion.ui.screens

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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindBikeScreen(viewModel: MainViewModel, navController: NavController) {
    val profile by viewModel.profile.collectAsState()
    val latestLocation by viewModel.latestLocation.collectAsState()
    val context = LocalContext.current
    
    var isRequestSent by remember { mutableStateOf(false) }

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
                        "Please set the Bike GSM Number in your Rider Profile first.",
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
                
                Button(
                    onClick = {
                        viewModel.sendLocationRequest()
                        isRequestSent = true
                        Toast.makeText(context, "Location request sent to $bikeGsm", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send 'LOC' Request SMS")
                }
                
                if (isRequestSent) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for SMS response...", color = Color.Gray)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))
                Divider()
                Spacer(modifier = Modifier.height(32.dp))

                if (latestLocation != null) {
                    val loc = latestLocation!!
                    Text("Latest Known Location", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val dateStr = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date(loc.timestamp))
                    Text("Received at: $dateStr")
                    Text("Lat: ${loc.latitude}")
                    Text("Lon: ${loc.longitude}")

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (loc.latitude != null && loc.longitude != null) {
                        Button(
                            onClick = {
                                val uri = "geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(Bike+Location)"
                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open in Google Maps")
                        }
                    }
                } else {
                    Text("No location data received yet.")
                }
            }
        }
    }
}
