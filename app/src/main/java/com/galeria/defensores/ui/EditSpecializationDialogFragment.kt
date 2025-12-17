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

class EditSpecializationDialogFragment(
    private val specialization: AdvantageItem?,
    private val onSave: (AdvantageItem) -> Unit,
    private val onDelete: ((AdvantageItem) -> Unit)? = null
) : DialogFragment() {

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_specialization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_spec_name)
        val editCost = view.findViewById<TextInputEditText>(R.id.edit_spec_cost)
        val editDesc = view.findViewById<TextInputEditText>(R.id.edit_spec_description)
        val btnRemove = view.findViewById<Button>(R.id.btn_remove)
        val titleView = view.findViewById<TextView>(R.id.dialog_title)

        if (specialization != null) {
            editName.setText(specialization.name)
            editCost.setText(specialization.cost)
            editDesc.setText(specialization.description)
            titleView.text = "Editar Especialização"
        } else {
            titleView.text = "Nova Especialização"
            // Default parent skills often empty for custom
            editCost.setText("") 
            btnRemove.visibility = View.GONE
        }

        if (onDelete == null) {
            btnRemove.visibility = View.GONE
        }

        btnRemove.setOnClickListener {
            if (specialization != null) {
                onDelete?.invoke(specialization)
                dismiss()
            }
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val name = editName.text.toString()
            val cost = editCost.text.toString()
            val desc = editDesc.text.toString()

            if (name.isNotBlank()) {
                val newItem = specialization?.copy(
                    name = name,
                    cost = cost,
                    description = desc
                ) ?: AdvantageItem(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    cost = cost,
                    description = desc
                )
                onSave(newItem)
                dismiss()
            }
        }
    }
}
