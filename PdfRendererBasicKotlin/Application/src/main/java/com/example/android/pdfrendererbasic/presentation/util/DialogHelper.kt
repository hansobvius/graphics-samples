package com.example.android.pdfrendererbasic.presentation.util

import android.app.AlertDialog
import android.content.Context
import com.example.android.pdfrendererbasic.R

object DialogHelper {

    fun showAlert(context: Context){
        val dialog = AlertDialog.Builder(context).apply {
            setTitle(context.resources.getString(R.string.pdf_alert_msg))
            setPositiveButton(context.resources.getString(R.string.pdf_alert_button)) { _, _ ->
                this.setOnDismissListener {
                    it.dismiss()
                }
            }
        }
        dialog.show()
    }
}