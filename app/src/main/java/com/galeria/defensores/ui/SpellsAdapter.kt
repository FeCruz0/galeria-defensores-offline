package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.Spell

class SpellsAdapter(
    private val spells: List<Spell>,
    private val onSpellClick: (Spell) -> Unit,
    private val onSpellLongClick: ((Spell) -> Unit)? = null
) : RecyclerView.Adapter<SpellsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.text_spell_name)
        val schoolText: TextView = view.findViewById(R.id.text_spell_school)
        val costText: TextView = view.findViewById(R.id.text_spell_cost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val spell = spells[position]
        holder.nameText.text = spell.name
        holder.schoolText.text = "Escola: ${spell.school}"
        holder.costText.text = "${spell.cost} PM"

        holder.itemView.setOnClickListener {
            onSpellClick(spell)
        }
        
        if (onSpellLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onSpellLongClick.invoke(spell)
                true
            }
        }
    }

    override fun getItemCount() = spells.size
}
