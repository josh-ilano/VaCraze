package com.example.myapplication

import MyAppNavigation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.myapplication.HelperViews.SettingsViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            //  Observe the current dark mode state
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val textSize by settingsViewModel.textSize.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode, textScale = textSize) {

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        MyAppNavigation(
                            modifier = Modifier.padding(innerPadding),
                            settingsViewModel = settingsViewModel
                            )
                    }


            }
        }
    }
}

