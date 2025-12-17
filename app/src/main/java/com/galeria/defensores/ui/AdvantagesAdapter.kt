package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.AdvantageItem

class AdvantagesAdapter(
    private val items: List<AdvantageItem>,
    private val onItemClick: (AdvantageItem) -> Unit,
    private val onLongClick: ((AdvantageItem) -> Unit)? = null
) : RecyclerView.Adapter<AdvantagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_advantage_name)
        val cost: TextView = view.findViewById(R.id.text_advantage_cost)
        val description: TextView = view.findViewById(R.id.text_advantage_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_advantage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.cost.text = item.cost
        holder.description.text = item.description
        
        // Hide delete button if it exists in XML but we don't use it anymore
        holder.itemView.findViewById<View>(R.id.btn_delete_item)?.visibility = View.GONE

        holder.itemView.setOnClickListener { onItemClick(item) }
        
        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onLongClick.invoke(item)
                true
            }
        }
    }

    override fun getItemCount() = items.size
}
