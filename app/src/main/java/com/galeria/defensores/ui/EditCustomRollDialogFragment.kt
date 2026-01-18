package com.galeria.defensores.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.CustomRoll
import com.galeria.defensores.models.RollComponent

class EditCustomRollDialogFragment(
    private val existingRoll: CustomRoll? = null,
    private val onSave: (CustomRoll) -> Unit,
    private val onDelete: ((CustomRoll) -> Unit)? = null
) : DialogFragment() {

    private val components = mutableListOf<RollComponent>()
    private lateinit var componentsAdapter: ComponentsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_custom_roll, null)
        
        // ... (Binding) ...
        val editName = view.findViewById<EditText>(R.id.edit_roll_name)
        val editDesc = view.findViewById<EditText>(R.id.edit_roll_description)
        val editGlobalMod = view.findViewById<EditText>(R.id.edit_global_modifier)
        val spinnerPrimary = view.findViewById<Spinner>(R.id.spinner_primary_attr)
        val spinnerSecondary = view.findViewById<Spinner>(R.id.spinner_secondary_attr)
        val checkAccumulate = view.findViewById<CheckBox>(R.id.check_accumulate_crit)
        
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_roll_components)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_component)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete_roll)

        // Map UI String -> Internal Key
        val attrMap = mapOf(
            "Nenhum" to "none",
            "Força" to "forca",
            "Habilidade" to "habilidade",
            "Resistência" to "resistencia",
            "Armadura" to "armadura",
            "Poder de Fogo" to "poderFogo"
        )
        val attrKeys = attrMap.values.toList()
        
        if (existingRoll != null) {
            editName.setText(existingRoll.name)
            editDesc.setText(existingRoll.description)
            editGlobalMod.setText(existingRoll.globalModifier.toString())
            
            // Populate components
            components.clear()
            components.addAll(existingRoll.components.map { it.copy() }) // Use copies to avoid direct mutation until save
            
            // Set Spinners
            val pIndex = attrKeys.indexOf(existingRoll.primaryAttribute).coerceAtLeast(0)
            spinnerPrimary.setSelection(pIndex)
            
            val sIndex = attrKeys.indexOf(existingRoll.secondaryAttribute).coerceAtLeast(0)
            spinnerSecondary.setSelection(sIndex)
            
            checkAccumulate.isChecked = existingRoll.accumulateCrit
            
             if (onDelete != null) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Excluir Rolagem?")
                        .setMessage("Tem certeza que deseja excluir '${existingRoll.name}'?")
                        .setPositiveButton("Excluir") { _, _ ->
                            onDelete.invoke(existingRoll)
                            dismiss()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            } else {
                btnDelete.visibility = View.GONE
            }
        } else {
            // New Roll - No Delete Option
            btnDelete.visibility = View.GONE
            if (components.isEmpty()) {
                components.add(RollComponent())
            }
        }

        // ... (Recycler Setup) ...
        componentsAdapter = ComponentsAdapter(components)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = componentsAdapter

        btnAdd.setOnClickListener {
            components.add(RollComponent())
            componentsAdapter.notifyItemInserted(components.size - 1)
        }
        
        // ... (Dialog Builder with Save) ...
        return AlertDialog.Builder(requireContext())
            // ...
            .setTitle(if (existingRoll != null) "Editar Rolagem" else "Nova Rolagem")
            .setView(view)
            .setPositiveButton("Salvar") { _, _ ->
                val name = editName.text.toString().takeIf { it.isNotBlank() } ?: "Rolagem Sem Nome"
                val desc = editDesc.text.toString()
                val globalMod = editGlobalMod.text.toString().toIntOrNull() ?: 0
                
                val pAttr = attrKeys[spinnerPrimary.selectedItemPosition]
                val sAttr = attrKeys[spinnerSecondary.selectedItemPosition]
                val accumulate = checkAccumulate.isChecked

                val newRoll = existingRoll?.copy(
                    name = name,
                    description = desc,
                    components = components,
                    globalModifier = globalMod,
                    primaryAttribute = pAttr,
                    secondaryAttribute = sAttr,
                    accumulateCrit = accumulate
                ) ?: CustomRoll(
                    name = name,
                    description = desc,
                    components = components,
                    globalModifier = globalMod,
                    primaryAttribute = pAttr,
                    secondaryAttribute = sAttr,
                    accumulateCrit = accumulate
                )
                onSave(newRoll)
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    inner class ComponentsAdapter(private val items: MutableList<RollComponent>) : 
        RecyclerView.Adapter<ComponentsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val editCount: EditText = view.findViewById(R.id.edit_dice_count)
            val spinnerFaces: Spinner = view.findViewById(R.id.spinner_dice_faces)
            val editBonus: EditText = view.findViewById(R.id.edit_dice_bonus)
            val btnRemove: ImageButton = view.findViewById(R.id.btn_remove_component)
            val checkNegative: CheckBox = view.findViewById(R.id.check_negative)
            val checkCanCrit: CheckBox = view.findViewById(R.id.check_can_crit)
            val editCritRange: EditText = view.findViewById(R.id.edit_crit_range)
            val editCritMult: EditText = view.findViewById(R.id.edit_crit_mult)
            val layoutCrit: View = view.findViewById(R.id.layout_crit_details)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_roll_component, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            // Bind values (Watchers needed for saving typing)
            // THIS IS TRICKY in RecyclerView. To simplify, we read values on Save? 
            // NO, `items` passed to onSave are the same reference. 
            // But we need to update the object when user types.

            // Clear previous listeners to avoid recursion/wrong index
            // Ideally use doOnTextChanged extension or simple TextWatcher but correct position
            
            holder.editCount.setText(item.count.toString())
            holder.editBonus.setText(item.bonus.toString())
            holder.checkNegative.isChecked = item.isNegative
            holder.checkCanCrit.isChecked = item.canCrit
            
            holder.editCritRange.setText(item.critRangeStart?.toString() ?: "")
            holder.editCritMult.setText(item.critMultiplier.toString())

            holder.layoutCrit.visibility = if (item.canCrit) View.VISIBLE else View.GONE
            
            // Text Watchers (Simplified for brevity, ensure robust in prod)
            holder.editCount.setOnFocusChangeListener { _, hasFocus -> 
                if (!hasFocus) item.count = holder.editCount.text.toString().toIntOrNull() ?: 1
            }
            holder.editBonus.setOnFocusChangeListener { _, hasFocus -> 
                if (!hasFocus) item.bonus = holder.editBonus.text.toString().toIntOrNull() ?: 0
            }
            
            holder.checkNegative.setOnCheckedChangeListener { _, isChecked -> item.isNegative = isChecked }
            
            holder.checkCanCrit.setOnCheckedChangeListener { _, isChecked -> 
                item.canCrit = isChecked
                holder.layoutCrit.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            holder.editCritRange.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) item.critRangeStart = holder.editCritRange.text.toString().toIntOrNull()
            }
            
             holder.editCritMult.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) item.critMultiplier = holder.editCritMult.text.toString().toIntOrNull() ?: 2
            }

            // Spinner Faces
            val facesList = holder.itemView.context.resources.getStringArray(R.array.dice_faces_array).toList()
            val faceIndex = facesList.indexOf(item.faces.toString())
            if (faceIndex >= 0) holder.spinnerFaces.setSelection(faceIndex)
            
            holder.spinnerFaces.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                     items[holder.adapterPosition].faces = facesList[pos].toInt()
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            holder.btnRemove.setOnClickListener {
                if (items.size > 1) { // Prevent empty
                    items.removeAt(holder.adapterPosition)
                    notifyItemRemoved(holder.adapterPosition)
                } else {
                    Toast.makeText(requireContext(), "Deve ter pelo menos 1 componente", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
