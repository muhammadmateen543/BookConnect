package com.mateen.bookconnect

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore
    private lateinit var listNotifications: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = inflater.inflate(R.layout.fragment_notification, container, false)
        auth= FirebaseAuth.getInstance()
        authdb= FirebaseFirestore.getInstance()
        listNotifications=view.findViewById(R.id.listNotifications)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val notifilist = mutableListOf<notifi>()
        var adapter=CustomAdapterForNotification(requireContext(), notifilist)
        listNotifications.adapter=adapter
        authdb.collection("users")
            .document(auth.currentUser?.uid.toString())
            .collection("Notification")
            .document("notificationsList")
            .get()
            .addOnSuccessListener { data ->
                val nlist = data.get("notifications") as? List<String>
                if (nlist != null) {
                    for (text in nlist) {
                        val notif = notifi(text)
                        notifilist.add(notif)
                    }
                    notifilist.reverse()
                    adapter.notifyDataSetChanged()
                }
            }
    }
}