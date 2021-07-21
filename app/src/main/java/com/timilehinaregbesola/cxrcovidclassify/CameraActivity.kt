package com.timilehinaregbesola.cxrcovidclassify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.cxrcovidclassify.components.SimpleCameraPreview
import com.timilehinaregbesola.cxrcovidclassify.ui.CxrCovidClassifyTheme
import com.timilehinaregbesola.cxrcovidclassify.utils.CovidAnalyzer

// Listener for the result of the ImageAnalyzer
typealias RecognitionListener = (recognition: Recognition) -> Unit
@ExperimentalGetImage
class CameraActivity : ComponentActivity() {
    private val viewModel by viewModels<RecognitionListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (allPermissionsGranted()) {
            setViewContent()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.postDelayed(
            {
                window.decorView.systemUiVisibility = FLAGS_FULLSCREEN
            },
            IMMERSIVE_FLAG_TIMEOUT
        )
    }

    private fun setViewContent() {
        setContent {
            val prediction = viewModel.recognition.observeAsState()
            CxrCovidClassifyTheme {
                Box {
                    SimpleCameraPreview(
                        analyzer = CovidAnalyzer(this@CameraActivity, viewModel.viewModelScope) { item ->
                            // updating the list of recognised objects
                            viewModel.updateData(item)
                        }
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
                        color = White
                    )
                    if (prediction.value != null) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(color = Color(0, 0, 0, 53)),
                        ) {
                            Text(
                                modifier = Modifier
                                    .weight(2f)
                                    .padding(8.dp),
                                text = prediction.value!!.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = White
                            )
                            Text(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp),
                                text = prediction.value!!.probabilityString,
                                textAlign = TextAlign.End,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    }
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
        const val FLAGS_FULLSCREEN =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
}
