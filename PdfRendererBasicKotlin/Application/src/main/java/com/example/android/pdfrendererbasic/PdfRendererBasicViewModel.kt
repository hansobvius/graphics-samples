/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PdfRendererBasicViewModel constructor(application: Application) : AndroidViewModel(application) {

    private val job = Job()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var cleared = false

    init {
        scope.launch {
            openPdfRenderer()
            if (cleared) {
                closePdfRenderer()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.launch {
            closePdfRenderer()
            cleared = true
            job.cancel()
        }
    }

    private fun openPdfRenderer() {
        val application = getApplication<Application>()
        val file = File(application.cacheDir, FILENAME)
        if (!file.exists()) {
            application.assets.open(FILENAME).use { asset ->
                file.writeBytes(asset.readBytes())
            }
        }
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).also {
            pdfRenderer = PdfRenderer(it)
        }
    }

    private fun closePdfRenderer() {
        currentPage?.close()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }

    companion object {
        const val FILENAME = "sample.pdf"
    }
}
