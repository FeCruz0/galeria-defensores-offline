package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.models.InventoryItem
import com.google.android.material.textfield.TextInputEditText

class EditInventoryItemDialogFragment(
    private val item: InventoryItem?,
    private val onSave: (InventoryItem) -> Unit,
    private val onDelete: ((InventoryItem) -> Unit)? = null
) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_item_name)
        val editQty = view.findViewById<TextInputEditText>(R.id.edit_item_quantity)
        val btnRemove = view.findViewById<Button>(R.id.btn_remove)
        val titleView = view.findViewById<TextView>(R.id.dialog_title)

        if (item != null) {
            editName.setText(item.name)
            editQty.setText(item.quantity)
            titleView.text = "Editar Item"
        } else {
            titleView.text = "Novo Item"
            editQty.setText("1")
            btnRemove.visibility = View.GONE
        }

        if (onDelete == null) {
            btnRemove.visibility = View.GONE
        }

        btnRemove.setOnClickListener {
            if (item != null) {
                onDelete?.invoke(item)
                dismiss()
            }
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btn_save).setOnClickListener {
            val name = editName.text.toString()
            val qty = editQty.text.toString()

            if (name.isNotBlank()) {
                val newItem = item?.copy(
                    name = name,
                    quantity = qty
                ) ?: InventoryItem(
                    name = name,
                    quantity = qty
                )
                onSave(newItem)
                dismiss()
            }
        }
    }
}
