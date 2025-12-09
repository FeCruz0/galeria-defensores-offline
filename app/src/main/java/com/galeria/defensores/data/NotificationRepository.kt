package com.galeria.defensores.data

import com.galeria.defensores.models.Notification
import com.galeria.defensores.models.NotificationStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object NotificationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    suspend fun sendNotification(notification: Notification) {
        try {
            notificationsCollection.document(notification.id).set(notification).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getNotificationsForUser(userId: String): List<Notification> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            val list = snapshot.toObjects(Notification::class.java).sortedByDescending { it.timestamp }
            android.util.Log.d("NotificationDebug", "Fetched ${list.size} pending notifications for user $userId")
            list
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("NotificationDebug", "Error fetching notifications: ${e.message}")
            emptyList()
        }
    }
    
    // Check if there is already a pending request for this user and table
    suspend fun hasPendingRequest(userId: String, tableId: String): Boolean {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("tableId", tableId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateStatus(notificationId: String, status: NotificationStatus) {
        try {
            notificationsCollection.document(notificationId).update("status", status).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun observeNotificationsCount(userId: String): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Cancel the flow on error
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    trySend(snapshot.size())
                }
            }
        
        awaitClose { listener.remove() }
    }
}
