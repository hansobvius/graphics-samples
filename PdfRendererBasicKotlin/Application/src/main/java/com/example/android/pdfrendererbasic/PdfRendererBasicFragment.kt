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

import android.animation.ObjectAnimator
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.android.pdfrendererbasic.MainActivity.Companion.FRAGMENT_INFO
import com.example.android.pdfrendererbasic.databinding.PdfRendererBasicFragmentBinding
import kotlinx.android.synthetic.main.pdf_renderer_basic_fragment.view.*
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
    private var isExpandable = true
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
        this.buildToolbar()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        this.apply {
            this.initPagerView()
        }
        this.initCLickListener()
        this.toolbarAnimation()
    }

    private fun buildToolbar(){
        binding.mainToolbar.apply{
            this.inflateMenu(R.menu.main)
            setBackgroundColor(
                this@PdfRendererBasicFragment
                    .requireActivity()
                    .resources
                    .getColor(R.color.toolbar_color)
            )
            title = "Pdf Renderer"
            setTitleTextColor(
                this@PdfRendererBasicFragment
                    .requireActivity()
                    .resources
                    .getColor(R.color.toolbar_text_color)
            )
            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.action_info -> {
                        AlertDialog.Builder(requireContext())
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun initCLickListener(){
        binding.root.setOnClickListener {
            Log.i("TEST", "listener clicked")
            isExpandable = !isExpandable
        }
    }

    private fun toolbarAnimation(){
        val toolbarAnimation = getObjectAnimator()
        toolbarAnimation.apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 1000
            expandLayout()
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

    /**
     * EXPAND TOOLBAR
     */
    private fun getObjectAnimator(): ObjectAnimator =
        if(isExpandable){
            ObjectAnimator.ofFloat(
                binding.root.main_toolbar,
                View.TRANSLATION_Y,
                0f,
                - binding.root.height.toFloat()
            )
        }else{
            ObjectAnimator.ofFloat(
                binding.root.main_toolbar,
                View.TRANSLATION_Y,
                - binding.root.height.toFloat(),
                0f
            )
        }

    private fun ObjectAnimator.expandLayout(){
        start()
    }

    companion object {
        const val FILENAME = "sample.pdf"
    }
}
