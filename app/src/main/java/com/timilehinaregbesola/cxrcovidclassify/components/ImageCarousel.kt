package com.timilehinaregbesola.cxrcovidclassify.components

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.timilehinaregbesola.cxrcovidclassify.screens.uriToBitmap
import kotlin.math.absoluteValue

@OptIn(ExperimentalPagerApi::class)
@Composable
fun ImageCarousel(
    pagerState: PagerState?,
    result: MutableState<List<Uri?>?>,
    context: Activity
) {
    HorizontalPager(
        state = pagerState!!,
        modifier = Modifier.fillMaxWidth(),
    ) { page ->
        Card(
            modifier = Modifier
                .width(261.dp)
                .height(316.dp)
                .graphicsLayer {
                    val pageOffset =
                        calculateCurrentOffsetForPage(page).absoluteValue

                    // We animate the scaleX + scaleY, between 85% and 100%
                    lerp(
                        start = 0.85f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    ).also { scale ->
                        scaleX = scale
                        scaleY = scale
                    }

                    // We animate the alpha, between 50% and 100%
                    alpha = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                },
            shape = MaterialTheme.shapes.medium.copy(CornerSize(24.dp)),
            elevation = 8.dp
        ) {
            if (result.value != null) {
                result.value!![page]?.let { it1 -> uriToBitmap(it1, context).asImageBitmap() }
                    ?.let { it2 ->
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = it2,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds
                        )
                    }
            }
        }
    }
}
