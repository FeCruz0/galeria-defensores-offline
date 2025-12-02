package com.galeria.defensores.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.models.Character

class AddCharacterDialogFragment(private val onCharacterCreated: (Character) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_character, null)
        
        val nameInput = view.findViewById<EditText>(R.id.input_character_name)
        val forcaPicker = view.findViewById<NumberPicker>(R.id.picker_forca).apply { minValue = 0; maxValue = 5 }
        val habilidadePicker = view.findViewById<NumberPicker>(R.id.picker_habilidade).apply { minValue = 0; maxValue = 5 }
        val resistenciaPicker = view.findViewById<NumberPicker>(R.id.picker_resistencia).apply { minValue = 0; maxValue = 5 }
        val armaduraPicker = view.findViewById<NumberPicker>(R.id.picker_armadura).apply { minValue = 0; maxValue = 5 }
        val pdfPicker = view.findViewById<NumberPicker>(R.id.picker_pdf).apply { minValue = 0; maxValue = 5 }

        return AlertDialog.Builder(requireContext())
            .setTitle("Novo Defensor")
            .setView(view)
            .setPositiveButton("Criar") { _, _ ->
                val name = nameInput.text.toString()
                val isHidden = view.findViewById<android.widget.CheckBox>(R.id.check_hidden).isChecked
                if (name.isNotBlank()) {
                    val character = Character(
                        name = name,
                        forca = forcaPicker.value,
                        habilidade = habilidadePicker.value,
                        resistencia = resistenciaPicker.value,
                        armadura = armaduraPicker.value,
                        poderFogo = pdfPicker.value,
                        isHidden = isHidden
                    )
                    onCharacterCreated(character)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    companion object {
        const val TAG = "AddCharacterDialog"
    }
}
