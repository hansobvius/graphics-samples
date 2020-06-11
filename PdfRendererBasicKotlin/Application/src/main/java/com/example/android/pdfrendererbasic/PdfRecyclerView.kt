package com.example.android.pdfrendererbasic

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.content_adapter.view.*

class PdfRecyclerView(private val bitmapList: List<Bitmap>): RecyclerView.Adapter<PdfRecyclerView.PdfViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder =
        PdfViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.content_adapter, parent, false)
        )

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.makeToast(position)
        holder.pdfContent.setImageBitmap(bitmapList[position])
    }

    override fun getItemCount(): Int = bitmapList.count()

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