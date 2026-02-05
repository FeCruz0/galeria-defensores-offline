package com.galeria.defensores.models



enum class MessageType {
    TEXT, ROLL, SYSTEM, IMAGE
}

data class ChatMessage(
    val id: String = "",
    val tableId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String? = null,
    val imageUrl: String? = null,
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val rollResult: RollResult? = null,
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToContent: String? = null,
    val replyToType: MessageType? = null,
    val isEdited: Boolean = false
)
