package com.timilehinaregbesola.cxrcovidclassify.screens

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timilehinaregbesola.cxrcovidclassify.Recognition
import com.timilehinaregbesola.cxrcovidclassify.components.CameraResultsPanel
import com.timilehinaregbesola.cxrcovidclassify.components.SimpleCameraPreview
import com.timilehinaregbesola.cxrcovidclassify.utils.CovidAnalyzer

@ExperimentalGetImage
@Composable
fun CameraScreen(
    analyzer: CovidAnalyzer,
    prediction: State<Recognition?>
) {
    Box {
        SimpleCameraPreview(
            analyzer = analyzer
        )
        Text(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(56.dp)
                .background(color = Color(0, 0, 0, 53))
                .padding(16.dp),
            text = "Covid Scanner",
            textAlign = TextAlign.Center,
            color = Color.White
        )
        if (prediction.value != null) {
            CameraResultsPanel(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(color = Color(0, 0, 0, 53)),
                recognition = prediction.value!!
            )
        }
    }
}
