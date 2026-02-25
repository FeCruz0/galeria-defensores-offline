package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.models.AdvantageItem
import com.google.android.material.textfield.TextInputEditText

class EditAdvantageDialogFragment(
    private val advantage: AdvantageItem?,
    private val onSave: (AdvantageItem) -> Unit,
    private val onDelete: ((AdvantageItem) -> Unit)? = null
) : DialogFragment() {



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_advantage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_advantage_name)
        val editCost = view.findViewById<TextInputEditText>(R.id.edit_advantage_cost)
        val editDesc = view.findViewById<TextInputEditText>(R.id.edit_advantage_description)
        val btnRemove = view.findViewById<Button>(R.id.btn_remove)
        val btnSave = view.findViewById<View>(R.id.btn_save)
        val btnEdit = view.findViewById<Button>(R.id.btn_edit) // Assumes I will add this to XML
        val titleView = view.findViewById<TextView>(R.id.dialog_title)

        if (advantage != null) {
            editName.setText(advantage.name)
            editCost.setText(advantage.cost)
            // Show paragraph spacing in read-only mode
            editDesc.setText(advantage.description.replace("\n", "\n\n"))
            titleView.text = "Detalhes"
            
            // Read-Only Mode Initially
            editName.isEnabled = false
            editCost.isEnabled = false
            editDesc.isEnabled = false
            btnSave.visibility = View.GONE
            btnRemove.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
        } else {
            titleView.text = "Nova Vantagem"
            btnRemove.visibility = View.GONE
            btnEdit.visibility = View.GONE
            // Default Edit Mode
            editName.isEnabled = true
            editCost.isEnabled = true
            editDesc.isEnabled = true
             btnSave.visibility = View.VISIBLE
        }

        if (onDelete == null) {
            btnRemove.visibility = View.GONE
        }

        btnEdit.setOnClickListener {
            editName.isEnabled = true
            editCost.isEnabled = true
            editDesc.isEnabled = true
            // Restore original text (without doubled paragraph spacing) before editing
            editDesc.setText(advantage?.description ?: "")
            btnSave.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
            if (onDelete != null) btnRemove.visibility = View.VISIBLE
            titleView.text = "Editar Vantagem"
        }

        btnRemove.setOnClickListener {
            if (advantage != null) {
                onDelete?.invoke(advantage)
                dismiss()
            }
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val costStr = editCost.text.toString()
            val desc = editDesc.text.toString()

            val costInt = costStr.toIntOrNull() // Validation only, we store string
            // Allow complex costs like "1-3" or "1pt" if the user wants, but currently logic checks int?
            // "1-3" will fail toIntOrNull. The user's requested data has "1-3", "1 a -2". 
            // The original code `val costInt = costStr.toIntOrNull()` and `if (costInt == null)` prevents non-integer costs!
            // I MUST FIX THIS VALIDATION to allow string costs for the user's data!
            // The Gaiden data has ranges. The validation blocks them.
            
            if (name.isNotBlank()) {
                val newItem = advantage?.copy(
                    name = name,
                    cost = costStr, 
                    description = desc
                ) ?: AdvantageItem(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    cost = costStr,
                    description = desc
                )
                onSave(newItem)
                dismiss()
            } else {
                 editName.error = "Nome é obrigatório"
            }
        }
    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
