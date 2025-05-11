package com.mateen.bookconnect

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

class SellFragment : Fragment() {
    private lateinit var btstart: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_sell, container, false)
        btstart=view.findViewById(R.id.btstart)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btstart.setOnClickListener{
            //startActivity(Intent(requireContext(), SellBook::class.java))
            parentFragmentManager.beginTransaction().replace(R.id.fragmetSell, SellFragment1()).addToBackStack(null).commit()
        }
    }
}