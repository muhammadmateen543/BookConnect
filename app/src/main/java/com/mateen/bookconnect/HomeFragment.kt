package com.mateen.bookconnect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.widget.addTextChangedListener

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore
    private lateinit var sort: ImageView
    private lateinit var list: ListView
    private lateinit var etsearch: EditText
    private lateinit var progressBar: ProgressBar
    var currentBooksList: List<Book> = listOf()
    private var isLoading: Boolean = false // Track loading state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        auth = FirebaseAuth.getInstance()
        authdb = FirebaseFirestore.getInstance()
        list = view.findViewById(R.id.lv1)
        sort = view.findViewById(R.id.sort)
        etsearch = view.findViewById(R.id.etsearch)
        progressBar = view.findViewById(R.id.pb_home)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etsearch.isEnabled = false
        showLoading(true) // Show ProgressBar and set loading state

        displayAllBooksForSale { books ->
            currentBooksList = books

            if (isAdded && view != null) { // Ensure fragment is still valid
                val adapter = CustomAdapterForBook(
                    requireContext(),
                    requireActivity().supportFragmentManager,
                    books.toMutableList()
                )
                list.adapter = adapter
                etsearch.isEnabled = true
                showLoading(false) // Hide ProgressBar and clear loading state
            }
        }

        sort.setOnClickListener {
            showSortOptions()
        }

        etsearch.addTextChangedListener { editable: Editable? ->
            val query = editable.toString().trim()
            val filteredBooks = currentBooksList.filter { book ->
                book.name.contains(query, ignoreCase = true)
            }
            if (isAdded && view != null) {
                val adapter = CustomAdapterForBook(
                    requireContext(),
                    requireActivity().supportFragmentManager,
                    filteredBooks.toMutableList()
                )
                list.adapter = adapter
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Notify MainActivity of loading state
        (requireActivity() as? MainActivity)?.onFragmentLoadingStateChanged(isLoading)
    }

    fun isLoading(): Boolean = isLoading // Expose loading state to MainActivity

    private fun showSortOptions() {
        val sortOptions = arrayOf("Sort by Name", "Filter by (For Sale)", "Filter by (For Exchange)", "Filter by (For Both)")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Sort By")
        builder.setItems(sortOptions) { dialog, item ->
            val selectedOption = sortOptions[item]
            sort.tag = selectedOption
            showLoading(true) // Show ProgressBar for sorting
            displayAllBooksSortby(selectedOption) { books ->
                currentBooksList = books
                if (isAdded && view != null) {
                    val adapter = CustomAdapterForBook(
                        requireContext(),
                        requireActivity().supportFragmentManager,
                        books.toMutableList()
                    )
                    list.adapter = adapter
                    showLoading(false) // Hide ProgressBar after sorting
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#4b90de")))
        dialog.show()
    }

    private fun displayAllBooksForSale(onBooksLoaded: (List<Book>) -> Unit) {
        val books = mutableListOf<Book>()

        authdb.collection("TradeBooks").get().addOnSuccessListener { result ->
            if (result.isEmpty) {
                onBooksLoaded(books)
                showLoading(false)
                return@addOnSuccessListener
            }

            var processedCount = 0
            for (doc in result) {
                val data = doc.data
                val bookId = data["bookId"].toString()
                val author = data["Author"].toString()
                val condition = data["Condition"].toString()
                val dealMode = data["Deal Mode"].toString()
                val description = data["Description"].toString()
                val edition = data["Edition"].toString()
                val isbn = data["ISBN"].toString()
                val publisher = data["Publisher"].toString()
                val title = data["Title"].toString()
                var sellPrice = ""
                var exchangeBook = ""

                if (dealMode == "Sell") sellPrice = data["Sell Price"].toString()
                else if (dealMode == "Exchange") exchangeBook = data["Exchange Book"].toString()
                else if (dealMode == "Both") {
                    sellPrice = data["Sell Price"].toString()
                    exchangeBook = data["Exchange Book"].toString()
                }

                val base64Images = arrayListOf<String>()

                authdb.collection("Base64Images").document(bookId).collection("Images").get()
                    .addOnSuccessListener { images ->
                        for (item in images) {
                            val imageData = item.data
                            base64Images.add(imageData["base64"].toString())
                        }
                        books.add(
                            Book(
                                bookId,
                                base64Images,
                                title,
                                description,
                                condition,
                                publisher,
                                author,
                                edition,
                                isbn,
                                dealMode,
                                sellPrice,
                                exchangeBook
                            )
                        )
                        processedCount++
                        if (processedCount == result.size()) {
                            onBooksLoaded(books)
                            showLoading(false)
                        }
                    }
                    .addOnFailureListener {
                        processedCount++
                        if (processedCount == result.size()) {
                            onBooksLoaded(books)
                            showLoading(false)
                        }
                    }
            }
        }.addOnFailureListener {
            onBooksLoaded(books)
            showLoading(false)
        }
    }

    private fun displayAllBooksSortby(selectedOption: String, onBooksLoaded: (List<Book>) -> Unit) {
        val books = mutableListOf<Book>()

        authdb.collection("TradeBooks").get().addOnSuccessListener { result ->
            if (result.isEmpty) {
                onBooksLoaded(books)
                showLoading(false)
                return@addOnSuccessListener
            }

            var processedCount = 0
            for (doc in result) {
                val data = doc.data
                val bookId = data["bookId"].toString()
                val author = data["Author"].toString()
                var condition = data["Condition"].toString()
                val dealMode = data["Deal Mode"].toString()
                val description = data["Description"].toString()
                val edition = data["Edition"].toString()
                val isbn = data["ISBN"].toString()
                val publisher = data["Publisher"].toString()
                val title = data["Title"].toString()
                var sellPrice = ""
                var exchangeBook = ""

                if (dealMode == "Sell") sellPrice = data["Sell Price"].toString()
                else if (dealMode == "Exchange") exchangeBook = data["Exchange Book"].toString()
                else if (dealMode == "Both") {
                    sellPrice = data["Sell Price"].toString()
                    exchangeBook = data["Exchange Book"].toString()
                }

                val base64Images = arrayListOf<String>()

                authdb.collection("Base64Images").document(bookId).collection("Images").get()
                    .addOnSuccessListener { images ->
                        for (item in images) {
                            val imageData = item.data
                            base64Images.add(imageData["base64"].toString())
                        }
                        books.add(
                            Book(
                                bookId,
                                base64Images,
                                title,
                                description,
                                condition,
                                publisher,
                                author,
                                edition,
                                isbn,
                                dealMode,
                                sellPrice,
                                exchangeBook
                            )
                        )
                        processedCount++
                        if (processedCount == result.size()) {
                            val finalBooks = when (selectedOption) {
                                "Sort by Name" -> books.sortedBy { it.name }
                                "Filter by (For Sale)" -> books.filter { it.dealMode == "Sell" }
                                "Filter by (For Exchange)" -> books.filter { it.dealMode == "Exchange" }
                                "Filter by (For Both)" -> books.filter { it.dealMode == "Both" }
                                else -> books
                            }
                            onBooksLoaded(finalBooks)
                            showLoading(false)
                        }
                    }
                    .addOnFailureListener {
                        processedCount++
                        if (processedCount == result.size()) {
                            val finalBooks = when (selectedOption) {
                                "Sort by Name" -> books.sortedBy { it.name }
                                "Filter by (For Sale)" -> books.filter { it.dealMode == "Sell" }
                                "Filter by (For Exchange)" -> books.filter { it.dealMode == "Exchange" }
                                "Filter by (For Both)" -> books.filter { it.dealMode == "Both" }
                                else -> books
                            }
                            onBooksLoaded(finalBooks)
                            showLoading(false)
                        }
                    }
            }
        }.addOnFailureListener {
            onBooksLoaded(books)
            showLoading(false)
        }
    }
}