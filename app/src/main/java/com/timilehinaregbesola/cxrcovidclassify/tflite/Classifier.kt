package com.timilehinaregbesola.cxrcovidclassify.tflite

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.os.Trace
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.io.IOException
import java.nio.MappedByteBuffer
import java.util.*
import kotlin.math.min

/** A classifier specialized to label images using TensorFlow Lite.  */
abstract class Classifier protected constructor(
    activity: Activity?,
    device: Device?,
    numThreads: Int
) {
    /** The runtime device type used for executing classification.  */
    enum class Device {
        CPU, GPU
    }

    /** The loaded TensorFlow Lite model.  */
    private var tfliteModel: MappedByteBuffer?
    /** Get the image size along the x axis.  */
    /** Image size along the x axis.  */
    val imageSizeX: Int
    /** Get the image size along the y axis.  */
    /** Image size along the y axis.  */
    val imageSizeY: Int

    /** Optional GPU delegate for acceleration.  */ //  Declare a GPU delegate
    private var gpuDelegate: GpuDelegate? = null

    /** An instance of the driver class to run model inference with Tensorflow Lite.  */ // Declare a TFLite interpreter
    protected var tflite: Interpreter?

    /** Options for configuring the Interpreter.  */
    private val tfliteOptions = Interpreter.Options()

    /** Labels corresponding to the output of the vision model.  */
    private val labels: List<String>

    /** Input image TensorBuffer.  */
    private var inputImageBuffer: TensorImage

    /** Output probability TensorBuffer.  */
    private val outputProbabilityBuffer: TensorBuffer

    /** Processer to apply post processing of the output probability.  */
    private val probabilityProcessor: TensorProcessor

    /** An immutable result returned by a Classifier describing what was recognized.  */
    data class Recognition(
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        val id: String?,
        /** Display name for the recognition.  */
        val title: String?,
        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        val confidence: Float?,
        /** Optional location within the source image for the location of the recognized object.  */
        private var location: RectF?
    ) {
        override fun toString(): String {
            var resultString = ""
            if (id != null) {
                resultString += "[$id] "
            }
            if (title != null) {
                resultString += "$title "
            }
            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f)
            }
            if (location != null) {
                resultString += location.toString() + " "
            }
            return resultString.trim { it <= ' ' }
        }
    }

//    /** Runs inference and returns the classification results.  */
//    fun recognizeImage(bitmap: Bitmap, sensorOrientation: Int): List<Recognition> {
//        // Logs this method so that it can be analyzed with systrace.
//        Trace.beginSection("recognizeImage")
//        Trace.beginSection("loadImage")
//        val startTimeForLoadImage = SystemClock.uptimeMillis()
//        inputImageBuffer = loadImage(bitmap, sensorOrientation)
//        val endTimeForLoadImage = SystemClock.uptimeMillis()
//        Trace.endSection()
//        Timber.v("Timecost to load the image: %s", (endTimeForLoadImage - startTimeForLoadImage))
//
//        // Runs the inference call.
//        Trace.beginSection("runInference")
//        val startTimeForReference = SystemClock.uptimeMillis()
//        // Run TFLite inference
//        tflite!!.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
//        val endTimeForReference = SystemClock.uptimeMillis()
//        Trace.endSection()
//        Timber.v("Timecost to run model inference: %s", (endTimeForReference - startTimeForReference))
//
//        // Gets the map of label and probability.
//        // Use TensorLabel from TFLite Support Library to associate the probabilities
//        //       with category labels
//        val labeledProbability: Map<String, Float> =
//            TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
//                .mapWithFloatValue
//        Trace.endSection()
//
//        // Gets top-k results.
//        return getTopKProbability(labeledProbability)
//    }

    /** Runs inference and returns the classification results.  */
    fun recognizeImage(bitmap: Bitmap, sensorOrientation: Int): Float {
        // Logs this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage")
        Trace.beginSection("loadImage")
        Timber.d("fire: $sensorOrientation")
        val startTimeForLoadImage = SystemClock.uptimeMillis()
        inputImageBuffer = loadImage(bitmap, sensorOrientation)
        val endTimeForLoadImage = SystemClock.uptimeMillis()
        Trace.endSection()
        Timber.v("Timecost to load the image: %s", (endTimeForLoadImage - startTimeForLoadImage))

        // Runs the inference call.
        Trace.beginSection("runInference")
        val startTimeForReference = SystemClock.uptimeMillis()
        // Run TFLite inference
        tflite!!.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
        val endTimeForReference = SystemClock.uptimeMillis()
        Trace.endSection()
        Timber.v("Timecost to run model inference: %s", (endTimeForReference - startTimeForReference))

        // Gets the map of label and probability.
        // Use TensorLabel from TFLite Support Library to associate the probabilities
        //       with category labels
        probabilityProcessor.process(outputProbabilityBuffer)
        Timber.d(outputProbabilityBuffer.floatArray[0].toString())
//        val labeledProbability: Map<String, Float> =
//            TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
//                .mapWithFloatValue
//        Timber.d(labeledProbability.toString())
        Trace.endSection()

        // Gets top-k results.
        return outputProbabilityBuffer.floatArray[0]
    }

    /** Closes the interpreter and model to release resources.  */
    fun close() {
        if (tflite != null) {
            // Close the interpreter
            tflite!!.close()
            tflite = null
        }
        // Close the GPU delegate
        if (gpuDelegate != null) {
            gpuDelegate!!.close()
            gpuDelegate = null
        }
        tfliteModel = null
    }

    /** Loads input image, and applies preprocessing.  */
    private fun loadImage(bitmap: Bitmap, sensorOrientation: Int): TensorImage {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap)

        // Creates processor for the TensorImage.
        val cropSize = min(bitmap.width, bitmap.height)
        val numRoration = sensorOrientation / 90
        // Fuse ops inside ImageProcessor.
        // Define an ImageProcessor from TFLite Support Library to do preprocessing
        val imageProcessor: ImageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(Rot90Op(numRoration))
            .add(preprocessNormalizeOp)
            .build()
        return imageProcessor.process(inputImageBuffer)
    }

    /** Gets the name of the model file stored in Assets.  */
    protected abstract val modelPath: String?

    /** Gets the name of the label file stored in Assets.  */
    protected abstract val labelPath: String?

    /** Gets the TensorOperator to nomalize the input image in preprocessing.  */
    protected abstract val preprocessNormalizeOp: TensorOperator?

    /**
     * Gets the TensorOperator to dequantize the output probability in post processing.
     *
     *
     * For quantized model, we need de-quantize the prediction with NormalizeOp (as they are all
     * essentially linear transformation). For float model, de-quantize is not required. But to
     * uniform the API, de-quantize is added to float model too. Mean and std are set to 0.0f and
     * 1.0f, respectively.
     */
    protected abstract val postprocessNormalizeOp: TensorOperator?

    companion object {
        /** Number of results to show in the UI.  */
        private const val MAX_RESULTS = 2

        /**
         * Creates a classifier with the provided configuration.
         *
         * @param activity The current Activity.
         * @param device The device to use for classification.
         * @param numThreads The number of threads to use for classification.
         * @return A classifier with the desired configuration.
         */
        @Throws(IOException::class)
        fun create(activity: Activity?, device: Device?, numThreads: Int): Classifier {
            return ClassifierQuantizedResNet(activity, device, numThreads)
        }

        /** Gets the top-k results.  */
        private fun getTopKProbability(labelProb: Map<String, Float>): List<Recognition> {
            // Find the best classifications.
            val pq = PriorityQueue(
                MAX_RESULTS,
                Comparator<Recognition?> { lhs, rhs -> // Intentionally reversed to put high confidence at the head of the queue.
                    (rhs?.confidence!!).compareTo(lhs?.confidence!!)
                }
            )
            for ((key, value) in labelProb) {
                pq.add(Recognition("" + key, key, value, null))
            }
            val recognitions = ArrayList<Recognition>()
            val recognitionsSize = min(pq.size, MAX_RESULTS)
            for (i in 0 until recognitionsSize) {
                recognitions.add(pq.poll()!!)
            }
            return recognitions
        }
    }

    /** Initializes a `Classifier`.  */
    init {
        tfliteModel = FileUtil.loadMappedFile(activity!!, modelPath!!)
//        tfliteModel = ModelQuant.newInstance(activity!!)
        when (device) {
            Device.GPU -> {
                // Create a GPU delegate instance and add it to the interpreter options
                gpuDelegate = GpuDelegate()
                tfliteOptions.addDelegate(gpuDelegate)
            }
            Device.CPU -> {
            }
        }
        tfliteOptions.setNumThreads(numThreads)
        // Create a TFLite interpreter instance
        tflite = Interpreter(tfliteModel!!, tfliteOptions)

        // Loads labels out from the label file.
        labels = FileUtil.loadLabels(activity, labelPath!!)

        // Reads type and shape of input and output tensors, respectively.
        val imageTensorIndex = 0
        val imageShape = tflite!!.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}
        imageSizeY = imageShape[1]
        imageSizeX = imageShape[2]
        val imageDataType = tflite!!.getInputTensor(imageTensorIndex).dataType()
        val probabilityTensorIndex = 0
        val probabilityShape =
            tflite!!.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}
        val probabilityDataType = tflite!!.getOutputTensor(probabilityTensorIndex).dataType()

        // Creates the input tensor.
        inputImageBuffer = TensorImage(imageDataType)

        // Creates the output tensor and its processor.
        outputProbabilityBuffer =
            TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)

        // Creates the post processor for the output probability.
        probabilityProcessor = TensorProcessor.Builder().add(postprocessNormalizeOp).build()
        Timber.d("Created a Tensorflow Lite Image Classifier.")
    }
}
