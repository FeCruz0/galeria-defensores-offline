package com.galeria.defensores.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.galeria.defensores.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        val switchAnimation = view.findViewById<Switch>(R.id.switch_animation)
        val switchDarkMode = view.findViewById<Switch>(R.id.switch_dark_mode)

        // Load saved state
        switchAnimation.isChecked = prefs.getBoolean("animation_enabled", true)
        val isDarkMode = prefs.getBoolean("dark_mode_enabled", false)
        switchDarkMode.isChecked = isDarkMode

        // Listeners
        switchAnimation.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("animation_enabled", isChecked).apply()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            
            val mode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            com.galeria.defensores.data.FirebaseAuthManager.logout()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }
}
