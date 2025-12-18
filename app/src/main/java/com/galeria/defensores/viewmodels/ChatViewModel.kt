package com.galeria.defensores.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galeria.defensores.data.ChatRepository
import com.galeria.defensores.models.ChatMessage
import com.galeria.defensores.models.RollResult
import com.galeria.defensores.models.MessageType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _currentTableId = MutableLiveData<String>()
    val currentTableId: LiveData<String> = _currentTableId

    private val _isMember = MutableLiveData<Boolean>()
    val isMember: LiveData<Boolean> = _isMember

    private val _joinRequestStatus = MutableLiveData<String>() // "PENDING", "NONE", "VISITOR"
    val joinRequestStatus: LiveData<String> = _joinRequestStatus

    private val _currentUser = FirebaseAuth.getInstance().currentUser
    private var currentTable: com.galeria.defensores.models.Table? = null

    fun setTableId(tableId: String) {
        _currentTableId.value = tableId
        loadMessages(tableId)
        checkMembership(tableId)
    }

    private fun checkMembership(tableId: String) {
         viewModelScope.launch {
             val user = _currentUser ?: return@launch
             val table = com.galeria.defensores.data.TableRepository.getTable(tableId)
             currentTable = table
             
             if (table != null) {
                 val isMaster = table.masterId == user.uid
                 val isPlayer = table.players.contains(user.uid)
                 val member = isMaster || isPlayer
                 _isMember.value = member

                 if (!member) {
                     // Check if there is a pending request
                     val hasPending = com.galeria.defensores.data.NotificationRepository.hasPendingRequest(user.uid, tableId)
                     if (hasPending) {
                         _joinRequestStatus.value = "PENDING"
                     } else {
                         _joinRequestStatus.value = "NONE"
                     }
                 }
             }
         }
    }

    fun sendJoinRequest() {
        val table = currentTable ?: return
        val user = _currentUser ?: return
        
        viewModelScope.launch {
            val notification = com.galeria.defensores.models.Notification(
                fromUserId = user.uid,
                fromUserName = user.displayName ?: user.email ?: "Unknown",
                toUserId = table.masterId,
                tableId = table.id,
                tableName = table.name,
                status = com.galeria.defensores.models.NotificationStatus.PENDING
            )
            com.galeria.defensores.data.NotificationRepository.sendNotification(notification)
            _joinRequestStatus.value = "PENDING"
        }
    }

    private fun loadMessages(tableId: String) {
        viewModelScope.launch {
            repository.getMessagesFlow(tableId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(content: String) {
        val tableId = _currentTableId.value ?: return
        val user = _currentUser ?: return
        
        // Need to fetch user name properly from somewhere, for now using display name or email
        val senderName = user.displayName ?: user.email ?: "Unknown"

        val message = ChatMessage(
            tableId = tableId,
            senderId = user.uid,
            senderName = senderName,
            content = content,
            type = MessageType.TEXT
        )

        viewModelScope.launch {
            repository.sendMessage(tableId, message)
        }
    }

    fun sendRollResult(rollResult: RollResult) {
         val tableId = _currentTableId.value ?: return
         // rollResult already has character info usually, but we ensure basic fields
         val user = _currentUser ?: return

         val message = ChatMessage(
            tableId = tableId,
            senderId = user.uid,
            senderName = rollResult.name, // Use character name for rolls usually
            content = "Rolou ${rollResult.total}",
            type = MessageType.ROLL,
            rollResult = rollResult
        )

        viewModelScope.launch {
            repository.sendMessage(tableId, message)
        }
    }
}
