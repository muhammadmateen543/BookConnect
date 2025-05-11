package com.mateen.bookconnect

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class CustomAdapterForNotification(var context: Context, var mdata: MutableList<notifi>): BaseAdapter() {
    override fun getCount(): Int = mdata.size

    override fun getItem(position: Int): Any = mdata[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view=convertView?: LayoutInflater.from(context).inflate(R.layout.single_notification, parent, false)
        var notifitext: TextView=view.findViewById(R.id.notifitext)
        notifitext.text=mdata[position].text
        return view
    }
}