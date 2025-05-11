package com.mateen.bookconnect

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler

public class ShowMessage(var context: Context) {
    public fun showMessage(message: String)
    {
        var alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle("Message")
        alertDialog.setMessage(message)
        var dialog = alertDialog.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#f73b3b")))
        dialog.show()
        Handler().postDelayed({
            dialog.dismiss()
        }, 1500)
    }
}