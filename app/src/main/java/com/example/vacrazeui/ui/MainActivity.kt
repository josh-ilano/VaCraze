package com.example.vacrazeui.ui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.platform.LocalConfiguration
import com.example.vacrazeui.ui.theme.VaCrazeUITheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaCrazeUITheme  {
                val configuration = LocalConfiguration.current
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                    AppLandscape()
                }
                else {

                    App()
                }

            }
        }
    }
}

