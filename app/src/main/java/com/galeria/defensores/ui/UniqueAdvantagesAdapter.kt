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

        // Show all fields in the selection list
        holder.groupText.visibility = View.VISIBLE
        holder.groupText.text = ua.group

        holder.costText.visibility = View.VISIBLE
        holder.costText.text = "${ua.cost} pts"

        // Show full benefits + weaknesses with paragraph spacing
        holder.benefitsText.visibility = View.VISIBLE
        val fullDesc = buildString {
            if (ua.benefits.isNotBlank()) append(ua.benefits.replace("\n", "\n\n"))
            if (ua.weaknesses.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append(ua.weaknesses.replace("\n", "\n\n"))
            }
        }
        holder.benefitsText.text = fullDesc

        holder.itemView.setOnClickListener { onSelect(ua) }

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
