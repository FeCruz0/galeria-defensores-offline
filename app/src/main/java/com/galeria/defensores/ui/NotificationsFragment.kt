package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.NotificationRepository
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.models.Notification
import com.galeria.defensores.models.NotificationStatus
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private lateinit var recyclerNotifications: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var adapter: NotificationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerNotifications = view.findViewById(R.id.recycler_notifications)
        textEmpty = view.findViewById(R.id.text_empty)
        
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadNotifications()
    }

    private fun loadNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUser = SessionManager.currentUser
            if (currentUser != null) {
                val notifications = NotificationRepository.getNotificationsForUser(currentUser.id)
                if (notifications.isEmpty()) {
                    textEmpty.visibility = View.VISIBLE
                    recyclerNotifications.visibility = View.GONE
                } else {
                    textEmpty.visibility = View.GONE
                    recyclerNotifications.visibility = View.VISIBLE
                    adapter = NotificationsAdapter(notifications, 
                        onAccept = { notification -> handleAccept(notification) },
                        onReject = { notification -> handleReject(notification) }
                    )
                    recyclerNotifications.adapter = adapter
                }
            }
        }
    }

    private fun handleAccept(notification: Notification) {
        viewLifecycleOwner.lifecycleScope.launch {
            // 1. Add player to table
            TableRepository.addPlayerToTable(notification.tableId, notification.fromUserId)
            // 2. Update notification status
            NotificationRepository.updateStatus(notification.id, NotificationStatus.ACCEPTED)
            
            Toast.makeText(context, "Solicitação aceita!", Toast.LENGTH_SHORT).show()
            loadNotifications()
        }
    }

    private fun handleReject(notification: Notification) {
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationRepository.updateStatus(notification.id, NotificationStatus.REJECTED)
            Toast.makeText(context, "Solicitação recusada.", Toast.LENGTH_SHORT).show()
            loadNotifications()
        }
    }

    class NotificationsAdapter(
        private val notifications: List<Notification>,
        private val onAccept: (Notification) -> Unit,
        private val onReject: (Notification) -> Unit
    ) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

        class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.notification_title)
            val message: TextView = view.findViewById(R.id.notification_message)
            val btnAccept: Button = view.findViewById(R.id.btn_accept)
            val btnReject: Button = view.findViewById(R.id.btn_reject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = notifications[position]
            holder.title.text = "Solicitação de: ${notification.fromUserName}"
            holder.message.text = "Gostaria de participar da mesa: ${notification.tableName}"
            
            holder.btnAccept.setOnClickListener { onAccept(notification) }
            holder.btnReject.setOnClickListener { onReject(notification) }
        }

        override fun getItemCount() = notifications.size
    }
}
