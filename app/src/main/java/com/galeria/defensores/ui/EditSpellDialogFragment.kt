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
        val titleView = view.findViewById<TextView>(R.id.dialog_title)

        if (spell != null) {
            titleView.text = "Editar Magia"
            editName.setText(spell.name)
            editSchool.setText(spell.school)
            editRequirements.setText(spell.requirements)
            editCost.setText(spell.cost)
            editRange.setText(spell.range)
            editDuration.setText(spell.duration)
            editDesc.setText(spell.description)
        } else {
            titleView.text = "Nova Magia"
            btnRemove.visibility = View.GONE
        }

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
                    description = editDesc.text.toString()
                ) ?: Spell(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    school = editSchool.text.toString(),
                    requirements = editRequirements.text.toString(),
                    cost = editCost.text.toString(),
                    range = editRange.text.toString(),
                    duration = editDuration.text.toString(),
                    description = editDesc.text.toString()
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
