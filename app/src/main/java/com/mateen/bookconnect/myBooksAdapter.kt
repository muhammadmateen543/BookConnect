package com.mateen.bookconnect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class myBooksAdapter(
    var context: Context,
    var myBooks: MutableList<Book>
) : RecyclerView.Adapter<myBooksAdapter.myBooksViewHolder>() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore
    var selectedIndex=-1;
    inner class myBooksViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iv_myBook_picture: ImageView = view.findViewById(R.id.iv_myBook_picture)
        val sp_myBooks: Spinner = view.findViewById(R.id.sp_myBook)
        val name_myBooks: TextView = view.findViewById(R.id.tv_myBook_name)
        val mode_myBooks: TextView = view.findViewById(R.id.tv_myBook_mode)
        val btnshowbids_myBooks: RelativeLayout =view.findViewById(R.id.btnshowbids)
        val description_myBooks: TextView = view.findViewById(R.id.description_myBooks)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myBooksViewHolder {
        auth= FirebaseAuth.getInstance()
        authdb= FirebaseFirestore.getInstance()
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_my_book, parent, false)
        return myBooksViewHolder(view)
    }

    override fun onBindViewHolder(holder: myBooksViewHolder, position: Int) {
        holder.sp_myBooks.setSelection(0, false)
        holder.name_myBooks.text = myBooks[position].name
        holder.mode_myBooks.text = myBooks[position].dealMode
        holder.description_myBooks.text = myBooks[position].description
        holder.iv_myBook_picture.setImageBitmap(base64ToBitmap(myBooks[position].images.first()))
        val options = ArrayList<String>()
        options.add("None")
        options.add("Remove Post")
        options.add("Edit Book Name")
        options.add("Edit Book Description")
        options.add("Change Book Condition")
        if (myBooks[position].publisher != null) {
            options.add("Edit Book Publisher")
        } else {
            options.add("Add Book Publisher")
        }
        options.add("Edit Book Author")
        if (myBooks[position].edition != null) {
            options.add("Edit Book Edition")
        } else {
            options.add("Add Book Edition")
        }
        if (myBooks[position].isbn != null) {
            options.add("Edit Book ISBN")
        } else {
            options.add("Add Book ISBN")
        }
        options.add("Edit Book Mode")
        if (myBooks[position].dealMode == "Sell" || myBooks[position].dealMode == "Both") {
            options.add("Edit Book Price")
        }
        if (myBooks[position].dealMode == "Exchange" || myBooks[position].dealMode == "Both") {
            options.add("Edit Exchange Book")
        }
        val arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, options)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        holder.sp_myBooks.adapter = arrayAdapter

        holder.sp_myBooks.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                pos: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(pos).toString().trim()
                performOperation(selectedItem, position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.btnshowbids_myBooks.setOnClickListener {
            seeOrApplyActionOnBids(position)
        }

    }

    override fun getItemCount() = myBooks.size

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun seeOrApplyActionOnBids(position: Int) {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialogbidlist, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.bidContainer)
        var alertDialogBuilder = android.app.AlertDialog.Builder(context)

        alertDialogBuilder.setTitle("Bids")
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setPositiveButton("Close", null)
        var alertDialog = alertDialogBuilder.create()

        val bidslist = mutableListOf<bidclass>()

        authdb.collection("users")
            .document(auth.currentUser?.uid.toString())
            .collection("My Books")
            .document(myBooks[position].bookId)
            .get()
            .addOnSuccessListener { result ->
                val bids = result.get("Bids") as? List<Map<String, Any>>
                if (bids != null) {
                    for (bid in bids) {
                        val bidderId = bid["bidderId"].toString()
                        val bidAmount = bid["bidAmount"]?.toString()
                        val bidBook = bid["bidBook"]?.toString()

                        authdb.collection("users").document(bidderId).get()
                            .addOnSuccessListener { data ->
                                val bidItem = bidclass(
                                    bidderId = bidderId,
                                    Title = result.getString("Title").orEmpty(),
                                    bidderName = data["Full Name"].toString(),
                                    bidAmount = bidAmount,
                                    bidBook = bidBook
                                )

                                bidslist.add(bidItem)

                                val itemView = inflater.inflate(R.layout.itembidlist, null)

                                val tvName = itemView.findViewById<TextView>(R.id.tvName)
                                val tvBidPrice =
                                    itemView.findViewById<TextView>(R.id.tvbidAmount)
                                val tvBidBook = itemView.findViewById<TextView>(R.id.tvbidBook)
                                val btnAccept = itemView.findViewById<Button>(R.id.btnAccept)
                                val btnReject = itemView.findViewById<Button>(R.id.btnReject)

                                tvName.text = "Name: ${bidItem.bidderName}"

                                if (bidItem.bidAmount.isNullOrEmpty()) {
                                    tvBidPrice.visibility = View.GONE
                                } else {
                                    tvBidPrice.text = "Offered Amount: ${bidItem.bidAmount}"
                                    tvBidPrice.visibility = View.VISIBLE
                                }

                                if (bidItem.bidBook.isNullOrEmpty()) {
                                    tvBidBook.visibility = View.GONE
                                } else {
                                    tvBidBook.text = "Offered Book: ${bidItem.bidBook}"
                                    tvBidBook.visibility = View.VISIBLE
                                }

                                btnAccept.setOnClickListener {
                                    val notificationDocRef = authdb.collection("users")
                                        .document(bidItem.bidderId)
                                        .collection("Notification")
                                        .document("notificationsList")
                                    notificationDocRef.get().addOnSuccessListener { documentSnapshot ->

                                        var notificationMsg = ""
                                        if(!bidItem.bidAmount.isNullOrEmpty() && !bidItem.bidBook.isNullOrEmpty())
                                        {
                                            notificationMsg = "Your offer of (Amount: ${bidItem.bidAmount} and Book: ${bidItem.bidBook}) for Book ${bidItem.Title} is Accepted."
                                        }
                                        else if(!bidItem.bidAmount.isNullOrEmpty())
                                        {
                                            notificationMsg = "Your offer of (PKR ${bidItem.bidAmount}) for Book ${bidItem.Title} is Accepted."
                                        }
                                        else if(!bidItem.bidBook.isNullOrEmpty())
                                        {
                                            notificationMsg = "Your offer of (PKR ${bidItem.bidBook}) for Book ${bidItem.Title} is Accepted."
                                        }
                                        if (documentSnapshot.exists()) {
                                            notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                        } else {
                                            val newNotification = hashMapOf(
                                                "notifications" to listOf(notificationMsg)
                                            )
                                            notificationDocRef.set(newNotification)
                                        }
                                        //
                                        authdb.collection("users")
                                            .document(auth.currentUser?.uid.toString())
                                            .collection("My Books")
                                            .document(myBooks[position].bookId)
                                            .get()
                                            .addOnSuccessListener { documentSnapshot ->
                                                val bids = documentSnapshot.get("Bids") as? List<Map<String, Any>>
                                                if (bids != null) {
                                                    for (bid in bids) {
                                                        val bidderId = bid["bidderId"]?.toString()
                                                        if (bidderId != null && bidderId!=bidItem.bidderId) {
                                                            val notificationMsg = "Your bid for Book ${documentSnapshot.get("Title").toString()} was removed because the book was sold."
                                                            val notificationDocRef = authdb.collection("users")
                                                                .document(bidderId)
                                                                .collection("Notification")
                                                                .document("notificationsList")

                                                            notificationDocRef.get().addOnSuccessListener { notifSnapshot ->
                                                                if (notifSnapshot.exists()) {
                                                                    notificationDocRef.update(
                                                                        "notifications",
                                                                        FieldValue.arrayUnion(notificationMsg)
                                                                    )
                                                                } else {
                                                                    val newNotification = hashMapOf(
                                                                        "notifications" to listOf(notificationMsg)
                                                                    )
                                                                    notificationDocRef.set(newNotification)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        //
                                    }
                                    val notificationDocRef1 = authdb.collection("users")
                                        .document(auth.currentUser?.uid.toString())
                                        .collection("Notification")
                                        .document("notificationsList")
                                    notificationDocRef1.get().addOnSuccessListener { documentSnapshot ->
                                        var notificationMsg = ""
                                        if(!bidItem.bidAmount.isNullOrEmpty() && !bidItem.bidBook.isNullOrEmpty())
                                        {
                                            notificationMsg = "Accepted offer of ${bidItem.bidderName} by (Amount: ${bidItem.bidAmount} and Book: ${bidItem.bidBook}) for Book ${bidItem.Title}."
                                        }
                                        else if(!bidItem.bidAmount.isNullOrEmpty())
                                        {
                                            notificationMsg = "Accepted offer of ${bidItem.bidderName} by (PKR ${bidItem.bidAmount}) for Book ${bidItem.Title}."
                                        }
                                        else if(!bidItem.bidBook.isNullOrEmpty())
                                        {
                                            notificationMsg = "Accepted offer of ${bidItem.bidderName} by (${bidItem.bidBook}) for Book ${bidItem.Title}."
                                        }
                                        if (documentSnapshot.exists()) {
                                            notificationDocRef1.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                        } else {
                                            val newNotification = hashMapOf(
                                                "notifications" to listOf(notificationMsg)
                                            )
                                            notificationDocRef1.set(newNotification)
                                        }
                                    }
                                    authdb.collection("TradeBooks")
                                        .document(myBooks[position].bookId).delete().addOnSuccessListener {
                                            authdb.collection("users")
                                                .document(auth.currentUser?.uid.toString())
                                                .collection("My Books")
                                                .document(myBooks[position].bookId).delete().addOnSuccessListener {
                                                    myBooks.removeAt(position)
                                                    notifyItemRemoved(position)
                                                    notifyItemRangeChanged(position, myBooks.size)
                                                }
                                        }
                                    alertDialog.dismiss()
                                }

                                btnReject.setOnClickListener {
                                    val bidMap = mutableMapOf<String, Any>(
                                        "bidderId" to bidItem.bidderId
                                    )
                                    if (!bidItem.bidAmount.isNullOrEmpty()) {
                                        bidMap["bidAmount"] = bidItem.bidAmount!!
                                    }
                                    if (!bidItem.bidBook.isNullOrEmpty()) {
                                        bidMap["bidBook"] = bidItem.bidBook!!
                                    }
                                    authdb.collection("users")
                                        .document(auth.currentUser?.uid.toString())
                                        .collection("My Books")
                                        .document(myBooks[position].bookId)
                                        .update("Bids", FieldValue.arrayRemove(bidMap))
                                        .addOnSuccessListener {
                                            val notificationDocRef = authdb.collection("users")
                                                .document(bidItem.bidderId)
                                                .collection("Notification")
                                                .document("notificationsList")
                                            notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                                var notificationMsg = ""
                                                if(!bidItem.bidAmount.isNullOrEmpty() && !bidItem.bidBook.isNullOrEmpty())
                                                {
                                                    notificationMsg = "Your offer of (Amount: ${bidItem.bidAmount} and Book: ${bidItem.bidBook}) for Book ${bidItem.Title} is Rejected."
                                                }
                                                else if(!bidItem.bidAmount.isNullOrEmpty())
                                                {
                                                    notificationMsg = "Your offer of (PKR ${bidItem.bidAmount}) for Book ${bidItem.Title} is Rejected."
                                                }
                                                else if(!bidItem.bidBook.isNullOrEmpty())
                                                {
                                                    notificationMsg = "Your offer of (PKR ${bidItem.bidBook}) for Book ${bidItem.Title} is Rejected."
                                                }
                                                if (documentSnapshot.exists()) {
                                                    notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                                } else {
                                                    val newNotification = hashMapOf(
                                                        "notifications" to listOf(notificationMsg)
                                                    )
                                                    notificationDocRef.set(newNotification)
                                                }
                                            }
                                            val notificationDocRef1 = authdb.collection("users")
                                                .document(auth.currentUser?.uid.toString())
                                                .collection("Notification")
                                                .document("notificationsList")
                                            notificationDocRef1.get().addOnSuccessListener { documentSnapshot ->
                                                var notificationMsg = ""
                                                if(!bidItem.bidAmount.isNullOrEmpty() && !bidItem.bidBook.isNullOrEmpty())
                                                {
                                                    notificationMsg = "Rejected offer of ${bidItem.bidderName} by (Amount: ${bidItem.bidAmount} and Book: ${bidItem.bidBook}) for Book ${bidItem.Title}."
                                                }
                                                else if(!bidItem.bidAmount.isNullOrEmpty())
                                                {
                                                    notificationMsg = "Rejected offer of ${bidItem.bidderName} by (PKR ${bidItem.bidAmount}) for Book ${bidItem.Title}."
                                                }
                                                else if(!bidItem.bidBook.isNullOrEmpty())
                                                {
                                                    notificationMsg = "Rejected offer of ${bidItem.bidderName} by (${bidItem.bidBook}) for Book ${bidItem.Title}."
                                                }
                                                if (documentSnapshot.exists()) {
                                                    notificationDocRef1.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                                } else {
                                                    val newNotification = hashMapOf(
                                                        "notifications" to listOf(notificationMsg)
                                                    )
                                                    notificationDocRef1.set(newNotification)
                                                }
                                            }
                                            container.removeView(itemView)
                                        }
                                }
                                container.addView(itemView)
                            }
                    }
                }



                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#b7d9f7")))
                alertDialog.show()
            }
    }

    fun performOperation(selectedItem: String, position: Int) {
        when (selectedItem) {
            "Remove Post" -> {
                authdb.collection("TradeBooks")
                    .document(myBooks[position].bookId)
                    .delete()
                    .addOnSuccessListener {
                        //
                        authdb.collection("users")
                            .document(auth.currentUser?.uid.toString())
                            .collection("My Books")
                            .document(myBooks[position].bookId)
                            .get()
                            .addOnSuccessListener { documentSnapshot ->
                                val bids = documentSnapshot.get("Bids") as? List<Map<String, Any>>
                                if (bids != null) {
                                    for (bid in bids) {
                                        val bidderId = bid["bidderId"]?.toString()
                                        if (bidderId != null) {
                                            val notificationMsg = "Your bid for Book ${documentSnapshot.get("Title").toString()} was removed because the book was deleted."
                                            val notificationDocRef = authdb.collection("users")
                                                .document(bidderId)
                                                .collection("Notification")
                                                .document("notificationsList")

                                            notificationDocRef.get().addOnSuccessListener { notifSnapshot ->
                                                if (notifSnapshot.exists()) {
                                                    notificationDocRef.update(
                                                        "notifications",
                                                        FieldValue.arrayUnion(notificationMsg)
                                                    )
                                                } else {
                                                    val newNotification = hashMapOf(
                                                        "notifications" to listOf(notificationMsg)
                                                    )
                                                    notificationDocRef.set(newNotification)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        //
                        authdb.collection("users").document(auth.currentUser?.uid.toString())
                            .collection("My Books")
                            .document(myBooks[position].bookId)
                            .delete()
                            .addOnSuccessListener {
                                myBooks.removeAt(position)
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, myBooks.size)
                                val notificationDocRef = authdb.collection("users")
                                    .document(auth.currentUser?.uid.toString())
                                    .collection("Notification")
                                    .document("notificationsList")
                                notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                    removebidnotification(position)
                                    var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                    if (documentSnapshot.exists()) {
                                        notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                    } else {
                                        val newNotification = hashMapOf(
                                            "notifications" to listOf(notificationMsg)
                                        )
                                        notificationDocRef.set(newNotification)
                                    }
                                }

                            }
                    }
            }

            "Edit Book Name" -> {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Enter New Name For ${myBooks[position].name}")
                val input = EditText(context)
                input.setText(myBooks[position].name.toString())
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)
                builder.setPositiveButton("Rename") { _, _ ->
                    val bookName = input.text.toString()
                    if (bookName.isNotEmpty()) {
                        notifyItemChanged(position)
                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                        x.get().addOnSuccessListener {
                                result->
                            x.update("Title", bookName)
                            var userBookRef = authdb.collection("users")
                                .document(auth.currentUser?.uid.toString())
                                .collection("My Books")
                                .document(myBooks[position].bookId)

                            userBookRef.update("Title", bookName).addOnSuccessListener {
                                notifyItemRangeChanged(position, myBooks.size)
                                userBookRef.update("Bids", FieldValue.delete())
                                myBooks[position].name = bookName
                                val notificationDocRef = authdb.collection("users")
                                    .document(auth.currentUser?.uid.toString())
                                    .collection("Notification")
                                    .document("notificationsList")
                                notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                    removebidnotification(position)
                                    var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                    if (documentSnapshot.exists()) {
                                        notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                    } else {
                                        val newNotification = hashMapOf(
                                            "notifications" to listOf(notificationMsg)
                                        )
                                        notificationDocRef.set(newNotification)
                                    }
                                }
                            }

                        }
                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                builder.show()
            }
            "Edit Book Description" -> {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Enter New Description For ${myBooks[position].name}")
                val input = EditText(context)
                input.inputType = InputType.TYPE_CLASS_TEXT
                input.setText(myBooks[position].description.toString())
                builder.setView(input)
                builder.setPositiveButton("Confirm") { _, _ ->
                    val bookDesc = input.text.toString()
                    if (bookDesc.isNotEmpty()) {
                        notifyItemChanged(position)
                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                        x.get().addOnSuccessListener {
                                result->
                            x.update("Description", bookDesc)
                            var userBookRef = authdb.collection("users")
                                .document(auth.currentUser?.uid.toString())
                                .collection("My Books")
                                .document(myBooks[position].bookId)

                            userBookRef.update("Description", bookDesc).addOnSuccessListener {
                                userBookRef.update("Bids", FieldValue.delete())
                                notifyItemRangeChanged(position, myBooks.size)
                                myBooks[position].description = bookDesc
                                val notificationDocRef = authdb.collection("users")
                                    .document(auth.currentUser?.uid.toString())
                                    .collection("Notification")
                                    .document("notificationsList")
                                notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                    removebidnotification(position)
                                    var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                    if (documentSnapshot.exists()) {
                                        notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                    } else {
                                        val newNotification = hashMapOf(
                                            "notifications" to listOf(notificationMsg)
                                        )
                                        notificationDocRef.set(newNotification)
                                    }
                                }
                            }
                        }

                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                builder.show()
            }
            "Change Book Condition" -> {
                notifyItemChanged(position)
                var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                x.get().addOnSuccessListener {
                        result->
                    x.update("Condition", if (myBooks[position].condition == "Old") "New" else "Old")
                    var userBookRef = authdb.collection("users")
                        .document(auth.currentUser?.uid.toString())
                        .collection("My Books")
                        .document(myBooks[position].bookId)

                    userBookRef.update("Description", if (myBooks[position].condition == "Old") "New" else "Old").addOnSuccessListener {
                        userBookRef.update("Bids", FieldValue.delete())
                        myBooks[position].condition = if (myBooks[position].condition == "Old") "New" else "Old"
                        notifyItemRangeChanged(position, myBooks.size)
                        val notificationDocRef = authdb.collection("users")
                            .document(auth.currentUser?.uid.toString())
                            .collection("Notification")
                            .document("notificationsList")
                        notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                            removebidnotification(position)
                            var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                            if (documentSnapshot.exists()) {
                                notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                            } else {
                                val newNotification = hashMapOf(
                                    "notifications" to listOf(notificationMsg)
                                )
                                notificationDocRef.set(newNotification)
                            }
                        }
                    }

                }
            }
            "Edit Book Publisher", "Add Book Publisher" -> {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Enter ${if (selectedItem == "Add Book Publisher") "" else "New "}Publisher For ${myBooks[position].name}")
                val input = EditText(context)
                input.setText(myBooks[position].publisher.toString())
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)
                builder.setPositiveButton("Confirm") { _, _ ->
                    val bookPublisher = input.text.toString()
                    if (bookPublisher.isNotEmpty()) {
                        notifyItemChanged(position)
                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                        x.get().addOnSuccessListener {
                                result->
                            x.update("Publisher", bookPublisher)
                            var userBookRef = authdb.collection("users")
                                .document(auth.currentUser?.uid.toString())
                                .collection("My Books")
                                .document(myBooks[position].bookId)

                            userBookRef.update("Publisher", bookPublisher).addOnSuccessListener {
                                userBookRef.update("Bids", FieldValue.delete())
                                notifyItemRangeChanged(position, myBooks.size)
                                val notificationDocRef = authdb.collection("users")
                                    .document(auth.currentUser?.uid.toString())
                                    .collection("Notification")
                                    .document("notificationsList")
                                notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                    removebidnotification(position)
                                    var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                    if (documentSnapshot.exists()) {
                                        notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                    } else {
                                        val newNotification = hashMapOf(
                                            "notifications" to listOf(notificationMsg)
                                        )
                                        notificationDocRef.set(newNotification)
                                    }
                                }
                            }

                        }

                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                builder.show()
            }
            "Edit Book Author" -> {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Enter New Author For ${myBooks[position].name}")
                val input = EditText(context)
                input.setText(myBooks[position].author.toString())
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)
                builder.setPositiveButton("Confirm") { _, _ ->
                    val bookAuthor = input.text.toString()
                    if (bookAuthor.isNotEmpty()) {
                        notifyItemChanged(position)
                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                        x.get().addOnSuccessListener {
                                result->
                            x.update("Author", bookAuthor)
                            var userBookRef = authdb.collection("users")
                                .document(auth.currentUser?.uid.toString())
                                .collection("My Books")
                                .document(myBooks[position].bookId)

                            userBookRef.update("Author", bookAuthor).addOnSuccessListener {
                                userBookRef.update("Bids", FieldValue.delete())
                                myBooks[position].author = bookAuthor
                                notifyItemRangeChanged(position, myBooks.size)
                                val notificationDocRef = authdb.collection("users")
                                    .document(auth.currentUser?.uid.toString())
                                    .collection("Notification")
                                    .document("notificationsList")
                                notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                    removebidnotification(position)
                                    var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                    if (documentSnapshot.exists()) {
                                        notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                    } else {
                                        val newNotification = hashMapOf(
                                            "notifications" to listOf(notificationMsg)
                                        )
                                        notificationDocRef.set(newNotification)
                                    }
                                }
                            }

                        }

                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                builder.show()
            }
            "Edit Book Edition", "Add Book Edition" -> {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Enter ${if (selectedItem == "Add Book Edition") "" else "New "}Edition For ${myBooks[position].name}")
                val input = EditText(context)
                input.setText(myBooks[position].edition.toString())
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)
                builder.setPositiveButton("Confirm") { _, _ ->
                    val bookEdition = input.text.toString()
                    if (bookEdition.isNotEmpty()) {
                        notifyItemChanged(position)
                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                        x.get().addOnSuccessListener {
                                result->
                            x.update("Edition", bookEdition)
                            var userBookRef = authdb.collection("users")
                                .document(auth.currentUser?.uid.toString())
                                .collection("My Books")
                                .document(myBooks[position].bookId)

                            userBookRef.update("Edition", bookEdition).addOnSuccessListener {
                                userBookRef.update("Bids", FieldValue.delete())
                                myBooks[position].edition = bookEdition
                                notifyItemRangeChanged(position, myBooks.size)
                                val notificationDocRef = authdb.collection("users")
                                    .document(auth.currentUser?.uid.toString())
                                    .collection("Notification")
                                    .document("notificationsList")
                                notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                    removebidnotification(position)
                                    var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                    if (documentSnapshot.exists()) {
                                        notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                    } else {
                                        val newNotification = hashMapOf(
                                            "notifications" to listOf(notificationMsg)
                                        )
                                        notificationDocRef.set(newNotification)
                                    }
                                }
                            }

                        }

                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                builder.show()
            }
            "Edit Book ISBN", "Add Book ISBN" -> {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Enter ${if (selectedItem == "Add Book ISBN") "" else "New "}ISBN For ${myBooks[position].name}")
                val input = EditText(context)
                input.setText(myBooks[position].isbn.toString())
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)
                builder.setPositiveButton("Confirm") { _, _ ->
                    val bookISBN = input.text.toString()
                    if (bookISBN.isNotEmpty()) {
                        notifyItemChanged(position)
                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                        x.get().addOnSuccessListener {
                                result->
                            x.update("ISBN", bookISBN)
                            var userBookRef = authdb.collection("users")
                                .document(auth.currentUser?.uid.toString())
                                .collection("My Books")
                                .document(myBooks[position].bookId)

                            userBookRef.update("ISBN", bookISBN).addOnSuccessListener {
                                userBookRef.update("Bids", FieldValue.delete())
                                myBooks[position].isbn = bookISBN
                                notifyItemRangeChanged(position, myBooks.size)
                                val notificationDocRef = authdb.collection("users")
                                    .document(auth.currentUser?.uid.toString())
                                    .collection("Notification")
                                    .document("notificationsList")
                                notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                    removebidnotification(position)
                                    var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                    if (documentSnapshot.exists()) {
                                        notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                    } else {
                                        val newNotification = hashMapOf(
                                            "notifications" to listOf(notificationMsg)
                                        )
                                        notificationDocRef.set(newNotification)
                                    }
                                }
                            }

                        }

                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                builder.show()
            }
            "Edit Book Mode" -> {
                val items = ArrayList<String>()
                when (myBooks[position].dealMode) {
                    "Sell" -> {
                        items.add("Exchange")
                        items.add("Both")
                    }
                    "Exchange" -> {
                        items.add("Sell")
                        items.add("Both")
                    }
                    else -> {
                        items.add("Sell")
                        items.add("Exchange")
                    }
                }
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Select New Mode For ${myBooks[position].name}")
                val arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, items)
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                val input = Spinner(context)
                input.adapter = arrayAdapter
                input.setSelection(0, false)
                var selectedMode: String? = null
                input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        pos: Int,
                        id: Long
                    ) {
                        selectedMode = parent.getItemAtPosition(pos).toString()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                builder.setView(input)
                builder.setPositiveButton("Confirm") { _, _ ->
                    if (selectedMode != null) {
                        when (selectedMode) {
                            "Sell" -> {
                                myBooks[position].exchangeBook = null
                                val newBuilder = AlertDialog.Builder(context)
                                newBuilder.setTitle("Enter Price For ${myBooks[position].name}")
                                val newInput = EditText(context)
                                newInput.inputType = InputType.TYPE_CLASS_NUMBER
                                newBuilder.setView(newInput)
                                newBuilder.setCancelable(false)
                                newBuilder.setPositiveButton("Confirm") { _, _ ->
                                    val bookPrice = newInput.text.toString()
                                    if (bookPrice.isNotEmpty()) {
                                        notifyItemChanged(position)
                                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                                        x.get().addOnSuccessListener {
                                                result->
                                            x.update(
                                                mapOf(
                                                    "Sell Price" to FieldValue.delete(),
                                                    "Exchange Book" to FieldValue.delete(),
                                                    "Deal Mode" to FieldValue.delete()
                                                )
                                            ).addOnSuccessListener {
                                                x.update(
                                                    mapOf(
                                                        "Deal Mode" to selectedMode,
                                                        "Sell Price" to bookPrice
                                                    )
                                                )
                                                val bookRef = authdb.collection("users")
                                                    .document(auth.currentUser?.uid.toString())
                                                    .collection("My Books")
                                                    .document(myBooks[position].bookId)

                                                val updatesToDelete = hashMapOf<String, Any>(
                                                    "Sell Price" to FieldValue.delete(),
                                                    "Exchange Book" to FieldValue.delete(),
                                                    "Deal Mode" to FieldValue.delete()
                                                )

                                                bookRef.update(updatesToDelete)
                                                    .addOnSuccessListener {
                                                        val newUpdates = hashMapOf<String, Any>(
                                                            "Deal Mode" to selectedMode,
                                                            "Sell Price" to bookPrice
                                                        )
                                                        bookRef.update(newUpdates)
                                                            .addOnSuccessListener {
                                                                bookRef.update("Bids", FieldValue.delete())
                                                                myBooks[position].dealMode = selectedMode
                                                                myBooks[position].price = bookPrice
                                                                notifyItemRangeChanged(position, myBooks.size)
                                                                val notificationDocRef = authdb.collection("users")
                                                                    .document(auth.currentUser?.uid.toString())
                                                                    .collection("Notification")
                                                                    .document("notificationsList")
                                                                notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                                                    removebidnotification(position)
                                                                    var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                                                    if (documentSnapshot.exists()) {
                                                                        notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                                                    } else {
                                                                        val newNotification = hashMapOf(
                                                                            "notifications" to listOf(notificationMsg)
                                                                        )
                                                                        notificationDocRef.set(newNotification)
                                                                    }
                                                                }
                                                            }
                                                    }

                                            }

                                        }

                                    }
                                }
                                newBuilder.setNegativeButton("Cancel", null)
                                var dialog1 = newBuilder.create()
                                dialog1.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                                dialog1.show()
                            }
                            "Exchange" -> {
                                myBooks[position].price = null
                                val newBuilder = AlertDialog.Builder(context)
                                newBuilder.setTitle("Enter Exchange Book For ${myBooks[position].name}")
                                val newInput = EditText(context)
                                newInput.inputType = InputType.TYPE_CLASS_TEXT
                                newBuilder.setView(newInput)
                                newBuilder.setCancelable(false)
                                newBuilder.setPositiveButton("Confirm") { _, _ ->
                                    val exchangeBook = newInput.text.toString()
                                    if (exchangeBook.isNotEmpty()) {
                                        notifyItemChanged(position)
                                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                                        x.get().addOnSuccessListener {
                                                result->
                                            val updateMap = hashMapOf<String, Any>(
                                                "Sell Price" to FieldValue.delete(),
                                                "Exchange Book" to FieldValue.delete(),
                                                "Deal Mode" to FieldValue.delete()
                                            )
                                            x.update(updateMap).addOnSuccessListener {
                                                val newValues = hashMapOf<String, Any>(
                                                    "Deal Mode" to selectedMode,
                                                    "Exchange Book" to exchangeBook
                                                )
                                                x.update(newValues)
                                                val bookDoc = authdb.collection("users")
                                                    .document(auth.currentUser?.uid.toString())
                                                    .collection("My Books")
                                                    .document(myBooks[position].bookId)

                                                val updates = hashMapOf<String, Any>(
                                                    "Sell Price" to FieldValue.delete(),
                                                    "Deal Mode" to selectedMode,
                                                    "Exchange Book" to exchangeBook
                                                )

                                                bookDoc.update(updates)
                                                    .addOnSuccessListener {
                                                        bookDoc.update("Bids", FieldValue.delete())
                                                        myBooks[position].dealMode = selectedMode
                                                        myBooks[position].exchangeBook = exchangeBook
                                                        notifyItemRangeChanged(position, myBooks.size)
                                                        val notificationDocRef = authdb.collection("users")
                                                            .document(auth.currentUser?.uid.toString())
                                                            .collection("Notification")
                                                            .document("notificationsList")
                                                        notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                                            removebidnotification(position)
                                                            var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                                            if (documentSnapshot.exists()) {
                                                                notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                                            } else {
                                                                val newNotification = hashMapOf(
                                                                    "notifications" to listOf(notificationMsg)
                                                                )
                                                                notificationDocRef.set(newNotification)
                                                            }
                                                        }
                                                    }

                                            }

                                        }

                                    }
                                }
                                newBuilder.setNegativeButton("Cancel", null)
                                var dialog1 = newBuilder.create()
                                dialog1.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                                dialog1.show()
                            }
                            "Both" -> {
                                val newBuilder = AlertDialog.Builder(context)
                                newBuilder.setTitle("Enter Details For ${myBooks[position].name}")
                                val layout = LinearLayout(context)
                                layout.orientation = LinearLayout.VERTICAL
                                val newPrice = EditText(context)
                                newPrice.hint = "Price"
                                newPrice.inputType = InputType.TYPE_CLASS_NUMBER
                                layout.addView(newPrice)
                                val newExchangeBook = EditText(context)
                                newExchangeBook.hint = "Exchange Book"
                                newExchangeBook.inputType = InputType.TYPE_CLASS_TEXT
                                layout.addView(newExchangeBook)
                                newBuilder.setView(layout)
                                newBuilder.setCancelable(false)
                                newBuilder.setPositiveButton("Confirm") { _, _ ->
                                    val price = newPrice.text.toString().trim()
                                    val exchangeBook = newExchangeBook.text.toString()
                                    if (price.isNotEmpty() && exchangeBook.isNotEmpty()) {
                                        notifyItemChanged(position)
                                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                                        x.get().addOnSuccessListener { result ->
                                            x.update("Sell Price", FieldValue.delete())
                                            x.update("Exchange Book", FieldValue.delete())
                                            x.update("Deal Mode", FieldValue.delete())

                                            x.update("Deal Mode", selectedMode)
                                            x.update("Sell Price", price)
                                            x.update("Exchange Book", exchangeBook)
                                            val docRef = authdb.collection("users")
                                                .document(auth.currentUser?.uid.toString())
                                                .collection("My Books")
                                                .document(myBooks[position].bookId)

                                            val updates = hashMapOf<String, Any>(
                                                "Sell Price" to price,
                                                "Exchange Book" to exchangeBook,
                                                "Deal Mode" to selectedMode
                                            )
                                            docRef.update(updates)
                                                .addOnSuccessListener {
                                                    docRef.update("Bids", FieldValue.delete())
                                                    myBooks[position].dealMode = selectedMode
                                                    myBooks[position].price = price
                                                    myBooks[position].exchangeBook = exchangeBook
                                                    notifyItemRangeChanged(position, myBooks.size)
                                                    val notificationDocRef = authdb.collection("users")
                                                        .document(auth.currentUser?.uid.toString())
                                                        .collection("Notification")
                                                        .document("notificationsList")
                                                    notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                                        removebidnotification(position)
                                                        var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                                        if (documentSnapshot.exists()) {
                                                            notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                                        } else {
                                                            val newNotification = hashMapOf(
                                                                "notifications" to listOf(notificationMsg)
                                                            )
                                                            notificationDocRef.set(newNotification)
                                                        }
                                                    }
                                                }

                                        }


                                    }
                                }
                                newBuilder.setNegativeButton("Cancel", null)
                                var dialog1 = newBuilder.create()
                                dialog1.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                                dialog1.show()
                            }
                        }
                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                dialog.show()
            }
            "Edit Book Price" -> {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Enter New Price For ${myBooks[position].name}")
                val input = EditText(context)
                input.setText(myBooks[position].price.toString())
                input.inputType = InputType.TYPE_CLASS_NUMBER
                builder.setView(input)
                builder.setPositiveButton("Confirm") { _, _ ->
                    val bookPrice = input.text.toString()
                    if (bookPrice.isNotEmpty()) {
                        notifyItemChanged(position)
                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                        x.get().addOnSuccessListener {
                                result->
                            x.update("Book Price", bookPrice)
                            val docRef = authdb.collection("users")
                                .document(auth.currentUser?.uid.toString())
                                .collection("My Books")
                                .document(myBooks[position].bookId)

                            docRef.update("Book Price", bookPrice)
                                .addOnSuccessListener {
                                    docRef.update("Bids", FieldValue.delete())
                                    myBooks[position].price = bookPrice
                                    notifyItemRangeChanged(position, myBooks.size)
                                    val notificationDocRef = authdb.collection("users")
                                        .document(auth.currentUser?.uid.toString())
                                        .collection("Notification")
                                        .document("notificationsList")
                                    notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                        removebidnotification(position)
                                        var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                        if (documentSnapshot.exists()) {
                                            notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                        } else {
                                            val newNotification = hashMapOf(
                                                "notifications" to listOf(notificationMsg)
                                            )
                                            notificationDocRef.set(newNotification)
                                        }
                                    }
                                }

                        }

                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                dialog.show()
            }
            "Edit Exchange Book" -> {
                val builder = AlertDialog.Builder(context)
                builder.setTitle("Enter New Exchange Book For ${myBooks[position].name}")
                val input = EditText(context)
                input.setText(myBooks[position].exchangeBook.toString())
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)
                builder.setPositiveButton("Confirm") { _, _ ->
                    val exchangeBook = input.text.toString()
                    if (exchangeBook.isNotEmpty()) {
                        notifyItemChanged(position)
                        var x  = authdb.collection("TradeBooks").document(myBooks[position].bookId)
                        x.get().addOnSuccessListener {
                                result->
                            x.update("Exchange Book", exchangeBook)
                            val docRef = authdb.collection("users")
                                .document(auth.currentUser?.uid.toString())
                                .collection("My Books")
                                .document(myBooks[position].bookId)

                            docRef.update("Exchange Book", exchangeBook)
                                .addOnSuccessListener {

                                    docRef.update("Bids", FieldValue.delete())
                                    myBooks[position].exchangeBook = exchangeBook
                                    notifyItemRangeChanged(position, myBooks.size)
                                    val notificationDocRef = authdb.collection("users")
                                        .document(auth.currentUser?.uid.toString())
                                        .collection("Notification")
                                        .document("notificationsList")
                                    notificationDocRef.get().addOnSuccessListener { documentSnapshot ->
                                        removebidnotification(position)
                                        var notificationMsg = "Book ${myBooks[position].name} details have been updated."
                                        if (documentSnapshot.exists()) {
                                            notificationDocRef.update("notifications", FieldValue.arrayUnion(notificationMsg))
                                        } else {
                                            val newNotification = hashMapOf(
                                                "notifications" to listOf(notificationMsg)
                                            )
                                            notificationDocRef.set(newNotification)
                                        }
                                    }
                                }
                        }

                    }
                }
                builder.setNegativeButton("Cancel", null)
                var dialog = builder.create()
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
                dialog.show()
            }
        }
    }
    fun removebidnotification(position: Int)
    {
        authdb.collection("users")
            .document(auth.currentUser?.uid.toString())
            .collection("My Books")
            .document(myBooks[position].bookId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val bids = documentSnapshot.get("Bids") as? List<Map<String, Any>>
                if (bids != null) {
                    for (bid in bids) {
                        val bidderId = bid["bidderId"]?.toString()
                        if (bidderId != null) {
                            val notificationMsg = "Your bid for Book ${documentSnapshot.get("Title").toString()} was removed because the book details were updated."
                            val notificationDocRef = authdb.collection("users")
                                .document(bidderId)
                                .collection("Notification")
                                .document("notificationsList")

                            notificationDocRef.get().addOnSuccessListener { notifSnapshot ->
                                if (notifSnapshot.exists()) {
                                    notificationDocRef.update(
                                        "notifications",
                                        FieldValue.arrayUnion(notificationMsg)
                                    )
                                } else {
                                    val newNotification = hashMapOf(
                                        "notifications" to listOf(notificationMsg)
                                    )
                                    notificationDocRef.set(newNotification)
                                }
                            }
                        }
                    }
                }
            }
    }
}