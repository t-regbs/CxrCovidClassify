package com.timilehinaregbesola.cxrcovidclassify.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.timilehinaregbesola.cxrcovidclassify.Recognition
import com.timilehinaregbesola.cxrcovidclassify.RecognitionListener
import com.timilehinaregbesola.cxrcovidclassify.ml.Covid
import kotlinx.coroutines.CoroutineScope
import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber

@ExperimentalGetImage
class CovidAnalyzer(ctx: Context, val scope: CoroutineScope, private val listener: RecognitionListener) : ImageAnalysis.Analyzer {

    private val cxrModel: Covid by lazy {

        // Optional GPU acceleration
        val compatList = CompatibilityList()

        val options = if (compatList.isDelegateSupportedOnThisDevice) {
            Timber.d("This device is GPU Compatible ")
            Model.Options.Builder().setDevice(Model.Device.GPU).build()
        } else {
            Timber.d("This device is GPU Incompatible ")
            Model.Options.Builder().setNumThreads(4).build()
        }

        // Initialize the CovidCXR Model
        Covid.newInstance(ctx, options)
    }

    private var inprogress = false

    override fun analyze(imageProxy: ImageProxy) {
        if (inprogress) {
            return
        }
        var item: Recognition
//        scope.launch {
        imageProxy.image?.let {
            inprogress = true
            // Convert Image to Bitmap, resize then to ByteBuffer
            val image = toBitmap(imageProxy)
            val resizedImage = Bitmap.createScaledBitmap(image!!, 224, 224, true)
            val byteBuffer = ImageUtils.convertBitmapToByteBuffer(resizedImage)

            val tfBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            tfBuffer.loadBuffer(byteBuffer)

            // Process the image using the trained model, sort and pick out the top results
            val outputs = cxrModel.process(tfBuffer)
                .outputFeature0AsTensorBuffer.floatArray[0]

            item = if (outputs < 0.5) {
                // means prediction was for category corresponding to 0
                Recognition("Negative", outputs)
            } else {
                // means prediction was for category corresponding to 1
                Recognition("Positive", outputs)
            }

            // Return the result
            listener(item)

            inprogress = false
            imageProxy.close()
        }
//        }
    }

    private val yuvToRgbConverter = YuvToRgbConverter(context = ctx)
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var rotationMatrix: Matrix

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun toBitmap(imageProxy: ImageProxy): Bitmap? {

        val image = imageProxy.image ?: return null

        // Initialise Buffer
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            Timber.d("Initalise toBitmap()")
            rotationMatrix = Matrix()
            rotationMatrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
            )
        }

        // Pass image to an image analyser
        yuvToRgbConverter.yuvToRgb(image, bitmapBuffer)

        // Create the Bitmap in the correct orientation
        return Bitmap.createBitmap(
            bitmapBuffer,
            0,
            0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            rotationMatrix,
            false
        )
    }
}
