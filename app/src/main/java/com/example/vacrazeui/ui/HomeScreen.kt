package com.example.vacrazeui.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation.compose.composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.xr.compose.testing.toDp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


//import com.example.vacrazeui.ui.planner.PlannerScreen
import com.example.vacrazeui.ui.favorite.FavoriteScreen
import com.example.vacrazeui.ui.account.AccountScreen
import com.example.vacrazeui.ui.map.MapScreen
import com.example.vacrazeui.ui.planner.InputExample
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

enum class VacrazeScreen() {
    Home,
    Add,
    Favorite,
    Map,
    Account
}

private var TOTAL_TIME = 24 * 3600
private val HOUR_TIME = 60 * 60


// The bottom layout for the 5 buttons
@Composable
fun ButtonLayout(navController: NavHostController) {
    BottomAppBar() {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FloatingActionButton(
                onClick = {},
                modifier = Modifier.padding(horizontal = 10.dp)
            ) { Icon(Icons.Filled.Add, "Add") }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(
                onClick = { navController.navigate(VacrazeScreen.Favorite.name) },
                modifier = Modifier.padding(horizontal = 10.dp)
            ) { Icon(Icons.Filled.Star, "Star") }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(
                onClick = { navController.navigate(VacrazeScreen.Home.name) },
                modifier = Modifier.padding(horizontal = 10.dp)
            ) { Icon(Icons.Filled.Home, "Home") }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(
                onClick = { navController.navigate(VacrazeScreen.Account.name) },
                modifier = Modifier.padding(horizontal = 10.dp)
            ) { Icon(Icons.Filled.Person, "Account") }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(
                onClick = { navController.navigate(VacrazeScreen.Map.name) },
                modifier = Modifier.padding(horizontal = 10.dp)
            ) { Icon(Icons.Filled.LocationOn, "Explore Map") }
        }
    }
}


data class TimeInterval(
    val scale: Float, // the size of the interval
    val offset_y: Int, // how far 'down' we should go
    val color: Color, // the color of the interval
    val title: String, // the name of the event
    var layer: Int = 0
) {

    companion object {
        /**
         * Given a time interval, compare the next one, and if they overlap, push the
         * current interval up so that it is on a different layer
         */


        fun pushLayers(timeIntervalList: SnapshotStateList<TimeInterval>,
                       timeInterval: TimeInterval,
                       scrollHeight: Float,
                       initialMax: Int): Int {

            // sort in ascending order (from AM to PM)
            val sortedList = timeIntervalList.sortedBy { it.offset_y }
            var max = initialMax // layers are indexed from 0

            Log.d("SORTED LIST", sortedList.toString())
            Log.d("TIME INTERVALS", timeIntervalList.toString())
            Log.d("INITIAL MAX", initialMax.toString())

            val index = sortedList.indexOfFirst {
                Log.d("ELEMENT", it.toString())
                Log.d("TIME INTERVAL", timeInterval.toString())
                it == timeInterval
            } // find the timeInterval in the sorted list


            if(index < sortedList.size -1) {
                val currentInterval = sortedList[index]
                val nextInterval = sortedList[index+1]

                val endTimeBefore = currentInterval.returnSeconds(scrollHeight, false) // the finish time
                val beginTimeAfter = nextInterval.returnSeconds(scrollHeight, true)
                while(endTimeBefore >= beginTimeAfter && currentInterval.layer == nextInterval.layer) {
                    // if they overlap and they are on the same layer...

                    currentInterval.layer++
                    if(currentInterval.layer > max) { max = currentInterval.layer }
                    // when we discover a new layer, we add it

                    Log.d("LAYER PUSHED", currentInterval.layer.toString())
                }
            }

            Log.d("MAX INFO", "$max $initialMax" )
            return max - initialMax // how many layers to add

        }
    }



    private fun convertSecondsToTime(seconds: Float): String {
        // Convert seconds to milliseconds
        val milliseconds = (seconds * 1000).toLong()

        // Create a Date object from the milliseconds
        val date = Date(milliseconds)

        // Format the date to a time string (e.g., HH:mm:ss)
        val sdf = SimpleDateFormat("hh:mm a")
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    // start is 'higher' up than the end
    private fun returnSeconds(scrollHeight: Float, start: Boolean): Float {
        val base: Float = if (start) offset_y.toFloat() else offset_y + (scrollHeight * scale)
        return (base / scrollHeight) * TOTAL_TIME // complimentary
    }


    /**
     * Return the start time given the maximum available height as a string
     */
    fun returnEndTime(scrollHeight: Float): String {
        return convertSecondsToTime(returnSeconds(scrollHeight, true))
    }

    fun returnStartTime(scrollHeight: Float): String {
        return convertSecondsToTime(returnSeconds(scrollHeight, false))
    }

    /**
     * Return the start time given the maximum available height as an integer
     */
    fun returnEndTimeNum(scrollHeight: Float): Pair<Int, Int> {
        val seconds = returnSeconds(scrollHeight, true)
        return Pair((seconds/ HOUR_TIME).toInt(), ((seconds % HOUR_TIME) / 60).toInt())
    }

    fun returnStartTimeNum(scrollHeight: Float): Pair<Int, Int> {
        val seconds = returnSeconds(scrollHeight, false)
        return Pair((seconds/ HOUR_TIME).toInt(), ((seconds % HOUR_TIME) / 60).toInt())
    }


}




/**
 * Dragger to resize (Experimental: Can implement later)
 *     Box(
 *                 modifier = Modifier
 *                     .offset(x = 35.dp)
 *                     .clip(RoundedCornerShape(100))
 *                     .size(32.dp)
 *                     .align(Alignment.CenterEnd)
 *
 *                     .background(Color.Black)
 *                     .draggable( // enable dragging up and down
 *                         orientation = Orientation.Vertical,
 *                         state = rememberDraggableState { delta ->
 *                             size=(size + delta/500).coerceIn(0f, 1f - (offsetY / scrollHeight))
 *                         }
 *                     )
 *
 *             )
 *
 */

/**
 * Sets up the time grid
 *
 */
@Composable
private fun Background() {


    val configuration = LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

        Box(modifier=Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .height(screenHeightDp * 1.25f)
            .background(Color.Gray)) { // actual time container

            Column {
                for(i in 0..24) { // 0th hour to the 24th hour
                    val offset = ((screenHeightDp*1.25f)/24) * i // dp value
                    Row(modifier=Modifier.offset (x=0.dp, offset)){
                        Text(text=i.toString())
                        Box( // line
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.Black)
                        )
                    }
                }
            }
//
//            Box(modifier = Modifier.fillMaxWidth(1f)
//                .height(maxHeight.dp)
//                .background(Color(210, 210, 210, 255))) {
//
//
//            }

        }



}



@Composable
private fun TransformableSample() {


//    var offsetY by remember { mutableFloatStateOf(0f) }
//    var size by remember { mutableFloatStateOf(.1f) }

    var dragSelect by remember { mutableIntStateOf(-1) }
    var select by remember { mutableIntStateOf(-1) }
    val info = remember { mutableStateListOf(
        TimeInterval(scale = .25f, offset_y = 0, color=Color.Cyan, title="Dinner"),
        TimeInterval(scale = .15f, offset_y = 700, color=Color.Red, title="Mini golfing")
    ) }


    val layers = remember { mutableStateListOf<Boolean>(true) }
    var maxHeightPx: Int by remember { mutableIntStateOf(0) }



    Background()


    Row {

        Column {
            layers.forEachIndexed { // this will trigger recomposition
                    index, layer -> Checkbox(layer, onCheckedChange = { checked -> layers[index] = checked })
            }
        }


        BoxWithConstraints(modifier = Modifier
            .fillMaxHeight(.9f)
            .fillMaxWidth(.5f)
            .background(Color.LightGray)) { // actual time container


            val dimensions = this // represents the dimensions of the current container box
            maxHeightPx = dimensions.constraints.maxHeight  // the height of the actual timebar

            info.forEachIndexed {
                    index, timeInterval ->

                if(layers[timeInterval.layer]) {
                     // total timebar - the size of time interval
                    val scrollHeight = (maxHeightPx).toFloat()

                    Box( // the box representing time interval which can be dragged up and down
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(timeInterval.scale)
                            .offset { IntOffset(0, timeInterval.offset_y) }
                            .clickable { select = index }
                            .background(timeInterval.color)
                            .zIndex(if (dragSelect == index) 10000f else 0f)
                            .draggable( // enable dragging up and down
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    info[index] = timeInterval.copy( // allows you to drag and change time
                                        scale = timeInterval.scale,
                                        offset_y = (timeInterval.offset_y+delta).coerceIn(0f, maxHeightPx - (maxHeightPx * timeInterval.scale)).toInt())
                                    // restrict time between 12:00 AM and 11:59 PM
                                },
                                onDragStarted = {
                                    _ -> dragSelect = index
                                    val color = timeInterval.color
                                    info[index] = timeInterval.copy(color = Color(color.red, color.blue, color.green, 0.5f))
                                },
                                onDragStopped = {
                                    _ -> dragSelect = -1
                                    val color = timeInterval.color
                                    info[index] = timeInterval.copy(color = Color(color.red, color.blue, color.green, 1f))

                                    val layersToAdd = TimeInterval.pushLayers(info, info[index], maxHeightPx.toFloat(), layers.size-1)
                                    if (layersToAdd > 0)
                                        layers.addAll(List(layersToAdd) { true }) // simultaneously add however many layers you need to add
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {


                        val startTime = timeInterval.returnStartTime(scrollHeight) // as strings
                        val endTime = timeInterval.returnEndTime(scrollHeight)

                        Text(text = startTime, fontSize = 20.sp, modifier=Modifier.align(
                            Alignment.BottomCenter))
                        Text(text = timeInterval.title, fontSize=30.sp, modifier=Modifier.align(
                            Alignment.Center))
                        Text(text = endTime, fontSize = 20.sp, modifier=Modifier.align(
                            Alignment.TopCenter))


                    }
                }

            }
        }

    }


    if(select != -1) // displays the timer
        InputExample(
            timeIntervalList = info,
            select = select,
            maxHeightPx = maxHeightPx,
            onConfirm = {
//                Log.d("NEW INTERVAL", info[select].toString())
                //                Log.d("LAYERSTOADD", layersToAdd.toString())

                val layersToAdd = TimeInterval.pushLayers(info, info[select], maxHeightPx.toFloat(), layers.size-1)
                if (layersToAdd > 0)
                    layers.addAll(List(layersToAdd) { true }) // simultaneously add however many layers you need to add

                // update
            }) { select = -1 }

}



@Composable
fun HomeScreen() {

    val coroutineScope = rememberCoroutineScope()
    val timeInstance = DateFormat.getTimeInstance(DateFormat.SHORT)

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(modifier=Modifier
        .fillMaxSize()) {

        TransformableSample()


//        Box(modifier = Modifier.size(48.dp, 16.dp)
//            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
//            .background(Color.Black)
//            .pointerInput(Unit) {
//                detectDragGestures { change, dragAmount ->
//                    change.consume()
//                    offsetX += dragAmount.x
//                    offsetY += dragAmount.y
//                }
//            }) {
//        }

    }



//    val testInterval = TimeInterval(HOUR_TIME*5, HOUR_TIME * 8) // 2 AM to 5 AM
//    val testInterval_2 = TimeInterval(HOUR_TIME*13, HOUR_TIME * 19) // 1 PM to 7 PM


//    var currentTime by remember { mutableStateOf(timeInstance.format(Date())) }
//    var totalSeconds by remember { mutableIntStateOf(0) }

//    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
//    val formatted = currentTime.format(formatter)


//    LaunchedEffect(Unit) { // keep track of time
//        coroutineScope.launch(Dispatchers.IO) {
//            while (true) {
//                  Insert current time below
//                currentTime = LocalDateTime.now()
//                totalSeconds = (currentTime.hour * 3600) + (currentTime.minute * 60)
//                delay(1000 * 30) // update every 30 seconds
//            }
//        }
//    }
//
//    Row(modifier = Modifier.fillMaxSize()) {



}


@Composable
fun App(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
            .background(Color.Gray),
        bottomBar = { ButtonLayout(navController) }
    ) { innerPadding ->
        NavHost( // what allows us to navigate (Define all the possible navigation routes)
            navController = navController,
            startDestination = VacrazeScreen.Home.name,
            modifier = Modifier.padding(innerPadding)

        ) {
            composable(route = VacrazeScreen.Home.name) { HomeScreen() }
            composable(route = VacrazeScreen.Favorite.name) { FavoriteScreen() }
            composable(route = VacrazeScreen.Account.name) { AccountScreen(Modifier) }
            composable(route = VacrazeScreen.Map.name) { MapScreen(Modifier) }
        }
    }
}





@Composable
@Preview
fun DisplayHome() {
    HomeScreen()
}
