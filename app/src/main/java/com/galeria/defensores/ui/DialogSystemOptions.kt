package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R

class DialogSystemOptions(
    private val onSaveAsClick: () -> Unit,
    private val onExportClick: () -> Unit,
    private val onImportClick: () -> Unit,
    private val onResetClick: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_system_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btn_save_as).setOnClickListener {
            onSaveAsClick()
            dismiss()
        }

        view.findViewById<Button>(R.id.btn_export).setOnClickListener {
            onExportClick()
            dismiss()
        }

        view.findViewById<Button>(R.id.btn_import).setOnClickListener {
            onImportClick()
            dismiss()
        }

        view.findViewById<Button>(R.id.btn_reset_system).setOnClickListener {
            onResetClick()
            dismiss()
        }

        view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }
    }
}
