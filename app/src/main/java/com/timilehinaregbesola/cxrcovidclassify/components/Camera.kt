package com.timilehinaregbesola.cxrcovidclassify.components

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

@Composable
@ExperimentalGetImage
fun SimpleCameraPreview(analyzer: ImageAnalysis.Analyzer) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
//    val previewView = remember { PreviewView(context).apply { id = R.id.preview_view } }
    // TODO: Only cos of Camera not showing well on emulator. Remove later...
//    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

    AndroidView(
        factory = { ctx ->
            val preview = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(
                        lifecycleOwner,
                        preview,
                        cameraProvider,
                        analyzer,
                        executor
                    )
                },
                executor
            )
            preview
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun bindPreview(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraProvider: ProcessCameraProvider,
    analyzer: ImageAnalysis.Analyzer,
    executor: Executor
) {
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
        CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        setupImageAnalysis(previewView, executor, analyzer)
    )
}

private fun setupImageAnalysis(
    previewView: PreviewView,
    executor: Executor,
    analyzer: ImageAnalysis.Analyzer
): ImageAnalysis {
    return ImageAnalysis.Builder()
        // This sets the ideal size for the image to be analyse, CameraX will choose the
        // the most suitable resolution which may not be exactly the same or hold the same
        // aspect ratio
        .setTargetResolution(Size(224, 224))
        // How the Image Analyser should pipe in input, 1. every frame but drop no frame, or
        // 2. go to the latest frame and may drop some frame. The default is 2.
        // STRATEGY_KEEP_ONLY_LATEST. The following line is optional, kept here for clarity
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(executor, analyzer)
        }
}
