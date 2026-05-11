package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.galeria.defensores.data.DisadvantagesRepository
import com.galeria.defensores.data.AdvantagesRepository
import com.galeria.defensores.data.GaidenData
import com.galeria.defensores.models.AdvantageItem
import com.galeria.defensores.models.ModifierOption
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.utils.TextFormatUtils
import com.google.android.material.textfield.TextInputEditText

class EditAdvantageDialogFragment(
    private val incomingAdvantage: AdvantageItem?,
    private val onSave: (AdvantageItem) -> Unit,
    private val onDelete: ((AdvantageItem) -> Unit)? = null
) : DialogFragment() {

    private val selectedModifiers = mutableSetOf<String>()
    private var baseCostForModular: Int = 0
    private var referenceModularItem: AdvantageItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_advantage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editName = view.findViewById<TextInputEditText>(R.id.edit_advantage_name)
        val editCost = view.findViewById<TextInputEditText>(R.id.edit_advantage_cost)
        val editDesc = view.findViewById<TextInputEditText>(R.id.edit_advantage_description)
        val btnRemove = view.findViewById<Button>(R.id.btn_remove)
        val btnSave = view.findViewById<View>(R.id.btn_save)
        val btnEdit = view.findViewById<Button>(R.id.btn_edit)
        val titleView = view.findViewById<TextView>(R.id.dialog_title)
        val containerModifiersWrapper = view.findViewById<LinearLayout>(R.id.edit_container_modifiers_wrapper)
        val containerModifiers = view.findViewById<LinearLayout>(R.id.edit_container_modifiers)
        val rgType = view.findViewById<RadioGroup>(R.id.rg_advantage_type)
        val rbCommon = view.findViewById<RadioButton>(R.id.rb_type_common)
        val rbModular = view.findViewById<RadioButton>(R.id.rb_type_modular)
        val btnAddModifier = view.findViewById<Button>(R.id.btn_add_modifier)
        val editBaseCostLayout = view.findViewById<View>(R.id.edit_modular_base_cost_layout)
        val editBaseCost = view.findViewById<TextInputEditText>(R.id.edit_modular_base_cost)
        val tvTotalCostLabel = view.findViewById<TextView>(R.id.tv_total_cost_label)

        var advantage = incomingAdvantage

        // Upgrade legacy item by name if it exists in any active repository as modular
        if (advantage != null && !advantage.isModular) {
            val possibleAdvMatch = AdvantagesRepository.getAllAdvantages().find { it.name.trim().equals(advantage?.name?.trim(), ignoreCase = true) }
            val possibleDisMatch = DisadvantagesRepository.getAllDisadvantages().find { it.name.trim().equals(advantage?.name?.trim(), ignoreCase = true) }
            
            val match = possibleAdvMatch ?: possibleDisMatch
            
            if (match != null && match.isModular) {
                advantage = advantage.copy(
                    isModular = true,
                    modifiers = match.modifiers,
                    baseCostPt = match.baseCostPt
                )
            }
        }

        if (advantage != null) {
            editName.setText(advantage.name)
            editCost.setText(advantage.cost)
            editDesc.setText(com.galeria.defensores.utils.TextFormatUtils.formatParagraphSpacing(advantage.description))
            titleView.text = "Detalhes"
            
            // Read-Only Mode Initially
            editName.isEnabled = false
            editCost.isEnabled = false
            editDesc.isEnabled = false
            btnSave.visibility = View.GONE
            btnRemove.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE

            if (advantage.isModular) {
                referenceModularItem = advantage
                baseCostForModular = advantage.baseCostPt
                selectedModifiers.addAll(advantage.selectedModifiers)
                containerModifiersWrapper.visibility = View.VISIBLE
                editBaseCostLayout.visibility = View.VISIBLE
                tvTotalCostLabel.visibility = View.VISIBLE
                rbModular.isChecked = true
                editCost.visibility = View.GONE
                editBaseCost.setText(baseCostForModular.toString())
                editBaseCost.isEnabled = false
                buildCheckboxes(containerModifiers, false)
            } else {
                rbCommon.isChecked = true
                containerModifiersWrapper.visibility = View.GONE
                editBaseCostLayout.visibility = View.GONE
                tvTotalCostLabel.visibility = View.GONE
                editCost.visibility = View.VISIBLE
            }
            
            // Fixed system items cannot change type
            rbCommon.isEnabled = false
            rbModular.isEnabled = false
        } else {
            titleView.text = "Nova Vantagem"
            btnRemove.visibility = View.GONE
            btnEdit.visibility = View.GONE
            // Default Edit Mode
            editName.isEnabled = true
            editCost.isEnabled = true
            editDesc.isEnabled = true
            btnSave.visibility = View.VISIBLE
            
            rgType.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId == R.id.rb_type_modular) {
                    containerModifiersWrapper.visibility = View.VISIBLE
                    editBaseCostLayout.visibility = View.VISIBLE
                    tvTotalCostLabel.visibility = View.VISIBLE
                    editCost.visibility = View.GONE
                    if (referenceModularItem == null) {
                        referenceModularItem = AdvantageItem(
                            id = java.util.UUID.randomUUID().toString(),
                            name = editName.text.toString(),
                            cost = "0 pontos",
                            description = "",
                            isModular = true,
                            modifiers = mutableListOf()
                        )
                    }
                    buildCheckboxes(containerModifiers, true)
                } else {
                    containerModifiersWrapper.visibility = View.GONE
                    editBaseCostLayout.visibility = View.GONE
                    tvTotalCostLabel.visibility = View.GONE
                    editCost.visibility = View.VISIBLE
                }
            }
        }

        editBaseCost.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                baseCostForModular = s?.toString()?.toIntOrNull() ?: 0
                buildCheckboxes(containerModifiers, editName.isEnabled) // Trigger updateCostField
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnAddModifier.setOnClickListener {
            showAddModifierDialog(containerModifiers)
        }

        editDesc.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s != null && editDesc.hasFocus()) {
                    com.galeria.defensores.utils.TextFormatUtils.applyParagraphSpacingToEditable(s)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        if (onDelete == null) {
            btnRemove.visibility = View.GONE
        }

        btnEdit.setOnClickListener {
            editName.isEnabled = true
            editBaseCost.isEnabled = true
            editCost.isEnabled = true
            editDesc.isEnabled = true
            btnSave.visibility = View.VISIBLE
            btnEdit.visibility = View.GONE
            if (onDelete != null) btnRemove.visibility = View.VISIBLE
            titleView.text = "Editar Vantagem"

            if (referenceModularItem != null) {
                buildCheckboxes(containerModifiers, true)
            }
        }

        btnRemove.setOnClickListener {
            if (advantage != null) {
                onDelete?.invoke(advantage)
                dismiss()
            }
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val isModularNow = rbModular.isChecked
            val costStrValue = if (isModularNow) {
                val totalCost = calculateCurrentTotalCost()
                val ptLabel = if (Math.abs(totalCost) == 1) "ponto" else "pontos"
                "$totalCost $ptLabel"
            } else {
                editCost.text.toString()
            }
            
            val desc = com.galeria.defensores.utils.TextFormatUtils.cleanParagraphSpacing(editDesc.text.toString())

            if (name.isNotBlank()) {
                val newItem = if (isModularNow && referenceModularItem != null) {
                    referenceModularItem!!.copy(
                        name = name,
                        cost = costStrValue,
                        description = desc,
                        isModular = true,
                        baseCostPt = baseCostForModular,
                        selectedModifiers = selectedModifiers.toList(),
                        id = advantage?.id ?: java.util.UUID.randomUUID().toString()
                    )
                } else {
                    // Force non-modular if common is selected
                    advantage?.copy(
                        name = name,
                        cost = costStrValue, 
                        description = desc,
                        isModular = false,
                        modifiers = emptyList(),
                        selectedModifiers = emptyList()
                    ) ?: AdvantageItem(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        cost = costStrValue,
                        description = desc,
                        isModular = false
                    )
                }
                onSave(newItem)
                dismiss()
            } else {
                 editName.error = "Nome é obrigatório"
            }
        }
    }

    private fun calculateCurrentTotalCost(): Int {
        val item = referenceModularItem ?: return 0
        return if (item.name.equals("MANOBRAS ESPECIAIS", ignoreCase = true) || 
                        item.name.equals("QUALIDADES ESPECIAIS", ignoreCase = true) ||
                        item.name.equals("SENTIDOS ESPECIAIS", ignoreCase = true) ||
                        item.name.equals("STATUS NEGATIVOS", ignoreCase = true)) {
            baseCostForModular + Math.ceil(selectedModifiers.size / 3.0).toInt()
        } else {
            baseCostForModular + item.modifiers
                .filter { it.id in selectedModifiers }
                .sumOf { it.costPt }
        }
    }

    private fun buildCheckboxes(container: LinearLayout, isEditable: Boolean) {
        val item = referenceModularItem ?: return
        container.removeAllViews()
        val ctx = container.context

        fun updateCostField() {
            val total = calculateCurrentTotalCost()
            val ptLabel = if (Math.abs(total) == 1) "ponto" else "pontos"
            view?.findViewById<TextView>(R.id.tv_total_cost_label)?.text = "Custo Total: $total $ptLabel"
        }

        for (mod in item.modifiers) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val cb = CheckBox(ctx)
            cb.setPadding(0, 16, 0, 16)
            cb.text = if (mod.description.isNotEmpty()) {
                "${mod.name}: ${mod.description}"
            } else {
                when {
                    mod.costPt > 0 -> {
                        val ptLabel = if (Math.abs(mod.costPt) == 1) "ponto" else "pontos"
                        "${mod.name}  (+${mod.costPt} $ptLabel)"
                    }
                    mod.costPt < 0 -> {
                        val ptLabel = if (Math.abs(mod.costPt) == 1) "ponto" else "pontos"
                        "${mod.name}  (${mod.costPt} $ptLabel)"
                    }
                    else -> mod.name
                }
            }
            cb.isChecked = mod.id in selectedModifiers
            cb.isEnabled = isEditable
            
            cb.setOnCheckedChangeListener { _, checked ->
                if (checked) selectedModifiers.add(mod.id) else selectedModifiers.remove(mod.id)
                updateCostField()
            }
            
            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            row.addView(cb, params)

            if (isEditable) {
                val btnDelete = Button(ctx, null, 0, androidx.appcompat.R.style.Widget_AppCompat_Button_Borderless).apply {
                    text = "X"
                    setTextColor(android.graphics.Color.RED)
                    setOnClickListener {
                        val currentList = referenceModularItem?.modifiers?.toMutableList() ?: mutableListOf()
                        currentList.remove(mod)
                        selectedModifiers.remove(mod.id)
                        referenceModularItem = referenceModularItem?.copy(modifiers = currentList)
                        buildCheckboxes(container, true)
                        updateCostField()
                    }
                }
                row.addView(btnDelete, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }

            container.addView(row)
        }
    }

    private fun showAddModifierDialog(container: LinearLayout) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_modifier, null)
        val editModName = dialogView.findViewById<TextInputEditText>(R.id.edit_modifier_name)
        val editModCost = dialogView.findViewById<TextInputEditText>(R.id.edit_modifier_cost)
        val editModDesc = dialogView.findViewById<TextInputEditText>(R.id.edit_modifier_description)

        builder.setView(dialogView)
            .setTitle("Adicionar Opção")
            .setPositiveButton("Adicionar") { _, _ ->
                val mName = editModName.text.toString()
                val mCost = editModCost.text.toString().toIntOrNull() ?: 0
                val mDesc = editModDesc.text.toString()

                if (mName.isNotBlank()) {
                    val newMod = ModifierOption(
                        id = "custom_${java.util.UUID.randomUUID()}",
                        name = mName,
                        costPt = mCost,
                        description = mDesc
                    )
                    val currentList = referenceModularItem?.modifiers?.toMutableList() ?: mutableListOf()
                    currentList.add(newMod)
                    referenceModularItem = referenceModularItem?.copy(modifiers = currentList)
                    buildCheckboxes(container, true)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
