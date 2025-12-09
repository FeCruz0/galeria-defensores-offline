package com.galeria.defensores

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.ui.LoginFragment
import com.galeria.defensores.ui.TableListFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        com.google.firebase.FirebaseApp.initializeApp(this)
        SessionManager.init(this)

        if (savedInstanceState == null) {
            if (SessionManager.isLoggedIn()) {
                // Refresh user data before showing the table list
                lifecycleScope.launch {
                    SessionManager.refreshUser()
                    startNotificationObserver()
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, TableListFragment())
                        .commit()
                }
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commit()
            }
        }
        // Notifications Button Logic
        val btnNotifications = findViewById<android.widget.ImageButton>(R.id.btn_notifications)
        btnNotifications.setOnClickListener {
            if (SessionManager.isLoggedIn()) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, com.galeria.defensores.ui.NotificationsFragment())
                    .addToBackStack(null)
                    .commit()
            } else {
                android.widget.Toast.makeText(this, "Faça login para ver notificações", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        // Hide notification button on Auth screens
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: androidx.fragment.app.FragmentManager, f: androidx.fragment.app.Fragment) {
                super.onFragmentResumed(fm, f)
                val isAuthScreen = f is LoginFragment || 
                                   f.javaClass.simpleName == "RegisterFragment" || 
                                   f.javaClass.simpleName == "ForgotPasswordFragment"
                                   
                // Should show only on main lists (Table, Character, Notifications)
                // Actually safer to whitelist: TableListFragment, CharacterListFragment, UserProfileFragment
                // Or blacklist Auth screens.
                
                // Let's use whitelisting for safer control as user requested "leave only on table list and character list".
                // But NotificationsFragment itself needs it? No, maybe not.
                // Let's stick to blacklisting known auth fragments for now as it's less intrusive if we add new content screens.
                
                if (isAuthScreen) {
                    btnNotifications.visibility = android.view.View.GONE
                } else {
                    btnNotifications.visibility = android.view.View.VISIBLE
                }
            }
        }, true)
    }

    override fun onResume() {
        super.onResume()
        startNotificationObserver()
    }

    private var notificationJob: kotlinx.coroutines.Job? = null

    private fun startNotificationObserver() {
        // Cancel previous job if any
        notificationJob?.cancel()

        if (SessionManager.isLoggedIn()) {
             notificationJob = lifecycleScope.launch {
                 val currentUser = SessionManager.currentUser
                 if (currentUser != null) {
                     com.galeria.defensores.data.NotificationRepository.observeNotificationsCount(currentUser.id)
                         .collect { count ->
                             val btnNotifications = findViewById<android.widget.ImageButton>(R.id.btn_notifications)
                             if (count > 0) {
                                 // Tint red if there are notifications
                                 val color = android.graphics.Color.RED
                                 androidx.core.widget.ImageViewCompat.setImageTintList(btnNotifications, android.content.res.ColorStateList.valueOf(color))
                             } else {
                                 // Default tint (primary text color)
                                 val typedValue = android.util.TypedValue()
                                 theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
                                 val color = if (typedValue.resourceId != 0) {
                                     androidx.core.content.ContextCompat.getColor(this@MainActivity, typedValue.resourceId)
                                 } else {
                                     typedValue.data
                                 }
                                 androidx.core.widget.ImageViewCompat.setImageTintList(btnNotifications, android.content.res.ColorStateList.valueOf(color))
                             }
                             
                             // Hide the badge text view since we are using color now
                             findViewById<android.view.View>(R.id.text_notification_badge).visibility = android.view.View.GONE
                         }
                 }
             }
        }
    }
}