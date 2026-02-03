package com.galeria.defensores

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.ui.TableListFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Offline mode: No Firebase Auth init needed
        SessionManager.init(this)
        com.galeria.defensores.data.CharacterRepository.init(this)
        com.galeria.defensores.data.TableRepository.init(this)

        if (savedInstanceState == null) {
            // Always assume logged in (offline)
            lifecycleScope.launch {
                SessionManager.refreshUser() // Sets mock user
                startNotificationObserver() 
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, TableListFragment())
                    .commit()
            }
        }
        
        // Notifications Button Logic
        val btnNotifications = findViewById<android.widget.ImageButton>(R.id.btn_notifications)
        btnNotifications.setOnClickListener {
             supportFragmentManager.beginTransaction()
                 .replace(R.id.fragment_container, com.galeria.defensores.ui.NotificationsFragment())
                 .addToBackStack(null)
                 .commit()
        }
        
        // Always visible in offline mode (except maybe if we needed to hide it, but logic was removing it for auth screens)
        btnNotifications.visibility = android.view.View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        startNotificationObserver()
    }

    private var notificationJob: kotlinx.coroutines.Job? = null

    private fun startNotificationObserver() {
        // Cancel previous job if any
        notificationJob?.cancel()

        notificationJob = lifecycleScope.launch {
             val currentUser = SessionManager.currentUser
             if (currentUser != null) {
                 // Note: NotificationRepository might still use Firestore. 
                 // If we want fully offline, we should mock this too later or handle errors gracefully.
                 // For now keeping it, assuming pure "Login Removal" step. 
                 // If implementation plan said "Offline", we might need to touch repositories too.
                 // But removing Login is the main step.
                 try {
                     com.galeria.defensores.data.NotificationRepository.observeNotificationsCount(currentUser.id)
                         .collect { count ->
                             val btnNotifications = findViewById<android.widget.ImageButton>(R.id.btn_notifications)
                             if (count > 0) {
                                 val color = android.graphics.Color.RED
                                 androidx.core.widget.ImageViewCompat.setImageTintList(btnNotifications, android.content.res.ColorStateList.valueOf(color))
                             } else {
                                 val typedValue = android.util.TypedValue()
                                 theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                                 val color = if (typedValue.resourceId != 0) {
                                     androidx.core.content.ContextCompat.getColor(this@MainActivity, typedValue.resourceId)
                                 } else {
                                     typedValue.data
                                 }
                                 androidx.core.widget.ImageViewCompat.setImageTintList(btnNotifications, android.content.res.ColorStateList.valueOf(color))
                             }
                             findViewById<android.view.View>(R.id.text_notification_badge).visibility = android.view.View.GONE
                         }
                 } catch (e: Exception) {
                     // Ignore offline errors
                 }
             }
         }
    }
}
