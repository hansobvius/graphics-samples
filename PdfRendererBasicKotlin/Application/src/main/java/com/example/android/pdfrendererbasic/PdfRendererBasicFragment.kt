/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.pdfrendererbasic

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.android.pdfrendererbasic.databinding.PdfRendererBasicFragmentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PdfRendererBasicFragment : Fragment() {

    lateinit var binding: PdfRendererBasicFragmentBinding
    private lateinit var pdfRecyclerView: PdfRecyclerView
    private lateinit var pdfRenderer: PdfRenderer
    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var file: File
    private val useInstantExecutor = true
    private val job = Job()
    private val executor = if (useInstantExecutor) {
        Executor { it.run() }
    } else {
        Executors.newSingleThreadExecutor()
    }
    private val scope = CoroutineScope(executor.asCoroutineDispatcher() + job)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = PdfRendererBasicFragmentBinding.inflate(inflater).also {
            it.lifecycleOwner = this@PdfRendererBasicFragment
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        this.apply {
            this.initPagerView()
        }
    }

    private fun initPagerView(){
        renderContent()
    }

    private fun getFile(): File{
        val file = File(requireActivity().application.cacheDir, FILENAME)
        if (!file.exists()) {
            requireActivity().application.assets.open(PdfRendererBasicViewModel.FILENAME).use { asset ->
                file.writeBytes(asset.readBytes())
            }
        }
        return file
    }

    private fun loadPdf(){
        file = getFile()
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(parcelFileDescriptor)
    }

    private fun renderContent(){
        loadPdf().also {
            pdfRecyclerView = PdfRecyclerView(renderPdf(pdfRenderer.pageCount))
            binding.viewPager.apply{
                adapter = pdfRecyclerView
            }
        }
    }

    private fun renderPdf(count: Int): List<Bitmap> {
        val list = ArrayList<Bitmap>()
        for (i in 0 until count) list.add(renderPage(pdfRenderer.openPage(i)))
        return list
    }

    private fun renderPage(page: PdfRenderer.Page): Bitmap {
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    companion object {
        const val FILENAME = "sample.pdf"
    }
}
