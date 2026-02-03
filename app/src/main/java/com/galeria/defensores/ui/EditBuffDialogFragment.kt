package com.galeria.defensores.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.models.Buff

class EditBuffDialogFragment(
    private val buff: Buff? = null,
    private val onSave: (Buff) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_buff, null) // Needs layout

        val nameEdit = view.findViewById<EditText>(R.id.edit_buff_name)
        val valueEdit = view.findViewById<EditText>(R.id.edit_buff_value)
        val targetSpinner = view.findViewById<Spinner>(R.id.spinner_buff_target)

        // Setup Spinner
        // Options: ATTACK, DEFENSE, MAGIC, Attributes (F, H, R, A, PdF), ALL
        val targets = listOf("ATTACK", "DEFENSE", "MAGIC", "forca", "habilidade", "resistencia", "armadura", "poderFogo", "ALL")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, targets)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        targetSpinner.adapter = adapter

        if (buff != null) {
            nameEdit.setText(buff.name)
            valueEdit.setText(buff.value.toString())
            val idx = targets.indexOf(buff.targetType)
            if (idx >= 0) targetSpinner.setSelection(idx)
        }

        builder.setView(view)
            .setTitle(if (buff == null) "Novo Buff/Debuff" else "Editar Buff")
            .setPositiveButton("Salvar") { _, _ ->
                val name = nameEdit.text.toString()
                val value = valueEdit.text.toString().toIntOrNull() ?: 0
                val target = targetSpinner.selectedItem.toString()

                val newBuff = buff?.copy(name = name, value = value, targetType = target)
                    ?: Buff(name = name, value = value, targetType = target)
                
                onSave(newBuff)
            }
            .setNegativeButton("Cancelar", null)

        return builder.create()
    }
}
