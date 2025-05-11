package com.mateen.bookconnect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Spinner

class SellFragment2 : Fragment() {

    //Will receive from SellFragment1
    private lateinit var Base64Images: ArrayList<String>
    private lateinit var Title: String
    private lateinit var Description: String

    private lateinit var btback: ImageView
    private lateinit var condition: Spinner
    private lateinit var publisher: EditText
    private lateinit var author: EditText
    private lateinit var edition: EditText
    private lateinit var isbn: EditText
    private lateinit var btsubmit: RelativeLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_sell2, container, false)
        btback = view.findViewById(R.id.btback)
        condition = view.findViewById(R.id.condition)
        publisher = view.findViewById(R.id.publisher)
        author = view.findViewById(R.id.author)
        edition = view.findViewById(R.id.edition)
        isbn = view.findViewById(R.id.isbn)
        btsubmit=view.findViewById(R.id.btsubmit)
        Base64Images = arguments?.getStringArrayList("Base64Images") ?: ArrayList()
        Title=arguments?.getString("Title").toString()
        Description=arguments?.getString("Description").toString()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btback.setOnClickListener({
            parentFragmentManager.popBackStack()
        })

        //For Spinner (Condition)
        var items = listOf("Old", "New")
        var arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        condition.adapter = arrayAdapter
        condition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?){}
        }
        btsubmit.setOnClickListener({
            if(author.text.toString().isEmpty())
            {
                ShowError(requireContext()).showError("AUTHOR IS MISSING.")
            }
            else if(isbn.text.toString().isEmpty())
            {
                ShowError(requireContext()).showError("ISBN IS MISSING.")
            }
            else
            {
                var bundle=Bundle()
                bundle.putString("Condition", condition.selectedItem.toString())
                bundle.putString("Publisher", publisher.text.toString())
                bundle.putString("Author", author.text.toString())
                bundle.putString("Edition", edition.text.toString())
                bundle.putString("ISBN", isbn.text.toString())
                bundle.putString("Title", Title)
                bundle.putString("Description", Description)
                bundle.putStringArrayList("Base64Images",Base64Images)
                var sellFragment3=SellFragment3()
                sellFragment3.arguments=bundle
                parentFragmentManager.beginTransaction().replace(R.id.fragmetSell2, sellFragment3).addToBackStack(null).commit()
            }
        })
    }
}