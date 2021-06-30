package com.timilehinaregbesola.cxrcovidclassify.customview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.*

// ktlint-disable no-wildcard-imports

/** A simple View providing a render callback to other classes.  */
class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private val callbacks: MutableList<DrawCallback> = LinkedList()
    fun addCallback(callback: DrawCallback) {
        callbacks.add(callback)
    }

    @Synchronized
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        for (callback in callbacks) {
            callback.drawCallback(canvas)
        }
    }

    /** Interface defining the callback for client classes.  */
    interface DrawCallback {
        fun drawCallback(canvas: Canvas?)
    }
}
