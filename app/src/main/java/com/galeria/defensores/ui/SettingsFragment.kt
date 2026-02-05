package com.galeria.defensores.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.galeria.defensores.R

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    private val exportAllLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                android.widget.Toast.makeText(context, "Iniciando backup...", android.widget.Toast.LENGTH_SHORT).show()
                val success = com.galeria.defensores.data.BackupRepository.exportAll(requireContext(), uri)
                if (success) {
                    android.widget.Toast.makeText(context, "Backup completo salvo com sucesso!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Erro ao salvar backup.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
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

        view.findViewById<View>(R.id.btn_full_backup).setOnClickListener {
            val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault()).format(java.util.Date())
            exportAllLauncher.launch("backup_3det_$dateStr.zip")
        }

        view.findViewById<View>(R.id.btn_full_restore).setOnClickListener {
            importAllLauncher.launch(arrayOf("application/zip"))
        }

        view.findViewById<View>(R.id.btn_logout).visibility = View.GONE
    }

    private val importAllLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                android.widget.Toast.makeText(context, "Restaurando backup...", android.widget.Toast.LENGTH_SHORT).show()
                val success = com.galeria.defensores.data.BackupRepository.importAll(requireContext(), uri)
                if (success) {
                    android.widget.Toast.makeText(context, "Restauração completa! Por favor, navegue entre as telas para atualizar.", android.widget.Toast.LENGTH_LONG).show()
                } else {
                     android.widget.Toast.makeText(context, "Erro ao restaurar backup.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
