package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.ResourceDefinition

class ResourcesAdapter(
    private var resources: List<ResourceDefinition>,
    private var currentValues: Map<String, Int>,
    private var maxValues: Map<String, Int>,
    private val onValueChange: (String, Int) -> Unit,
    private val onResourceLongClick: (ResourceDefinition) -> Unit
) : RecyclerView.Adapter<ResourcesAdapter.ViewHolder>() {

    fun updateData(newResources: List<ResourceDefinition>, newCurrent: Map<String, Int>, newMax: Map<String, Int>) {
        resources = newResources
        currentValues = newCurrent
        maxValues = newMax
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Reuse view_status_compact or create a similar layout?
        // view_status_compact has: label_status, text_current, text_max, progress_bar, btn_minus, btn_plus
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_status_compact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val resource = resources[position]
        val current = currentValues[resource.key] ?: 0
        val max = maxValues[resource.key] ?: 1
        holder.bind(resource, current, max)
    }

    override fun getItemCount(): Int = resources.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.status_label)
        private val valueText: TextView = itemView.findViewById(R.id.status_value) // "Current / Max"
        private val progressBar: ProgressBar = itemView.findViewById(R.id.status_bar)
        
        // Buttons
        private val btnMinus1: View = itemView.findViewById(R.id.btn_minus_1)
        private val btnPlus1: View = itemView.findViewById(R.id.btn_plus_1)
        private val btnMinus5: View = itemView.findViewById(R.id.btn_minus_5)
        private val btnPlus5: View = itemView.findViewById(R.id.btn_plus_5)


        fun bind(res: ResourceDefinition, current: Int, max: Int) {
            label.text = res.name
            try {
                label.setTextColor(android.graphics.Color.parseColor(res.color))
                // Tint progress bar if possible
                 progressBar.progressDrawable?.setTint(android.graphics.Color.parseColor(res.color))
            } catch (e: Exception) { }

            valueText.text = "$current / $max"
            
            progressBar.max = max
            progressBar.progress = current

            btnMinus1.setOnClickListener { onValueChange(res.key, -1) }
            btnPlus1.setOnClickListener { onValueChange(res.key, 1) }
            
            // Optional: Support +/- 5 if logic allows
            btnMinus5.setOnClickListener { onValueChange(res.key, -5) }
            btnPlus5.setOnClickListener { onValueChange(res.key, 5) }

            itemView.setOnLongClickListener {
                onResourceLongClick(res)
                true
            }
        }
    }
}
