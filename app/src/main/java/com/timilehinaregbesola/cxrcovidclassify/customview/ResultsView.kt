package com.timilehinaregbesola.cxrcovidclassify.customview

import com.timilehinaregbesola.cxrcovidclassify.tflite.Classifier.Recognition

interface ResultsView {
    fun setResults(results: List<Recognition?>?)
}
