package com.timilehinaregbesola.cxrcovidclassify.tflite

import android.app.Activity
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.ops.NormalizeOp

/** This TensorFlow Lite classifier works with the quantized ResNet model.  */
class ClassifierQuantizedResNet
/**
 * Initializes a `ClassifierQuantizedResNet`.
 *
 * @param activity
 */
(activity: Activity?, device: Device?, numThreads: Int) :
    Classifier(activity, device, numThreads) {
    override val modelPath: String
        get() = "model_quant.tflite"

    override val labelPath: String
        get() = "labels.txt"
    override val preprocessNormalizeOp: TensorOperator
        get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)
    override val postprocessNormalizeOp: TensorOperator
        get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

    companion object {
        /**
         * The quantized model does not require normalization, thus set mean as 0.0f, and std as 1.0f to
         * bypass the normalization.
         */
        private const val IMAGE_MEAN = 0.0f
        private const val IMAGE_STD = 1.0f

        /** Quantized MobileNet requires additional dequantization to the output probability.  */
        private const val PROBABILITY_MEAN = 0.0f
        private const val PROBABILITY_STD = 255.0f
    }
}
