package com.example.android.pdfrendererbasic

import android.content.Context
import android.graphics.Matrix
import android.view.ScaleGestureDetector
import android.widget.ImageView

class TouchEvent(val context: Context, val image: ImageView): ScaleGestureDetector.OnScaleGestureListener {

    private var mScaleFactor = 1f

    private lateinit var mScaleDetector: ScaleGestureDetector

    private var matrix: Matrix? = null

    init{
        matrix = Matrix()
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        mScaleFactor = mScaleFactor * detector!!.scaleFactor
        mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5f))
        matrix?.setScale(mScaleFactor, mScaleFactor)
        image.imageMatrix = matrix
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        TODO("Not yet implemented")
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        TODO("Not yet implemented")
    }
}