package com.example.android.pdfrendererbasic

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import androidx.databinding.ViewDataBinding
import java.io.File

object PdfUtil {

    fun renderPdfFile(file: File, position: Int): Bitmap?{
        var page: PdfRenderer.Page? = null
        var bitmap: Bitmap? = null
        var renderer: PdfRenderer? = null
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).also {
            renderer = PdfRenderer(it)
        }
        val pdfRenderer: PdfRenderer? = returnPdfRender(file)
        pdfRenderer?.let{
            page = pdfRenderer.openPage(position)
            bitmap = Bitmap.createBitmap(page!!.width, page!!.height, Bitmap.Config.ARGB_8888)
            page!!.render(bitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
        return bitmap
    }

    fun pageCounter(file: File) = returnPdfRender(file).pageCount

    private fun returnPdfRender(file: File) = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
}