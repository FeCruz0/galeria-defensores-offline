package com.galeria.defensores.models

import com.google.firebase.firestore.PropertyName

enum class MessageType {
    TEXT, ROLL, SYSTEM
}

data class ChatMessage(
    val id: String = "",
    val tableId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val rollResult: RollResult? = null
)
