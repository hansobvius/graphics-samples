package com.example.android.pdfrendererbasic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.android.pdfrendererbasic.databinding.PdfRendererBasicFragmentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PdfRendererBasicFragment : Fragment() {

    private lateinit var binding: PdfRendererBasicFragmentBinding
    private lateinit var pdfAdapter: PdfAdapter
    private lateinit var pdfRenderer: PdfRenderer
    private lateinit var parcelFileDescriptor: ParcelFileDescriptor
    private lateinit var file: File

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
            pdfAdapter = PdfAdapter(renderPdf(pdfRenderer.pageCount))
            binding.viewPager.apply{
                adapter = pdfAdapter
            }
        }
    }

    private fun renderPdf(count: Int): List<Bitmap> {
        val list = ArrayList<Bitmap>()
        for (i in 0 until count) list.add(renderPage(pdfRenderer.openPage(i)))
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

    companion object {
        const val FILENAME = "sample.pdf"
    }
}
