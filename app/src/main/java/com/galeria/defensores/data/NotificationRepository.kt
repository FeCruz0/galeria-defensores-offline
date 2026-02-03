package com.galeria.defensores.data

import com.galeria.defensores.models.Notification
import com.galeria.defensores.models.NotificationStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object NotificationRepository {
    // Offline: No notifications
    
    suspend fun sendNotification(notification: Notification) {
        // No-op
    }

    suspend fun getNotificationsForUser(userId: String): List<Notification> {
        return emptyList()
    }
    
    suspend fun hasPendingRequest(userId: String, tableId: String): Boolean {
        return false
    }

    suspend fun updateStatus(notificationId: String, status: NotificationStatus) {
        // No-op
    }

    suspend fun deleteNotificationsForTable(tableId: String) {
        // No-op
    }

    fun observeNotificationsCount(userId: String): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flow {
        emit(0)
    }
}
