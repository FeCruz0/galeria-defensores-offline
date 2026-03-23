package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.Spell
import com.galeria.defensores.utils.TextFormatUtils

class SpellsAdapter(
    private var spells: List<Spell>,
    private val onSpellClick: (Spell) -> Unit
) : RecyclerView.Adapter<SpellsAdapter.ViewHolder>() {

    fun updateData(newItems: List<Spell>) {
        if (spells != newItems) {
            spells = newItems
            notifyDataSetChanged()
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.text_spell_name)
        val schoolText: TextView = view.findViewById(R.id.text_spell_school)
        val costText: TextView = view.findViewById(R.id.text_spell_cost)
        val requirements: TextView = view.findViewById(R.id.text_spell_requirements)
        val rangeDuration: TextView = view.findViewById(R.id.text_spell_range_duration)
        val description: TextView = view.findViewById(R.id.text_spell_description)
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
        holder.requirements.text = "Exigências: ${spell.requirements.ifBlank { "Nenhuma" }}"
        holder.rangeDuration.text = "Alcance: ${spell.range.ifBlank { "Nenhum" }} | Duração: ${spell.duration.ifBlank { "Nenhuma" }}"
        holder.description.text = TextFormatUtils.formatParagraphSpacing(spell.description)
        holder.description.setLineSpacing(0f, 1.2f)

        holder.itemView.setOnClickListener {
            onSpellClick(spell)
        }
    }

    override fun getItemCount() = spells.size
}
