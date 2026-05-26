package com.galeria.defensores.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.RollType
import com.galeria.defensores.models.RuleSystem
import com.galeria.defensores.utils.TextFormatUtils
import com.galeria.defensores.viewmodels.RollViewModel
import com.galeria.defensores.viewmodels.SystemManagementViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SystemManagementFragment : Fragment() {

    private val viewModel: SystemManagementViewModel by viewModels()
    private val rollViewModel: RollViewModel by viewModels()

    private lateinit var spinner: Spinner
    private lateinit var btnSave: ImageButton
    private lateinit var btnRevert: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnExport: ImageButton
    private lateinit var btnImport: ImageButton
    private lateinit var btnBack: ImageButton

    // Metadata controls
    private lateinit var editSysName: EditText
    private lateinit var editSysDesc: EditText
    private lateinit var textFormulaValidation: TextView

    // Sandbox sheet sub-views
    private lateinit var sheetView: View
    private lateinit var nameEdit: EditText
    private lateinit var advantagesAdapter: AdvantagesAdapter
    private lateinit var disadvantagesAdapter: AdvantagesAdapter
    private lateinit var skillsAdapter: AdvantagesAdapter
    private lateinit var specializationsAdapter: AdvantagesAdapter
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var spellsAdapter: SpellsAdapter
    private lateinit var customRollsAdapter: CustomRollsAdapter
    private lateinit var resourcesAdapter: ResourcesAdapter

    private var adaptersInitialized = false
    private var spinnerInitialized = false

    private val systemExportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = viewModel.exportSystemJson()
            lifecycleScope.launch {
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    Toast.makeText(context, "Sistema exportado!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val systemImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                try {
                    val json = requireContext().contentResolver.openInputStream(uri)
                        ?.use { it.bufferedReader().readText() }
                    if (json != null) {
                        viewModel.importSystemJson(json) { success, system ->
                            if (success && system != null) {
                                Toast.makeText(context, "Sistema importado: ${system.name}", Toast.LENGTH_SHORT).show()
                                adaptersInitialized = false // Force reinit for new system structure
                            } else {
                                Toast.makeText(context, "Conteúdo JSON inválido.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao ler arquivo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_system_management, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Top bar controls
        btnBack   = view.findViewById(R.id.btn_back)
        btnSave   = view.findViewById(R.id.btn_save_system)
        btnRevert = view.findViewById(R.id.btn_revert_system)
        btnExport = view.findViewById(R.id.btn_export_system)
        btnImport = view.findViewById(R.id.btn_import_system)
        btnDelete = view.findViewById(R.id.btn_delete_system)
        spinner   = view.findViewById(R.id.spinner_rule_systems)

        // The included character-sheet layout
        sheetView = view.findViewById(R.id.sandbox_sheet)

        // Hide the sheet's own header — we replace it with our management bar
        sheetView.findViewById<View>(R.id.layout_sheet_header).visibility = View.GONE

        // Disable avatar editing (not meaningful in sandbox)
        sheetView.findViewById<View>(R.id.img_edit_avatar_icon).visibility = View.GONE
        sheetView.findViewById<View>(R.id.img_character_avatar).isEnabled = false

        // Hide character-specific actions that don't apply in sandbox
        sheetView.findViewById<View>(R.id.btn_export_character).visibility = View.GONE
        sheetView.findViewById<View>(R.id.btn_delete_character).visibility = View.GONE

        // Set all add buttons visible in system management screen (sandbox)
        sheetView.findViewById<View>(R.id.btn_add_attribute).visibility = View.VISIBLE
        sheetView.findViewById<View>(R.id.btn_add_resource).visibility = View.VISIBLE
        sheetView.findViewById<View>(R.id.btn_add_spell).visibility = View.VISIBLE
        sheetView.findViewById<View>(R.id.btn_add_custom_roll).visibility = View.VISIBLE

        // Initialize metadata controls
        val metadataLayout = view.findViewById<View>(R.id.layout_system_metadata)
        editSysName = metadataLayout.findViewById(R.id.edit_system_name)
        editSysDesc = metadataLayout.findViewById(R.id.edit_system_description)
        textFormulaValidation = metadataLayout.findViewById(R.id.text_formula_validation)

        val metadataWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val sysName = editSysName.text.toString().trim()
                val sysDesc = editSysDesc.text.toString().trim()
                if (editSysName.hasFocus() || editSysDesc.hasFocus()) {
                    viewModel.updateMetadata(sysName, sysDesc)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        editSysName.addTextChangedListener(metadataWatcher)
        editSysDesc.addTextChangedListener(metadataWatcher)

        nameEdit = sheetView.findViewById(R.id.edit_char_name)
        nameEdit.hint = "Nome do Personagem de Teste"
        nameEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (nameEdit.hasFocus()) viewModel.updateName(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setupTopBarListeners()
        setupSheetButtonListeners()
        setupFlowCollectors()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top Bar
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupTopBarListeners() {
        btnBack.setOnClickListener {
            if (viewModel.isDirty.value) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Alterações não salvas")
                    .setMessage("Você tem modificações nas regras não salvas. Deseja sair sem salvar?")
                    .setPositiveButton("Sair sem salvar") { _, _ -> parentFragmentManager.popBackStack() }
                    .setNegativeButton("Continuar editando", null)
                    .show()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        btnSave.setOnClickListener {
            val system = viewModel.selectedSystem.value ?: return@setOnClickListener
            if (system.isBaseSystem) {
                // Prompt for "Save As" since base systems are protected
                showSaveAsDialog()
            } else {
                viewModel.saveSystem { success, errorMsg ->
                    if (success) {
                        Toast.makeText(context, "Sistema salvo!", Toast.LENGTH_SHORT).show()
                        updateSaveButtonState(false)
                    } else {
                        Toast.makeText(context, errorMsg ?: "Erro ao salvar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnRevert.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Descartar Alterações?")
                .setMessage("Isso irá reverter todas as mudanças feitas nas regras e limpar a ficha de teste.")
                .setPositiveButton("Descartar") { _, _ ->
                    viewModel.revertSystem()
                    adaptersInitialized = false
                    Toast.makeText(context, "Alterações descartadas.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnExport.setOnClickListener {
            val sysName = viewModel.selectedSystem.value?.name ?: "sistema"
            val safeName = sysName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            systemExportLauncher.launch("system_${safeName}.json")
        }

        btnImport.setOnClickListener {
            if (viewModel.isDirty.value) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Importar Sistema?")
                    .setMessage("A importação substituirá o rascunho atual. Alterações não salvas serão perdidas.")
                    .setPositiveButton("Importar") { _, _ ->
                        systemImportLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                systemImportLauncher.launch(arrayOf("application/json", "application/octet-stream"))
            }
        }

        btnDelete.setOnClickListener {
            val system = viewModel.selectedSystem.value ?: return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("Excluir '${system.name}'?")
                .setMessage("Esta ação não pode ser desfeita. O sistema será removido permanentemente.")
                .setPositiveButton("Excluir") { _, _ ->
                    viewModel.deleteSystem { success, errorMsg ->
                        if (success) {
                            Toast.makeText(context, "Sistema excluído.", Toast.LENGTH_SHORT).show()
                            adaptersInitialized = false
                        } else {
                            Toast.makeText(context, errorMsg ?: "Erro ao excluir.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showSaveAsDialog() {
        val context = requireContext()
        val input = EditText(context)
        val suggested = viewModel.selectedSystem.value?.name ?: ""
        input.setText(if (suggested.endsWith("(Cópia)")) suggested else "$suggested (Cópia)")
        input.selectAll()

        AlertDialog.Builder(context)
            .setTitle("Salvar Como Novo Sistema")
            .setMessage("Informe um nome para o novo sistema customizado:")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isBlank()) {
                    Toast.makeText(context, "O nome não pode estar vazio.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.saveSystem(newName = newName) { success, errorMsg ->
                    if (success) {
                        Toast.makeText(context, "Sistema salvo como '$newName'!", Toast.LENGTH_SHORT).show()
                        adaptersInitialized = false
                    } else {
                        Toast.makeText(context, errorMsg ?: "Erro ao salvar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateSaveButtonState(isDirty: Boolean) {
        val tint = if (isDirty) "#34D399" else "#888888"
        btnSave.setColorFilter(Color.parseColor(tint))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sandbox Sheet Button Listeners
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupSheetButtonListeners() {
        // Attribute structure editing
        sheetView.findViewById<Button>(R.id.btn_add_attribute).setOnClickListener {
            val sys = viewModel.selectedSystem.value
            if (sys != null && sys.isBaseSystem) {
                showBaseSystemWarning()
                return@setOnClickListener
            }
            DialogEditAttributeDefinition(null, { newAttr ->
                viewModel.addAttributeDefinition(newAttr)
            }).show(parentFragmentManager, "SM_AddAttr")
        }

        sheetView.findViewById<Button>(R.id.btn_add_resource).setOnClickListener {
            val sys = viewModel.selectedSystem.value
            if (sys != null && sys.isBaseSystem) {
                showBaseSystemWarning()
                return@setOnClickListener
            }
            DialogEditResourceDefinition(null, { newRes ->
                viewModel.addResourceDefinition(newRes)
            }).show(parentFragmentManager, "SM_AddRes")
        }

        // Trait add buttons
        sheetView.findViewById<Button>(R.id.btn_add_advantage).setOnClickListener {
            SelectAdvantageDialogFragment { viewModel.addAdvantage(it) }
                .show(parentFragmentManager, "SM_AddAdv")
        }

        sheetView.findViewById<Button>(R.id.btn_add_disadvantage).setOnClickListener {
            SelectDisadvantageDialogFragment { viewModel.addDisadvantage(it) }
                .show(parentFragmentManager, "SM_AddDis")
        }

        sheetView.findViewById<Button>(R.id.btn_add_skill).setOnClickListener {
            SelectSkillDialogFragment { viewModel.addSkill(it) }
                .show(parentFragmentManager, "SM_AddSkill")
        }

        sheetView.findViewById<Button>(R.id.btn_add_specialization).setOnClickListener {
            MultiSelectSpecializationDialogFragment { viewModel.addSpecializations(it) }
                .show(parentFragmentManager, "SM_AddSpec")
        }

        sheetView.findViewById<Button>(R.id.btn_add_spell).setOnClickListener {
            EditSpellDialogFragment(null, { viewModel.addSpell(it) })
                .show(parentFragmentManager, "SM_AddSpell")
        }

        sheetView.findViewById<Button>(R.id.btn_add_inventory).setOnClickListener {
            EditInventoryItemDialogFragment(null, { viewModel.addInventoryItem(it) })
                .show(parentFragmentManager, "SM_AddInv")
        }

        // Score buttons
        sheetView.findViewById<Button>(R.id.btn_minus_saved).setOnClickListener {
            val current = viewModel.sandboxCharacter.value?.savedPoints ?: 0
            if (current > 0) viewModel.updateSavedPoints(current - 1)
        }
        sheetView.findViewById<Button>(R.id.btn_plus_saved).setOnClickListener {
            val current = viewModel.sandboxCharacter.value?.savedPoints ?: 0
            viewModel.updateSavedPoints(current + 1)
        }
        sheetView.findViewById<Button>(R.id.btn_minus_xp).setOnClickListener {
            val current = viewModel.sandboxCharacter.value?.experience ?: 0
            if (current > 0) viewModel.updateExperience(current - 1)
        }
        sheetView.findViewById<Button>(R.id.btn_plus_xp).setOnClickListener {
            val current = viewModel.sandboxCharacter.value?.experience ?: 0
            viewModel.updateExperience(current + 1)
        }

        // Scale picker
        sheetView.findViewById<TextView>(R.id.text_scale).setOnClickListener {
            val scales = arrayOf("Ningen (x1)", "Sugoi (x10)", "Kiodai (x100)", "Kami (x1000)")
            val current = viewModel.sandboxCharacter.value?.scale ?: 0
            AlertDialog.Builder(requireContext())
                .setTitle("Alterar Escala de Poder")
                .setSingleChoiceItems(scales, current) { dialog, which ->
                    viewModel.updateScale(which)
                    dialog.dismiss()
                }
                .show()
        }

        // Damage type management (sandbox uses no tableId — defaults)
        sheetView.findViewById<View>(R.id.btn_manage_damage_types).setOnClickListener {
            val sys = viewModel.selectedSystem.value ?: return@setOnClickListener
            if (sys.isBaseSystem) {
                showBaseSystemWarning()
                return@setOnClickListener
            }
            ManageDamageTypesDialogFragment(
                availableTypesFlow = viewModel.damageTypes,
                onAdd = { viewModel.addDamageType(it) },
                onRemove = { viewModel.removeDamageType(it) }
            ).show(parentFragmentManager, "SM_ManageDamageTypes")
        }

        // Roll buttons delegate to rollViewModel (reads SharedCharacterState updated by SystemManagementViewModel)
        sheetView.findViewById<Button>(R.id.btn_attack_f).setOnClickListener {
            rollViewModel.rollDice(RollType.ATTACK_F)
        }
        sheetView.findViewById<Button>(R.id.btn_attack_pdf).setOnClickListener {
            rollViewModel.rollDice(RollType.ATTACK_PDF)
        }
        sheetView.findViewById<Button>(R.id.btn_defense).setOnClickListener {
            rollViewModel.rollDice(RollType.DEFENSE)
        }
        sheetView.findViewById<Button>(R.id.btn_initiative).setOnClickListener {
            rollViewModel.rollDice(RollType.INITIATIVE)
        }

        // Virtual dice result listener
        parentFragmentManager.setFragmentResultListener(
            VirtualDiceFragment.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val diceValues = bundle.getIntegerArrayList("diceValues")?.toList() ?: emptyList()
            rollViewModel.finalizeVirtualRoll(diceValues)
        }

        // Notes
        val notesEdit = sheetView.findViewById<EditText>(R.id.edit_notes)
        notesEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && notesEdit.hasFocus()) TextFormatUtils.applyParagraphSpacingToEditable(s)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        notesEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val html = androidx.core.text.HtmlCompat.toHtml(
                    notesEdit.text as android.text.Spanned,
                    androidx.core.text.HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
                )
                viewModel.updateNotes(html)
            }
        }

        // Custom rolls
        sheetView.findViewById<RecyclerView>(R.id.recycler_custom_rolls)
            .layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)

        sheetView.findViewById<Button>(R.id.btn_add_custom_roll).setOnClickListener {
            EditCustomRollDialogFragment(null, { viewModel.addCustomRoll(it) }, null)
                .show(parentFragmentManager, "SM_AddCustomRoll")
        }
    }

    private fun showBaseSystemWarning() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sistema protegido")
            .setMessage("O sistema '${viewModel.selectedSystem.value?.name}' é um sistema base e não pode ser editado diretamente.\n\nClique em 'Salvar' para criar uma cópia editável.")
            .setPositiveButton("Salvar como cópia") { _, _ -> showSaveAsDialog() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Flow Collectors
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupFlowCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Systems list → populate spinner
                launch {
                    viewModel.systems.collect { systems ->
                        if (systems.isEmpty()) return@collect
                        updateSpinner(systems)
                        // Load first system if nothing is selected yet
                        if (viewModel.selectedSystem.value == null) {
                            viewModel.loadSystem(systems.first())
                        }
                    }
                }

                // 2. Selected system changes → update rule structure in sheet
                launch {
                    viewModel.selectedSystem.collect { system ->
                        if (system == null) return@collect

                        // Update system name in the sheet's header label (it's hidden but let's keep data consistent)
                        sheetView.findViewById<TextView>(R.id.text_system_name).text = system.name.uppercase()

                        // Update system metadata text fields
                        if (!editSysName.hasFocus()) {
                            editSysName.setText(system.name)
                        }
                        if (!editSysDesc.hasFocus()) {
                            editSysDesc.setText(system.description)
                        }

                        // Re-render attribute views when structure changes
                        renderAttributeStructure(system)

                        // Update delete button visibility
                        btnDelete.visibility = if (system.isBaseSystem) View.GONE else View.VISIBLE

                        // Update dirty indicator
                        updateSaveButtonState(viewModel.isDirty.value)

                        // Formula validation feedback
                        val err = viewModel.formulaValidationError.value
                        if (err != null) {
                            Toast.makeText(context, "⚠ $err", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                // 3. Dirty state → update save button tint
                launch {
                    viewModel.isDirty.collect { dirty ->
                        updateSaveButtonState(dirty)
                    }
                }

                // 4. Formula validation feedback
                launch {
                    viewModel.formulaValidationError.collect { error ->
                        if (error != null) {
                            textFormulaValidation.visibility = View.VISIBLE
                            textFormulaValidation.text = "⚠ $error"
                            textFormulaValidation.setTextColor(Color.parseColor("#F87171")) // soft red
                        } else {
                            textFormulaValidation.visibility = View.VISIBLE
                            textFormulaValidation.text = "✓ Validação de Fórmulas: OK"
                            textFormulaValidation.setTextColor(Color.parseColor("#34D399")) // soft green
                        }
                    }
                }

                // 5. Sandbox character state
                launch {
                    viewModel.sandboxCharacter.collect { char ->
                        if (char == null) return@collect
                        val system = viewModel.selectedSystem.value ?: return@collect

                        // Basic info
                        if (!nameEdit.hasFocus()) nameEdit.setText(char.name)
                        val scaleName = when (char.scale) {
                            1 -> "Sugoi (x10)"; 2 -> "Kiodai (x100)"; 3 -> "Kami (x1000)"; else -> "Ningen (x1)"
                        }
                        sheetView.findViewById<TextView>(R.id.text_scale).text = "Escala: $scaleName"
                        sheetView.findViewById<TextView>(R.id.text_score_value).text = char.calculateScore().toString()
                        sheetView.findViewById<TextView>(R.id.text_saved_points).text = char.savedPoints.toString()
                        sheetView.findViewById<TextView>(R.id.text_experience).text = char.experience.toString()

                        // Unique advantage
                        val uaCard = sheetView.findViewById<View>(R.id.card_unique_advantage)
                        val btnSelectUA = sheetView.findViewById<Button>(R.id.btn_select_ua)
                        val showSelect = char.uniqueAdvantage == null
                        btnSelectUA.text = if (showSelect) "Adicionar Vantagem Única" else "Trocar Vantagem"
                        uaCard.visibility = if (showSelect) View.GONE else View.VISIBLE
                        btnSelectUA.setOnClickListener {
                            val available = com.galeria.defensores.data.UniqueAdvantagesData.defaults + viewModel.customUniqueAdvantages.value
                            SelectUniqueAdvantageDialogFragment(
                                availableUAs = available,
                                canManage = true,
                                onSelect = { viewModel.setUniqueAdvantage(it) },
                                onAddCustom = { viewModel.addCustomUniqueAdvantage(it) },
                                onEditCustom = { old, new -> viewModel.updateCustomUniqueAdvantage(old, new) },
                                onDeleteCustom = { viewModel.removeCustomUniqueAdvantage(it) }
                            ).show(parentFragmentManager, "SM_SelectUA")
                        }
                        if (char.uniqueAdvantage != null) {
                            val ua = char.uniqueAdvantage!!
                            uaCard.findViewById<TextView>(R.id.text_ua_name).text = ua.name
                            uaCard.findViewById<TextView>(R.id.text_ua_cost).text =
                                if (ua.cost < 0) ua.cost.toString() else "+${ua.cost}"
                            uaCard.findViewById<TextView>(R.id.text_ua_description).text =
                                "Benefícios: ${ua.benefits}\nFraquezas: ${ua.weaknesses}"
                            uaCard.setOnClickListener {
                                EditUniqueAdvantageDialogFragment(
                                    ua = ua,
                                    onSave = { viewModel.setUniqueAdvantage(it) },
                                    onDelete = { viewModel.setUniqueAdvantage(null) }
                                ).show(parentFragmentManager, "SM_EditUA")
                            }
                        }

                        // Attribute values
                        updateAttributeValues(char)

                        // Adapters
                        if (!adaptersInitialized) {
                            setupRecyclerAdapters()
                            adaptersInitialized = true
                        }
                        updateAdapters(char, system)

                        // Notes
                        val notesEdit = sheetView.findViewById<EditText>(R.id.edit_notes)
                        if (!notesEdit.hasFocus()) {
                            notesEdit.setText(
                                androidx.core.text.HtmlCompat.fromHtml(
                                    char.anotacoes,
                                    androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
                                )
                            )
                        }
                    }
                }

                // 6. Damage type spinners in sandbox
                launch {
                    viewModel.damageTypes.collect { damageTypes ->
                        if (damageTypes.isEmpty()) return@collect
                        val spinnerF = sheetView.findViewById<android.widget.Spinner>(R.id.spinner_damage_forca)
                        val spinnerPdf = sheetView.findViewById<android.widget.Spinner>(R.id.spinner_damage_pdf)
                        val char = viewModel.sandboxCharacter.value

                        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, damageTypes)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                        spinnerF.onItemSelectedListener = null
                        spinnerPdf.onItemSelectedListener = null
                        spinnerF.adapter = adapter
                        spinnerPdf.adapter = adapter

                        val indexF = damageTypes.indexOf(char?.damageTypeForca).coerceAtLeast(0)
                        val indexP = damageTypes.indexOf(char?.damageTypePdf).coerceAtLeast(0)
                        spinnerF.setSelection(indexF)
                        spinnerPdf.setSelection(indexP)

                        spinnerF.post {
                            spinnerF.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                                    viewModel.updateDamageType(damageTypes[pos], false)
                                }
                                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                            }
                        }
                        spinnerPdf.post {
                            spinnerPdf.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                                    viewModel.updateDamageType(damageTypes[pos], true)
                                }
                                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                            }
                        }
                    }
                }

                // 7. Roll result display
                launch {
                    rollViewModel.lastRoll.collect { result ->
                        val rollCard = sheetView.findViewById<androidx.cardview.widget.CardView>(R.id.card_roll_result)
                        val rollTotal = sheetView.findViewById<TextView>(R.id.text_roll_total)
                        val rollDetail = sheetView.findViewById<TextView>(R.id.text_roll_detail)
                        val rollName = sheetView.findViewById<TextView>(R.id.text_roll_name)
                        if (result != null) {
                            rollCard.visibility = View.VISIBLE
                            rollName.text = result.name
                            rollTotal.text = result.total.toString()
                            rollDetail.text = result.details.ifEmpty { "${result.attributeUsed}(${result.attributeValue}) + ${result.die}" }
                            rollTotal.setTextColor(if (result.isCritical) Color.parseColor("#D97706") else Color.WHITE)
                        } else {
                            rollCard.visibility = View.GONE
                        }
                    }
                }

                // 7. Virtual roll requests
                launch {
                    rollViewModel.virtualRollRequest.collect { request ->
                        VirtualDiceFragment.newInstance(
                            diceCount = request.diceCount,
                            bonus = request.bonus,
                            attrVal = request.attributeValue,
                            skillVal = request.skillValue,
                            attrName = request.attributeName,
                            charId = viewModel.sandboxCharacter.value?.id ?: "",
                            expectedResults = request.diceOverride,
                            canCrit = request.canCrit,
                            isNegative = request.isNegative,
                            critRangeStart = request.critRangeStart,
                            diceProperties = request.diceProperties
                        ).show(parentFragmentManager, "SM_VirtualDice")
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spinner
    // ─────────────────────────────────────────────────────────────────────────

    private fun updateSpinner(systems: List<RuleSystem>) {
        val names = systems.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.onItemSelectedListener = null
        spinner.adapter = adapter

        // Restore selection to match currently selected system
        val selectedId = viewModel.selectedSystem.value?.id
        val idx = systems.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        spinner.setSelection(idx)

        spinner.post {
            spinnerInitialized = true
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (!spinnerInitialized) return
                    val chosen = systems[pos]
                    if (chosen.id == viewModel.selectedSystem.value?.id) return

                    val performSwitch = {
                        adaptersInitialized = false
                        viewModel.loadSystem(chosen)
                    }

                    if (viewModel.isDirty.value) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Alterações não salvas")
                            .setMessage("Trocar de sistema irá descartar o rascunho atual e limpar a ficha de teste. Continuar?")
                            .setPositiveButton("Trocar") { _, _ -> performSwitch() }
                            .setNegativeButton("Cancelar") { _, _ ->
                                // Revert spinner to current
                                val revertIdx = systems.indexOfFirst { it.id == viewModel.selectedSystem.value?.id }.coerceAtLeast(0)
                                spinner.setSelection(revertIdx)
                            }
                            .show()
                    } else {
                        performSwitch()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Attribute Structure Rendering (mirrors CharacterSheetFragment)
    // ─────────────────────────────────────────────────────────────────────────

    private fun renderAttributeStructure(system: RuleSystem) {
        val container = sheetView.findViewById<android.widget.LinearLayout>(R.id.container_attributes)
            ?: return
        val inflater = LayoutInflater.from(context)

        val currentKeys = (0 until container.childCount)
            .mapNotNull { container.getChildAt(it).tag as? String }
        val targetKeys = system.attributes.map { it.key }
        if (currentKeys == targetKeys) return

        container.removeAllViews()
        system.attributes.forEach { attr ->
            val itemView = inflater.inflate(R.layout.view_attribute_input, container, false)
            itemView.tag = attr.key

            itemView.findViewById<Button>(R.id.btn_minus).setOnClickListener {
                val input = itemView.findViewById<EditText>(R.id.attribute_input)
                input.clearFocus()
                viewModel.updateAttribute(attr.key, (input.text.toString().toIntOrNull() ?: 0) - 1)
            }
            itemView.findViewById<Button>(R.id.btn_plus).setOnClickListener {
                val input = itemView.findViewById<EditText>(R.id.attribute_input)
                input.clearFocus()
                viewModel.updateAttribute(attr.key, (input.text.toString().toIntOrNull() ?: 0) + 1)
            }
            itemView.findViewById<EditText>(R.id.attribute_input).setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    val qty = (v as EditText).text.toString().toIntOrNull() ?: 0
                    val stored = viewModel.sandboxCharacter.value?.attributeValues?.get(attr.key) ?: 0
                    if (qty != stored) viewModel.updateAttribute(attr.key, qty)
                }
            }
            itemView.setOnLongClickListener {
                val sys = viewModel.selectedSystem.value
                if (sys != null && sys.isBaseSystem) { showBaseSystemWarning(); return@setOnLongClickListener true }
                DialogEditAttributeDefinition(attr,
                    onSave = { viewModel.updateAttributeDefinition(it) },
                    onDelete = { viewModel.removeAttributeDefinition(it) }
                ).show(parentFragmentManager, "SM_EditAttr")
                true
            }

            val label = itemView.findViewById<TextView>(R.id.attribute_label)
            val icon = itemView.findViewById<android.widget.ImageView>(R.id.attribute_icon)
            label.text = (attr.name ?: "UNNAMED").uppercase()
            try {
                val color = Color.parseColor(attr.color.ifEmpty { "#000000" })
                label.setTextColor(color); icon.setColorFilter(color)
            } catch (e: Exception) {
                label.setTextColor(Color.WHITE); icon.setColorFilter(Color.WHITE)
            }

            container.addView(itemView)
        }
    }

    private fun updateAttributeValues(char: com.galeria.defensores.models.Character) {
        val container = sheetView.findViewById<android.widget.LinearLayout>(R.id.container_attributes)
            ?: return
        for (i in 0 until container.childCount) {
            val itemView = container.getChildAt(i)
            val key = itemView.tag as? String ?: continue
            val input = itemView.findViewById<EditText>(R.id.attribute_input) ?: continue
            val value = char.attributeValues[key] ?: 0
            if (input.text.toString() != value.toString() && !input.hasFocus()) {
                input.setText(value.toString())
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RecyclerView Adapters
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupRecyclerAdapters() {
        val char = viewModel.sandboxCharacter.value ?: return

        advantagesAdapter = AdvantagesAdapter(char.vantagens, onItemClick = { item ->
            EditAdvantageDialogFragment(item, { viewModel.updateAdvantage(it) }, { viewModel.removeAdvantage(it) })
                .show(parentFragmentManager, "SM_EditAdv")
        })
        sheetView.findViewById<RecyclerView>(R.id.recycler_advantages).apply {
            layoutManager = LinearLayoutManager(context); adapter = advantagesAdapter
        }

        disadvantagesAdapter = AdvantagesAdapter(char.desvantagens, onItemClick = { item ->
            EditAdvantageDialogFragment(item, { viewModel.updateDisadvantage(it) }, { viewModel.removeDisadvantage(it) })
                .show(parentFragmentManager, "SM_EditDis")
        })
        sheetView.findViewById<RecyclerView>(R.id.recycler_disadvantages).apply {
            layoutManager = LinearLayoutManager(context); adapter = disadvantagesAdapter
        }

        skillsAdapter = AdvantagesAdapter(char.pericias, onItemClick = { item ->
            EditSkillDialogFragment(item, { viewModel.updateSkill(it) }, { viewModel.removeSkill(item) })
                .show(parentFragmentManager, "SM_EditSkill")
        })
        sheetView.findViewById<RecyclerView>(R.id.recycler_skills).apply {
            layoutManager = LinearLayoutManager(context); adapter = skillsAdapter
        }

        specializationsAdapter = AdvantagesAdapter(char.especializacoes, onItemClick = { item ->
            EditSpecializationDialogFragment(item, { viewModel.updateSpecialization(it) }, { viewModel.removeSpecialization(item) })
                .show(parentFragmentManager, "SM_EditSpec")
        })
        sheetView.findViewById<RecyclerView>(R.id.recycler_specializations).apply {
            layoutManager = LinearLayoutManager(context); adapter = specializationsAdapter
        }

        inventoryAdapter = InventoryAdapter(
            char.inventario, canEdit = true,
            onItemClick = { item ->
                EditInventoryItemDialogFragment(item, { viewModel.updateInventoryItem(it) }, { viewModel.removeInventoryItem(it) })
                    .show(parentFragmentManager, "SM_EditInv")
            },
            onQuantityChange = { item, delta -> viewModel.adjustInventoryQuantity(item, delta) }
        )
        sheetView.findViewById<RecyclerView>(R.id.recycler_inventory).apply {
            layoutManager = LinearLayoutManager(context); adapter = inventoryAdapter
        }

        spellsAdapter = SpellsAdapter(char.magias) { item ->
            EditSpellDialogFragment(item, { viewModel.updateSpell(it) }, { viewModel.removeSpell(it) })
                .show(parentFragmentManager, "SM_EditSpell")
        }
        sheetView.findViewById<RecyclerView>(R.id.recycler_spells).apply {
            layoutManager = LinearLayoutManager(context); adapter = spellsAdapter
        }

        customRollsAdapter = CustomRollsAdapter(
            items = char.customRolls.toMutableList(),
            onRollClick = { rollViewModel.rollCustom(it) },
            onEditClick = { roll ->
                EditCustomRollDialogFragment(roll, { viewModel.updateCustomRoll(it) }, { viewModel.removeCustomRoll(it) })
                    .show(parentFragmentManager, "SM_EditRoll")
            },
            canEdit = true
        )
        sheetView.findViewById<RecyclerView>(R.id.recycler_custom_rolls).apply {
            layoutManager = LinearLayoutManager(context); adapter = customRollsAdapter
        }

        resourcesAdapter = ResourcesAdapter(
            resources = emptyList(), currentValues = emptyMap(), maxValues = emptyMap(),
            onValueChange = { key, delta -> viewModel.updateResource(key, delta) },
            onResourceLongClick = { res ->
                val sys = viewModel.selectedSystem.value
                if (sys != null && sys.isBaseSystem) { showBaseSystemWarning(); return@ResourcesAdapter }
                DialogEditResourceDefinition(res,
                    onSave = { viewModel.updateResourceDefinition(it) },
                    onDelete = { viewModel.removeResourceDefinition(it) }
                ).show(parentFragmentManager, "SM_EditRes")
            }
        )
        sheetView.findViewById<RecyclerView>(R.id.recycler_resources).apply {
            layoutManager = LinearLayoutManager(context)
            (itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
            adapter = resourcesAdapter
        }
    }

    private fun updateAdapters(
        char: com.galeria.defensores.models.Character,
        system: RuleSystem
    ) {
        val maxValues = mutableMapOf<String, Int>()
        val currentResourceValues = mutableMapOf<String, Int>()
        system.resources.forEach { res ->
            val max = com.galeria.defensores.domain.usecases.GetResourceMaxUseCase()(res, char, system)
            maxValues[res.key] = max
            currentResourceValues[res.key] = when {
                res.key.equals("pv", ignoreCase = true) -> char.currentPv
                res.key.equals("pm", ignoreCase = true) -> char.currentPm
                else -> char.resourceValues[res.key] ?: max
            }
        }
        resourcesAdapter.updateData(system.resources, currentResourceValues, maxValues)
        advantagesAdapter.updateItems(char.vantagens)
        disadvantagesAdapter.updateItems(char.desvantagens)
        skillsAdapter.updateItems(char.pericias)
        specializationsAdapter.updateItems(char.especializacoes)
        inventoryAdapter.updateData(char.inventario)
        spellsAdapter.updateData(char.magias)
        customRollsAdapter.updateData(char.customRolls)
    }
}
