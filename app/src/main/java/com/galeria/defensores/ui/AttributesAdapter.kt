package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.AttributeDefinition

class AttributesAdapter(
    private var attributes: List<AttributeDefinition>,
    private var values: Map<String, Int>,
    private val onValueChange: (String, Int) -> Unit,
    private val onAttributeLongClick: (AttributeDefinition) -> Unit
) : RecyclerView.Adapter<AttributesAdapter.ViewHolder>() {

    fun updateData(newAttributes: List<AttributeDefinition>, newValues: Map<String, Int>) {
        attributes = newAttributes
        values = newValues
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_attribute_input, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attr = attributes[position]
        // android.util.Log.d("AdapterDebug", "Binding position $position: ${attr.name}")
        val value = values[attr.key] ?: 0
        holder.bind(attr, value)
    }

    override fun getItemCount(): Int = attributes.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.attribute_label)
        private val input: EditText = itemView.findViewById(R.id.attribute_input)
        private val btnMinus: Button = itemView.findViewById(R.id.btn_minus)
        private val btnPlus: Button = itemView.findViewById(R.id.btn_plus)
        private val icon: ImageView = itemView.findViewById(R.id.attribute_icon)


        fun bind(attr: AttributeDefinition, value: Int) {
            // Safely set name and key handling
            val safeName = attr.name ?: "Unnamed"
            label.text = safeName.uppercase()
            
            // Remove Debug colors
            itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            
            // Color Logic
            try {
                val colorStr = if (!attr.color.isNullOrEmpty()) attr.color else "#FFFFFF"
                val parsedColor = android.graphics.Color.parseColor(colorStr)
                label.setTextColor(parsedColor)
                icon.setColorFilter(parsedColor)
                input.setTextColor(android.graphics.Color.WHITE) // Always white for readability
            } catch (e: Exception) {
                // Fallback color
                label.setTextColor(android.graphics.Color.WHITE)
                icon.setColorFilter(android.graphics.Color.WHITE)
                input.setTextColor(android.graphics.Color.WHITE)
            }

            // Avoid infinite loop with TextWatcher
            if (input.text.toString() != value.toString()) {
                input.setText(value.toString())
            }

            btnMinus.setOnClickListener {
                val newValue = (input.text.toString().toIntOrNull() ?: 0) - 1
                onValueChange(attr.key, newValue)
            }

            btnPlus.setOnClickListener {
                val newValue = (input.text.toString().toIntOrNull() ?: 0) + 1
                onValueChange(attr.key, newValue)
            }
            
            // TextWatcher for direct input
            // Removing old watchers if any? RecyclerView rebinds usually fine if we simple set listener in init or rely on bind
            // But here we set text. Let's use simple logic.
            // Ideally should use a debouncer or only update on focus loss/action done.
            // For now rely on buttons primarily, or strict TextWatcher handling.
            
            // The issue with updating live data from text watcher is loops.
            // We only update if parsed int != current value.

            input.setOnFocusChangeListener { _, hasFocus ->
               if (!hasFocus) {
                   val quantity = input.text.toString().toIntOrNull() ?: 0
                   onValueChange(attr.key, quantity)
               }
            }

            itemView.setOnLongClickListener {
                onAttributeLongClick(attr)
                true
            }
        }
    }
}
