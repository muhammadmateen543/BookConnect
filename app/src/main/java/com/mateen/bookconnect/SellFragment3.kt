package com.mateen.bookconnect

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SellFragment3 : Fragment() {
    //Will receive from SellFragment2
    private lateinit var Base64Images: ArrayList<String>
    private lateinit var Title: String
    private lateinit var Description: String
    private lateinit var Condition: String
    private lateinit var Publisher: String
    private lateinit var Author: String
    private lateinit var Edition: String
    private lateinit var ISBN: String

    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore

    private lateinit var dealmode: Spinner
    private lateinit var dealsell: LinearLayout
    private lateinit var dealexchange: LinearLayout
    private lateinit var dealboth: LinearLayout
    private lateinit var btback: ImageView
    private lateinit var btsubmit: RelativeLayout

    private lateinit var etsellprice: EditText
    private lateinit var etexchangebook: EditText
    private lateinit var etbothprice: EditText
    private lateinit var etbothbook: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_sell3, container, false)
        btback=view.findViewById(R.id.btback)
        dealmode=view.findViewById(R.id.dealmode)
        dealsell=view.findViewById(R.id.dealsell)
        dealexchange=view.findViewById(R.id.dealexchange)
        dealboth=view.findViewById(R.id.dealboth)
        btsubmit=view.findViewById(R.id.btsubmit)

        etsellprice=view.findViewById(R.id.etsellprice)
        etexchangebook=view.findViewById(R.id.etexchangebook)
        etbothprice=view.findViewById(R.id.etbothprice)
        etbothbook=view.findViewById(R.id.etbothbook)
        Base64Images = arguments?.getStringArrayList("Base64Images") ?:ArrayList()
        Title=arguments?.getString("Title").toString()
        Description=arguments?.getString("Description").toString()
        Condition=arguments?.getString("Condition").toString()
        Publisher=arguments?.getString("Publisher").toString()
        Author=arguments?.getString("Author").toString()
        Edition=arguments?.getString("Edition").toString()
        ISBN=arguments?.getString("ISBN").toString()
        auth=FirebaseAuth.getInstance()
        authdb=FirebaseFirestore.getInstance()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btback.setOnClickListener({
            parentFragmentManager.popBackStack()
        })
        var selectedItem=""
        var modes= arrayOf("Sell", "Exchange", "Both")
        var arrayAdapter=ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modes)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dealmode.adapter=arrayAdapter
        dealmode.onItemSelectedListener=object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedItem = parent.getItemAtPosition(position).toString()
                if(selectedItem==modes[0])
                {
                    dealsell.visibility=View.VISIBLE
                    dealexchange.visibility=View.GONE
                    dealboth.visibility=View.GONE
                }
                else if(selectedItem==modes[1])

                {
                    dealsell.visibility=View.GONE
                    dealexchange.visibility=View.VISIBLE
                    dealboth.visibility=View.GONE
                }
                else if(selectedItem==modes[2])
                {
                    dealsell.visibility=View.GONE
                    dealexchange.visibility=View.GONE
                    dealboth.visibility=View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        btsubmit.setOnClickListener({
            if(selectedItem==modes[0])
            {
                if(etsellprice.text.toString().isEmpty())
                {
                    ShowError(requireContext()).showError("PRICE IS MISSING.")
                }
                else
                {
                    btsubmit.isEnabled = false // disable to prevent multiple clicks
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val myBook = hashMapOf(
                            "Title" to Title,
                            "Description" to Description,
                            "Condition" to Condition,
                            "Publisher" to Publisher,
                            "Author" to Author,
                            "Edition" to Edition,
                            "ISBN" to ISBN,
                            "Deal Mode" to "Sell",
                            "Sell Price" to etsellprice.text.toString()
                        )
                        authdb.collection("users").document(userId).collection("My Books").add(myBook)
                            .addOnSuccessListener { documentReference ->
                                val bookId = documentReference.id

                                val imagesCollectionRef = authdb.collection("Base64Images").document(bookId).collection("Images")
                                for (image in Base64Images) {
                                    val imageData = hashMapOf("base64" to image)
                                    imagesCollectionRef.add(imageData)
                                }

                                val tradeBook = HashMap(myBook)
                                tradeBook["uid"] = userId
                                tradeBook["bookId"] = bookId

                                authdb.collection("TradeBooks").document(bookId).set(tradeBook)
                                    .addOnSuccessListener {
                                        startActivity(Intent(requireContext(), MainActivity::class.java))
                                        requireActivity().finish()
                                    }
                                    .addOnFailureListener {
                                        btsubmit.isEnabled = true
                                    }
                            }
                            .addOnFailureListener {
                                btsubmit.isEnabled = true
                            }

                    }
                }
            }
            else if(selectedItem==modes[1])
            {
                if(etexchangebook.text.toString().isEmpty())
                {
                    ShowError(requireContext()).showError("WITH BOOK IS MISSING.")
                }
                else
                {
                    btsubmit.isEnabled = false
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val myBook = hashMapOf(
                            "Title" to Title,
                            "Description" to Description,
                            "Condition" to Condition,
                            "Publisher" to Publisher,
                            "Author" to Author,
                            "Edition" to Edition,
                            "ISBN" to ISBN,
                            "Deal Mode" to "Exchange",
                            "Exchange Book" to etexchangebook.text.toString()
                        )
                        authdb.collection("users").document(userId).collection("My Books").add(myBook)
                            .addOnSuccessListener { documentReference ->
                                val bookId = documentReference.id

                                val imagesCollectionRef = authdb.collection("Base64Images").document(bookId).collection("Images")
                                for (image in Base64Images) {
                                    val imageData = hashMapOf("base64" to image)
                                    imagesCollectionRef.add(imageData)
                                }

                                val tradeBook = HashMap(myBook)
                                tradeBook["uid"] = userId
                                tradeBook["bookId"] = bookId

                                authdb.collection("TradeBooks").document(bookId).set(tradeBook)
                                    .addOnSuccessListener {
                                        startActivity(Intent(requireContext(), MainActivity::class.java))
                                        requireActivity().finish()
                                    }
                                    .addOnFailureListener {
                                        btsubmit.isEnabled = true
                                    }
                            }
                            .addOnFailureListener {
                                btsubmit.isEnabled = true
                            }

                    }
                }
            }
            else if(selectedItem==modes[2])
            {
                if(etbothprice.text.toString().isEmpty())
                {
                    ShowError(requireContext()).showError("PRICE IS MISSING.")
                }
                else if(etbothbook.text.toString().isEmpty())
                {
                    ShowError(requireContext()).showError("WITH BOOK IS MISSING.")
                }
                else
                {
                    btsubmit.isEnabled = false
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val myBook = hashMapOf(
                            "Title" to Title,
                            "Description" to Description,
                            "Condition" to Condition,
                            "Publisher" to Publisher,
                            "Author" to Author,
                            "Edition" to Edition,
                            "ISBN" to ISBN,
                            "Deal Mode" to "Both",
                            "Sell Price" to etbothprice.text.toString(),
                            "Exchange Book" to etbothbook.text.toString()
                        )
                        authdb.collection("users").document(userId).collection("My Books").add(myBook)
                            .addOnSuccessListener { documentReference ->
                                val bookId = documentReference.id

                                val imagesCollectionRef = authdb.collection("Base64Images").document(bookId).collection("Images")
                                for (image in Base64Images) {
                                    val imageData = hashMapOf("base64" to image)
                                    imagesCollectionRef.add(imageData)
                                }

                                val tradeBook = HashMap(myBook)
                                tradeBook["uid"] = userId
                                tradeBook["bookId"] = bookId

                                authdb.collection("TradeBooks").document(bookId).set(tradeBook)
                                    .addOnSuccessListener {
                                        startActivity(Intent(requireContext(), MainActivity::class.java))
                                        requireActivity().finish()
                                    }
                                    .addOnFailureListener {
                                        btsubmit.isEnabled = true
                                    }
                            }
                            .addOnFailureListener {
                                btsubmit.isEnabled = true
                            }

                    }
                }
            }
        })
    }
}