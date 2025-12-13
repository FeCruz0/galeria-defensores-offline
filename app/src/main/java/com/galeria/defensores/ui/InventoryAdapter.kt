package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.InventoryItem

class InventoryAdapter(
    private val items: List<InventoryItem>,
    private val canEdit: Boolean,
    private val onItemClick: (InventoryItem) -> Unit,
    private val onQuantityChange: (InventoryItem, Int) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_item_name)
        val quantity: TextView = view.findViewById(R.id.text_item_quantity)
        val btnMinus: View = view.findViewById(R.id.btn_minus)
        val btnPlus: View = view.findViewById(R.id.btn_plus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.quantity.text = item.quantity
        
        if (canEdit) {
            holder.btnMinus.visibility = View.VISIBLE
            holder.btnPlus.visibility = View.VISIBLE
            
            holder.btnMinus.setOnClickListener { onQuantityChange(item, -1) }
            holder.btnPlus.setOnClickListener { onQuantityChange(item, 1) }
        } else {
            holder.btnMinus.visibility = View.GONE
            holder.btnPlus.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size
}
