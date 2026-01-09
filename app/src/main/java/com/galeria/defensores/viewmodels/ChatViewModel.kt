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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private var appUser: com.galeria.defensores.models.User? = null

    init {
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            appUser = com.galeria.defensores.data.UserRepository.getUser(uid)
        }
    }

    private val _messages = MutableLiveData<List<com.galeria.defensores.ui.ChatListItem>>()
    val messages: LiveData<List<com.galeria.defensores.ui.ChatListItem>> = _messages

    // ... (keep intermediate fields same)

    private fun updateCombinedList() {
        val combined = _historyMessages + _realtimeMessages
        // Deduplicate just in case overlap
        val distinctMessages = combined.distinctBy { it.id }

        // Transform to ChatListItem with Date Separators
        val listItems = mutableListOf<com.galeria.defensores.ui.ChatListItem>()
        var lastDate: String? = null
        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())

        distinctMessages.forEach { message ->
            val messageDate = Date(message.timestamp)
            val dateKey = dateFormat.format(messageDate)

            if (dateKey != lastDate) {
                listItems.add(com.galeria.defensores.ui.ChatListItem.DateSeparatorItem(message.timestamp))
                lastDate = dateKey
            }
            listItems.add(com.galeria.defensores.ui.ChatListItem.MessageItem(message))
        }

        _messages.value = listItems
    }

    private val _currentTableId = MutableLiveData<String>()
    val currentTableId: LiveData<String> = _currentTableId

    private val _isMember = MutableLiveData<Boolean>()
    val isMember: LiveData<Boolean> = _isMember

    private val _joinRequestStatus = MutableLiveData<String>() // "NONE", "PENDING", "ACCEPTED"
    val joinRequestStatus: LiveData<String> = _joinRequestStatus
    
    // Reply
    private val _replyingToMessage = MutableLiveData<ChatMessage?>(null)
    val replyingToMessage: LiveData<ChatMessage?> = _replyingToMessage

    private val _currentUser = FirebaseAuth.getInstance().currentUser
    val currentUser get() = _currentUser
    private var currentTable: com.galeria.defensores.models.Table? = null

    fun setTableId(tableId: String) {
        _currentTableId.value = tableId
        loadMessages(tableId)
        checkMembership(tableId)
        listenToTyping(tableId)
        
        // Trigger auto-deletion (Fire and forget)
        viewModelScope.launch {
            repository.cleanupMessages(tableId)
        }
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

    private val _realtimeMessages = mutableListOf<ChatMessage>()
    private val _historyMessages = mutableListOf<ChatMessage>()
    private val _combinedMessages = androidx.lifecycle.MediatorLiveData<List<ChatMessage>>()

    // Replace original _messages with a property that observes combining
    // Actually, simpler: just update _messages everytime either list changes.
    // _messages is already MutableLiveData.
    
    // We need to change loadMessages to ONLY populate _realtimeMessages initially?
    // The current flow limits to 50.
    
    private fun loadMessages(tableId: String) {
        viewModelScope.launch {
            repository.getMessagesFlow(tableId)
                .catch { e -> 
                    e.printStackTrace() 
                    // Optional: handle error ui
                }
                .collect { msgs ->
                    // This flow returns the LAST 50 messages.
                    // We treat these as "realtime/latest".
                    _realtimeMessages.clear()
                    _realtimeMessages.addAll(msgs)
                    updateCombinedList()
                }
        }
    }

    private val _paginationStatus = MutableLiveData<String>() // "LOADING", "SUCCESS", "EMPTY"
    val paginationStatus: LiveData<String> = _paginationStatus

    fun loadOlderMessages() {
        val tableId = _currentTableId.value ?: return
        val oldestCurrent = _historyMessages.firstOrNull() ?: _realtimeMessages.firstOrNull() ?: return
        
        _paginationStatus.value = "LOADING"
        
        viewModelScope.launch {
            try {
                val older = repository.getOlderMessages(tableId, oldestCurrent.timestamp)
                if (older.isNotEmpty()) {
                    val sortedOlder = older.sortedBy { it.timestamp }
                    _historyMessages.addAll(0, sortedOlder)
                    updateCombinedList()
                    _paginationStatus.value = "SUCCESS"
                } else {
                    _paginationStatus.value = "EMPTY"
                }
            } catch (e: Exception) {
                 _paginationStatus.value = "ERROR"
                 e.printStackTrace()
            }
        }
    }

    // Logic moved to updateCombinedList with transformation... (omitted comment for brevity in replacement if possible, but keeping context to find match if I can)
    // Actually I'll just target loadMessages and listenToTyping separately or together if close.
    // They are far apart. I will use multi_replace.


    // Logic moved to updateCombinedList with transformation
    // private fun updateCombinedList() { ... } implementation is now handled above 
    // or rather, I should have replaced the function body in the previous call or this one.
    // Let's replace the OLD function body with the NEW one here to be safe and correct.
    
    // ERROR in thought process: The previous call replaced the PROPERTY definition. 
    // Now I need to replace the FUNCTION definition. 
    // The previous call inserted the NEW function body? No, looking at my prompt I inserted logic BUT it was inside the property definition area? 
    // Ah, I see. I replaced the property definition with the property AND the function? That would be messy. 
    // Let's look at what I actually sent:
    // ReplacementContent: _messages = ... private fun updateCombinedList() { ... }
    // This effectively inserted the function at the top of the file! 
    // And left the old function at the bottom! 
    // This causes DUPLICATE FUNCTION ERROR.
    
    // CORRECTION STRATEGY: 
    // 1. Remove the old function definition from the bottom.
    // 2. Ideally I should have split this. 
    // Since I already sent the previous tool call, code likely has the function defined twice now (once near top, once near bottom).
    
    // Wait, the previous tool call target content was just the property definition.
    // So I replaced:
    // val messages: LiveData<List<ChatMessage>> = _messages
    // with:
    // ... updateCombinedList() { ... }
    
    // So yes, I have two updateCombinedList functions now. One I just added at the top, and one existing at line 132.
    // I MUST remove the one at line 132.
    
    // ALSO, checking imports for ChatListItem. I used fully qualified name in the snippet so it should be fine.
    
    /* Intentionally Empty replacement to delete the old function */

    fun sendMessage(content: String) {
        val tableId = _currentTableId.value ?: return
        val user = _currentUser ?: return

        val senderName = appUser?.name ?: user.displayName ?: user.email ?: "Unknown"

        // Check if replying
        val replyTarget = _replyingToMessage.value
        
        val message = ChatMessage(
            tableId = tableId,
            senderId = user.uid,
            senderName = senderName,
            content = content,
            type = MessageType.TEXT,
            replyToMessageId = replyTarget?.id,
            replyToSenderName = replyTarget?.senderName,
            replyToContent = replyTarget?.content, // Or a summary if it's special type
            replyToType = replyTarget?.type
        )

        viewModelScope.launch {
            repository.sendMessage(tableId, message)
            _replyingToMessage.value = null // Clear reply after sending
        }
    }

    fun enterReplyMode(message: ChatMessage) {
        _replyingToMessage.value = message
    }

    fun cancelReplyMode() {
        _replyingToMessage.value = null
    }

    fun processAndSendImage(context: android.content.Context, uri: android.net.Uri) {
        val tableId = _currentTableId.value ?: return
        val user = _currentUser ?: return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
             val compressedBytes = com.galeria.defensores.utils.ImageUtils.compressImage(context, uri, maxDimension = 600)
             
             if (compressedBytes != null) {
                 val base64String = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.DEFAULT)
                 val dataUri = "data:image/jpeg;base64,$base64String"
                 
                 val senderName = appUser?.name ?: user.displayName ?: user.email ?: "Unknown"
                 
                 val replyTarget = _replyingToMessage.value

                 val message = ChatMessage(
                     tableId = tableId,
                     senderId = user.uid,
                     senderName = senderName,
                     content = "Enviou uma imagem",
                     type = MessageType.IMAGE,
                     imageUrl = dataUri,
                     replyToMessageId = replyTarget?.id,
                     replyToSenderName = replyTarget?.senderName,
                     replyToContent = replyTarget?.content,
                     replyToType = replyTarget?.type
                 )

                 repository.sendMessage(tableId, message)
                 _replyingToMessage.postValue(null) // Use postValue from background thread
             }
        }
    }

    fun sendRollResult(rollResult: RollResult, avatarUrl: String? = null) {
         val tableId = _currentTableId.value ?: return
         // rollResult already has character info usually, but we ensure basic fields
         val user = _currentUser ?: return

         val message = ChatMessage(
            tableId = tableId,
            senderId = user.uid,
            senderName = rollResult.name, // Use character name for rolls usually
            senderAvatar = avatarUrl,
            content = if (rollResult.details.isNotEmpty()) rollResult.details else "Rolou ${rollResult.total}",
            type = MessageType.ROLL,
            rollResult = rollResult
        )

        viewModelScope.launch {
            repository.sendMessage(tableId, message)
        }
    }



    fun editMessage(message: ChatMessage, newContent: String) {
        val user = _currentUser ?: return
        if (user.uid != message.senderId) return // Validation

        viewModelScope.launch {
            repository.editMessage(message.tableId, message.id, newContent)
        }
    }

    fun deleteMessage(message: ChatMessage) {
        val user = _currentUser ?: return
        if (user.uid != message.senderId) return // Validation

        viewModelScope.launch {
            repository.deleteMessage(message.tableId, message.id)
        }
    }

    private val _typingStatusText = MutableLiveData<String>()
    val typingStatusText: LiveData<String> = _typingStatusText

    private var isLocallyTyping = false
    private var typingTimeoutJob: kotlinx.coroutines.Job? = null

    fun onTypingInput(text: String) {
        if (text.isEmpty()) {
           typingTimeoutJob?.cancel()
           if (isLocallyTyping) {
               isLocallyTyping = false
               updateTypingStatus(false)
           }
           return
        }

        typingTimeoutJob?.cancel()
        if (!isLocallyTyping) {
            isLocallyTyping = true
            updateTypingStatus(true)
        }
        
        typingTimeoutJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // 3 seconds timeout
            if (isLocallyTyping) {
                isLocallyTyping = false
                updateTypingStatus(false)
            }
        }
    }

    private fun updateTypingStatus(typing: Boolean) {
        val tableId = _currentTableId.value ?: return
        val user = _currentUser ?: return
        val name = appUser?.name ?: user.displayName ?: user.email ?: "Alguém"
        viewModelScope.launch {
            repository.setTypingStatus(tableId, user.uid, name, typing)
        }
    }
    
    // Call this from setTableId or loadMessages
    private fun listenToTyping(tableId: String) {
         viewModelScope.launch {
            repository.getTypingUsersFlow(tableId)
                .catch { e -> 
                    e.printStackTrace() 
                }
                .collect { typingMap ->
                    val currentUser = _currentUser
                    val myUid = currentUser?.uid
                    
                    // Filter out self by UID
                    val others = typingMap.filterKeys { it != myUid }.values.toList()
                    
                    _typingStatusText.value = when {
                        others.isEmpty() -> ""
                        others.size == 1 -> "${others[0]} está digitando..."
                        others.size == 2 -> "${others[0]} e ${others[1]} estão digitando..."
                        else -> "Várias pessoas estão digitando..."
                    }
                }
        }
    }
}
