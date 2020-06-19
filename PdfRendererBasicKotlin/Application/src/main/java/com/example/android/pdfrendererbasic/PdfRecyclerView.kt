package com.example.android.pdfrendererbasic

import android.animation.Animator
import android.graphics.*
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.content_adapter.view.*

class PdfRecyclerView(private val bitmapList: List<Bitmap>): RecyclerView.Adapter<PdfRecyclerView.PdfViewHolder>() {

    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var scaleListener: ScaleGestureDetector.OnScaleGestureListener
    private var motionEvent: MotionEvent? = null
    private var currentAnimator: Animator? = null
    private var shortAnimationDuration: Int = 0
    val matrix = Matrix()
    private var scale = 1f

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder =
        PdfViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.content_adapter, parent, false)
        )

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.makeToast(position)

        gestureListener(holder.pdfContent)

        scaleDetector = ScaleGestureDetector(holder.itemView.context, scaleListener)

        holder.pdfContent.apply{
            this.setOnTouchListener { view, event ->
                scaleDetector!!.onTouchEvent(event)
                true
            }
            setImageBitmap(bitmapList[position])
        }
    }

    override fun getItemCount(): Int = bitmapList.count()

    private fun gestureListener(imageView: ImageView){
        this.scaleListener = object : ScaleGestureDetector.OnScaleGestureListener{
            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                Log.i("TEST", "detectorScale onScaleBegin")
                imageView.scaleX = 0f
                imageView.scaleY = 0f
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector?) {
                Log.i("TEST", "detectorScale onScaleEnd")
            }

            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                Log.i("TEST", "detectorScale onScale")
                scale *= detector!!.scaleFactor
                scale = Math.max(0.1f, Math.min(scale, 5.0f))
                imageView.scaleX = scale
                imageView.scaleY = scale
                return true
            }
        }
    }

    inner class PdfViewHolder(view: View): RecyclerView.ViewHolder(view){
        val pdfContent = view.pdf_content
        fun makeToast(position: Int){
            Toast.makeText(
                itemView.context,
                "${position + 1} de ${bitmapList.size}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}