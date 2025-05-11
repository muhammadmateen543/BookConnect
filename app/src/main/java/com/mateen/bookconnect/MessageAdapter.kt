package com.mateen.bookconnect

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(var context: Context,
                     private val currentUserId: String,
                     var mMessage:MutableList<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewMessage: TextView = itemView.findViewById(R.id.text_message)
        val textViewTime: TextView = itemView.findViewById(R.id.time_message)
        val messageView: LinearLayout = itemView.findViewById(R.id.message_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.single_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = mMessage[position]
        holder.textViewMessage.text = message.content
        holder.textViewTime.text = formatTimestamp(message.timestamp)
        val isSent = message.senderId == currentUserId
        holder.messageView.setBackgroundResource(
            if (isSent) R.drawable.bg_message_sent else R.drawable.bg_message_received
        )
        val layoutParams = holder.messageView.layoutParams as LinearLayout.LayoutParams
        layoutParams.gravity = if (isSent) Gravity.END else Gravity.START
        holder.messageView.layoutParams = layoutParams
    }

    override fun getItemCount() = mMessage.size

    fun updateMessages(newMessages: List<Message>) {
        val diffCallback = MessageDiffCallback(mMessage, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        mMessage.clear()
        mMessage.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
        Log.d("MessageAdapter", "Updated with ${newMessages.size} messages")
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].timestamp == newList[newItemPosition].timestamp &&
                    oldList[oldItemPosition].senderId == newList[newItemPosition].senderId

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition] == newList[newItemPosition]
    }
}