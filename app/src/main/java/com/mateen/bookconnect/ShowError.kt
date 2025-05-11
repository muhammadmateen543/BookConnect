package com.mateen.bookconnect

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler

public class ShowError(var context: Context) {
    public fun showError(message: String)
    {
        var alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("ERROR")
        alertDialog.setMessage(message)
        var dialog = alertDialog.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#f73b3b")))
        dialog.show()
        Handler().postDelayed({
            dialog.dismiss()
        }, 1500)
    }
}