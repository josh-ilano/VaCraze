package com.example.vacrazeui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vacrazeui.ui.theme.VaCrazeUITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaCrazeUITheme  {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Composable
fun Label(text: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center)
        FloatingActionButton(
            modifier = Modifier.weight(1f),
            onClick = {  },
        ) { Icon(icon, text) }
    }

}

@Composable
fun ButtonLayout() {
    Column() {


        Label("Add", Icons.Filled.Add)
        Spacer(Modifier.height(16.dp))
        Label("Favorites", Icons.Filled.Star)
        Spacer(Modifier.height(16.dp))
        Label("Account", Icons.Filled.Person)
        Spacer(Modifier.height(16.dp))
        Label("Explore Map", Icons.Filled.LocationOn)
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    val coroutineScope = rememberCoroutineScope()
    val TOTAL_TIME = (24 * 3600)

    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    var totalSeconds by remember { mutableStateOf(0) }

    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    val formatted = currentTime.format(formatter)


    LaunchedEffect(Unit) { // keep track of time
        coroutineScope.launch(Dispatchers.IO) {
            while(true) {
                currentTime = LocalDateTime.now()
                totalSeconds = (currentTime.hour * 3600) + (currentTime.minute * 60)
                delay(1000 * 30) // update every 30 seconds
            }
        }
    }


    Row(modifier = modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxWidth(.4f)
            .verticalScroll(rememberScrollState())) {

            Canvas(modifier = Modifier.fillMaxWidth()
                .height(900.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                drawRoundRect(
                    cornerRadius = CornerRadius(16f, 16f),
                    color = Color.LightGray,
                    size = Size(canvasWidth, canvasHeight)
                ) // the gray bar used to track time

                val timeLeft = canvasHeight - ((totalSeconds / TOTAL_TIME.toFloat()) * canvasHeight)
                drawLine(
                    strokeWidth = 5f,
                    start = Offset(x = canvasWidth-30, y = timeLeft),
                    end = Offset(x = canvasWidth+75, y = timeLeft),
                    color = Color.Black
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text=formatted, fontSize = 30.sp)
            ButtonLayout()
        }

    }



}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
//    VaCrazeTheme {
//        Greeting("Android")
//    }
}
fun GreetingPreview() {
    VaCrazeTheme {
//        Greeting("Android")
    }
}
