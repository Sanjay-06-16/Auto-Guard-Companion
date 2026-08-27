package com.autoguard.companion.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.autoguard.companion.ui.theme.PrimaryTeal
import com.autoguard.companion.ui.theme.TextWhite
import com.autoguard.companion.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MainViewModel, navController: NavController) {
    val profile by viewModel.profile.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var bikeNumber by remember { mutableStateOf("") }
    var bikeGsmNumber by remember { mutableStateOf("") }
    var riderPhoneNumber by remember { mutableStateOf("") }
    var emergencyContact1 by remember { mutableStateOf("") }
    var emergencyContact2 by remember { mutableStateOf("") }
    var medicalNotes by remember { mutableStateOf("") }

    // Load existing profile data
    LaunchedEffect(profile) {
        profile?.let { p ->
            name = p.name
            bloodGroup = p.bloodGroup
            bikeNumber = p.bikeNumber
            bikeGsmNumber = p.bikeGsmNumber
            riderPhoneNumber = p.riderPhoneNumber
            emergencyContact1 = p.emergencyContact1
            emergencyContact2 = p.emergencyContact2
            medicalNotes = p.medicalNotes
        }
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        if (phone.isBlank()) return true // Allow empty if optional
        return phone.all { it.isDigit() || it == '+' } && phone.length >= 10
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rider Profile", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTeal)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Rider Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bloodGroup,
                onValueChange = { bloodGroup = it },
                label = { Text("Blood Group (e.g., O+)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bikeNumber,
                onValueChange = { bikeNumber = it },
                label = { Text("Bike License Plate Number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bikeGsmNumber,
                onValueChange = { bikeGsmNumber = it },
                label = { Text("Bike GSM SIM Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = riderPhoneNumber,
                onValueChange = { riderPhoneNumber = it },
                label = { Text("Rider Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = emergencyContact1,
                onValueChange = { emergencyContact1 = it },
                label = { Text("Emergency Contact 1") },
                placeholder = { Text("+91XXXXXXXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = emergencyContact2,
                onValueChange = { emergencyContact2 = it },
                label = { Text("Emergency Contact 2") },
                placeholder = { Text("+91XXXXXXXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = medicalNotes,
                onValueChange = { medicalNotes = it },
                label = { Text("Medical History / Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (!isValidPhoneNumber(emergencyContact1)) {
                        Toast.makeText(context, "Invalid Emergency Contact 1", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!isValidPhoneNumber(emergencyContact2)) {
                        Toast.makeText(context, "Invalid Emergency Contact 2", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    viewModel.saveProfile(
                        name, bloodGroup, bikeNumber, bikeGsmNumber, riderPhoneNumber,
                        emergencyContact1, emergencyContact2, medicalNotes
                    )
                    Toast.makeText(context, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                    
                    // Requirement: Home -> Rider Profile -> Save Profile -> Home
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("Save Profile")
            }
        }
    }
}
