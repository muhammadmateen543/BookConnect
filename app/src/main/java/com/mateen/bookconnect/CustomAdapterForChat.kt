package com.mateen.bookconnect

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class CustomAdapterForChat(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private var mchat: MutableList<Chat>
) : BaseAdapter() {
    private lateinit var auth: FirebaseAuth

    override fun getCount(): Int = mchat.size

    override fun getItem(position: Int): Any = mchat[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.single_chat, parent, false)
        auth = FirebaseAuth.getInstance()

        val chatProfilePic: ImageView = view.findViewById(R.id.iv_chat)
        val textViewChatName: TextView = view.findViewById(R.id.tv_chat_username)
        val textViewLastMessage: TextView = view.findViewById(R.id.tv_chat_last)
        val textViewLastMessageTime: TextView = view.findViewById(R.id.tv_chat_time)

        val data = mchat[position]
        val currentUid = auth.currentUser?.uid
        val otherUid = data.participants.first { it != currentUid }
        val chatId = data.chatId

        // Use preloaded data
        chatProfilePic.setImageBitmap(data.otherUserProfileImage ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        textViewChatName.text = data.otherUserName.takeIf { it.isNotEmpty() } ?: "Unknown User"
        textViewLastMessage.text = data.lastMessage
        textViewLastMessageTime.text = formatTimestamp(data.lastMessageTimestamp)

        view.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable("profilePicture", data.otherUserProfileImage)
                putString("ChatName", data.otherUserName)
                putString("ChatId", chatId)
            }
            val fragment = ChatDetailFragment().apply { arguments = bundle }
            Toast.makeText(context, "Opening chat with ${data.otherUserName}", Toast.LENGTH_SHORT).show()
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    fun updateChats(newChats: List<Chat>) {
        Log.d("CustomAdapterForChat", "Updating adapter with ${newChats.size} chats")
        mchat.clear()
        mchat.addAll(newChats)
        notifyDataSetChanged()
        Log.d("CustomAdapterForChat", "Adapter updated, new count: ${mchat.size}")
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}