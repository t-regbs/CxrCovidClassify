package com.timilehinaregbesola.cxrcovidclassify

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.cxrcovidclassify.ml.Covid
import com.timilehinaregbesola.cxrcovidclassify.utils.ImageUtils.convertBitmapToByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber

class UploadViewModel : ViewModel() {
    private val result: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    fun process(model: Covid, bitmap: Bitmap) {
        viewModelScope.launch {
            processImage(model, bitmap)
        }
    }
    private suspend fun processImage(model: Covid, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val byteBuffer = convertBitmapToByteBuffer(resized)
            val tfBuffer =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            println("buffer size: ${byteBuffer.capacity()}")
            println("tfbuffer size: ${tfBuffer.buffer.capacity()}")
            tfBuffer.loadBuffer(byteBuffer)
            println("tfbuffer: ${tfBuffer.buffer.array()[10]}")
            println("buffer: ${byteBuffer.array()[10]}")

            val outputs = model.process(tfBuffer)
                .outputFeature0AsTensorBuffer.getFloatValue(0)
            Timber.v("Detect: %s", outputs)
            result.postValue(outputs)
            model.close()
        }
    }
    fun getResult(): LiveData<Float> = result
}
