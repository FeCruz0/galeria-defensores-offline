package com.galeria.defensores.ui

import android.app.Dialog
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
        val view = inflater.inflate(R.layout.dialog_edit_unique_advantage, container, false)

        val titleText: TextView = view.findViewById(R.id.text_dialog_title)
        val nameEdit: EditText = view.findViewById(R.id.edit_ua_name)
        val groupEdit: EditText = view.findViewById(R.id.edit_ua_group)
        val costEdit: EditText = view.findViewById(R.id.edit_ua_cost)
        val benefitsEdit: EditText = view.findViewById(R.id.edit_ua_benefits)
        val weaknessesEdit: EditText = view.findViewById(R.id.edit_ua_weaknesses)
        val saveButton: Button = view.findViewById(R.id.btn_save)
        val cancelButton: Button = view.findViewById(R.id.btn_cancel)
        val deleteButton: Button = view.findViewById(R.id.btn_delete)

        if (ua != null) {
            titleText.text = "Editar Vantagem Única"
            nameEdit.setText(ua.name)
            groupEdit.setText(ua.group)
            costEdit.setText(ua.cost.toString())
            benefitsEdit.setText(ua.benefits)
            weaknessesEdit.setText(ua.weaknesses)
            
            if (onDelete != null) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    onDelete.invoke(ua)
                    dismiss()
                }
            }
        } else {
            titleText.text = "Nova Vantagem Única"
        }

        saveButton.setOnClickListener {
            val name = nameEdit.text.toString()
            val group = groupEdit.text.toString()
            val costStr = costEdit.text.toString()
            val benefits = benefitsEdit.text.toString()
            val weaknesses = weaknessesEdit.text.toString()

            val costP = costStr.toIntOrNull()

            if (name.isEmpty()) {
                nameEdit.error = "Nome é obrigatório"
                return@setOnClickListener
            }

            if (costP == null) {
                costEdit.error = "Insira um número válido"
                return@setOnClickListener
            }

            val newUA = UniqueAdvantage(name, group, costP, benefits, weaknesses)
            onSave(newUA)
            dismiss()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
