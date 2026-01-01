package com.galeria.defensores.ui

import com.galeria.defensores.models.ChatMessage

sealed class ChatListItem {
    data class MessageItem(val message: ChatMessage) : ChatListItem()
    data class DateSeparatorItem(val date: Long) : ChatListItem()
    
    // Helper to get ID for DiffUtil
    val id: String
        get() = when (this) {
            is MessageItem -> message.id
            is DateSeparatorItem -> "date_$date"
        }
}
