package com.mateen.bookconnect

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class BookDetailFragment : Fragment() {

    private var highestBidAmount: Int = 0
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreDB: FirebaseFirestore
    private var currentImageIndex = 0

    private lateinit var bookId: String
    private lateinit var images: ArrayList<String>
    private lateinit var bookTitle: String
    private lateinit var bookDescription: String
    private lateinit var bookCondition: String
    private lateinit var bookEdition: String
    private lateinit var bookPublisher: String
    private lateinit var bookISBN: String
    private lateinit var bookAuthor: String

    private lateinit var backButton: ImageView
    private lateinit var bookImage: ImageView
    private lateinit var leftArrowButton: ImageView
    private lateinit var rightArrowButton: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var authorTextView: TextView
    private lateinit var publisherTextView: TextView
    private lateinit var editionTextView: TextView
    private lateinit var isbnTextView: TextView
    private lateinit var conditionTextView: TextView
    private lateinit var bidAmountEditText: EditText
    private lateinit var bidBookEditText: EditText

    private lateinit var dealtype: TextView
    private lateinit var pricetitle: TextView
    private lateinit var pricet: TextView
    private lateinit var exchanget: TextView
    private lateinit var exchangetitle: TextView

    private lateinit var placeBidButtonLayout: LinearLayout
    private lateinit var placeBidButtonIcon: ImageView

    private lateinit var bidPriceTextView: TextView
    private lateinit var bidBookTextView: TextView
    private lateinit var bidBookLayout: LinearLayout
    private lateinit var bidPriceLayout: LinearLayout

    private lateinit var bookOwnerId: String
    private var dealMode: String = ""
    private var pricee: String = ""
    private var exchangeBooke: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookId = it.getString("bookId").toString()
            images = it.getStringArrayList("images") ?: ArrayList()
            bookTitle = it.getString("title").toString()
            bookDescription = it.getString("description").toString()
            bookAuthor = it.getString("author").toString()
            bookPublisher = it.getString("publisher").toString()
            bookEdition = it.getString("edition").toString()
            bookISBN = it.getString("isbn").toString()
            bookCondition = it.getString("condition").toString()
            dealMode=it.getString("dealMode").toString()
            pricee=it.getString("price").toString()
            exchangeBooke=it.getString("exchangeBook").toString()
            Toast.makeText(requireContext(), dealMode, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_detail, container, false)
        auth = FirebaseAuth.getInstance()
        firestoreDB = FirebaseFirestore.getInstance()

        backButton = view.findViewById(R.id.btback)
        bookImage = view.findViewById(R.id.ivimage)
        titleTextView = view.findViewById(R.id.title)
        descriptionTextView = view.findViewById(R.id.description)
        authorTextView = view.findViewById(R.id.author)
        publisherTextView = view.findViewById(R.id.publisher)
        isbnTextView = view.findViewById(R.id.isbn)
        editionTextView = view.findViewById(R.id.edition)
        conditionTextView = view.findViewById(R.id.condition)
        leftArrowButton = view.findViewById(R.id.ivleft)
        rightArrowButton = view.findViewById(R.id.ivright)

        dealtype = view.findViewById(R.id.dealtype)
        pricetitle = view.findViewById(R.id.pricetitle)
        pricet = view.findViewById(R.id.price)
        exchangetitle = view.findViewById(R.id.exchangetitle)
        exchanget = view.findViewById(R.id.exchange)

        bidAmountEditText = view.findViewById(R.id.etbidprice)
        bidBookEditText = view.findViewById(R.id.etbidbook)

        placeBidButtonLayout = view.findViewById(R.id.btnbid)
        placeBidButtonIcon = placeBidButtonLayout.findViewById(R.id.markdone)

        bidPriceTextView = view.findViewById(R.id.tvbidprice)
        bidBookTextView = view.findViewById(R.id.tvbidbook)
        bidBookLayout = view.findViewById(R.id.viewbidbook)
        bidPriceLayout = view.findViewById(R.id.viewbidprice)

        if(dealMode.toString()=="Sell")
        {
            exchanget.visibility=View.GONE
            exchangetitle.visibility= View.GONE
        }
        else if(dealMode.toString()=="Exchange")
        {
            pricet.visibility= View.GONE
            pricetitle.visibility= View.GONE
        }
        else
        {
            pricet.visibility= View.VISIBLE
            pricetitle.visibility= View.VISIBLE
            exchanget.visibility=View.VISIBLE
            exchangetitle.visibility= View.VISIBLE
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleTextView.text = bookTitle
        descriptionTextView.text = bookDescription
        authorTextView.text = bookAuthor
        publisherTextView.text = bookPublisher
        editionTextView.text = bookEdition
        conditionTextView.text = bookCondition
        isbnTextView.text = bookISBN
        dealtype.text=dealMode
        pricet.text=pricee
        exchanget.text=exchangeBooke

        if (images.isEmpty()) {
            bookImage.setImageResource(R.drawable.noimageicon)
            leftArrowButton.visibility = View.INVISIBLE
            rightArrowButton.visibility = View.INVISIBLE
        } else {
            bookImage.setImageBitmap(base64ToBitmap(images[currentImageIndex]))
            leftArrowButton.visibility = View.INVISIBLE
            rightArrowButton.visibility = if (images.size > 1) View.VISIBLE else View.INVISIBLE
        }

        leftArrowButton.setOnClickListener {
            if (currentImageIndex > 0) {
                currentImageIndex--
                bookImage.setImageBitmap(base64ToBitmap(images[currentImageIndex]))
                leftArrowButton.visibility = if (currentImageIndex != 0) View.VISIBLE else View.INVISIBLE
                rightArrowButton.visibility = View.VISIBLE
            }
        }

        rightArrowButton.setOnClickListener {
            if (currentImageIndex < images.size - 1) {
                currentImageIndex++
                bookImage.setImageBitmap(base64ToBitmap(images[currentImageIndex]))
                leftArrowButton.visibility = View.VISIBLE
                rightArrowButton.visibility = if (currentImageIndex != images.size - 1) View.VISIBLE else View.INVISIBLE
            }
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        fetchBookDetailsAndSetupUI()
    }

    private fun fetchBookDetailsAndSetupUI() {
        firestoreDB.collection("TradeBooks").document(bookId).get()
            .addOnSuccessListener { result ->

                bookOwnerId = result.getString("uid").toString()
                dealMode = result.getString("Deal Mode").toString()

                if (bookOwnerId == auth.currentUser?.uid) {
                    placeBidButtonLayout.visibility = View.GONE
                    bidBookLayout.visibility = View.GONE
                    bidPriceTextView.text = "You are the owner of this Book."
                    bidPriceLayout.visibility = View.VISIBLE
                    bidAmountEditText.visibility=View.GONE
                } else {
                    configureBidUIBasedOnDealMode()
                    checkIfUserAlreadyBiddedOnThisBook { existingBid ->
                        if (existingBid != null) {
                            placeBidButtonLayout.visibility = View.GONE
                            bidPriceLayout.visibility = View.GONE
                            bidBookLayout.visibility = View.GONE

                            val bidText = when (dealMode) {
                                "Sell" -> "Bid already added for PKR ${existingBid["bidAmount"]}."
                                "Exchange" -> "Bid already added with Book: ${existingBid["bidBook"]}."
                                else -> "Bid already added for PKR ${existingBid["bidAmount"]} and book ${existingBid["bidBook"]}."
                            }
                            bidPriceTextView.text = bidText
                            bidAmountEditText.visibility= View.GONE
                            bidPriceLayout.visibility = View.VISIBLE
                        } else {
                            placeBidButtonLayout.visibility= View.VISIBLE
                            placeBidButtonLayout.isEnabled = true
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                placeBidButtonLayout.isEnabled = false
            }
    }

    private fun configureBidUIBasedOnDealMode() {
        firestoreDB.collection("users")
            .document(bookOwnerId)
            .collection("My Books")
            .document(bookId)
            .get()
            .addOnSuccessListener { data ->
                val bids = data.get("Bids") as? List<Map<String, String>>
                highestBidAmount = bids?.mapNotNull { it["bidAmount"]?.toIntOrNull() }?.maxOrNull() ?: 0

                when (dealMode) {
                    "Sell" -> {
                        bidPriceLayout.visibility = View.VISIBLE
                        bidBookLayout.visibility = View.GONE
                        bidAmountEditText.inputType = InputType.TYPE_CLASS_NUMBER
                        bidPriceTextView.text = if (highestBidAmount > 0) "Current highest bid: PKR $highestBidAmount. Place a higher bid." else "Place a bid (Price)."
                    }
                    "Exchange" -> {
                        bidPriceLayout.visibility = View.GONE
                        bidBookLayout.visibility = View.VISIBLE
                        bidBookEditText.inputType = InputType.TYPE_CLASS_TEXT
                        bidBookTextView.text = "Place a bid (Book for Exchange)."
                    }
                    "Both" -> {
                        bidPriceLayout.visibility = View.VISIBLE
                        bidBookLayout.visibility = View.VISIBLE
                        bidAmountEditText.inputType = InputType.TYPE_CLASS_NUMBER
                        bidBookEditText.inputType = InputType.TYPE_CLASS_TEXT
                        bidPriceTextView.text = "Place a bid (Price)."
                        bidBookTextView.text = "Place a bid (Book for Exchange)."
                    }
                }
                setupPlaceBidButtonListener()
            }
            .addOnFailureListener { e ->
                placeBidButtonLayout.isEnabled = false
            }
    }

    private fun setupPlaceBidButtonListener() {
        placeBidButtonLayout.setOnClickListener {
            val currentUserId = auth.currentUser?.uid.toString()
            when (dealMode) {
                "Sell" -> {
                    if (bidAmountEditText.text.isEmpty()) {
                        ShowError(requireContext()).showError("BID PRICE IS EMPTY.")
                        return@setOnClickListener
                    }
                    val bidPrice = bidAmountEditText.text.toString().toIntOrNull()
                    if (bidPrice == null || bidPrice <= 0) {
                        ShowError(requireContext()).showError("Please enter a valid bid price.")
                        return@setOnClickListener
                    }
//                    if (highestBidAmount > 0 && bidPrice <= highestBidAmount) {
//                        ShowMessage(requireContext()).showMessage("Your bid must be higher than the current highest bid of PKR $highestBidAmount.")
//                        return@setOnClickListener
//                    }
                    placeBidButtonLayout.isEnabled = false
                    saveBid(currentUserId, bidAmount = bidPrice.toString())
                }
                "Exchange" -> {
                    if (bidBookEditText.text.isEmpty()) {
                        ShowError(requireContext()).showError("BID BOOK IS EMPTY.")
                        return@setOnClickListener
                    }
                    val bidBook = bidBookEditText.text.toString()
                    placeBidButtonLayout.isEnabled = false
                    saveBid(currentUserId, bidBook = bidBook)
                }
                "Both" -> {
                    if (bidAmountEditText.text.isEmpty() || bidBookEditText.text.isEmpty()) {
                        ShowError(requireContext()).showError("BID PRICE OR BID BOOK IS EMPTY.")
                        return@setOnClickListener
                    }
                    val bidPrice = bidAmountEditText.text.toString().toIntOrNull()
                    if (bidPrice == null || bidPrice <= 0) {
                        ShowError(requireContext()).showError("Please enter a valid bid price.")
                        return@setOnClickListener
                    }
                    val bidBook = bidBookEditText.text.toString()

                    placeBidButtonLayout.isEnabled = false
                    saveBid(currentUserId, bidAmount = bidPrice.toString(), bidBook = bidBook)
                }
                else -> {
                    ShowError(requireContext()).showError("Invalid deal mode.")
                }
            }
        }
    }

    private fun saveBid(bidderId: String, bidAmount: String? = null, bidBook: String? = null) {
        val newBid = mutableMapOf<String, String>("bidderId" to bidderId)
        bidAmount?.let { newBid["bidAmount"] = it }
        bidBook?.let { newBid["bidBook"] = it }

        val userBookRef = firestoreDB.collection("users")
            .document(bookOwnerId)
            .collection("My Books")
            .document(bookId)

        firestoreDB.runTransaction { transaction ->
            val snapshot = transaction.get(userBookRef)
            val bids = snapshot.get("Bids") as? List<Map<String, String>>
            val existingBids = bids?.toMutableList() ?: mutableListOf()

            existingBids.removeAll { it["bidderId"] == bidderId }

            existingBids.add(newBid)

            transaction.set(userBookRef, mapOf("Bids" to existingBids), SetOptions.merge())
            null
        }.addOnSuccessListener {
            ShowMessage(requireContext()).showMessage("Your bid has been added successfully.")
            placeBidButtonLayout.visibility = View.GONE
            bidBookLayout.visibility = View.GONE

            val bidText = when (dealMode) {
                "Sell" -> "Bid added for PKR ${bidAmount}."
                "Exchange" -> "Bid added with Book: ${bidBook}."
                else -> "Bid added for PKR ${bidAmount} and book ${bidBook}."
            }
            bidPriceTextView.text = bidText
            bidAmountEditText.visibility = View.GONE
            bidPriceLayout.visibility = View.VISIBLE

            if (dealMode == "Sell" && bidAmount != null) {
                val newBidPrice = bidAmount.toIntOrNull() ?: 0
                if (newBidPrice > highestBidAmount) {
                    highestBidAmount = newBidPrice
                }
            }

            val notificationDocRef = firestoreDB.collection("users")
                .document(bidderId)
                .collection("Notification")
                .document("notificationsList")

            notificationDocRef.get().addOnSuccessListener { notifSnapshot ->
                var notificationMsg=""
                if(dealMode=="Sell")
                {
                    notificationMsg = "Your bid of (PKR $bidAmount) for Book $bookTitle has been sent."
                }
                else if(dealMode=="Exchange")
                {
                    notificationMsg = "Your bid of (Book $bidBook) for Book $bookTitle has been sent."
                }
                else if(dealMode=="Both")
                {
                    notificationMsg = "Your bid of (PKR $bidAmount and Book $bidBook) for Book $bookTitle has been sent."
                }
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
            val notificationDocRef1 = firestoreDB.collection("users")
                .document(bookOwnerId)
                .collection("Notification")
                .document("notificationsList")

            notificationDocRef1.get().addOnSuccessListener { notifSnapshot ->
                firestoreDB.collection("users").document(bidderId).get().addOnSuccessListener{
                        d->
                    var biddername=d["Full Name"].toString()
                    var notificationMsg = ""
                    if (dealMode == "Sell") {
                        notificationMsg = "$biddername made a bid of (PKR $bidAmount) for Book $bookTitle."
                    } else if (dealMode == "Exchange") {
                        notificationMsg =
                            "$biddername made a bid of (Book $bidBook) for Book $bookTitle."
                    } else if (dealMode == "Both") {
                        notificationMsg =
                            "$biddername made a bid of (PKR $bidAmount and Book $bidBook) for Book $bookTitle."
                    }
                    if (notifSnapshot.exists()) {
                        notificationDocRef1.update(
                            "notifications",
                            FieldValue.arrayUnion(notificationMsg)
                        )
                    } else {
                        val newNotification = hashMapOf(
                            "notifications" to listOf(notificationMsg)
                        )
                        notificationDocRef1.set(newNotification)
                    }
                }
            }
        }.addOnFailureListener { e ->
            placeBidButtonLayout.isEnabled = true
            ShowError(requireContext()).showError("Failed to place bid: ${e.message}")
        }
    }

    private fun checkIfUserAlreadyBiddedOnThisBook(onResult1: (Map<String, String>?) -> Unit) {
        firestoreDB.collection("users")
            .document(bookOwnerId)
            .collection("My Books")
            .document(bookId)
            .get()
            .addOnSuccessListener { data ->
                val bids = data.get("Bids") as? List<Map<String, String>>
                val existingBid = bids?.firstOrNull { it["bidderId"] == auth.currentUser?.uid }
                onResult1(existingBid)
            }
            .addOnFailureListener { e ->
                onResult1(null)
            }
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