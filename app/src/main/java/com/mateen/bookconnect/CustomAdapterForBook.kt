package com.mateen.bookconnect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager

class CustomAdapterForBook(var context: Context, val fragmentManager: FragmentManager, var mdata: MutableList<Book>): BaseAdapter() {
    override fun getCount(): Int = mdata.size

    override fun getItem(position: Int): Any = mdata[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view=convertView?:LayoutInflater.from(context).inflate(R.layout.single_book, parent, false)
        var ivImage:ImageView=view.findViewById(R.id.image)
        var tvTitle:TextView=view.findViewById(R.id.title)
        var tvDescription:TextView=view.findViewById(R.id.description)
        var tvMode:TextView=view.findViewById(R.id.mode)
        var data = mdata[position]
        var images=data.images
        var title=data.name.toString()
        var description=data.description.toString()
        var dealmode = data.dealMode.toString()
        if(images[0].isEmpty())
        {
            ivImage.setImageResource(R.drawable.noimageicon)
        }
        else
        {
            ivImage.setImageBitmap(base64ToBitmap(images[0]))
        }
        tvTitle.text=title
        tvDescription.text=description
        tvMode.text=dealmode
        view.setOnClickListener {
            var bundle= Bundle()
            bundle.putString("bookId", data.bookId.toString())
            bundle.putString("title", title)
            bundle.putString("description", description)
            bundle.putString("author" , data.author.toString())
            bundle.putString("publisher", data.publisher.toString())
            bundle.putString("edition", data.edition.toString())
            bundle.putString("isbn", data.isbn.toString())
            bundle.putString("condition", data.condition.toString())
            bundle.putStringArrayList("images", images)
            var bookDetailFragment= BookDetailFragment()
            bookDetailFragment.arguments=bundle
            fragmentManager.beginTransaction().replace(R.id.buyFragment, bookDetailFragment)
                .addToBackStack(null).commit()
        }
        return view
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

}