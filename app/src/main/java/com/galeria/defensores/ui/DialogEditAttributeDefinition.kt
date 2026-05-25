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
import com.galeria.defensores.models.AttributeDefinition
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class DialogEditAttributeDefinition(
    private val attribute: AttributeDefinition?,
    private val onSave: (AttributeDefinition) -> Unit,
    private val onDelete: ((AttributeDefinition) -> Unit)? = null
) : DialogFragment() {

    private var selectedColor: String = "#000000"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_attribute_definition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_name)
        val editKey = view.findViewById<TextInputEditText>(R.id.edit_key)
        val viewColor = view.findViewById<View>(R.id.view_color_preview)
        val btnPickColor = view.findViewById<Button>(R.id.btn_pick_color)
        val btnSave = view.findViewById<Button>(R.id.btn_save)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)
        val textTitle = view.findViewById<TextView>(R.id.text_title)

        if (attribute != null) {
            textTitle.text = "Editar Atributo"
            editName.setText(attribute.name)
            editKey.setText(attribute.key) // Or abbreviation? Using key for now.
            // If we want abbreviation separate, we need a field for it.
            // Model has: key, name, abbreviation, color.
            // Layout has Name, Key.
            // Let's assume Key = Abbreviation for simplicity or add Abbreviation field?
            // "Chave/Sigla" hint implies both.
            // Let's use the input for Key, and derive Abbrev or just use Key?
            // Model: key="forca", abbreviation="F".
            // I should probably add an Abbreviation field or just use first letter of Name?
            // Let's stick to Name and Key (which acts as ID/Abbrev for now in simple UI).
            // Actually, I'll update the layout to have Abbreviation if I can, or just ignore it for now.
            // Let's disable Key editing if it's a base system attribute?
            // "3DeT Alpha" attributes might be locked?

            
            selectedColor = attribute.color
            btnDelete.visibility = if (onDelete != null) View.VISIBLE else View.GONE
        } else {
            textTitle.text = "Novo Atributo"
            btnDelete.visibility = View.GONE
            // Random default color
            selectedColor = listOf("#EF4444", "#3B82F6", "#10B981", "#F59E0B", "#8B5CF6").random()
        }

        // Apply filters to force uppercase and letters only (for both New and Edit)
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
            viewColor.setBackgroundColor(Color.WHITE)
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

            if (name.isEmpty() || key.isEmpty()) {
                Toast.makeText(context, "Nome e Chave são obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newAbbrev = if (key.length <= 3) key.uppercase() else key.take(3).uppercase()
            
            val newAttr = if (attribute != null) {
                attribute.copy(name = name, key = key, abbreviation = newAbbrev, color = selectedColor)
            } else {
                AttributeDefinition(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    key = key,
                    abbreviation = newAbbrev,
                    color = selectedColor
                )
            }
            try {
                onSave(newAttr)
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Erro ao salvar atributo", Toast.LENGTH_LONG).show()
            }
        }

        btnDelete.setOnClickListener {
            attribute?.let { onDelete?.invoke(it) }
            dismiss()
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
