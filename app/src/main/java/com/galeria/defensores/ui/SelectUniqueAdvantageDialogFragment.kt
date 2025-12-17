package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.UniqueAdvantage

class SelectUniqueAdvantageDialogFragment(
    private val availableUAs: List<UniqueAdvantage>,
    private val canManage: Boolean,
    private val onSelect: (UniqueAdvantage) -> Unit,
    private val onAddCustom: (UniqueAdvantage) -> Unit,
    private val onEditCustom: (UniqueAdvantage, UniqueAdvantage) -> Unit, // old, new
    private val onDeleteCustom: (UniqueAdvantage) -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_select_unique_advantage, container, false)

        val recycler: RecyclerView = view.findViewById(R.id.recycler_uas)
        val cancelButton: Button = view.findViewById(R.id.btn_cancel_selection)
        val addCustomButton: Button = view.findViewById(R.id.btn_add_custom_ua)

        recycler.layoutManager = LinearLayoutManager(context)
        
        // Setup Adapter
        val adapter = UniqueAdvantagesAdapter(availableUAs,
            onSelect = { ua ->
                onSelect(ua)
                dismiss()
            },
            onEdit = if (canManage) { ua ->
                 // Check if it's a custom UA (hacky way: checking if it is in available list provided by ViewModel which contains customs)
                 // Or we trust the viewModel passed the customs. 
                 // We will pass the UA to edit dialog.
                 val editDialog = EditUniqueAdvantageDialogFragment(
                     ua = ua,
                     onSave = { updatedUA ->
                         onEditCustom(ua, updatedUA)
                         dismiss() // Close selection dialog to refresh? or Adapter refresh?
                         // Ideally we should observe LiveData but this is a Dialog.
                         // For simplicity, we close both.
                     },
                     onDelete = { uaToDelete ->
                         onDeleteCustom(uaToDelete)
                         dismiss()
                     }
                 )
                 editDialog.show(parentFragmentManager, "EditCustomUADialog")
            } else null
        )
        recycler.adapter = adapter

        if (canManage) {
            addCustomButton.visibility = View.VISIBLE
            addCustomButton.setOnClickListener {
                val createDialog = EditUniqueAdvantageDialogFragment(
                    onSave = { newUA ->
                        onAddCustom(newUA)
                        dismiss()
                    }
                )
                createDialog.show(parentFragmentManager, "CreateCustomUADialog")
            }
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
