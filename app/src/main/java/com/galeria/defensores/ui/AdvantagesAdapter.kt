package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.utils.TextFormatUtils

/**
 * Adapter para exibição de vantagens/desvantagens.
 *
 * Modos de uso:
 * 1. **Lista de seleção** (`showDescription = true`, `onModularItemConfirmed != null`):
 *    - Item modular: expande inline mostrando checkboxes dos modificadores.
 *    - Item simples: chama `onItemClick` diretamente.
 *
 * 2. **Ficha do personagem** (`showDescription = false`, sem `onModularItemConfirmed`):
 *    - Mostra nome + custo + resumo dos modificadores selecionados.
 *    - Clique chama `onItemClick` (abre dialog de Detalhes).
 */
class AdvantagesAdapter(
    private var items: List<AdvantageItem>,
    private val onItemClick: (AdvantageItem) -> Unit,
    private val onLongClick: ((AdvantageItem) -> Unit)? = null,
    private val showDescription: Boolean = false,
    /** Se não-nulo, ativa o modo seleção modular. Retorna o item com selectedModifiers preenchidos. */
    private val onModularItemConfirmed: ((AdvantageItem) -> Unit)? = null
) : RecyclerView.Adapter<AdvantagesAdapter.ViewHolder>() {
    private val pendingSelections = mutableMapOf<String, MutableSet<String>>()

    fun updateItems(newItems: List<AdvantageItem>) {
        if (items != newItems) {
            items = newItems
            notifyDataSetChanged()
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_advantage_name)
        val cost: TextView = view.findViewById(R.id.text_advantage_cost)
        val description: TextView = view.findViewById(R.id.text_advantage_description)
        val selectedModifiers: TextView = view.findViewById(R.id.text_selected_modifiers)
        val containerModifiers: LinearLayout = view.findViewById(R.id.container_modifiers)
        val containerFooter: LinearLayout = view.findViewById(R.id.container_modular_footer)
        val computedCost: TextView = view.findViewById(R.id.text_computed_cost)
        val btnAdd: Button = view.findViewById(R.id.btn_add_modular)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_advantage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        // ── Custo exibido ──────────────────────────────────────────────────
        if (item.isModular) {
            val selectedIds = pendingSelections[item.id] ?: emptySet()
            val computed = if (item.name.equals("MANOBRAS ESPECIAIS", ignoreCase = true) || 
                               item.name.equals("QUALIDADES ESPECIAIS", ignoreCase = true) ||
                               item.name.equals("SENTIDOS ESPECIAIS", ignoreCase = true) ||
                               item.name.equals("STATUS NEGATIVOS", ignoreCase = true)) {
                item.baseCostPt + Math.ceil(selectedIds.size / 3.0).toInt()
            } else {
                item.baseCostPt + item.modifiers
                    .filter { it.id in selectedIds }
                    .sumOf { it.costPt }
            }
            holder.cost.text = if (computed != 0) "$computed PT" else item.cost
        } else {
            holder.cost.text = item.cost
        }

        // ── Descrição (lista de seleção) ───────────────────────────────────
        if (showDescription && item.description.isNotEmpty()) {
            holder.description.visibility = View.VISIBLE
            holder.description.text = TextFormatUtils.formatParagraphSpacing(item.description)
            holder.description.setLineSpacing(0f, 1.2f)
        } else {
            holder.description.visibility = View.GONE
        }

        // ── Resumo modificadores selecionados (ficha do personagem) ────────
        if (!showDescription && item.isModular && item.selectedModifiers.isNotEmpty()) {
            val names = item.modifiers
                .filter { it.id in item.selectedModifiers }
                .joinToString(", ") { it.name }
            holder.selectedModifiers.text = "▸ $names"
            holder.selectedModifiers.visibility = View.VISIBLE
        } else {
            holder.selectedModifiers.visibility = View.GONE
        }

        // ── Modo modular (lista de seleção) ────────────────────────────────
        val isSelectionMode = onModularItemConfirmed != null

        if (isSelectionMode && item.isModular) {
            holder.containerModifiers.visibility = View.VISIBLE
            holder.containerFooter.visibility = View.VISIBLE
            buildCheckboxes(holder, item)
        } else {
            holder.containerModifiers.visibility = View.GONE
            holder.containerFooter.visibility = View.GONE
            holder.containerModifiers.removeAllViews()
        }

        holder.itemView.findViewById<View>(R.id.btn_delete_item)?.visibility = View.GONE

        // ── Clique ────────────────────────────────────────────────────────
        holder.itemView.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

            if (isSelectionMode && item.isModular) {
                // Item modular no modo de seleção não faz nada ao clicar na linha
                // O usuário deve interagir com os checkboxes e o botão adicionar
            } else {
                onItemClick(item)
            }
        }

        if (onLongClick != null) {
            holder.itemView.setOnLongClickListener {
                onLongClick.invoke(item)
                true
            }
        }
    }

    private fun buildCheckboxes(holder: ViewHolder, item: AdvantageItem) {
        val ctx = holder.containerModifiers.context
        holder.containerModifiers.removeAllViews()

        val selected = pendingSelections.getOrPut(item.id) { mutableSetOf() }

        fun refreshCost() {
            val total = if (item.name.equals("MANOBRAS ESPECIAIS", ignoreCase = true) || 
                            item.name.equals("QUALIDADES ESPECIAIS", ignoreCase = true) ||
                            item.name.equals("SENTIDOS ESPECIAIS", ignoreCase = true) ||
                            item.name.equals("STATUS NEGATIVOS", ignoreCase = true)) {
                item.baseCostPt + Math.ceil(selected.size / 3.0).toInt()
            } else {
                item.baseCostPt + item.modifiers
                    .filter { it.id in selected }
                    .sumOf { it.costPt }
            }
            holder.computedCost.text = if (total >= 0) "$total PT" else "$total PT (desconto)"
        }
        refreshCost()

        for (mod in item.modifiers) {
            val cb = CheckBox(ctx)
            cb.setPadding(0, 16, 0, 16) // Added padding top and bottom (in pixels)
            cb.text = if (mod.description.isNotEmpty()) {
                "${mod.name}: ${mod.description}"
            } else {
                when {
                    mod.costPt > 0 -> "${mod.name}  (+${mod.costPt}PT)"
                    mod.costPt < 0 -> "${mod.name}  (${mod.costPt}PT)"
                    else -> mod.name
                }
            }
            cb.isChecked = mod.id in selected
            cb.setOnCheckedChangeListener { _, checked ->
                if (checked) selected.add(mod.id) else selected.remove(mod.id)
                refreshCost()
            }
            holder.containerModifiers.addView(cb)
        }

        holder.btnAdd.setOnClickListener {
            val confirmed = item.copy(
                selectedModifiers = selected.toList(),
                cost = run {
                    val total = item.baseCostPt + item.modifiers
                        .filter { it.id in selected }
                        .sumOf { it.costPt }
                    "$total PT"
                }
            )
            onModularItemConfirmed?.invoke(confirmed)
            pendingSelections.remove(item.id)
            notifyItemChanged(items.indexOf(item))
        }
    }

    override fun getItemCount() = items.size
}
