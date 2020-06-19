package com.example.android.pdfrendererbasic

import android.content.Context
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

    private var userTouchListener: OnTouchListener? = null
    private var listenerView: OnTouchImageViewListener? = null
    private var mMatrix: Matrix? = null
    private var matrixState: Matrix? = null
    private var typeGesture: TypeGesture? = null
    private var fMatrix: FloatArray? = null
    private var touchScaleType: ScaleType? = null
    private var flingGesture: FlingGesture? = null

    private var zoom = 0f
    private var zoomToggle = false
    private var screenView = false
    private var userSpecifiedMinScale = 0f
    private var minScale = 0f
    private var maxScale = 0f
    private var isMaxScaleResult = false
    private var maxScaleResult = 0f
    private var viewMinScale = 0f
    private var maxViewScale = 0f
    private var onDrawReady = false
    private var viewWidth = 0
    private var viewHeight = 0
    private var cachedWidth = 0
    private var cachedHeight = 0
    private var matchViewWidth = 0f
    private var matchViewHeight = 0f
    private var widthState = 0f
    private var heightState = 0f
    private val isZoomed get() = zoom != 1f
    private var mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private var mGestureDetector: GestureDetector = GestureDetector(context, GestureListener())

    init {
        mMatrix = Matrix()
        matrixState = Matrix()
        fMatrix = FloatArray(9)
        zoom = 1f
        touchScaleType = ScaleType.FIT_CENTER
        minScale = 1f
        maxScale = 3f
        viewMinScale = .75f * minScale
        maxViewScale = 1.25f * maxScale
        imageMatrix = mMatrix
        scaleType = ScaleType.MATRIX
        onDrawReady = false
        try {
            if (!isInEditMode) zoomToggle =
                context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.TouchImageView,
                    defStyle, 0).getBoolean(R.styleable.TouchImageView_zoom_enabled, true)
        } finally {
            context.theme.obtainStyledAttributes(attrs, R.styleable.TouchImageView, defStyle, 0).recycle()
        }
        flingGestureType(TypeGesture.NONE)
        super.setOnTouchListener(OnTouchViewListener())
    }

    override fun setOnTouchListener(onTouchListener: OnTouchListener) {
        userTouchListener = onTouchListener
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        setImageViewState()
        formatImage()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        setImageViewState()
        formatImage()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        setImageViewState()
        formatImage()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        setImageViewState()
        formatImage()
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
        setImageViewState()

        val width = totalViewWidth - paddingLeft - paddingRight
        val height = totalViewHeight - paddingTop - paddingBottom

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        formatImage()
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        mMatrix!!.getValues(fMatrix)
        val x = fMatrix!![Matrix.MTRANS_X]
        return if (imageWidth < viewWidth) false
        else if (x >= -1 && direction < 0) false
        else Math.abs(x) + viewWidth + 1 < imageWidth || direction <= 0
    }

    override fun canScrollVertically(direction: Int): Boolean {
        mMatrix!!.getValues(fMatrix)
        val y = fMatrix!![Matrix.MTRANS_Y]
        return if (imageHeight < viewHeight) {
            false
        } else if (y >= -1 && direction < 0) {
            false
        } else Math.abs(y) + viewHeight + 1 < imageHeight || direction <= 0
    }

    override fun getScaleType(): ScaleType {
        return touchScaleType!!
    }

    override fun onDraw(canvas: Canvas) {
        onDrawReady = true
        super.onDraw(canvas)
    }

    private fun setImageViewState() {
        if (mMatrix != null && viewHeight != 0 && viewWidth != 0) {
            mMatrix!!.getValues(fMatrix)
            matrixState!!.setValues(fMatrix)
            heightState = matchViewHeight
            widthState = matchViewWidth
            cachedHeight = viewHeight
            cachedWidth = viewWidth
        }
    }

    private fun zoomViewRatio(max: Float) {
        maxScaleResult = max
        maxScale = minScale * maxScaleResult
        maxViewScale = 1.25f * maxScale
        isMaxScaleResult = true
    }

    var minZoom: Float
        get() = minScale
        set(min) {
            userSpecifiedMinScale = min
            if (min == -1.0f) {
                if (touchScaleType == ScaleType.CENTER || touchScaleType == ScaleType.CENTER_CROP) {
                    val drawable = drawable
                    val drawableWidth = getDrawableWidth(drawable)
                    val drawableHeight = getDrawableHeight(drawable)
                    if (drawable != null && drawableWidth > 0 && drawableHeight > 0) {
                        val widthRatio = viewWidth.toFloat() / drawableWidth
                        val heightRatio = viewHeight.toFloat() / drawableHeight
                        minScale = if (touchScaleType == ScaleType.CENTER) {
                            Math.min(widthRatio, heightRatio)
                        } else {
                            Math.min(widthRatio, heightRatio) / Math.max(widthRatio, heightRatio)
                        }
                    }
                } else {
                    minScale = 1.0f
                }
            } else {
                minScale = userSpecifiedMinScale
            }
            if (isMaxScaleResult) {
                zoomViewRatio(maxScaleResult)
            }
            viewMinScale = .75f * minScale
        }

    fun resetZoom() {
        zoom = 1f
        formatImage()
    }

    fun setZoom(scale: Float, focusX: Float, focusY: Float, scaleType: ScaleType?) {
        if (userSpecifiedMinScale == -1.0f) {
            minZoom = -1.0f
            if (zoom < minScale) {
                zoom = minScale
            }
        }
        if (scaleType != touchScaleType) {
            setScaleType(scaleType!!)
        }
        resetZoom()
        scaleImage(scale.toDouble(), viewWidth / 2.toFloat(), viewHeight / 2.toFloat(), true)
        mMatrix!!.getValues(fMatrix)
        fMatrix!![Matrix.MTRANS_X] = -(focusX * imageWidth - viewWidth * 0.5f)
        fMatrix!![Matrix.MTRANS_Y] = -(focusY * imageHeight - viewHeight * 0.5f)
        mMatrix!!.setValues(fMatrix)
        setImageViewState()
        imageMatrix = mMatrix
    }

    fun setZoom(img: PdfImageView) {
        val center = img.scrollPosition
        setZoom(img.zoom, center.x, center.y, img.scaleType)
    }

    val scrollPosition: PointF
        get() {
            val drawable = drawable ?: return PointF(.5f, .5f)
            val drawableWidth = getDrawableWidth(drawable)
            val drawableHeight = getDrawableHeight(drawable)
            val point = imageViewCoordination(viewWidth / 2.toFloat(), viewHeight / 2.toFloat(), true)
            point.x /= drawableWidth.toFloat()
            point.y /= drawableHeight.toFloat()
            return point
        }

    private fun getDrawableWidth(drawable: Drawable?): Int {
        return if (screenView) {
            drawable!!.intrinsicHeight
        } else drawable!!.intrinsicWidth
    }

    private fun getDrawableHeight(drawable: Drawable?): Int {
        return if (screenView) {
            drawable!!.intrinsicWidth
        } else drawable!!.intrinsicHeight
    }

    private fun fixScaleTrans() {
        mMatrix!!.getValues(fMatrix)
        if (imageWidth < viewWidth) {
            var xOffset = (viewWidth - imageWidth) / 2
            if (screenView) {
                xOffset += imageWidth
            }
            fMatrix!![Matrix.MTRANS_X] = xOffset
        }
        if (imageHeight < viewHeight) {
            fMatrix!![Matrix.MTRANS_Y] = (viewHeight - imageHeight) / 2
        }
        mMatrix!!.setValues(fMatrix)
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else
            delta
    }

    private val imageWidth get() = matchViewWidth * zoom

    private val imageHeight get() = matchViewHeight * zoom

    private fun formatImage() {
        val drawable = drawable
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            return
        }
        if (mMatrix == null || matrixState == null) {
            return
        }
        if (userSpecifiedMinScale == -1.0f) {
            minZoom = -1.0f
            if (zoom < minScale) {
                zoom = minScale
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
        if (!isZoomed) {

            if (screenView) {
                mMatrix!!.setRotate(90f)
                mMatrix!!.postTranslate(drawableWidth.toFloat(), 0f)
                mMatrix!!.postScale(scaleX, scaleY)
            } else {
                mMatrix!!.setScale(scaleX, scaleY)
            }
            when (touchScaleType) {
                ScaleType.FIT_START -> mMatrix!!.postTranslate(0f, 0f)
                ScaleType.FIT_END -> mMatrix!!.postTranslate(redundantXSpace, redundantYSpace)
                else -> mMatrix!!.postTranslate(redundantXSpace / 2, redundantYSpace / 2)
            }
            zoom = 1f
        } else {
            if (widthState == 0f || heightState == 0f) {
                setImageViewState()
            }

            matrixState!!.getValues(fMatrix)

            fMatrix!![Matrix.MSCALE_X] = matchViewWidth / drawableWidth * zoom
            fMatrix!![Matrix.MSCALE_Y] = matchViewHeight / drawableHeight * zoom

            mMatrix!!.setValues(fMatrix)
        }
        imageMatrix = mMatrix
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

    private fun flingGestureType(typeGesture: TypeGesture) {
        this.typeGesture = typeGesture
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            flingGesture?.cancelFling()
            flingGesture = FlingGesture(velocityX.toInt(), velocityY.toInt())
                .also { compatPostOnAnimation(it) }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    interface OnTouchImageViewListener {
        fun onMove()
    }

    private inner class OnTouchViewListener : OnTouchListener {

        private val last = PointF()
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (drawable == null) {
                flingGestureType(TypeGesture.NONE)
                return false
            }
            if (zoomToggle) {
                mScaleDetector.onTouchEvent(event)
            }
            mGestureDetector.onTouchEvent(event)
            val curr = PointF(event.x, event.y)
            if (typeGesture == TypeGesture.NONE || typeGesture == TypeGesture.DRAG || typeGesture == TypeGesture.FLING) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        last.set(curr)
                        if (flingGesture != null) flingGesture!!.cancelFling()
                        flingGestureType(TypeGesture.DRAG)
                    }
                    MotionEvent.ACTION_MOVE -> if (typeGesture == TypeGesture.DRAG) {
                        val deltaX = curr.x - last.x
                        val deltaY = curr.y - last.y
                        val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), imageWidth)
                        val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), imageHeight)
                        mMatrix!!.postTranslate(fixTransX, fixTransY)
                        last[curr.x] = curr.y
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> flingGestureType(TypeGesture.NONE)
                }
            }
            imageMatrix = mMatrix

            if (userTouchListener != null) {
                userTouchListener!!.onTouch(v, event)
            }

            if (listenerView != null) {
                listenerView!!.onMove()
            }

            return true
        }
    }

    private fun scaleImage(deltaScale: Double, focusX: Float, focusY: Float, stretchImageToSuper: Boolean) {
        var deltaScaleLocal = deltaScale
        val lowerScale: Float
        val upperScale: Float
        if (stretchImageToSuper) {
            lowerScale = viewMinScale
            upperScale = maxViewScale
        } else {
            lowerScale = minScale
            upperScale = maxScale
        }
        val origScale = zoom
        zoom *= deltaScaleLocal.toFloat()
        if (zoom > upperScale) {
            zoom = upperScale
            deltaScaleLocal = upperScale / origScale.toDouble()
        } else if (zoom < lowerScale) {
            zoom = lowerScale
            deltaScaleLocal = lowerScale / origScale.toDouble()
        }
        mMatrix!!.postScale(deltaScaleLocal.toFloat(), deltaScaleLocal.toFloat(), focusX, focusY)
        fixScaleTrans()
    }

    protected fun imageViewCoordination(x: Float, y: Float, clipToBitmap: Boolean): PointF {
        mMatrix!!.getValues(fMatrix)
        val origW = drawable.intrinsicWidth.toFloat()
        val origH = drawable.intrinsicHeight.toFloat()
        val transX = fMatrix!![Matrix.MTRANS_X]
        val transY = fMatrix!![Matrix.MTRANS_Y]
        var finalX = (x - transX) * origW / imageWidth
        var finalY = (y - transY) * origH / imageHeight
        if (clipToBitmap) {
            finalX = Math.min(Math.max(finalX, 0f), origW)
            finalY = Math.min(Math.max(finalY, 0f), origH)
        }
        return PointF(finalX, finalY)
    }

    private fun compatPostOnAnimation(runnable: Runnable) {
        postOnAnimation(runnable)
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            flingGestureType(TypeGesture.ZOOM)
            return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleImage(detector.scaleFactor.toDouble(), detector.focusX, detector.focusY, true)
            if (listenerView != null) {
                listenerView!!.onMove()
            }
            return true
        }
    }

    inner class FlingGesture internal constructor(velocityX: Int, velocityY: Int) : Runnable {
        var scroller: ScrollState?
        var currX: Int
        var currY: Int
        val minX: Int
        val maxX: Int
        val minY: Int
        val maxY: Int

        fun cancelFling() {
            if (scroller != null) {
                flingGestureType(TypeGesture.NONE)
                scroller!!.forceFinished(true)
            }
        }

        override fun run() {
            if (listenerView != null) {
                listenerView!!.onMove()
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
                mMatrix!!.postTranslate(transX.toFloat(), transY.toFloat())
                imageMatrix = mMatrix
                compatPostOnAnimation(this)
            }
        }

        init {
            flingGestureType(TypeGesture.FLING)
            scroller = ScrollState(context)
            mMatrix!!.getValues(fMatrix)
            var startX = fMatrix!![Matrix.MTRANS_X].toInt()
            val startY = fMatrix!![Matrix.MTRANS_Y].toInt()
            if (screenView) {
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

    inner class ScrollState internal constructor(context: Context?) {

        val currX get() = overScroller.currX
        val currY get() = overScroller.currY
        val isFinished get() = overScroller.isFinished
        var overScroller: OverScroller = OverScroller(context)

        fun fling(startX: Int, startY: Int, velocityX: Int, velocityY: Int, minX: Int, maxX: Int, minY: Int, maxY: Int) {
            overScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        }

        fun forceFinished(finished: Boolean) {
            overScroller.forceFinished(finished)
        }

        fun computeScrollOffset(): Boolean {
            overScroller.computeScrollOffset()
            return overScroller.computeScrollOffset()
        }
    }

    enum class TypeGesture {
        NONE, DRAG, ZOOM, FLING
    }
}

