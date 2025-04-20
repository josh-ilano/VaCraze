package com.example.myapplication.Pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.AuthState
import com.example.myapplication.AuthViewModel
import com.example.myapplication.Tools.FirebaseInput
import com.example.myapplication.Tools.MapWithSearchScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel){

    val authState = authViewModel.authState.observeAsState()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val uid = currentUser?.uid

    LaunchedEffect(authState.value){
        when (authState.value){
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    Column(modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        //horizontalAlignment = Alignment.CenterHorizontally
        )
     {
        Text(text = "HomePage", fontSize = 32.sp)

         Spacer(modifier = Modifier.height(12.dp))

         Text(text = "User ID:" + uid.toString(), fontSize = 12.sp)

         Spacer(modifier = Modifier.height(12.dp))


         Button(onClick ={
             authViewModel.signout()
         }){
             Text(text = "Sign Out")
         }

         Spacer(modifier = Modifier.height(12.dp))


         Box(modifier = Modifier
             .fillMaxWidth()
             .height(500.dp)){

             MapWithSearchScreen()
         }

         Spacer(modifier = Modifier.height(12.dp))


         Box(modifier = Modifier
             .fillMaxWidth()
             .height(500.dp)){

             FirebaseInput()
         }
    }



}







