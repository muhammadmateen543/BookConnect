package com.mateen.bookconnect

import android.graphics.Bitmap

data class Book (var bookId: String,
                 var images: ArrayList<String>,
                 var name: String,
                 var description: String,
                 var condition: String,
                 var publisher: String?,
                 var author: String,
                 var edition: String?,
                 var isbn: String?,
                 var dealMode: String?,
                 var price: String?,
                 var exchangeBook: String?)

data class User(
    val userId: String = "",
    val fullName: String = "",
    val email: String = ""
)

data class Chat(
    val chatId: String = "",
    val participants: List<String>,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0,
    val otherUserName: String = "", // New field
    val otherUserProfileImage: Bitmap? = null // New field
)

data class Message(
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0
)

data class bidclass(var bidderId: String,
                    var Title: String,
                    var bidderName: String,
                    var bidAmount: String?,
                    var bidBook: String?)

data class notifi(var text: String)

data class offer(
    var ownerId: String,
    val bookId: String,
    val title: String,
    val dealMode: String,
    val sellPrice: String? = null,
    val exchangeBook: String? = null
)
