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

class EditSkillDialogFragment(
    private val skill: AdvantageItem?,
    private val onSave: (AdvantageItem) -> Unit,
    private val onDelete: ((AdvantageItem) -> Unit)? = null
) : DialogFragment() {



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_skill, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_skill_name)
        val editCost = view.findViewById<TextInputEditText>(R.id.edit_skill_cost)
        val editDesc = view.findViewById<TextInputEditText>(R.id.edit_skill_description)
        val btnRemove = view.findViewById<Button>(R.id.btn_remove)
        val titleView = view.findViewById<TextView>(R.id.dialog_title)

        if (skill != null) {
            editName.setText(skill.name)
            editCost.setText(skill.cost)
            editDesc.setText(skill.description)
            titleView.text = "Editar Perícia"
        } else {
            titleView.text = "Nova Perícia"
            // Default cost is "2 pontos" for skills, but editable
            editCost.setText("2 pontos") 
            btnRemove.visibility = View.GONE
        }

        if (onDelete == null) {
            btnRemove.visibility = View.GONE
        }

        btnRemove.setOnClickListener {
            if (skill != null) {
                onDelete?.invoke(skill)
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
                val newItem = skill?.copy(
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
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
