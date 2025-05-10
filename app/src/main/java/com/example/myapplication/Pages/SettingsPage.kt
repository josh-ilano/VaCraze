package com.example.myapplication.Pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.HelperViews.AuthState
import com.example.myapplication.HelperViews.AuthViewModel
import com.example.myapplication.HelperViews.SettingsViewModel

@Composable
fun SettingsPage(settingsViewModel: SettingsViewModel, authViewModel: AuthViewModel = viewModel(), navController: NavController) {

    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val textSize by settingsViewModel.textSize.collectAsState()
    val useMiles by settingsViewModel.useMiles.collectAsState()
    val use24HourFormat by settingsViewModel.use24HourFormat.collectAsState()

    val authState by authViewModel.authState.observeAsState()

    var showSignOutDialog by remember { mutableStateOf(false) }



    // React to auth state changes
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true } // Clear the back stack
                launchSingleTop = true
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        // Show dialog instead of immediate sign out
        Button(onClick = { showSignOutDialog = true }) {
            Text(text = "Sign Out")
        }

        // Add confirmation dialog
        if (showSignOutDialog) {
            AlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to sign out?") },
                confirmButton = {
                    Button(onClick = {
                        authViewModel.signout()
                        showSignOutDialog = false
                    }) {
                        Text("Sign Out")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSignOutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark Mode", modifier = Modifier.weight(1f))
            Switch(checked = isDarkMode, onCheckedChange = { settingsViewModel.toggleDarkMode() })
        }

        Column {
            Text("Text Size: ${textSize.toInt()}sp")
            Slider(
                value = textSize,
                onValueChange = { settingsViewModel.updateTextSize(it) },
                valueRange = 12f..30f
            )
        }


        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("24-hour Format", modifier = Modifier.weight(1f))
            Switch(checked = use24HourFormat, onCheckedChange = { settingsViewModel.toggleTimeFormat() })
        }

    }
}
