package com.example.android.pdfrendererbasic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

class PdfRendererBasicViewModel : ViewModel() {

    private lateinit var parcelFileDescriptor: ParcelFileDescriptor

    private val _bitmapList = MutableLiveData<List<Bitmap>>()
    val bitmapList: LiveData<List<Bitmap>> get() = _bitmapList

    fun loadPdf(file: File){
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch{
            val parcel = async(start = CoroutineStart.LAZY){ getParcelFile(file) }
            parcel.start()
            parcelFileDescriptor = parcel.await()
            withContext(Dispatchers.Main){ setParcelFileToRender(parcelFileDescriptor) }
        }
    }

    private fun getParcelFile(file: File): ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

    private fun setParcelFileToRender(fileDescriptor: ParcelFileDescriptor){
        try{
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val listOfBitmap = renderPdf(pdfRenderer)
            _bitmapList.value = listOfBitmap
        }catch(e: IOException){
            e.stackTrace
        }
    }

    private fun renderPdf(pdfRenderer: PdfRenderer): List<Bitmap> {
        val list = ArrayList<Bitmap>()
        for (i in 0 until pdfRenderer.pageCount) list.add(renderPage(pdfRenderer.openPage(i)))
        return list
    }

    private fun renderPage(page: PdfRenderer.Page): Bitmap {
        var bitmap: Bitmap? = null
        try{
            bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        }catch(e: OutOfMemoryError){
            e.stackTrace
        }finally {
            val canvas = Canvas(bitmap!!)
            canvas.apply{
                this.drawColor(Color.WHITE)
                this.drawBitmap(bitmap, 0F, 0F, null)
            }
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return bitmap
        }
    }
}
