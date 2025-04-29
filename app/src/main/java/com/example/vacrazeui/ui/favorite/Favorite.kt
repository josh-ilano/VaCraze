package com.example.vacrazeui.ui.favorite

import android.widget.RemoteViews.RemoteView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest

data class ImageInfo (
    val imageLink: String,
    val review: Int,
    val title: String,
    val description: String
)


@Composable
fun MinimalDialog(title: String, description: String, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier=Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 25.sp,
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = description,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center),
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = { onDismissRequest()},
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Dismiss")
                }
            }

        }
    }
}

@Composable
fun Star() {
    Box(
        modifier = Modifier
            .size(32.dp) // Size of your star
    ) {
        // Full empty star (gray background)
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Empty Star",
            tint = Color.LightGray,
            modifier = Modifier.fillMaxSize()
        )

        // Half star (yellow), clip it to show only half
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RectangleShape) // Clipping to a rectangle
                .offset(x= (-16).dp)
                .graphicsLayer {
                    // Position the second half of the star off-screen to make it a "half" star
                    translationX = 16.dp.toPx() // Move it to the left to only show half
                }
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Half Star",
                tint = Color.Yellow,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


@Composable
fun Image(index: Int, imageInfo: ImageInfo, selection: MutableState<Int>) {

        Card(modifier=Modifier
            .size(200.dp, 200.dp)
            .border(BorderStroke(2.dp, Color.Black))
            .clickable { selection.value = index }
        ) {
            Column(modifier=Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage( // loads the image of the scene
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(imageInfo.imageLink)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(.9f)
                        .height(150.dp)
                        .padding(top = 10.dp)
                )
                Text(imageInfo.title, fontSize = 15.sp)
                Row(modifier=
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center) {
                    for (i in 1..<imageInfo.review) { // full stars up until last star
                        Star()
                    }

                }
            }
        }
}





@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteScreen() {

    val selection = remember { mutableIntStateOf(-1) }

    val info = listOf(
        ImageInfo(
            "https://lh3.googleusercontent.com/p/AF1QipOopkvADF48w2c8Vl_SmrhYqXPN3bUuQT-JNXLH=w390-h262-n-k-no",
            5,
            "Place A",
            "Lovely resort. Lots of clouds."
        ),

        ImageInfo(
            "https://encrypted-tbn1.gstatic.com/licensed-image?q=tbn:ANd9GcQ-idtGHTgKcT6uApQYW0EvdxMABxAm__T-7PTWJdbHy_O1ejBK04M2Dvp6o6-MJO2bxJBpUtCP3MrevwP9iSZJYOtA_ELkA4b1mxSn6g",
            5,
            "Place B",
            "Great during the fall"
        )


    )

    Column(modifier = Modifier // might have to add vertical padding
        .verticalScroll(rememberScrollState())
        .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally) {


        FlowRow(
            horizontalArrangement = Arrangement
                .spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            maxItemsInEachRow = 2, // Space between items horizontally
        ) {
            info.forEachIndexed {
                index, imageInfo ->
                    Image(index, imageInfo, selection)
                    if(index==selection.intValue) {
                        MinimalDialog(imageInfo.title, imageInfo.description) {
                            selection.intValue = -1
                        }
                    }

            }
        }
    }
}


//@Preview
//@Composable
//fun Preview() {
//    FavoriteScreen()
//}