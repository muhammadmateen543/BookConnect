package com.mateen.bookconnect

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Editable
import android.util.Base64
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration

class ChatFragment() : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore
    private lateinit var editTextSearch: EditText
    private lateinit var listViewChats: ListView

    private val chats = mutableListOf<Chat>()

    private var chatsListener: ListenerRegistration? = null

    private lateinit var adapter: CustomAdapterForChat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        auth = FirebaseAuth.getInstance()
        authdb = FirebaseFirestore.getInstance()
        editTextSearch = view.findViewById(R.id.et_search_chat)
        listViewChats = view.findViewById(R.id.lv_chat)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CustomAdapterForChat(requireContext(), requireActivity().supportFragmentManager, chats)
        listViewChats.adapter = adapter

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            Log.e("ChatFragment", "No authenticated user found")
            return
        }

        Log.d("ChatFragment", "Starting to load chats for user: $currentUserId")
        // Set up real-time listener for chats
        displayAllChats { updatedChats ->
            Log.d("ChatFragment", "onChatsLoaded received ${updatedChats.size} chats")
            adapter.updateChats(updatedChats)
            listViewChats.invalidateViews()
            Log.d("ChatFragment", "Updated ListView with ${updatedChats.size} chats")
        }

        editTextSearch.addTextChangedListener { editable: Editable? ->
            val query = editable.toString().trim()
            val filteredChats = chats.filter { chat ->
                chat.otherUserName.contains(query, ignoreCase = true)
            }
            val adapter = CustomAdapterForChat(
                requireContext(),
                requireActivity().supportFragmentManager,
                filteredChats.toMutableList()
            )
            listViewChats.adapter = adapter
        }
    }

    override fun onResume() {
        super.onResume()
        displayAllChats { updatedChats ->
            Log.d("ChatFragment OnResume", "onChatsLoaded received ${updatedChats.size} chats")
            adapter.updateChats(updatedChats)
            adapter.notifyDataSetChanged()
            listViewChats.invalidateViews()
            Log.d("ChatFragment OnResume", "Updated ListView with ${updatedChats.size} chats")
        }
    }

    private fun displayAllChats(onChatsLoaded: (List<Chat>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        chatsListener = authdb.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Failed to load chats: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ChatFragment", "Snapshot listener error: ${e.message}", e)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d("ChatFragment", "No chats found for user: $currentUserId")
                    lifecycleScope.launch(Dispatchers.Main) {
                        onChatsLoaded(emptyList())
                    }
                    return@addSnapshotListener
                }

                // Ignore local writes to prevent duplicate updates
                if (snapshot.metadata.hasPendingWrites()) {
                    Log.d("ChatFragment", "Ignoring snapshot with pending writes")
                    return@addSnapshotListener
                }

                Log.d("ChatFragment", "Received snapshot with ${snapshot.documents.size} chat documents")
                lifecycleScope.launch(Dispatchers.IO) {
                    val tempChats = mutableListOf<Chat>()
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        Log.d("ChatFragment", "Processing chat document: ${doc.id}")
                        val participants = data["participants"] as? List<String>
                        if (participants == null) {
                            Log.w("ChatFragment", "Invalid participants for chat: ${doc.id}")
                            continue
                        }
                        if (currentUserId in participants
                        ) {
                            val otherUid = participants.firstOrNull { it != currentUserId }
                            if (otherUid == null) {
                                Log.w("ChatFragment", "No other participant found for chat: ${doc.id}")
                                continue
                            }

                            // Fetch username
                            val userSnapshot = try {
                                authdb.collection("users")
                                    .document(otherUid)
                                    .get()
                                    .await()
                            } catch (e: Exception) {
                                Log.e("ChatFragment", "Failed to fetch user data for $otherUid: ${e.message}")
                                null
                            }
                            val otherUserName = userSnapshot?.getString("Full Name") ?: "Unknown User"

                            // Fetch profile picture
                            val picSnapshot = try {
                                authdb.collection("Base64Images")
                                    .document(otherUid)
                                    .collection("ProfilePicture")
                                    .get()
                                    .await()
                            } catch (e: Exception) {
                                Log.e("ChatFragment", "Failed to fetch profile picture for $otherUid: ${e.message}")
                                null
                            }
                            val base64Image = picSnapshot?.documents?.firstOrNull()?.getString("base64")
                            val profileImage = base64ToBitmap(base64Image.toString())

                            val chat = Chat(
                                chatId = data["chatId"]?.toString() ?: "",
                                participants = participants,
                                lastMessage = data["lastMessage"]?.toString() ?: "",
                                lastMessageTimestamp = data["lastMessageTimestamp"]?.toString()?.toLongOrNull() ?: 0L,
                                otherUserName = otherUserName,
                                otherUserProfileImage = profileImage
                            )
                            tempChats.add(chat)
                            Log.d("ChatFragment", "Added chat: ${chat.chatId} with user: $otherUserName")
                        }
                    }

                    Log.d("ChatFragment", "Prepared ${tempChats.size} chats for update")
                    launch(Dispatchers.Main) {
                        Log.d("ChatFragment", "Updating UI with ${tempChats.size} chats")
                        onChatsLoaded(tempChats)
                        chats.clear()
                        chats.addAll(tempChats)
                        Log.d("ChatFragment", "Updated chats list with ${chats.size} chats")
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatsListener?.remove() // Clean up listener
        Log.d("ChatFragment", "Removed snapshot listener")
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            Log.e("ChatFragment", "Failed to decode base64 image: ${e.message}")
            null
        }
    }
}