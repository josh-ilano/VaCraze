// File: app/src/main/java/com/example/myapplication/Pages/HomePage.kt
package com.example.myapplication.Pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.AuthState
import com.example.myapplication.AuthViewModel
import com.example.myapplication.Tools.FirebaseInput
import com.example.myapplication.Tools.MapWithSearchScreen
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // ← 1) track the last‐tapped pin
    var pinLocation by remember { mutableStateOf<LatLng?>(null) }

    val authState = authViewModel.authState.observeAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uid = currentUser?.uid

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            navController.navigate("login")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "HomePage", fontSize = 32.sp)
        Spacer(Modifier.height(12.dp))

        Text(text = "User ID: $uid", fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))

        Button(onClick = { authViewModel.signout() }) {
            Text("Sign Out")
        }
        Spacer(Modifier.height(24.dp))

        // ← 2) Map that reports back taps via the lambda
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            MapWithSearchScreen { latLng ->
                pinLocation = latLng
            }
        }
        Spacer(Modifier.height(12.dp))

        // ← 3) Weather button, only enabled once pinLocation is non-null
        Button(
            onClick = {
                pinLocation?.let {
                    navController.navigate("weather/${it.latitude}/${it.longitude}")
                }
            },
            enabled = pinLocation != null
        ) {
            Text("Weather")
        }
        Spacer(Modifier.height(24.dp))

        // ← your existing FirebaseInput area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            FirebaseInput()
        }
    }
}
