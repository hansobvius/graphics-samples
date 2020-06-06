package com.example.android.pdfrendererbasic

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.viewpager.widget.PagerAdapter
import com.example.android.pdfrendererbasic.databinding.ContentAdapterBinding
import java.io.File

class PdfPagerAdapter(val file: File): PagerAdapter() {

    lateinit var binding: ContentAdapterBinding

    override fun instantiateItem(container: ViewGroup, position: Int): ViewDataBinding {
//        binding = LayoutInflater.from(container.context).inflate(R.layout.content_adapter, container) as ContentAdapterBinding
        val inflater = LayoutInflater.from(container.context)
        binding = ContentAdapterBinding.inflate(inflater)
        binding.pdfContent.setImageBitmap(PdfUtil.renderPdfFile(file, position))
        return binding
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun getCount(): Int = PdfUtil.pageCounter(file)

}