package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.UniqueAdvantage

class UniqueAdvantagesAdapter(
    private var uas: List<UniqueAdvantage>,
    private val onSelect: (UniqueAdvantage) -> Unit,
    private val onEdit: ((UniqueAdvantage) -> Unit)? = null
) : RecyclerView.Adapter<UniqueAdvantagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.text_ua_name)
        val costText: TextView = view.findViewById(R.id.text_ua_cost)
        val groupText: TextView = view.findViewById(R.id.text_ua_group)
        val benefitsText: TextView = view.findViewById(R.id.text_ua_benefits)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_unique_advantage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ua = uas[position]
        holder.nameText.text = ua.name
        holder.costText.text = "${ua.cost} pts"
        holder.groupText.text = ua.group
        
        // Show benefits preview
        holder.benefitsText.text = ua.benefits
        holder.benefitsText.visibility = View.VISIBLE

        holder.itemView.setOnClickListener {
            onSelect(ua)
        }

        // Long click to edit (if Master)
        if (onEdit != null) {
            holder.itemView.setOnLongClickListener {
                onEdit.invoke(ua)
                true
            }
        }
    }

    override fun getItemCount() = uas.size
    
    fun updateList(newList: List<UniqueAdvantage>) {
        uas = newList
        notifyDataSetChanged()
    }
}
