package com.example.android.pdfrendererbasic

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.example.android.pdfrendererbasic.databinding.ContentAdapterBinding

class PdfPagerAdapter: PagerAdapter() {

    lateinit var binding: ContentAdapterBinding
    lateinit var page: MutableList<Bitmap>

    fun initializeData(list: List<Bitmap>){
        page.addAll(list)
        this.notifyDataSetChanged()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        binding = LayoutInflater.from(container.context).inflate(R.layout.content_adapter, container) as ContentAdapterBinding
        binding.pdfContent.setImageBitmap(page.get(position))
        return binding.root
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun getCount(): Int = page.count() ?: 0

}