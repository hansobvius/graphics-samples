package com.example.android.pdfrendererbasic

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class PdfAdapter(private val bitmapList: List<Bitmap>): RecyclerView.Adapter<PdfAdapter.PdfViewHolder>() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder =
        PdfViewHolder(PdfImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
            setOnTouchListener { view, event ->
                var result = true
                if (event.pointerCount >= 2 || view.canScrollHorizontally(1) && canScrollHorizontally(-1)) {
                    result = when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            parent.requestDisallowInterceptTouchEvent(true)
                            false
                        }
                        MotionEvent.ACTION_UP -> {
                            parent.requestDisallowInterceptTouchEvent(false)
                            true
                        }
                        else -> true
                    }
                }
                result
            }
        })

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.makeToast(position)
        holder.pdfContent.setImageBitmap(bitmapList[position])
    }

    override fun getItemCount(): Int = bitmapList.count()

    inner class PdfViewHolder(view: PdfImageView): RecyclerView.ViewHolder(view){
        val pdfContent = view
        fun makeToast(position: Int){
            Toast.makeText(
                itemView.context,
                "${getPagePosition(position)} de ${bitmapList.size}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getPagePosition(pos: Int) =
        if(pos == 0) (pos + 1)
        else pos
}