package com.example.android.pdfrendererbasic.presentation.home.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import com.example.android.pdfrendererbasic.R
import com.example.android.pdfrendererbasic.presentation.home.fragment.PdfRendererFragment

class MainActivity : AppCompatActivity(R.layout.main_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, PdfRendererFragment())
            }
        }
    }

}
