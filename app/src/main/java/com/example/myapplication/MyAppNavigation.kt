// File: app/src/main/java/com/example/myapplication/MyAppNavigation.kt
package com.example.myapplication

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.Pages.HomePage
import com.example.myapplication.Pages.LoginPage
import com.example.myapplication.Pages.SignupPage
import com.example.myapplication.Pages.WeatherScreen

import com.example.myapplication.BuildConfig


@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login",
        builder = {
            composable("login") {
                LoginPage(modifier, navController, authViewModel)
            }
            composable("signup") {
                SignupPage(modifier, navController, authViewModel)
            }
            composable("home") {
                HomePage(modifier, navController, authViewModel)
            }

            // ← new weather route
            composable(
                route = "weather/{lat}/{lon}",
                arguments = listOf(
                    navArgument("lat") { type = NavType.FloatType },
                    navArgument("lon") { type = NavType.FloatType }
                )
            ) { backStack ->
                // pull out the args
                val lat = backStack.arguments!!.getFloat("lat").toDouble()
                val lon = backStack.arguments!!.getFloat("lon").toDouble()

                WeatherScreen(
                    navController = navController,
                    lat = lat,
                    lon = lon,
                    apiKey = BuildConfig.WEATHER_API_KEY
                )
            }
        }
    )
}
