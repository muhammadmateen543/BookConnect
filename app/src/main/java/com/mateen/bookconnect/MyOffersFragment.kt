package com.mateen.bookconnect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyOffersFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore
    private lateinit var list: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var myUid: String
    private var isLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        authdb = FirebaseFirestore.getInstance()
        myUid = auth.currentUser?.uid.toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_offers, container, false)
        list = view.findViewById(R.id.lv1)
        progressBar = view.findViewById(R.id.pb_myOffers)

        showLoading(true)
        displayAllOffers { myOffersList ->
            if (isAdded && view != null) {
                val adapter = CustomerAdapterForOffer(
                    requireContext(),
                    requireActivity().supportFragmentManager,
                    myOffersList as MutableList<offer>
                )
                list.adapter = adapter
            }
            showLoading(false)
        }

        return view
    }

    private fun showLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        (requireActivity() as? MainActivity)?.onFragmentLoadingStateChanged(isLoading)
    }

    fun isLoading(): Boolean = isLoading

    private fun displayAllOffers(onBooksLoaded: (List<offer>) -> Unit) {
        val myOffersList = mutableListOf<offer>()

        authdb.collection("users").get().addOnSuccessListener { usersSnapshot ->
            var pendingUsers = usersSnapshot.size()
            if (pendingUsers == 0) {
                onBooksLoaded(myOffersList)
                showLoading(false)
                return@addOnSuccessListener
            }

            for (userDoc in usersSnapshot) {
                val userId = userDoc.id
                authdb.collection("users")
                    .document(userId)
                    .collection("My Books")
                    .get()
                    .addOnSuccessListener { booksSnapshot ->
                        for (bookDoc in booksSnapshot) {
                            val bookId = bookDoc.id
                            val title = bookDoc.getString("Title") ?: "Unknown Title"
                            val dealMode = bookDoc.getString("Deal Mode") ?: ""

                            val bidsList = bookDoc.get("Bids") as? List<Map<String, Any>>
                            if (bidsList != null) {
                                for (bid in bidsList) {
                                    val bidderUid = bid["bidderId"].toString()
                                    if (bidderUid == myUid) {
                                        val offer = when (dealMode) {
                                            "Sell" -> {
                                                val sellPrice = bid["bidAmount"]?.toString() ?: ""
                                                offer(userId, bookId, title, dealMode, sellPrice = sellPrice)
                                            }
                                            "Exchange" -> {
                                                val exchangeBook = bid["bidBook"]?.toString() ?: ""
                                                offer(userId, bookId, title, dealMode, exchangeBook = exchangeBook)
                                            }
                                            else -> {
                                                val sellPrice = bid["bidAmount"]?.toString() ?: ""
                                                val exchangeBook = bid["bidBook"]?.toString() ?: ""
                                                offer(userId, bookId, title, dealMode, sellPrice = sellPrice, exchangeBook = exchangeBook)
                                            }
                                        }
                                        myOffersList.add(offer)
                                    }
                                }
                            }
                        }
                        pendingUsers--
                        if (pendingUsers == 0) {
                            onBooksLoaded(myOffersList)
                            showLoading(false)
                        }
                    }
                    .addOnFailureListener {
                        pendingUsers--
                        if (pendingUsers == 0) {
                            onBooksLoaded(myOffersList)
                            showLoading(false)
                        }
                    }
            }
        }.addOnFailureListener {
            onBooksLoaded(myOffersList)
            showLoading(false)
        }
    }
}