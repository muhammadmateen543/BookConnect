package com.mateen.bookconnect

import android.graphics.Bitmap
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatDetailFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore

    private lateinit var chatId: String
    private lateinit var chatName: String
    private lateinit var profilePicBitMap: Bitmap

    private lateinit var buttonBack: ImageButton
    private lateinit var profilePic: ImageView
    private lateinit var textViewChatName: TextView
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton

    private var messages = mutableListOf<Message>()
    private var currentUserId: String? = ""

    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            profilePicBitMap = it.getParcelable("profilePicture") ?: throw IllegalArgumentException("Profile picture is missing")
            chatName = it.getString("ChatName") ?: throw IllegalArgumentException("Chat name is missing")
            chatId = it.getString("ChatId") ?: throw IllegalArgumentException("Chat ID is missing")
        } ?: run {
            throw IllegalStateException("Arguments bundle is null")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_detail, container, false)
        auth = FirebaseAuth.getInstance()
        authdb = FirebaseFirestore.getInstance()

        buttonBack = view.findViewById(R.id.buttonBack)
        profilePic = view.findViewById(R.id.iv_profile_chat)
        textViewChatName = view.findViewById(R.id.textViewChatName)
        recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages)
        editTextMessage = view.findViewById(R.id.editTextMessage)
        buttonSend = view.findViewById(R.id.buttonSend)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profilePic.setImageBitmap(profilePicBitMap)
        textViewChatName.text = chatName

        currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            Log.e("ChatDetailFragment", "No authenticated user found")
            parentFragmentManager.popBackStack()
            return
        }

        val layoutManager = LinearLayoutManager(context)
        recyclerViewMessages.layoutManager = layoutManager
        val adapter = MessageAdapter(requireContext(), currentUserId!!, messages)
        recyclerViewMessages.adapter = adapter

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                recyclerViewMessages.scrollToPosition(messages.size - 1)
                Log.d("ChatDetailFragment", "Scrolled to position ${messages.size - 1} after inserting $itemCount items")
            }
        })

        Log.d("ChatDetailFragment", "Starting to load messages for chat: $chatId")
        loadAllMessages { updatedMessages ->
            Log.d("ChatDetailFragment", "onMessagesLoaded received ${updatedMessages.size} messages")
            adapter.updateMessages(updatedMessages)
            recyclerViewMessages.scrollToPosition(updatedMessages.size - 1)
            Log.d("ChatDetailFragment", "Updated RecyclerView with ${updatedMessages.size} messages")
        }

        buttonSend.setOnClickListener {
            val content = editTextMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                val newMessageTime = System.currentTimeMillis()
                val newMessage = Message(
                    senderId = currentUserId!!,
                    content = content,
                    timestamp = newMessageTime
                )
                val batch = authdb.batch()
                val messageRef = authdb.collection("chats").document(chatId).collection("messages").document()
                batch.set(messageRef, newMessage)
                batch.update(authdb.collection("chats").document(chatId), mapOf(
                    "lastMessage" to content,
                    "lastMessageTimestamp" to newMessageTime
                ))
                batch.commit()
                    .addOnSuccessListener {
                        editTextMessage.text.clear()
                        messages.add(newMessage)
                        recyclerViewMessages.adapter?.notifyItemInserted(messages.size - 1)
                        recyclerViewMessages.scrollToPosition(messages.size - 1)
                        Log.d("ChatDetailFragment", "Message sent and chat updated: $content")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("ChatDetailFragment", "Failed to send message or update chat: ${e.message}", e)
                    }
            } else {
                Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadAllMessages(onMessagesLoaded: (List<Message>) -> Unit) {
        messagesListener = authdb.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Failed to load messages: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ChatDetailFragment", "Snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d("ChatDetailFragment", "No messages found for chat: $chatId")
                    lifecycleScope.launch(Dispatchers.Main) {
                        onMessagesLoaded(emptyList())
                    }
                    return@addSnapshotListener
                }

                if (snapshot.metadata.hasPendingWrites()) {
                    Log.d("ChatDetailFragment", "Ignoring snapshot with pending writes")
                    return@addSnapshotListener
                }

                Log.d("ChatDetailFragment", "Received snapshot with ${snapshot.documents.size} message documents")
                lifecycleScope.launch(Dispatchers.IO) {
                    val tempMessages = mutableListOf<Message>()
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        Log.d("ChatDetailFragment", "Processing message document: ${doc.id}")
                        val content = data["content"]?.toString() ?: ""
                        val senderId = data["senderId"]?.toString() ?: ""
                        val timestamp = data["timestamp"]?.toString()?.toLongOrNull() ?: 0L
                        val message = Message(
                            content = content,
                            senderId = senderId,
                            timestamp = timestamp
                        )
                        tempMessages.add(message)
                        Log.d("ChatDetailFragment", "Added message: $content at $timestamp")
                    }

                    Log.d("ChatDetailFragment", "Prepared ${tempMessages.size} messages for update")
                    lifecycleScope.launch(Dispatchers.Main) {
                        Log.d("ChatDetailFragment", "Updating UI with ${tempMessages.size} messages")
                        onMessagesLoaded(tempMessages)
                        messages.clear()
                        messages.addAll(tempMessages)
                        Log.d("ChatDetailFragment", "Updated messages list with ${messages.size} messages")
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messagesListener?.remove()
        Log.d("ChatDetailFragment", "Removed snapshot listener")
    }
}