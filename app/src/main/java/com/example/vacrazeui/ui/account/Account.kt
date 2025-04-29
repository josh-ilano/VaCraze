package com.example.vacrazeui.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController


@Composable
fun AccountScreen(modifier: Modifier) {
    Column(modifier=modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Person,
                contentDescription = "Favorite",
                tint = Color.Black)
        }
        Spacer(modifier=Modifier.padding(5.dp))
        Text("Josh Ilano")
        Spacer(modifier=Modifier.padding(5.dp))
        TextButton(onClick = {}, colors = ButtonColors(
            containerColor = Color(0xFF6200EE), // Deep purple
            contentColor = Color.White,
            disabledContentColor = Color.Black,
            disabledContainerColor = Color.LightGray
        )) { Text("Log Out")}
    }
}



@Preview
@Composable
fun Preview() {
    AccountScreen(Modifier)
}
