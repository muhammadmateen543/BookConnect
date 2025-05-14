package com.mateen.bookconnect

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CustomerAdapterForOffer(var context: Context, val fragmentManager: FragmentManager, var mdata: MutableList<offer>): BaseAdapter() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore

    override fun getCount(): Int = mdata.size

    override fun getItem(position: Int): Any = mdata[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var itemView = convertView
        if (itemView == null) {
            itemView = LayoutInflater.from(context).inflate(R.layout.single_offer, parent, false)
        }
        auth= FirebaseAuth.getInstance()
        authdb= FirebaseFirestore.getInstance()
        val offer = mdata[position]
        val tvTitle = itemView!!.findViewById<TextView>(R.id.tvTitle)
        val tvDealMode = itemView.findViewById<TextView>(R.id.tvDealMode)
        val tvSellPrice = itemView.findViewById<TextView>(R.id.tvSellPrice)
        val tvExchangeBook = itemView.findViewById<TextView>(R.id.tvExchangeBook)
        val cancelbtn = itemView.findViewById<RelativeLayout>(R.id.cancelbtn)

        tvTitle.text = "Title: ${offer.title}"
        tvDealMode.text = offer.dealMode

        if (offer.sellPrice != null && offer.sellPrice.isNotEmpty()) {
            tvSellPrice.text = "Bid Amount: ${offer.sellPrice}"
            tvSellPrice.visibility = View.VISIBLE
        } else {
            tvSellPrice.visibility = View.GONE
        }

        if (offer.exchangeBook != null && offer.exchangeBook.isNotEmpty()) {
            tvExchangeBook.text = "Bid Book: ${offer.exchangeBook}"
            tvExchangeBook.visibility = View.VISIBLE
        } else {
            tvExchangeBook.visibility = View.GONE
        }

        cancelbtn.setOnClickListener {
            cancelbtn.isEnabled = false
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Are you sure you want to remove this bid?")
            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)
            builder.setPositiveButton("Confirm") { _, _ ->
                authdb.collection("users")
                    .document(offer.ownerId)
                    .collection("My Books")
                    .document(offer.bookId)
                    .get()
                    .addOnSuccessListener { result ->
                        val bidsList = (result.get("Bids") as? List<Map<String, Any>>)?.toMutableList()
                        if (bidsList != null) {
                            val iterator = bidsList.iterator()
                            while (iterator.hasNext()) {
                                val bid = iterator.next()
                                if (bid["bidderId"].toString() == auth.currentUser?.uid) {
                                    iterator.remove()
                                }
                            }
                            authdb.collection("users")
                                .document(offer.ownerId)
                                .collection("My Books")
                                .document(offer.bookId)
                                .update("Bids", bidsList)
                                .addOnSuccessListener {
                                    mdata.removeAt(position)
                                    notifyDataSetChanged()
                                }
                        }
                    }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
            cancelbtn.isEnabled = true
        }

        return itemView
    }

}