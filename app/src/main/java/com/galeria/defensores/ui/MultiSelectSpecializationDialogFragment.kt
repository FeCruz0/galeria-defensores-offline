package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.SpecializationsRepository
import com.galeria.defensores.models.AdvantageItem

class MultiSelectSpecializationDialogFragment(
    private val onSpecializationsSelected: (List<AdvantageItem>) -> Unit
) : DialogFragment() {

    private val selectedItems = mutableListOf<AdvantageItem>()
    private val maxSelection = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_select_specialization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_specializations)
        val confirmButton = view.findViewById<Button>(R.id.btn_confirm_selection)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        fun updateConfirmButton() {
            val count = selectedItems.size
            if (count > 0) {
                confirmButton.text = "Adicionar ($count)"
                confirmButton.isEnabled = true
            } else {
                confirmButton.text = "Adicionar (0)"
                confirmButton.isEnabled = false
            }
        }

        fun loadSpecializations() {
            val items = SpecializationsRepository.getAllSpecializations()
            val adapter = MultiSelectAdapter(
                items = items,
                selectedItems = selectedItems,
                onSelectionChanged = { item, isSelected ->
                    if (isSelected) {
                        if (selectedItems.size < maxSelection) {
                            selectedItems.add(item)
                        } else {
                            // Already at max, maybe show toast or just ignore
                            // For simplistic UX, we'll block the check in the adapter or here
                            // But usually setChecked logic is in the adapter logic
                        }
                    } else {
                        selectedItems.remove(item)
                    }
                    updateConfirmButton()
                },
                canSelectMore = { selectedItems.size < maxSelection }
            )
            recyclerView.adapter = adapter
        }
        
        loadSpecializations()

        view.findViewById<View>(R.id.btn_cancel_selection).setOnClickListener {
            dismiss()
        }
        
        confirmButton.setOnClickListener {
            onSpecializationsSelected(selectedItems)
            dismiss()
        }

        view.findViewById<View>(R.id.btn_new_specialization).setOnClickListener {
            val editDialog = EditSpecializationDialogFragment(null, { newSpec ->
                SpecializationsRepository.addSpecialization(newSpec)
                loadSpecializations() // Refresh list
            })
            editDialog.show(parentFragmentManager, "EditSpecDialog")
        }
    }

    // Inner Adapter Class
    inner class MultiSelectAdapter(
        private val items: List<AdvantageItem>,
        private val selectedItems: List<AdvantageItem>,
        private val onSelectionChanged: (AdvantageItem, Boolean) -> Unit,
        private val canSelectMore: () -> Boolean
    ) : RecyclerView.Adapter<MultiSelectAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.checkbox_select) // Does not exist yet, need item layout
            val name: TextView = view.findViewById(R.id.text_spec_name)
            val skills: TextView = view.findViewById(R.id.text_spec_skills)
            val desc: TextView = view.findViewById(R.id.text_spec_desc)
        }
        
        // We need a specific item layout for checkboxes. 
        // Or we can reuse item_advantage if we modify it or create item_specialization_select.
        // Let's assume we create 'item_specialization_select'
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_specialization_select, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.skills.text = item.cost // Using cost field for parent skills
            holder.desc.text = item.description
            
            // Avoid triggering listener during binding
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = selectedItems.contains(item)
            
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !canSelectMore()) {
                    holder.checkBox.isChecked = false // Revert if max reached
                } else {
                    onSelectionChanged(item, isChecked)
                }
            }
            
            holder.itemView.setOnClickListener {
                holder.checkBox.toggle()
            }
        }

        override fun getItemCount() = items.size
    }
}
