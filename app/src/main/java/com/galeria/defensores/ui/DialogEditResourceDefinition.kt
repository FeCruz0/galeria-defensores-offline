package com.galeria.defensores.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.models.ResourceDefinition
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class DialogEditResourceDefinition(
    private val resource: ResourceDefinition?,
    private val onSave: (ResourceDefinition) -> Unit,
    private val onDelete: ((ResourceDefinition) -> Unit)? = null
) : DialogFragment() {

    private var selectedColor: String = "#EF4444"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_resource_definition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_name)
        val editKey = view.findViewById<TextInputEditText>(R.id.edit_key)
        val editFormula = view.findViewById<TextInputEditText>(R.id.edit_formula)
        val viewColor = view.findViewById<View>(R.id.view_color_preview)
        val btnPickColor = view.findViewById<Button>(R.id.btn_pick_color)
        val btnSave = view.findViewById<Button>(R.id.btn_save)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)
        val textTitle = view.findViewById<TextView>(R.id.text_title)

        if (resource != null) {
            textTitle.text = "Editar Recurso"
            editName.setText(resource.name)
            editKey.setText(resource.key)
            editFormula.setText(resource.formula)
            selectedColor = resource.color
            btnDelete.visibility = if (onDelete != null) View.VISIBLE else View.GONE
        } else {
            textTitle.text = "Novo Recurso"
            btnDelete.visibility = View.GONE
            selectedColor = listOf("#EF4444", "#3B82F6", "#10B981", "#F59E0B", "#8B5CF6").random()
        }

        // Apply filters to force uppercase and letters only
        editKey.filters = arrayOf(
            android.text.InputFilter.AllCaps(),
            android.text.InputFilter { source, start, end, dest, dstart, dend ->
                for (i in start until end) {
                    if (!Character.isLetter(source[i])) {
                        return@InputFilter ""
                    }
                }
                null
            }
        )

        try {
            viewColor.setBackgroundColor(Color.parseColor(selectedColor))
        } catch (e: Exception) {
            viewColor.setBackgroundColor(Color.BLACK)
        }

        btnPickColor.setOnClickListener {
            val colors = listOf(
                "Vermelho" to "#EF4444",
                "Azul" to "#3B82F6",
                "Verde" to "#10B981",
                "Amarelo" to "#F59E0B",
                "Roxo" to "#8B5CF6",
                "Rosa" to "#EC4899",
                "Índigo" to "#6366F1",
                "Turquesa" to "#14B8A6",
                "Preto" to "#000000"
            )
            
            val adapter = object : android.widget.ArrayAdapter<Pair<String, String>>(
                requireContext(),
                R.layout.item_color_selection,
                colors
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_color_selection, parent, false)
                    val item = getItem(position)
                    
                    val colorView = view.findViewById<View>(R.id.view_color_item)
                    val nameView = view.findViewById<TextView>(R.id.text_color_name)
                    
                    if (item != null) {
                        nameView.text = item.first
                        try {
                            colorView.setBackgroundColor(Color.parseColor(item.second))
                        } catch (e: Exception) {
                            colorView.setBackgroundColor(Color.BLACK)
                        }
                    }
                    return view
                }
            }

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Escolher Cor")
                .setAdapter(adapter) { _, which ->
                    selectedColor = colors[which].second
                    try {
                        viewColor.setBackgroundColor(Color.parseColor(selectedColor))
                    } catch (e: Exception) { }
                }
                .show()
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString().trim()
            val key = editKey.text.toString().trim()
            val formula = editFormula.text.toString().trim()

            if (name.isEmpty() || key.isEmpty()) {
                Toast.makeText(context, "Nome e Chave são obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newRes = if (resource != null) {
                resource.copy(name = name, key = key, formula = formula, color = selectedColor)
            } else {
                ResourceDefinition(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    key = key,
                    formula = formula,
                    color = selectedColor
                )
            }
            try {
                onSave(newRes)
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Erro ao salvar recurso", Toast.LENGTH_LONG).show()
            }
        }

        btnDelete.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Excluir Recurso")
                .setMessage("Tem certeza que deseja excluir o recurso '${resource?.name}'? Essa ação não pode ser desfeita.")
                .setPositiveButton("Excluir") { _, _ ->
                    resource?.let { onDelete?.invoke(it) }
                    dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
