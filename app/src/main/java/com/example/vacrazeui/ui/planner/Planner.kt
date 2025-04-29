package com.example.vacrazeui.ui.planner

import android.util.Log
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vacrazeui.ui.TimeInterval
import java.util.Calendar
import kotlin.math.ceil
import com.github.skydoves.colorpicker.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor


private var TOTAL_TIME = 24 * 3600

private fun convertToSeconds(hours: Int, minutes: Int): Int {
    return hours * 3600 + minutes * 60
}


/**
 *  // on below line we are adding a alpha slider.
 *             AlphaSlider(
 *                 // on below line we
 *                 // are adding a modifier to it.
 *                 modifier = Modifier
 *                     .fillMaxWidth()
 *                     .padding(10.dp)
 *                     .height(35.dp),
 *                 // on below line we are
 *                 // adding a controller.
 *                 controller = controller,
 *                 // on below line we are
 *                 // adding odd and even color.
 *                 tileOddColor = Color.White,
 *                 tileEvenColor = Color.Black
 *             )
 */


@Composable
fun colorPicker(timeIntervalList: SnapshotStateList<TimeInterval>,
                select: Int) {
    // on below line we are creating a variable for controller
    val timeInterval = timeIntervalList[select]
    val controller = rememberColorPickerController()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {

        // on below line we are creating a column,
        Column(
            // on below line we are adding a modifier to it,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
                // on below line we are adding a padding.
                .padding(all = 30.dp)
        ) {
            // on below line we are adding a row.
            Row(
                // on below line we are adding a modifier
                modifier = Modifier.fillMaxWidth(),
                // on below line we are adding horizontal
                // and vertical alignment.
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // on below line we are adding a alpha tile.
                AlphaTile(
                    // on below line we are
                    // adding modifier to it
                    modifier = Modifier
                        .fillMaxWidth()
                        // on below line
                        // we are adding a height.
                        .height(60.dp)
                        // on below line we are adding clip.
                        .clip(RoundedCornerShape(6.dp)),
                    // on below line we are adding controller.
                    controller = controller
                )
            }
            // on below line we are
            // adding horizontal color picker.
            HsvColorPicker(
                initialColor = timeInterval.color,
                // on below line we are
                // adding a modifier to it
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .padding(10.dp),
                // on below line we are
                // adding a controller
                controller = controller,
                // on below line we are
                // adding on color changed.
                onColorChanged = {
                    color -> timeIntervalList[select] = timeInterval.copy(color = color.color)
                },

            )

            // on below line we are
            // adding a brightness slider.
            BrightnessSlider(
                // on below line we
                // are adding a modifier to it.
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .height(35.dp),
                // on below line we are
                // adding a controller.
                controller = controller,
            )
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun intervalPicker(timeIntervalList: SnapshotStateList<TimeInterval>,
                   select: Int,
                   maxHeightPx: Int,
                   onDismiss: () -> Unit,
                   onConfirm: () -> Unit) {

    val timeInterval = timeIntervalList[select]
    val scrollHeight = maxHeightPx.toFloat()

    val startTime: Pair<Int, Int> = timeInterval.returnStartTimeNum(scrollHeight)
    val endTime: Pair<Int, Int> = timeInterval.returnEndTimeNum(scrollHeight)

    var success by remember { mutableStateOf(true) }

    val timePickerStartState = rememberTimePickerState(
        initialHour = startTime.first,
        initialMinute = startTime.second,
        is24Hour = false,
    )

    val timePickerEndState = rememberTimePickerState(
        initialHour = endTime.first,
        initialMinute = endTime.second,
        is24Hour = false,
    )


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier=Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {



            Text("Select Time", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(20.dp))

            TimeInput(state = timePickerEndState)
            TimeInput(state = timePickerStartState)

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Button(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Button(onClick =
                {
                    val startSecs = convertToSeconds(timePickerStartState.hour, timePickerStartState.minute)
                    val endSecs = convertToSeconds(timePickerEndState.hour, timePickerEndState.minute)

                    success = endSecs < startSecs
                    if (success) {
                        timeIntervalList[select] = timeInterval.copy( // allows you to drag and change time
                            scale = (startSecs - endSecs).toFloat() / TOTAL_TIME,
                            offset_y = ceil(((endSecs.toFloat() / TOTAL_TIME) * maxHeightPx).toDouble()).toInt()
                        )
                        onConfirm()
                        onDismiss()
                    }
                }) {
                    Text("Confirm")
                }
            }


            if (!success) { // output error message
                Text("The starting time has to be less than the ending time!", fontSize = 15.sp,
                    fontWeight = FontWeight.Black, color = Color.Red)
            }

        }

    }
}

@Composable
fun SimpleTextEntry(timeIntervalList: SnapshotStateList<TimeInterval>,
                    select: Int) {

    val timeInterval = timeIntervalList[select]
    var text by remember { mutableStateOf(timeInterval.title) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier=Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Text("Edit Event Name", fontSize = 30.sp, fontWeight = FontWeight.Black)
            TextField(
                modifier=Modifier.fillMaxWidth(.75f),
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter something...") }
            )
            Button(onClick = {
                timeIntervalList[select] = timeInterval.copy(title = text)
            }) {
                Text("Confirm")
            }
        }

    }

}


@Composable
fun PagerNavigationButton(
    text: String,
    targetPage: Int,
    pagerState: PagerState,
    scope: CoroutineScope
) {
    TextButton(
        onClick = {
            scope.launch {
                pagerState.animateScrollToPage(targetPage)
            }
        },
        colors = ButtonDefaults.textButtonColors(
            containerColor = Color(0xFF2196F3), // Blue background
            contentColor = Color.White          // White text
        )
    ) {
        Text(text)
    }
}


@Composable
fun InputExample(
    timeIntervalList: SnapshotStateList<TimeInterval>,
    select: Int,
    maxHeightPx: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()


    Dialog(onDismissRequest = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                PagerNavigationButton(
                    text = "Time",
                    targetPage = 0,
                    pagerState = pagerState,
                    scope = scope
                )
                Spacer(modifier = Modifier.width(5.dp))
                PagerNavigationButton(
                    text = "Colors",
                    targetPage = 1,
                    pagerState = pagerState,
                    scope = scope
                )
                Spacer(modifier = Modifier.width(5.dp))
                PagerNavigationButton(
                    text = "Event",
                    targetPage = 2,
                    pagerState = pagerState,
                    scope = scope
                )
            }

            HorizontalPager(modifier = Modifier.fillMaxSize(),
                state = pagerState,
                userScrollEnabled = false) {
                    page ->
                when(page) {
                    0 -> Column(modifier=Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        intervalPicker(
                            timeIntervalList = timeIntervalList,
                            select = select,
                            maxHeightPx = maxHeightPx,
                            onDismiss = onDismiss,
                            onConfirm = onConfirm
                        )
                    }
                    1 ->
                        Column(modifier=Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            colorPicker(timeIntervalList, select)
                        }
                    2 -> SimpleTextEntry(timeIntervalList, select)

                }






            }

        }


    }






//    colorPicker()

}