package com.example.myapplication.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    textScale: Float = 16f, // Dynamically adjust text size
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Custom Typography for Material 3 with dynamic text size
    val customTypography = Typography(
        headlineLarge = TextStyle(fontSize = (textScale + 4).sp, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontSize = (textScale + 2).sp, fontWeight = FontWeight.Bold),
        headlineSmall = TextStyle(fontSize = textScale.sp, fontWeight = FontWeight.Bold),

        titleLarge = TextStyle(fontSize = (textScale - 2).sp, fontWeight = FontWeight.Normal),
        titleMedium = TextStyle(fontSize = (textScale - 4).sp, fontWeight = FontWeight.Normal),
        titleSmall = TextStyle(fontSize = (textScale - 6).sp, fontWeight = FontWeight.Normal),

        bodyLarge = TextStyle(fontSize = textScale.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontSize = (textScale - 2).sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontSize = (textScale - 4).sp, fontWeight = FontWeight.Normal),

        labelLarge = TextStyle(fontSize = (textScale - 2).sp, fontWeight = FontWeight.Normal),
        labelMedium = TextStyle(fontSize = (textScale - 4).sp, fontWeight = FontWeight.Normal),
        labelSmall = TextStyle(fontSize = (textScale - 6).sp, fontWeight = FontWeight.Normal)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}
