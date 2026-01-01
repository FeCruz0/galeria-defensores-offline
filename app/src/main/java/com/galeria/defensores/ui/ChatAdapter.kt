package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.ChatMessage
import com.galeria.defensores.models.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper to bind reply
fun bindReplyView(itemView: View, message: ChatMessage, onReplyClick: (String) -> Unit) {
    val replyLayout = itemView.findViewById<View>(R.id.layout_reply_preview) ?: return
    
    if (message.replyToMessageId != null) {
        replyLayout.visibility = View.VISIBLE
        val nameText = replyLayout.findViewById<TextView>(R.id.text_reply_sender_name)
        val contentText = replyLayout.findViewById<TextView>(R.id.text_reply_preview_content)
        
        nameText.text = "Respondendo a ${message.replyToSenderName ?: "Alguém"}"
        
        val preview = when (message.replyToType) {
            MessageType.IMAGE -> "[Imagem]"
            MessageType.ROLL -> "[Rologem]"
            else -> message.replyToContent ?: ""
        }
        
        // Apply formatting to the preview as well
        if (message.replyToType == MessageType.TEXT || message.replyToType == null) {
            contentText.text = com.galeria.defensores.utils.TextFormatter.format(preview)
        } else {
            contentText.text = preview
        }
        
        replyLayout.setOnClickListener {
             message.replyToMessageId?.let { id -> onReplyClick(id) }
        }
    } else {
        replyLayout.visibility = View.GONE
    }
}

class ChatAdapter(
    private val onImageClick: (String) -> Unit,
    private val onMessageLongClick: (ChatMessage, View) -> Unit,
    private val onSenderClick: (ChatMessage) -> Unit,
    private val onReplyClick: (String) -> Unit
) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_ROLL = 1
        const val TYPE_IMAGE = 2
        const val TYPE_DATE = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ChatListItem.DateSeparatorItem -> TYPE_DATE
            is ChatListItem.MessageItem -> {
                when (item.message.type) {
                    MessageType.ROLL -> TYPE_ROLL
                    MessageType.IMAGE -> TYPE_IMAGE
                    else -> TYPE_TEXT
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE -> {
                val view = inflater.inflate(R.layout.item_date_separator, parent, false)
                DateSeparatorViewHolder(view)
            }
            TYPE_ROLL -> {
                val view = inflater.inflate(R.layout.message_item_roll, parent, false)
                RollMessageViewHolder(view, onSenderClick, onReplyClick)
            }
            TYPE_IMAGE -> {
                val view = inflater.inflate(R.layout.message_item_image, parent, false)
                ImageMessageViewHolder(view, onImageClick, onMessageLongClick, onSenderClick, onReplyClick)
            }
            else -> {
                val view = inflater.inflate(R.layout.message_item_text, parent, false)
                TextMessageViewHolder(view, onMessageLongClick, onSenderClick, onReplyClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ChatListItem.DateSeparatorItem -> (holder as DateSeparatorViewHolder).bind(item)
            is ChatListItem.MessageItem -> {
                val message = item.message
                when (holder) {
                    is TextMessageViewHolder -> holder.bind(message)
                    is RollMessageViewHolder -> holder.bind(message)
                    is ImageMessageViewHolder -> holder.bind(message)
                }
            }
        }
    }

    class DateSeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.text_date_separator)

        fun bind(item: ChatListItem.DateSeparatorItem) {
            val date = Date(item.date)
            // Helpers for logic "Today", "Yesterday"
            val now = System.currentTimeMillis()
            val diff = now - item.date
            val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
            
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            val year = calendar.get(java.util.Calendar.YEAR)
            
            val nowCalendar = java.util.Calendar.getInstance()
            val nowDay = nowCalendar.get(java.util.Calendar.DAY_OF_YEAR)
            val nowYear = nowCalendar.get(java.util.Calendar.YEAR)

            val text = when {
                 year == nowYear && dayOfYear == nowDay -> "Hoje"
                 year == nowYear && dayOfYear == nowDay - 1 -> "Ontem"
                 else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
            }
            dateText.text = text
        }
    }

    // ... (Keep existing ViewHolders: TextMessageViewHolder, ImageMessageViewHolder, RollMessageViewHolder)
    // IMPORTANT: I need to ensure I don't delete them. 
    // The replace_file_content replaces a block. I need to include them or be very careful.
    // Since I'm replacing the entire class body basically, I SHOULD include them.

    class TextMessageViewHolder(
        itemView: View,
        private val onLongClick: (ChatMessage, View) -> Unit,
        private val onSenderClick: (ChatMessage) -> Unit,
        private val onReplyClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_sender_name)
        private val contentText: TextView = itemView.findViewById(R.id.text_message_content)
        private val timeText: TextView = itemView.findViewById(R.id.text_message_time)

        fun bind(message: ChatMessage) {
            nameText.text = message.senderName
            nameText.setOnClickListener { onSenderClick(message) }
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeText.text = timeFormat.format(Date(message.timestamp))

            val formattedContent = com.galeria.defensores.utils.TextFormatter.format(message.content)

            if (message.isEdited) {
                val spannable = android.text.SpannableStringBuilder(formattedContent)
                spannable.append(" (editado)")
                val start = formattedContent.length
                val end = spannable.length
                
                spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(android.text.style.RelativeSizeSpan(0.8f), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.GRAY), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                contentText.text = spannable
            } else {
                contentText.text = formattedContent
            }
            
            bindReplyView(itemView, message, onReplyClick)

            itemView.setOnLongClickListener {
                onLongClick(message, itemView)
                true
            }
        }
    }

    class ImageMessageViewHolder(
        itemView: View,
        private val onImageClick: (String) -> Unit,
        private val onLongClick: (ChatMessage, View) -> Unit,
        private val onSenderClick: (ChatMessage) -> Unit,
        private val onReplyClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_sender_name)
        private val imageView: android.widget.ImageView = itemView.findViewById(R.id.image_message_content)
        private val timeText: TextView = itemView.findViewById(R.id.text_message_time)

        fun bind(message: ChatMessage) {
            nameText.text = message.senderName
            nameText.setOnClickListener { onSenderClick(message) }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeText.text = timeFormat.format(Date(message.timestamp))
            
            itemView.setOnLongClickListener { onLongClick(message, itemView); true }
            
            val base64 = message.imageUrl
            if (!base64.isNullOrEmpty()) {
                try {
                    val cleanBase64 = if (base64.contains(",")) base64.split(",")[1] else base64
                    val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    imageView.setOnClickListener { onImageClick(base64) }
                } catch (e: Exception) { imageView.visibility = View.GONE }
            } else { imageView.visibility = View.GONE }
            
            bindReplyView(itemView, message, onReplyClick)
        }
    }

    class RollMessageViewHolder(
        itemView: View,
        private val onSenderClick: (ChatMessage) -> Unit,
        private val onReplyClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_sender_name)
        private val titleText: TextView = itemView.findViewById(R.id.text_roll_title)
        private val totalText: TextView = itemView.findViewById(R.id.text_roll_total)
        private val detailText: TextView = itemView.findViewById(R.id.text_roll_detail)
        private val avatarImage: android.widget.ImageView = itemView.findViewById(R.id.image_message_avatar)
        private val timeText: TextView = itemView.findViewById(R.id.text_message_time)

        fun bind(message: ChatMessage) {
            nameText.text = message.senderName
            nameText.setOnClickListener { onSenderClick(message) }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeText.text = timeFormat.format(Date(message.timestamp))
            
            val avatarBase64 = message.senderAvatar
            if (!avatarBase64.isNullOrEmpty()) {
                try {
                    val cleanBase64 = if (avatarBase64.contains(",")) avatarBase64.split(",")[1] else avatarBase64
                    val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    avatarImage.setImageBitmap(bitmap)
                    avatarImage.visibility = View.VISIBLE
                } catch (e: Exception) { avatarImage.visibility = View.GONE }
            } else { avatarImage.visibility = View.GONE }

            val roll = message.rollResult
            if (roll != null) {
                titleText.text = "ROLOGEM: ${roll.name}"
                totalText.text = roll.total.toString()
                val detail = if (roll.bonus != 0) "${roll.attributeUsed}(${roll.attributeValue}) + ${roll.die} + ${roll.bonus}" else "${roll.attributeUsed}(${roll.attributeValue}) + ${roll.die}"
                detailText.text = detail
                detailText.text = detail
            } else { titleText.text = "ROLOGEM INVÁLIDA" }
            
            bindReplyView(itemView, message, onReplyClick)
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            return oldItem == newItem
        }
    }
}
