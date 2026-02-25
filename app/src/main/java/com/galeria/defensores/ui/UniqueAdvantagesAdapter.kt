package com.galeria.defensores.ui

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
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
    private val onEdit: ((UniqueAdvantage) -> Unit)? = null,
    /**
     * Quando `true` (modo lista de seleção): exibe custo, grupo e descrição completa.
     * Quando `false` (modo ficha): exibe apenas o nome — custo/grupo/benefits ficam ocultos.
     */
    private val showDetails: Boolean = true
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

        if (showDetails) {
            // Modo lista de seleção: exibe custo, grupo e descrição completa
            holder.costText.text = "${ua.cost} pts"
            holder.costText.visibility = View.VISIBLE

            holder.groupText.text = ua.group
            holder.groupText.visibility = View.VISIBLE

            // Monta o texto com espaçamento de parágrafo entre Benefícios e Penalidades
            val ssb = SpannableStringBuilder()

            if (ua.benefits.isNotBlank()) {
                ssb.append("Benefícios:\n")
                ssb.append(ua.benefits.trim())
            }

            if (ua.weaknesses.isNotBlank()) {
                if (ssb.isNotEmpty()) {
                    // Espaço de parágrafo: linha em branco de tamanho reduzido (8sp)
                    val start = ssb.length
                    ssb.append("\n\n")
                    // Reduz o tamanho da linha em branco para simular espaçamento de parágrafo
                    ssb.setSpan(AbsoluteSizeSpan(8, true), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                ssb.append("Penalidades:\n")
                ssb.append(ua.weaknesses.trim())
            }

            if (ssb.isNotEmpty()) {
                holder.benefitsText.text = ssb
                holder.benefitsText.visibility = View.VISIBLE
            } else {
                holder.benefitsText.visibility = View.GONE
            }
        } else {
            // Modo ficha: apenas o nome é exibido
            holder.costText.visibility = View.GONE
            holder.groupText.visibility = View.GONE
            holder.benefitsText.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onSelect(ua)
        }

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
