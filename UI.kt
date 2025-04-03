package com.example.vacraze

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.drawText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vacraze.ui.theme.VaCrazeTheme
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaCrazeTheme {
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    val formatted = currentTime.format(formatter)

    Row(modifier = modifier.fillMaxSize()) {
        Column(modifier = modifier.fillMaxWidth(.4f)
            .verticalScroll(rememberScrollState())) {
            Canvas(modifier = Modifier.fillMaxWidth()
                .height(900.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                drawRoundRect(
                    color = Color.LightGray,
                    size = Size(canvasWidth, canvasHeight)
                )


                drawLine(
                    strokeWidth = 5f,
                    start = Offset(x = canvasWidth-30, y = 300f),
                    end = Offset(x = canvasWidth+75, y = 300f),
                    color = Color.Blue
                )
            }


        }
        Text(text=formatted, fontSize = 30.sp)
    }



}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VaCrazeTheme {
//        Greeting("Android")
    }
}
