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
        val btnSave = view.findViewById<View>(R.id.btn_save)
        val btnEdit = view.findViewById<Button>(R.id.btn_edit)
        val titleView = view.findViewById<TextView>(R.id.dialog_title)

        if (skill != null) {
            editName.setText(skill.name)
            editCost.setText(skill.cost)
            editDesc.setText(com.galeria.defensores.utils.TextFormatUtils.formatParagraphSpacing(skill.description))
            titleView.text = "Detalhes"

            editName.isEnabled = false
            editCost.isEnabled = false
            editDesc.isEnabled = false
            btnSave.visibility = View.GONE
            btnRemove.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
        } else {
            titleView.text = "Nova Perícia"
            // Default cost is "2 pontos" for skills, but editable
            editCost.setText("2 pontos") 
            btnRemove.visibility = View.GONE
            btnEdit.visibility = View.GONE

            editName.isEnabled = true
            editCost.isEnabled = true
            editDesc.isEnabled = true
            btnSave.visibility = View.VISIBLE
        }

        editDesc.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s != null && editDesc.hasFocus()) {
                    com.galeria.defensores.utils.TextFormatUtils.applyParagraphSpacingToEditable(s)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        if (onDelete == null) {
            btnRemove.visibility = View.GONE
        }

        btnEdit.setOnClickListener {
            editName.isEnabled = true
            editCost.isEnabled = true
            editDesc.isEnabled = true
            btnSave.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
            if (onDelete != null) btnRemove.visibility = View.VISIBLE
            titleView.text = "Editar Perícia"
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
            val desc = com.galeria.defensores.utils.TextFormatUtils.cleanParagraphSpacing(editDesc.text.toString())

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
