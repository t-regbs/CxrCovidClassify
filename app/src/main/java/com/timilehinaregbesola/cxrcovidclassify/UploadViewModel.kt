package com.timilehinaregbesola.cxrcovidclassify

import android.graphics.Bitmap
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
    private val _uploadState = MutableLiveData<UploadState>(UploadState.Init)
    val uploadState
        get() = _uploadState

    fun process(model: Covid, bitmaps: MutableList<Bitmap>) {
        _uploadState.value = UploadState.Loading
        viewModelScope.launch {
            processImages(model, bitmaps)
        }
    }
    private suspend fun processImages(model: Covid, bitmaps: MutableList<Bitmap>) {
        withContext(Dispatchers.IO) {
            val resultList = mutableListOf<Float>()
            for (bitmap in bitmaps) {
                val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
                val byteBuffer = convertBitmapToByteBuffer(resized)
                val tfBuffer =
                    TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
                tfBuffer.loadBuffer(byteBuffer)

                val outputs = model.process(tfBuffer)
                    .outputFeature0AsTensorBuffer.getFloatValue(0)
                Timber.v("Detect: %s", outputs)
                resultList.add(outputs)
            }
            _uploadState.postValue(UploadState.Scanned(floatListToPred(resultList)))
            model.close()
        }
    }
    fun setState(value: UploadState) {
        _uploadState.value = value
    }
}

private fun floatListToPred(output: List<Float>): List<Recognition> {
    val list = mutableListOf<Recognition>()
    for (out in output) {
        if (out < 0.5) {
            list.add(Recognition("Negative", out))
        } else {
            list.add(Recognition("Positive", out))
        }
    }
    return list
}

sealed class UploadState(val pred: List<Recognition> = listOf(Recognition("Negative", 0f))) {
    class Scanned(result: List<Recognition>) : UploadState(result)

    object Init : UploadState()

    object Unscanned : UploadState()

    object Loading : UploadState()
}
