package com.galeria.defensores.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
// Actually, let's create a simple inner adapter or separate generic adapter

class ManageDamageTypesDialogFragment(
    private val currentTypes: List<String>,
    private val onAdd: (String) -> Unit,
    private val onRemove: (String) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_manage_damage_types, null)
        
        val editNewType = view.findViewById<EditText>(R.id.edit_new_type)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_type)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_types)
        
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = DamageTypeAdapter(currentTypes, onRemove)
        
        btnAdd.setOnClickListener {
            val newType = editNewType.text.toString().trim()
            if (newType.isNotEmpty()) {
                onAdd(newType)
                editNewType.text.clear()
                dismiss() // Or refresh? For now dismiss to refresh from Fragment
            }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Gerenciar Tipos de Dano")
            .setView(view)
            .setNegativeButton("Fechar", null)
            .create()
    }
}

class DamageTypeAdapter(
    private val types: List<String>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<DamageTypeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textType: TextView = view.findViewById(R.id.text_type_name)
        val btnRemove: ImageButton = view.findViewById(R.id.btn_remove_type)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_damage_type, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val type = types[position]
        holder.textType.text = type
        
        // Prevent removing defaults? Maybe. 
        // Logic should be in ViewModel/Fragment, but here we can visually indicate it.
        // For now, allow remove call, ViewModel handles logic (it handles custom list only).
        
        holder.btnRemove.setOnClickListener {
             onRemove(type)
        }
    }

    override fun getItemCount() = types.size
}
