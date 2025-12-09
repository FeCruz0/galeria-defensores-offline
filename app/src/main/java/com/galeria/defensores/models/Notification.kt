package com.galeria.defensores.models

import java.util.UUID

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

enum class NotificationType {
    JOIN_REQUEST
}

enum class NotificationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
