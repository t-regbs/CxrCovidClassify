package com.timilehinaregbesola.cxrcovidclassify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.cxrcovidclassify.screens.CameraScreen
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
                CameraScreen(
                    analyzer = CovidAnalyzer(this@CameraActivity, viewModel.viewModelScope) { item ->
                        // updating the list of recognised objects
                        viewModel.updateData(item)
                    },
                    prediction = prediction
                )
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setViewContent()
                }
            }
        }
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
