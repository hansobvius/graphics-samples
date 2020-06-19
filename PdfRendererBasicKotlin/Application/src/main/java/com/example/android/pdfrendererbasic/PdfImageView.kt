package com.example.android.pdfrendererbasic

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.appcompat.widget.AppCompatImageView

class PdfImageView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0) : AppCompatImageView(context, attrs, defStyle) {

    var currentZoom = 0f
        private set

    private var touchMatrix: Matrix? = null
    private var prevMatrix: Matrix? = null
    var isZoomEnabled = false
    private var isRotateImageToFitScreen = false

    enum class FixedPixel {
        CENTER, TOP_LEFT, BOTTOM_RIGHT
    }

    var orientationChangeFixedPixel: FixedPixel? = FixedPixel.CENTER
    var viewSizeChangeFixedPixel: FixedPixel? = FixedPixel.CENTER
    private var orientationJustChanged = false

    private enum class State {
        NONE, DRAG, ZOOM, FLING
    }

    private var state: State? = null
    private var userSpecifiedMinScale = 0f
    private var minScale = 0f
    private var maxScaleIsSetByMultiplier = false
    private var maxScaleMultiplier = 0f
    private var maxScale = 0f
    private var superMinScale = 0f
    private var superMaxScale = 0f
    private var floatMatrix: FloatArray? = null

    private var fling: Fling? = null
    private var orientation = 0
    private var touchScaleType: ScaleType? = null
    private var imageRenderedAtLeastOnce = false
    private var onDrawReady = false

    private var viewWidth = 0
    private var viewHeight = 0
    private var prevViewWidth = 0
    private var prevViewHeight = 0

    private var matchViewWidth = 0f
    private var matchViewHeight = 0f
    private var prevMatchViewWidth = 0f
    private var prevMatchViewHeight = 0f
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    private var userTouchListener: OnTouchListener? = null
    private var touchImageViewListener: OnTouchImageViewListener? = null

    init {
        super.setClickable(true)
        orientation = resources.configuration.orientation
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetector(context, GestureListener())
        touchMatrix = Matrix()
        prevMatrix = Matrix()
        floatMatrix = FloatArray(9)
        currentZoom = 1f
        if (touchScaleType == null) {
            touchScaleType = ScaleType.FIT_CENTER
        }
        minScale = 1f
        maxScale = 3f
        superMinScale = SUPER_MIN_MULTIPLIER * minScale
        superMaxScale = SUPER_MAX_MULTIPLIER * maxScale
        imageMatrix = touchMatrix
        scaleType = ScaleType.MATRIX
        setState(State.NONE)
        onDrawReady = false
        super.setOnTouchListener(PrivateOnTouchListener())
        val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.TouchImageView, defStyle, 0)
        try {
            if (!isInEditMode) {
                isZoomEnabled = attributes.getBoolean(R.styleable.TouchImageView_zoom_enabled, true)
            }
        } finally {
            attributes.recycle()
        }
    }

    override fun setOnTouchListener(onTouchListener: OnTouchListener) {
        userTouchListener = onTouchListener
    }

    override fun setImageResource(resId: Int) {
        imageRenderedAtLeastOnce = false
        super.setImageResource(resId)
        savePreviousImageValues()
        fitImageToView()
    }

    override fun setImageBitmap(bm: Bitmap) {
        imageRenderedAtLeastOnce = false
        super.setImageBitmap(bm)
        savePreviousImageValues()
        fitImageToView()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        imageRenderedAtLeastOnce = false
        super.setImageDrawable(drawable)
        savePreviousImageValues()
        fitImageToView()
    }

    override fun setImageURI(uri: Uri?) {
        imageRenderedAtLeastOnce = false
        super.setImageURI(uri)
        savePreviousImageValues()
        fitImageToView()
    }

    override fun setScaleType(type: ScaleType) {
        if (type == ScaleType.MATRIX) {
            super.setScaleType(ScaleType.MATRIX)
        } else {
            touchScaleType = type
            if (onDrawReady) {
                setZoom(this)
            }
        }
    }

    override fun getScaleType(): ScaleType {
        return touchScaleType!!
    }

    val isZoomed: Boolean
        get() = currentZoom != 1f

    fun savePreviousImageValues() {
        if (touchMatrix != null && viewHeight != 0 && viewWidth != 0) {
            touchMatrix!!.getValues(floatMatrix)
            prevMatrix!!.setValues(floatMatrix)
            prevMatchViewHeight = matchViewHeight
            prevMatchViewWidth = matchViewWidth
            prevViewHeight = viewHeight
            prevViewWidth = viewWidth
        }
    }

    override fun onDraw(canvas: Canvas) {
        onDrawReady = true
        imageRenderedAtLeastOnce = true
        super.onDraw(canvas)
    }

    public override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = resources.configuration.orientation
        if (newOrientation != orientation) {
            orientationJustChanged = true
            orientation = newOrientation
        }
        savePreviousImageValues()
    }
    fun setMaxZoomRatio(max: Float) {
        maxScaleMultiplier = max
        maxScale = minScale * maxScaleMultiplier
        superMaxScale = SUPER_MAX_MULTIPLIER * maxScale
        maxScaleIsSetByMultiplier = true
    }

    var minZoom: Float
        get() = minScale
        set(min) {
            userSpecifiedMinScale = min
            if (min == AUTOMATIC_MIN_ZOOM) {
                if (touchScaleType == ScaleType.CENTER || touchScaleType == ScaleType.CENTER_CROP) {
                    val drawable = drawable
                    val drawableWidth = getDrawableWidth(drawable)
                    val drawableHeight = getDrawableHeight(drawable)
                    if (drawable != null && drawableWidth > 0 && drawableHeight > 0) {
                        val widthRatio = viewWidth.toFloat() / drawableWidth
                        val heightRatio = viewHeight.toFloat() / drawableHeight
                        minScale = if (touchScaleType == ScaleType.CENTER) {
                            Math.min(widthRatio, heightRatio)
                        } else {  // CENTER_CROP
                            Math.min(widthRatio, heightRatio) / Math.max(widthRatio, heightRatio)
                        }
                    }
                } else {
                    minScale = 1.0f
                }
            } else {
                minScale = userSpecifiedMinScale
            }
            if (maxScaleIsSetByMultiplier) {
                setMaxZoomRatio(maxScaleMultiplier)
            }
            superMinScale = SUPER_MIN_MULTIPLIER * minScale
        }

    fun resetZoom() {
        currentZoom = 1f
        fitImageToView()
    }

    fun setZoom(scale: Float, focusX: Float, focusY: Float, scaleType: ScaleType?) {
        if (userSpecifiedMinScale == AUTOMATIC_MIN_ZOOM) {
            minZoom = AUTOMATIC_MIN_ZOOM
            if (currentZoom < minScale) {
                currentZoom = minScale
            }
        }
        if (scaleType != touchScaleType) {
            setScaleType(scaleType!!)
        }
        resetZoom()
        scaleImage(scale.toDouble(), viewWidth / 2.toFloat(), viewHeight / 2.toFloat(), true)
        touchMatrix!!.getValues(floatMatrix)
        floatMatrix!![Matrix.MTRANS_X] = -(focusX * imageWidth - viewWidth * 0.5f)
        floatMatrix!![Matrix.MTRANS_Y] = -(focusY * imageHeight - viewHeight * 0.5f)
        touchMatrix!!.setValues(floatMatrix)
        fixTrans()
        savePreviousImageValues()
        imageMatrix = touchMatrix
    }

    fun setZoom(img: PdfImageView) {
        val center = img.scrollPosition
        setZoom(img.currentZoom, center.x, center.y, img.scaleType)
    }

    val scrollPosition: PointF
        get() {
            val drawable = drawable ?: return PointF(.5f, .5f)
            val drawableWidth = getDrawableWidth(drawable)
            val drawableHeight = getDrawableHeight(drawable)
            val point = transformCoordTouchToBitmap(viewWidth / 2.toFloat(), viewHeight / 2.toFloat(), true)
            point.x /= drawableWidth.toFloat()
            point.y /= drawableHeight.toFloat()
            return point
        }

    private fun orientationMismatch(drawable: Drawable?): Boolean {
        return viewWidth > viewHeight != drawable!!.intrinsicWidth > drawable.intrinsicHeight
    }

    private fun getDrawableWidth(drawable: Drawable?): Int {
        return if (orientationMismatch(drawable) && isRotateImageToFitScreen) {
            drawable!!.intrinsicHeight
        } else drawable!!.intrinsicWidth
    }

    private fun getDrawableHeight(drawable: Drawable?): Int {
        return if (orientationMismatch(drawable) && isRotateImageToFitScreen) {
            drawable!!.intrinsicWidth
        } else drawable!!.intrinsicHeight
    }

    private fun fixTrans() {
        touchMatrix!!.getValues(floatMatrix)
        val transX = floatMatrix!![Matrix.MTRANS_X]
        val transY = floatMatrix!![Matrix.MTRANS_Y]
        var offset = 0f
        if (isRotateImageToFitScreen && orientationMismatch(drawable)) {
            offset = imageWidth
        }
        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), imageWidth, offset)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), imageHeight, 0f)
        touchMatrix!!.postTranslate(fixTransX, fixTransY)
    }

    private fun fixScaleTrans() {
        fixTrans()
        touchMatrix!!.getValues(floatMatrix)
        if (imageWidth < viewWidth) {
            var xOffset = (viewWidth - imageWidth) / 2
            if (isRotateImageToFitScreen && orientationMismatch(drawable)) {
                xOffset += imageWidth
            }
            floatMatrix!![Matrix.MTRANS_X] = xOffset
        }
        if (imageHeight < viewHeight) {
            floatMatrix!![Matrix.MTRANS_Y] = (viewHeight - imageHeight) / 2
        }
        touchMatrix!!.setValues(floatMatrix)
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float, offset: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = offset
            maxTrans = offset + viewSize - contentSize
        } else {
            minTrans = offset + viewSize - contentSize
            maxTrans = offset
        }
        if (trans < minTrans) return -trans + minTrans
        return if (trans > maxTrans) -trans + maxTrans else 0f
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else
            delta
    }

    private val imageWidth: Float
        get() = matchViewWidth * currentZoom

    private val imageHeight: Float
        get() = matchViewHeight * currentZoom

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val drawable = drawable
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            setMeasuredDimension(0, 0)
            return
        }
        val drawableWidth = getDrawableWidth(drawable)
        val drawableHeight = getDrawableHeight(drawable)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val totalViewWidth = setViewSize(widthMode, widthSize, drawableWidth)
        val totalViewHeight = setViewSize(heightMode, heightSize, drawableHeight)
        if (!orientationJustChanged) {
            savePreviousImageValues()
        }

        val width = totalViewWidth - paddingLeft - paddingRight
        val height = totalViewHeight - paddingTop - paddingBottom

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        fitImageToView()
    }

    private fun fitImageToView() {
        val fixedPixel = if (orientationJustChanged) orientationChangeFixedPixel else viewSizeChangeFixedPixel
        orientationJustChanged = false
        val drawable = drawable
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            return
        }
        if (touchMatrix == null || prevMatrix == null) {
            return
        }
        if (userSpecifiedMinScale == AUTOMATIC_MIN_ZOOM) {
            minZoom = AUTOMATIC_MIN_ZOOM
            if (currentZoom < minScale) {
                currentZoom = minScale
            }
        }
        val drawableWidth = getDrawableWidth(drawable)
        val drawableHeight = getDrawableHeight(drawable)

        var scaleX = viewWidth.toFloat() / drawableWidth
        var scaleY = viewHeight.toFloat() / drawableHeight
        when (touchScaleType) {
            ScaleType.CENTER -> {
                scaleY = 1f
                scaleX = scaleY
            }
            ScaleType.CENTER_CROP -> {
                scaleY = Math.max(scaleX, scaleY)
                scaleX = scaleY
            }
            ScaleType.CENTER_INSIDE -> {
                run {
                    scaleY = Math.min(1f, Math.min(scaleX, scaleY))
                    scaleX = scaleY
                }
                run {
                    scaleY = Math.min(scaleX, scaleY)
                    scaleX = scaleY
                }
            }
            ScaleType.FIT_CENTER, ScaleType.FIT_START, ScaleType.FIT_END -> {
                scaleY = Math.min(scaleX, scaleY)
                scaleX = scaleY
            }
            ScaleType.FIT_XY -> {
            }
            else -> {
            }
        }

        val redundantXSpace = viewWidth - scaleX * drawableWidth
        val redundantYSpace = viewHeight - scaleY * drawableHeight
        matchViewWidth = viewWidth - redundantXSpace
        matchViewHeight = viewHeight - redundantYSpace
        if (!isZoomed && !imageRenderedAtLeastOnce) {

            if (isRotateImageToFitScreen && orientationMismatch(drawable)) {
                touchMatrix!!.setRotate(90f)
                touchMatrix!!.postTranslate(drawableWidth.toFloat(), 0f)
                touchMatrix!!.postScale(scaleX, scaleY)
            } else {
                touchMatrix!!.setScale(scaleX, scaleY)
            }
            when (touchScaleType) {
                ScaleType.FIT_START -> touchMatrix!!.postTranslate(0f, 0f)
                ScaleType.FIT_END -> touchMatrix!!.postTranslate(redundantXSpace, redundantYSpace)
                else -> touchMatrix!!.postTranslate(redundantXSpace / 2, redundantYSpace / 2)
            }
            currentZoom = 1f
        } else {
            if (prevMatchViewWidth == 0f || prevMatchViewHeight == 0f) {
                savePreviousImageValues()
            }

            prevMatrix!!.getValues(floatMatrix)

            floatMatrix!![Matrix.MSCALE_X] = matchViewWidth / drawableWidth * currentZoom
            floatMatrix!![Matrix.MSCALE_Y] = matchViewHeight / drawableHeight * currentZoom

            val transX = floatMatrix!![Matrix.MTRANS_X]
            val transY = floatMatrix!![Matrix.MTRANS_Y]

            val prevActualWidth = prevMatchViewWidth * currentZoom
            val actualWidth = imageWidth
            floatMatrix!![Matrix.MTRANS_X] = newTranslationAfterChange(transX, prevActualWidth, actualWidth, prevViewWidth, viewWidth, drawableWidth, fixedPixel)

            val prevActualHeight = prevMatchViewHeight * currentZoom
            val actualHeight = imageHeight
            floatMatrix!![Matrix.MTRANS_Y] = newTranslationAfterChange(transY, prevActualHeight, actualHeight, prevViewHeight, viewHeight, drawableHeight, fixedPixel)

            touchMatrix!!.setValues(floatMatrix)
        }
        fixTrans()
        imageMatrix = touchMatrix
    }

    private fun setViewSize(mode: Int, size: Int, drawableWidth: Int): Int {
        val viewSize: Int
        viewSize = when (mode) {
            MeasureSpec.EXACTLY -> size
            MeasureSpec.AT_MOST -> Math.min(drawableWidth, size)
            MeasureSpec.UNSPECIFIED -> drawableWidth
            else -> size
        }
        return viewSize
    }

    private fun newTranslationAfterChange(trans: Float, prevImageSize: Float, imageSize: Float, prevViewSize: Int, viewSize: Int, drawableSize: Int, sizeChangeFixedPixel: FixedPixel?): Float {
        return if (imageSize < viewSize) {
            (viewSize - drawableSize * floatMatrix!![Matrix.MSCALE_X]) * 0.5f
        } else if (trans > 0) {

            -((imageSize - viewSize) * 0.5f)
        } else {

            var fixedPixelPositionInView = 0.5f // CENTER
            if (sizeChangeFixedPixel == FixedPixel.BOTTOM_RIGHT) {
                fixedPixelPositionInView = 1.0f
            } else if (sizeChangeFixedPixel == FixedPixel.TOP_LEFT) {
                fixedPixelPositionInView = 0.0f
            }

            val fixedPixelPositionInImage = (-trans + fixedPixelPositionInView * prevViewSize) / prevImageSize

            -(fixedPixelPositionInImage * imageSize - viewSize * fixedPixelPositionInView)
        }
    }

    private fun setState(state: State) {
        this.state = state
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        touchMatrix!!.getValues(floatMatrix)
        val x = floatMatrix!![Matrix.MTRANS_X]
        return if (imageWidth < viewWidth) {
            false
        } else if (x >= -1 && direction < 0) {
            false
        } else Math.abs(x) + viewWidth + 1 < imageWidth || direction <= 0
    }

    override fun canScrollVertically(direction: Int): Boolean {
        touchMatrix!!.getValues(floatMatrix)
        val y = floatMatrix!![Matrix.MTRANS_Y]
        return if (imageHeight < viewHeight) {
            false
        } else if (y >= -1 && direction < 0) {
            false
        } else Math.abs(y) + viewHeight + 1 < imageHeight || direction <= 0
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            fling?.cancelFling()
            fling = Fling(velocityX.toInt(), velocityY.toInt())
                .also { compatPostOnAnimation(it) }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    interface OnTouchImageViewListener {
        fun onMove()
    }

    private inner class PrivateOnTouchListener : OnTouchListener {

        private val last = PointF()
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (drawable == null) {
                setState(State.NONE)
                return false
            }
            if (isZoomEnabled) {
                mScaleDetector!!.onTouchEvent(event)
            }
            mGestureDetector!!.onTouchEvent(event)
            val curr = PointF(event.x, event.y)
            if (state == State.NONE || state == State.DRAG || state == State.FLING) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        last.set(curr)
                        if (fling != null) fling!!.cancelFling()
                        setState(State.DRAG)
                    }
                    MotionEvent.ACTION_MOVE -> if (state == State.DRAG) {
                        val deltaX = curr.x - last.x
                        val deltaY = curr.y - last.y
                        val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), imageWidth)
                        val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), imageHeight)
                        touchMatrix!!.postTranslate(fixTransX, fixTransY)
                        fixTrans()
                        last[curr.x] = curr.y
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> setState(State.NONE)
                }
            }
            imageMatrix = touchMatrix

            if (userTouchListener != null) {
                userTouchListener!!.onTouch(v, event)
            }

            if (touchImageViewListener != null) {
                touchImageViewListener!!.onMove()
            }

            return true
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            setState(State.ZOOM)
            return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleImage(detector.scaleFactor.toDouble(), detector.focusX, detector.focusY, true)

            if (touchImageViewListener != null) {
                touchImageViewListener!!.onMove()
            }
            return true
        }
    }

    private fun scaleImage(deltaScale: Double, focusX: Float, focusY: Float, stretchImageToSuper: Boolean) {
        var deltaScaleLocal = deltaScale
        val lowerScale: Float
        val upperScale: Float
        if (stretchImageToSuper) {
            lowerScale = superMinScale
            upperScale = superMaxScale
        } else {
            lowerScale = minScale
            upperScale = maxScale
        }
        val origScale = currentZoom
        currentZoom *= deltaScaleLocal.toFloat()
        if (currentZoom > upperScale) {
            currentZoom = upperScale
            deltaScaleLocal = upperScale / origScale.toDouble()
        } else if (currentZoom < lowerScale) {
            currentZoom = lowerScale
            deltaScaleLocal = lowerScale / origScale.toDouble()
        }
        touchMatrix!!.postScale(deltaScaleLocal.toFloat(), deltaScaleLocal.toFloat(), focusX, focusY)
        fixScaleTrans()
    }

    protected fun transformCoordTouchToBitmap(x: Float, y: Float, clipToBitmap: Boolean): PointF {
        touchMatrix!!.getValues(floatMatrix)
        val origW = drawable.intrinsicWidth.toFloat()
        val origH = drawable.intrinsicHeight.toFloat()
        val transX = floatMatrix!![Matrix.MTRANS_X]
        val transY = floatMatrix!![Matrix.MTRANS_Y]
        var finalX = (x - transX) * origW / imageWidth
        var finalY = (y - transY) * origH / imageHeight
        if (clipToBitmap) {
            finalX = Math.min(Math.max(finalX, 0f), origW)
            finalY = Math.min(Math.max(finalY, 0f), origH)
        }
        return PointF(finalX, finalY)
    }

    private inner class Fling internal constructor(velocityX: Int, velocityY: Int) : Runnable {
        var scroller: CompatScroller?
        var currX: Int
        var currY: Int
        fun cancelFling() {
            if (scroller != null) {
                setState(State.NONE)
                scroller!!.forceFinished(true)
            }
        }

        override fun run() {
            if (touchImageViewListener != null) {
                touchImageViewListener!!.onMove()
            }
            if (scroller!!.isFinished) {
                scroller = null
                return
            }
            if (scroller!!.computeScrollOffset()) {
                val newX = scroller!!.currX
                val newY = scroller!!.currY
                val transX = newX - currX
                val transY = newY - currY
                currX = newX
                currY = newY
                touchMatrix!!.postTranslate(transX.toFloat(), transY.toFloat())
                fixTrans()
                imageMatrix = touchMatrix
                compatPostOnAnimation(this)
            }
        }

        init {
            setState(State.FLING)
            scroller = CompatScroller(context)
            touchMatrix!!.getValues(floatMatrix)
            var startX = floatMatrix!![Matrix.MTRANS_X].toInt()
            val startY = floatMatrix!![Matrix.MTRANS_Y].toInt()
            val minX: Int
            val maxX: Int
            val minY: Int
            val maxY: Int
            if (isRotateImageToFitScreen && orientationMismatch(drawable)) {
                startX -= imageWidth.toInt()
            }
            if (imageWidth > viewWidth) {
                minX = viewWidth - imageWidth.toInt()
                maxX = 0
            } else {
                maxX = startX
                minX = maxX
            }
            if (imageHeight > viewHeight) {
                minY = viewHeight - imageHeight.toInt()
                maxY = 0
            } else {
                maxY = startY
                minY = maxY
            }
            scroller!!.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
            currX = startX
            currY = startY
        }
    }

    private inner class CompatScroller internal constructor(context: Context?) {
        var overScroller: OverScroller
        fun fling(startX: Int, startY: Int, velocityX: Int, velocityY: Int, minX: Int, maxX: Int, minY: Int, maxY: Int) {
            overScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        }

        fun forceFinished(finished: Boolean) {
            overScroller.forceFinished(finished)
        }

        val isFinished: Boolean
            get() = overScroller.isFinished

        fun computeScrollOffset(): Boolean {
            overScroller.computeScrollOffset()
            return overScroller.computeScrollOffset()
        }

        val currX: Int
            get() = overScroller.currX

        val currY: Int
            get() = overScroller.currY

        init {
            overScroller = OverScroller(context)
        }
    }

    private fun compatPostOnAnimation(runnable: Runnable) {
        postOnAnimation(runnable)
    }


    companion object {
        private const val SUPER_MIN_MULTIPLIER = .75f
        private const val SUPER_MAX_MULTIPLIER = 1.25f
        const val AUTOMATIC_MIN_ZOOM = -1.0f
    }

}