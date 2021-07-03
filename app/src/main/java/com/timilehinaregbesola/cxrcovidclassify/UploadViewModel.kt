package com.timilehinaregbesola.cxrcovidclassify

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.cxrcovidclassify.tflite.Classifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class UploadViewModel() : ViewModel() {
    private val result: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    fun process(classifier: Classifier, bitmap: Bitmap) {
        viewModelScope.launch {
            processImage(classifier, bitmap)
        }
    }
    private suspend fun processImage(classifier: Classifier, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val startTime = SystemClock.uptimeMillis()
//                val results: List<Recognition?> =
//                    classifier!!.recognizeImage(rgbFrameBitmap!!, sensorOrientation!!)
            val results: Float =
                classifier.recognizeImage(bitmap, 90)
//            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
            Timber.v("Detect: %s", results)
            result.postValue(results)
        }
    }
    fun getResult(): LiveData<Float> = result
}
