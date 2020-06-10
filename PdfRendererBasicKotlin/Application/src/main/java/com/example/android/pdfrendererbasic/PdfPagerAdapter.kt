package com.example.android.pdfrendererbasic

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.viewpager.widget.PagerAdapter
import java.io.File

class PdfPagerAdapter(val bitmap: List<Bitmap>): PagerAdapter() {

    override fun instantiateItem(container: ViewGroup, position: Int): View {
        val inflater = LayoutInflater.from(container.context)
        val view: View = inflater.inflate(R.layout.content_adapter, container, false)
        val image = view.findViewById<ImageView>(R.id.pdf_content)
        image.setImageBitmap(bitmap[position])
        container.addView(view)
        return view
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun getCount(): Int = bitmap.size

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}