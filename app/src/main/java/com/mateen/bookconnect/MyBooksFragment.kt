package com.mateen.bookconnect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.widget.addTextChangedListener

class MyBooksFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore
    private lateinit var myBooks: MutableList<Book>
    private lateinit var recyclerView: RecyclerView
    private lateinit var myBooksAdapter: myBooksAdapter
    private lateinit var etsearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        authdb = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_books, container, false)
        recyclerView = view.findViewById(R.id.rc_myBooks)
        etsearch=view.findViewById(R.id.etsearch)
        myBooks = mutableListOf()
        myBooksAdapter = myBooksAdapter(requireContext(), myBooks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = myBooksAdapter

        fetchBooks()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etsearch.addTextChangedListener { text ->
            val query = text.toString().trim()

            val filteredBooks = myBooks.filter { book ->
                book.name.contains(query, ignoreCase = true)
            } as MutableList

            myBooksAdapter = myBooksAdapter(requireContext(), filteredBooks)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = myBooksAdapter
        }
    }

    private fun fetchBooks() {
        val booksCollection = authdb
            .collection("users")
            .document(auth.currentUser?.uid.orEmpty())
            .collection("My Books")

        booksCollection.get().addOnSuccessListener { result ->
            myBooks.clear()

            for (doc in result) {
                val bookid = doc.id
                val name = doc.getString("Title") ?: ""
                val description = doc.getString("Description") ?: ""
                val author = doc.getString("Author") ?: ""
                val sellPrice = doc.getString("Sell Price") ?: ""
                val exchangeBook = doc.getString("Exchange Book") ?: ""
                val publisher = doc.getString("Publisher") ?: ""
                val edition = doc.getString("Edition") ?: ""
                val isbn = doc.getString("ISBN") ?: ""
                val condition = doc.getString("Condition") ?: ""

                val mode = when {
                    exchangeBook.isNotEmpty() && sellPrice.isNotEmpty() -> "Both"
                    sellPrice.isNotEmpty() -> "Sell"
                    exchangeBook.isNotEmpty() -> "Exchange"
                    else -> ""
                }

                val imagesCollectionRef = authdb
                    .collection("Base64Images")
                    .document(bookid)
                    .collection("Images")

                imagesCollectionRef.get().addOnSuccessListener { imagesResult ->
                    val imagesList = imagesResult.mapNotNull { it.getString("base64") }

                    val newBook = Book(
                        bookid,
                        ArrayList(imagesList),
                        name,
                        description,
                        condition,
                        publisher,
                        author,
                        edition,
                        isbn,
                        mode,
                        sellPrice,
                        exchangeBook
                    )
                    myBooks.add(newBook)
                    myBooksAdapter.notifyItemInserted(myBooks.size - 1)
                }.addOnFailureListener {
                    val newBook = Book(
                        bookid,
                        arrayListOf(),
                        name,
                        description,
                        condition,
                        publisher,
                        author,
                        edition,
                        isbn,
                        mode,
                        sellPrice,
                        exchangeBook
                    )
                    myBooks.add(newBook)
                    myBooksAdapter.notifyItemInserted(myBooks.size - 1)
                }
            }
        }
    }
}
