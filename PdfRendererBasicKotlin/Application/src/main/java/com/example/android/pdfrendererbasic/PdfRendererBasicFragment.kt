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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.android.pdfrendererbasic.databinding.PdfRendererBasicFragmentBinding
import java.io.File

class PdfRendererBasicFragment : Fragment(R.layout.pdf_renderer_basic_fragment) {

    lateinit var binding: PdfRendererBasicFragmentBinding
    private lateinit var pdfAdapter: PdfPagerAdapter
    private val viewModel: PdfRendererBasicViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = PdfRendererBasicFragmentBinding.inflate(inflater).also {
            it.lifecycleOwner = this@PdfRendererBasicFragment
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        this.apply {
            this.initPagerView()
        }
    }

//    private fun initListener(){
//        binding.previous.setOnClickListener { viewModel.showPrevious() }
//        binding.next.setOnClickListener { viewModel.showNext() }
//    }

//    private fun initObservers(){
//        viewModel.pageInfo.observe(viewLifecycleOwner, Observer { (index, count) ->
//            activity?.title = getString(R.string.app_name_with_index, index + 1, count)
//        })
//        viewModel.pageBitmap.observe(viewLifecycleOwner, Observer {
//            binding.image.setImageBitmap(it)
//        })
//        viewModel.previousEnabled.observe(viewLifecycleOwner, Observer {
//            binding.previous.isEnabled = it
//        })
//        viewModel.nextEnabled.observe(viewLifecycleOwner, Observer {
//            binding.next.isEnabled = it
//        })
//    }

    private fun initPagerView(){
        pdfAdapter = PdfPagerAdapter(getFile())
        binding.image.adapter = pdfAdapter
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


    companion object {
        const val FILENAME = "sample.pdf"
    }
}
