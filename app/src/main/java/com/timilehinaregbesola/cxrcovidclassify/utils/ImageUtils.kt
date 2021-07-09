package com.timilehinaregbesola.cxrcovidclassify.utils

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Utility class for manipulating images.  */
object ImageUtils {
    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(224 * 224)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until 224) {
            for (j in 0 until 224) {
                val pixelVal = pixels[pixel++]

                val IMAGE_MEAN = 127.5f
                val IMAGE_STD = 127.5f
                byteBuffer.putFloat(((pixelVal shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((pixelVal shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((pixelVal and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        bitmap.recycle()

        return byteBuffer
    }
}
