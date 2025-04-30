// File: app/src/main/java/com/example/myapplication/Pages/WeatherPage.kt
package com.example.myapplication.Pages

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// --- 1) Data model for one day’s forecast ---
data class WeatherDay(
    val date: String,
    val description: String,
    val tempMax: Double,
    val tempMin: Double
)

// --- 2) ViewModel to fetch & hold the 7-day forecast ---
class WeatherViewModel : ViewModel() {
    private val _forecast = androidx.lifecycle.MutableLiveData<List<WeatherDay>>(emptyList())
    val forecast = _forecast as androidx.lifecycle.LiveData<List<WeatherDay>>
    private val client = OkHttpClient()

    fun loadForecast(lat: Double, lon: Double, apiKey: String) {
        viewModelScope.launch {
            // Build the correct URL for the Google Weather API preview
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("weather.googleapis.com")
                .addPathSegments("v1/forecast/days:lookup")
                .addQueryParameter("location.latitude", lat.toString())
                .addQueryParameter("location.longitude", lon.toString())
                .addQueryParameter("days", "7")
                .addQueryParameter("key", apiKey)
                .build()

            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("WeatherVM", "Network error", e)
                }

                override fun onResponse(call: Call, resp: Response) {
                    if (!resp.isSuccessful) {
                        Log.e("WeatherVM", "HTTP ${resp.code}: ${resp.body?.string()}")
                        return
                    }
                    val body = resp.body?.string().orEmpty()
                    val root = try {
                        JSONObject(body)
                    } catch (e: JSONException) {
                        Log.e("WeatherVM", "Invalid JSON", e)
                        return
                    }

                    // ← use "forecastDays" here
                    val arr = root.optJSONArray("forecastDays") ?: run {
                        Log.e("WeatherVM", "Missing forecastDays")
                        return
                    }

                    val df = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                    val list = mutableListOf<WeatherDay>()
                    for (i in 0 until arr.length()) {
                        val day = arr.getJSONObject(i)
                        // 4) Build the display date from the "displayDate" object
                                val disp = day.getJSONObject("displayDate")
                                val year  = disp.optInt("year", 1970)
                                val month = disp.optInt("month", 1)
                                val dayOfMonth = disp.optInt("day", 1)
                                val cal = Calendar.getInstance().apply {
                                      set(year, month - 1, dayOfMonth)
                                    }
                                val date = df.format(cal.time)

                                // 5) Extract the high & low temperatures
                                val maxTempObj = day.getJSONObject("maxTemperature")
                                val minTempObj = day.getJSONObject("minTemperature")
                                val maxValue = maxTempObj.optDouble("degrees", Double.NaN)
                                val minValue = minTempObj.optDouble("degrees", Double.NaN)

                                // 6) Get the daytime weather description
                                val desc = day
                                  .getJSONObject("daytimeForecast")
                                  .getJSONObject("weatherCondition")
                                  .getJSONObject("description")
                                  .optString("text", "")

                                list += WeatherDay(
                                      date        = date,
                                      description = desc,
                                      tempMax     = maxValue,
                                      tempMin     = minValue
                                            )
                    }
                    _forecast.postValue(list)
                }



            })
        }
    }
}

// --- 3) The Composable Screen itself ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    navController: NavController,
    lat: Double,
    lon: Double,
    apiKey: String,
    viewModel: WeatherViewModel = viewModel()
) {
    val forecast by viewModel.forecast.observeAsState()

    // Trigger load when screen appears
    LaunchedEffect(lat, lon) {
        viewModel.loadForecast(lat, lon, apiKey)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("7-Day Forecast") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (forecast.isNullOrEmpty()) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    items(forecast!!) { day ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(day.date, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(day.description, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "High: ${day.tempMax}°, Low: ${day.tempMin}°",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
