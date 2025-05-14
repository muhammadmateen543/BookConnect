package com.mateen.bookconnect

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

class ChatFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authdb: FirebaseFirestore
    private lateinit var editTextSearch: EditText
    private lateinit var listViewChats: ListView
    private lateinit var progressBar: ProgressBar
    private val chats = mutableListOf<Chat>()
    private var chatsListener: ListenerRegistration? = null
    private lateinit var adapter: CustomAdapterForChat
    private var isLoading: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        auth = FirebaseAuth.getInstance()
        authdb = FirebaseFirestore.getInstance()
        editTextSearch = view.findViewById(R.id.et_search_chat)
        listViewChats = view.findViewById(R.id.lv_chat)
        progressBar = view.findViewById(R.id.pb_chat)
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
            showLoading(false)
            return
        }

        Log.d("ChatFragment", "Starting to load chats for user: $currentUserId")
        showLoading(true)
        displayAllChats { updatedChats ->
            if (isAdded && view != null) {
                Log.d("ChatFragment", "onChatsLoaded received ${updatedChats.size} chats")
                adapter.updateChats(updatedChats)
                adapter.notifyDataSetChanged()
                Log.d("ChatFragment", "Updated ListView with ${updatedChats.size} chats")
            }
            showLoading(false)
        }

        editTextSearch.addTextChangedListener { editable ->
            val query = editable.toString().trim()
            if (isAdded && view != null) {
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
    }

    override fun onResume() {
        super.onResume()
        showLoading(true)
        displayAllChats { updatedChats ->
            if (isAdded && view != null) {
                Log.d("ChatFragment OnResume", "onChatsLoaded received ${updatedChats.size} chats")
                adapter.updateChats(updatedChats)
                adapter.notifyDataSetChanged()
                Log.d("ChatFragment OnResume", "Updated ListView with ${updatedChats.size} chats")
            }
            showLoading(false)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        (requireActivity() as? MainActivity)?.onFragmentLoadingStateChanged(isLoading)
    }

    fun isLoading(): Boolean = isLoading

    private fun displayAllChats(onChatsLoaded: (List<Chat>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        chatsListener?.remove() // Remove existing listener to avoid duplicates
        chatsListener = authdb.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(requireContext(), "Failed to load chats: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ChatFragment", "Snapshot listener error: ${e.message}", e)
                    lifecycleScope.launch(Dispatchers.Main) {
                        onChatsLoaded(emptyList())
                        showLoading(false)
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.d("ChatFragment", "No chats found for user: $currentUserId")
                    lifecycleScope.launch(Dispatchers.Main) {
                        onChatsLoaded(emptyList())
                        showLoading(false)
                    }
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
                        if (currentUserId in participants) {
                            val otherUid = participants.firstOrNull { it != currentUserId }
                            if (otherUid == null) {
                                Log.w("ChatFragment", "No other participant found for chat: ${doc.id}")
                                continue
                            }

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
        chatsListener?.remove()
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