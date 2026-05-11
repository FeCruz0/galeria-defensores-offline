package com.galeria.defensores.models

import java.util.UUID

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationType = NotificationType.JOIN_REQUEST,
    val fromUserId: String = "",
    val fromUserName: String = "",
    val toUserId: String = "", // Master ID
    val tableId: String = "",
    val tableName: String = "",
    val status: NotificationStatus = NotificationStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class NotificationType {
    JOIN_REQUEST
}

@Serializable
enum class NotificationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
