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

        val switchCollision = view.findViewById<Switch>(R.id.switch_collision)
        switchCollision.isChecked = prefs.getBoolean("collision_enabled", true)
        
        switchCollision.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("collision_enabled", isChecked).apply()
        }

        view.findViewById<View>(R.id.btn_color_global).setOnClickListener {
            ColorPickerDialogFragment.newInstance("Escolher Cor Global").apply {
                setOnColorSelectedListener { color ->
                    prefs.edit()
                        .putInt("color_normal", color)
                        .putInt("color_non_crit", color)
                        .putInt("color_negative", color)
                        .apply()
                }
            }.show(parentFragmentManager, "color_picker")
        }

        view.findViewById<View>(R.id.btn_color_normal).setOnClickListener {
            ColorPickerDialogFragment.newInstance("Cor: Dados Normais").apply {
                setOnColorSelectedListener { color ->
                    prefs.edit().putInt("color_normal", color).apply()
                }
            }.show(parentFragmentManager, "color_picker")
        }

        view.findViewById<View>(R.id.btn_color_non_crit).setOnClickListener {
            ColorPickerDialogFragment.newInstance("Cor: Dados Difíceis/Sem Crit").apply {
                setOnColorSelectedListener { color ->
                    prefs.edit().putInt("color_non_crit", color).apply()
                }
            }.show(parentFragmentManager, "color_picker")
        }

        view.findViewById<View>(R.id.btn_color_negative).setOnClickListener {
            ColorPickerDialogFragment.newInstance("Cor: Dados Negativos").apply {
                setOnColorSelectedListener { color ->
                    prefs.edit().putInt("color_negative", color).apply()
                }
            }.show(parentFragmentManager, "color_picker")
        }

        view.findViewById<View>(R.id.btn_logout).visibility = View.GONE
    }
}
