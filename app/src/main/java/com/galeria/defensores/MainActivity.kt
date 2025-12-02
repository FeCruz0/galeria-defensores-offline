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

        SessionManager.init(this)

        if (savedInstanceState == null) {
            if (SessionManager.isLoggedIn()) {
                // Refresh user data before showing the table list
                lifecycleScope.launch {
                    SessionManager.refreshUser()
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
    }
}