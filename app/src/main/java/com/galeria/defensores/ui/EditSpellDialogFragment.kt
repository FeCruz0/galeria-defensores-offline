package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.models.Spell
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class EditSpellDialogFragment(
    private val spell: Spell?,
    private val onSave: (Spell) -> Unit,
    private val onDelete: ((Spell) -> Unit)? = null
) : DialogFragment() {



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_spell, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_spell_name)
        val editSchool = view.findViewById<TextInputEditText>(R.id.edit_spell_school)
        val editRequirements = view.findViewById<TextInputEditText>(R.id.edit_spell_requirements)
        val editCost = view.findViewById<TextInputEditText>(R.id.edit_spell_cost)
        val editRange = view.findViewById<TextInputEditText>(R.id.edit_spell_range)
        val editDuration = view.findViewById<TextInputEditText>(R.id.edit_spell_duration)
        val editDesc = view.findViewById<TextInputEditText>(R.id.edit_spell_description)
        
        val btnRemove = view.findViewById<Button>(R.id.btn_remove_spell)
        val btnSave = view.findViewById<View>(R.id.btn_save)
        val btnEdit = view.findViewById<Button>(R.id.btn_edit)
        val titleView = view.findViewById<TextView>(R.id.dialog_title)

        if (spell != null) {
            titleView.text = "Detalhes"
            editName.setText(spell.name)
            editSchool.setText(spell.school)
            editRequirements.setText(spell.requirements)
            editCost.setText(spell.cost)
            editRange.setText(spell.range)
            editDuration.setText(spell.duration)
            editDesc.setText(com.galeria.defensores.utils.TextFormatUtils.formatParagraphSpacing(spell.description))

            editName.isEnabled = false
            editSchool.isEnabled = false
            editRequirements.isEnabled = false
            editCost.isEnabled = false
            editRange.isEnabled = false
            editDuration.isEnabled = false
            editDesc.isEnabled = false

            btnSave.visibility = View.GONE
            btnRemove.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
        } else {
            titleView.text = "Nova Magia"
            btnRemove.visibility = View.GONE
            btnEdit.visibility = View.GONE

            editName.isEnabled = true
            editSchool.isEnabled = true
            editRequirements.isEnabled = true
            editCost.isEnabled = true
            editRange.isEnabled = true
            editDuration.isEnabled = true
            editDesc.isEnabled = true
            btnSave.visibility = View.VISIBLE
        }

        btnEdit.setOnClickListener {
            editName.isEnabled = true
            editSchool.isEnabled = true
            editRequirements.isEnabled = true
            editCost.isEnabled = true
            editRange.isEnabled = true
            editDuration.isEnabled = true
            editDesc.isEnabled = true
            
            btnSave.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
            if (onDelete != null) btnRemove.visibility = View.VISIBLE
            titleView.text = "Editar Magia"
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

        btnRemove.setOnClickListener {
            if (spell != null) {
                onDelete?.invoke(spell)
                dismiss()
            }
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val name = editName.text.toString()
            if (name.isNotBlank()) {
                val newSpell = spell?.copy(
                    name = name,
                    school = editSchool.text.toString(),
                    requirements = editRequirements.text.toString(),
                    cost = editCost.text.toString(),
                    range = editRange.text.toString(),
                    duration = editDuration.text.toString(),
                    description = com.galeria.defensores.utils.TextFormatUtils.cleanParagraphSpacing(editDesc.text.toString())
                ) ?: Spell(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    school = editSchool.text.toString(),
                    requirements = editRequirements.text.toString(),
                    cost = editCost.text.toString(),
                    range = editRange.text.toString(),
                    duration = editDuration.text.toString(),
                    description = com.galeria.defensores.utils.TextFormatUtils.cleanParagraphSpacing(editDesc.text.toString())
                )
                onSave(newSpell)
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
