package com.galeria.defensores.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R

enum class DialogMode {
    EXPORT, IMPORT
}

class DialogImportExportSystem(
    private val mode: DialogMode,
    private val initialContent: String = "",
    private val onImport: ((String) -> Unit)? = null
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_import_export_system, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleView = view.findViewById<TextView>(R.id.dialog_title)
        val messageView = view.findViewById<TextView>(R.id.dialog_message)
        val editJson = view.findViewById<EditText>(R.id.edit_json_content)
        val btnAction = view.findViewById<Button>(R.id.btn_action)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)

        if (mode == DialogMode.EXPORT) {
            titleView.text = "Exportar Sistema"
            messageView.text = "Copie o código JSON abaixo para salvar ou compartilhar:"
            editJson.setText(initialContent)
            editJson.isFocusable = false 
            editJson.isClickable = true
            // Allow copy but not edit
             // Actually, letting user select text is handled by standard EditText behavior.
             // Setting keyListener to null prevents typing but allows selection if focusableInTouchMode is true?
             // Or just simple way: 
            editJson.keyListener = null 
            
            btnAction.text = "Copiar"
            btnAction.setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Rule System JSON", editJson.text.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copiado para a área de transferência", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        } else {
            titleView.text = "Importar Sistema"
            messageView.text = "Cole o código JSON do sistema abaixo (ATENÇÃO: Isso substituirá o sistema atual!):"
            editJson.setText("")
            editJson.hint = "{ \"name\": ... }"
            
            btnAction.text = "Importar"
            btnAction.setOnClickListener {
                val json = editJson.text.toString()
                if (json.isBlank()) {
                    Toast.makeText(requireContext(), "Cole o JSON primeiro", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                onImport?.invoke(json)
                dismiss()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT // Use match parent height to give space for JSON
        )
    }
}
