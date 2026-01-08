package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.CustomRoll

class CustomRollsAdapter(
    private val items: MutableList<CustomRoll>,
    private val onRollClick: (CustomRoll) -> Unit,
    private val onEditClick: (CustomRoll) -> Unit,
    private val canEdit: Boolean
) : RecyclerView.Adapter<CustomRollsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: android.widget.TextView = view.findViewById(R.id.btn_custom_roll)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_custom_roll, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val roll = items[position]
        holder.txtName.text = roll.name
        
        holder.itemView.setOnClickListener {
            onRollClick(roll)
        }

        if (canEdit) {
            holder.itemView.setOnLongClickListener {
                onEditClick(roll)
                true
            }
        }
    }

    override fun getItemCount(): Int = items.size
    
    fun updateData(newItems: List<CustomRoll>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
