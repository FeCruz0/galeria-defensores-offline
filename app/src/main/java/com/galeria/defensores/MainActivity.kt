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
        com.galeria.defensores.data.RuleSystemRepository.init(this)

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
        // Notifications Button Logic - Removed
        
        // Always visible... logic removed
    }

    override fun onResume() {
        super.onResume()
        startNotificationObserver()
    }

    private var notificationJob: kotlinx.coroutines.Job? = null

    private fun startNotificationObserver() {
        // Removed notification logic
    }
}
