package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.Table

class TablesAdapter(
    private val tables: List<Table>,
    private val onTableClick: (Table) -> Unit,
    private val onEditClick: (Table) -> Unit,
    private val onDeleteClick: (Table) -> Unit,
    private val onExportClick: (Table) -> Unit // New callback
) : RecyclerView.Adapter<TablesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_table_name)
        val description: TextView = view.findViewById(R.id.text_table_description)
        val btnMore: ImageButton = view.findViewById(R.id.btn_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_table, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val table = tables[position]
        holder.name.text = table.name
        holder.description.text = table.description
        
        // Always show options in offline/single-user mode
        holder.btnMore.visibility = View.VISIBLE
        holder.btnMore.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(view.context, view)
            popup.menu.add("Editar")
            popup.menu.add("Exportar") // New Option
            popup.menu.add("Excluir")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Editar" -> onEditClick(table)
                    "Exportar" -> onExportClick(table)
                    "Excluir" -> onDeleteClick(table)
                }
                true
            }
            popup.show()
        }
        
        holder.itemView.setOnClickListener { onTableClick(table) }
    }

    override fun getItemCount() = tables.size
}
