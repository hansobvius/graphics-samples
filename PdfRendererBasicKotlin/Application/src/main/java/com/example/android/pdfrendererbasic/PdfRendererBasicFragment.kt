package com.example.android.pdfrendererbasic

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.android.pdfrendererbasic.databinding.PdfRendererBasicFragmentBinding
import java.io.File

class PdfRendererBasicFragment : Fragment() {

    private lateinit var binding: PdfRendererBasicFragmentBinding
    private lateinit var viewModel: PdfViewModel
    private lateinit var pdfAdapter: PdfAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = ViewModelProvider(requireActivity()).get(PdfViewModel::class.java).also {
            it.loadPdf(getFile())
        }
        binding = PdfRendererBasicFragmentBinding.inflate(inflater).also {
            it.lifecycleOwner = this@PdfRendererBasicFragment
        }
        this.buildToolbar()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        this@PdfRendererBasicFragment.apply {
            this.renderContent()
        }
    }

    private fun buildToolbar(){
        binding.mainToolbar.apply{
            this.inflateMenu(R.menu.main)
            setBackgroundColor(
                this@PdfRendererBasicFragment
                    .requireActivity()
                    .resources
                    .getColor(R.color.toolbar_color, null)
            )
            title = "Pdf Renderer"
            setTitleTextColor(
                this@PdfRendererBasicFragment
                    .requireActivity()
                    .resources
                    .getColor(R.color.toolbar_text_color, null)
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

    private fun getFile(): File{
        val file = File(requireActivity().application.cacheDir, FILENAME)
        if (!file.exists()) {
            requireActivity().application.assets.open(FILENAME).use { asset ->
                file.writeBytes(asset.readBytes())
            }
        }
        return file
    }

    private fun renderContent(){
        viewModel.bitmapList.observe(this, Observer {
            it?.let{
                pdfAdapter = PdfAdapter(it)
                binding.viewPager.adapter = pdfAdapter
            }
        })
    }

    companion object {
        const val FILENAME = "sample.pdf"
    }
}
