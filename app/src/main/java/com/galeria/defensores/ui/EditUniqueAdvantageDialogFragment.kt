package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.models.UniqueAdvantage

class EditUniqueAdvantageDialogFragment(
    private val ua: UniqueAdvantage? = null,
    private val onSave: (UniqueAdvantage) -> Unit,
    private val onDelete: ((UniqueAdvantage) -> Unit)? = null
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_unique_advantage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleText: TextView = view.findViewById(R.id.text_dialog_title)
        val nameEdit: EditText = view.findViewById(R.id.edit_ua_name)
        val groupEdit: EditText = view.findViewById(R.id.edit_ua_group)
        val costEdit: EditText = view.findViewById(R.id.edit_ua_cost)
        val benefitsEdit: EditText = view.findViewById(R.id.edit_ua_benefits)
        val weaknessesEdit: EditText = view.findViewById(R.id.edit_ua_weaknesses)
        val saveButton: Button = view.findViewById(R.id.btn_save)
        val cancelButton: Button = view.findViewById(R.id.btn_cancel)
        val deleteButton: Button = view.findViewById(R.id.btn_delete)

        // Try to find edit button (may not exist in older fragment managers, safe fallback)
        val editButton: Button? = view.findViewById(R.id.btn_edit)

        if (ua != null) {
            // ── Read-only / Detalhes mode ──
            titleText.text = "Detalhes"
            nameEdit.setText(ua.name)
            groupEdit.setText(ua.group)
            costEdit.setText(ua.cost.toString())
            // Show paragraph spacing in read-only mode
            benefitsEdit.setText(ua.benefits.replace("\n", "\n\n"))
            weaknessesEdit.setText(ua.weaknesses.replace("\n", "\n\n"))

            nameEdit.isEnabled = false
            groupEdit.isEnabled = false
            costEdit.isEnabled = false
            benefitsEdit.isEnabled = false
            weaknessesEdit.isEnabled = false

            saveButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
            editButton?.visibility = View.VISIBLE

            editButton?.setOnClickListener {
                // ── Switch to Edit mode ──
                titleText.text = "Editar Vantagem Única"
                nameEdit.isEnabled = true
                groupEdit.isEnabled = true
                costEdit.isEnabled = true
                benefitsEdit.isEnabled = true
                weaknessesEdit.isEnabled = true

                // Restore original text (without doubled newlines) for editing
                benefitsEdit.setText(ua.benefits)
                weaknessesEdit.setText(ua.weaknesses)

                saveButton.visibility = View.VISIBLE
                editButton.visibility = View.GONE

                if (onDelete != null) {
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.setOnClickListener {
                        onDelete.invoke(ua)
                        dismiss()
                    }
                }
            }

        } else {
            // ── Create mode ──
            titleText.text = "Nova Vantagem Única"
            saveButton.visibility = View.VISIBLE
            deleteButton.visibility = View.GONE
            editButton?.visibility = View.GONE
        }

        saveButton.setOnClickListener {
            val name = nameEdit.text.toString()
            val group = groupEdit.text.toString()
            val costStr = costEdit.text.toString()
            val benefits = benefitsEdit.text.toString()
            val weaknesses = weaknessesEdit.text.toString()

            if (name.isEmpty()) {
                nameEdit.error = "Nome é obrigatório"
                return@setOnClickListener
            }

            val costP = costStr.toIntOrNull() ?: 0
            val newUA = UniqueAdvantage(name, group, costP, benefits, weaknesses)
            onSave(newUA)
            dismiss()
        }

        cancelButton.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
