package com.galeria.defensores.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.RollType
import com.galeria.defensores.models.Spell
import com.galeria.defensores.utils.TextFormatUtils

import com.galeria.defensores.viewmodels.CharacterViewModel
import com.galeria.defensores.viewmodels.RollViewModel
import com.galeria.defensores.viewmodels.RuleSystemViewModel
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.galeria.defensores.data.BackupRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlin.random.Random

@AndroidEntryPoint
class CharacterSheetFragment : Fragment() {

    @Inject lateinit var backupRepository: BackupRepository
    @Inject lateinit var tableRepository: com.galeria.defensores.data.TableRepository
    private val viewModel: CharacterViewModel by activityViewModels()
    private val rollViewModel: RollViewModel by activityViewModels()
    private val ruleSystemViewModel: RuleSystemViewModel by activityViewModels()

    private lateinit var advantagesAdapter: AdvantagesAdapter
    private lateinit var disadvantagesAdapter: AdvantagesAdapter
    private lateinit var skillsAdapter: AdvantagesAdapter
    private lateinit var specializationsAdapter: AdvantagesAdapter
    private lateinit var inventoryAdapter: InventoryAdapter
    private lateinit var spellsAdapter: SpellsAdapter
    private lateinit var customRollsAdapter: CustomRollsAdapter
    private lateinit var resourcesAdapter: ResourcesAdapter
    private lateinit var btnSelectUA: Button
    private lateinit var uaCard: View
    private lateinit var btnAddCustomRoll: Button
    private lateinit var customRollsRecycler: RecyclerView
    private var adaptersInitialized = false

    private val cropImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = com.yalantis.ucrop.UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                val context = requireContext()
                Toast.makeText(requireContext(), "Processando imagem...", Toast.LENGTH_SHORT).show()
                viewModel.uploadCharacterAvatar(requireContext(), uri, 
                    onSuccess = {
                        Toast.makeText(requireContext(), "Avatar atualizado!", Toast.LENGTH_SHORT).show()
                        // Clean up temp file
                        try {
                             // Cleanup logic (empty catch for now is fine per existing code)
                        } catch (e: Exception) {}
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else if (result.resultCode == com.yalantis.ucrop.UCrop.RESULT_ERROR) {
            val cropError = com.yalantis.ucrop.UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Erro ao cortar: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val exportLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val char = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character
            if (char != null) {
                lifecycleScope.launch {
                    val success = backupRepository.exportCharacter(requireContext(), char.id, uri)
                    if (success) {
                        Toast.makeText(context, "Ficha exportada com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Erro ao exportar ficha.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val systemExportLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val json = ruleSystemViewModel.exportSystemJson()
            lifecycleScope.launch {
                 try {
                     requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                         output.write(json.toByteArray())
                     }
                     Toast.makeText(context, "Sistema exportado com sucesso!", Toast.LENGTH_SHORT).show()
                 } catch (e: Exception) {
                     e.printStackTrace()
                     Toast.makeText(context, "Erro ao exportar sistema: ${e.message}", Toast.LENGTH_SHORT).show()
                 }
            }
        }
    }

    private val systemImportLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                try {
                    val json = requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    }
                    if (json != null) {
                        ruleSystemViewModel.importSystemJson(json) { success, newSystem ->
                            if (success && newSystem != null) {
                                viewModel.updateRuleSystem(newSystem)
                                Toast.makeText(context, "Sistema importado com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Erro ao importar sistema. Conteúdo inválido.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Erro ao ler arquivo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let {
            startCrop(it)
        }
    }



    private fun startCrop(uri: android.net.Uri) {
        val destinationFileName = "cropped_avatar_${System.currentTimeMillis()}.jpg"
        val destinationUri = android.net.Uri.fromFile(java.io.File(requireContext().cacheDir, destinationFileName))
        
        val uCrop = com.yalantis.ucrop.UCrop.of(uri, destinationUri)
        uCrop.withAspectRatio(1f, 1f)
        uCrop.withMaxResultSize(500, 500) // Slightly larger than our final 300px to allow good downscaling
        
        val options = com.yalantis.ucrop.UCrop.Options()
        options.setCircleDimmedLayer(true) // Helper for circle avatars
        options.setShowCropGrid(false)
        options.setCompressionQuality(90) // High quality for the crop step, we compress in ViewModel
        uCrop.withOptions(options)
        
        cropImage.launch(uCrop.getIntent(requireContext()))
    }

    private var characterId: String? = null
    private var tableId: String? = null

    // UI References
    private lateinit var nameEdit: EditText
    private lateinit var rollResultCard: CardView
    private lateinit var rollTotalText: TextView
    private lateinit var rollDetailText: TextView
    private lateinit var rollNameText: TextView

    companion object {
        private const val ARG_CHARACTER_ID = "character_id"
        private const val ARG_TABLE_ID = "table_id"

        fun newInstance(characterId: String?, tableId: String? = null): CharacterSheetFragment {
            val fragment = CharacterSheetFragment()
            val args = Bundle()
            args.putString(ARG_CHARACTER_ID, characterId)
            args.putString(ARG_TABLE_ID, tableId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            characterId = it.getString(ARG_CHARACTER_ID)
            tableId = it.getString(ARG_TABLE_ID)
        }


    }

    private lateinit var attributesAdapter: AttributesAdapter

    private var isCurrentMaster = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_character_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Critical: Load the character into the shared Activity ViewModel
        viewModel.loadCharacter(characterId, tableId)
        
        // Attributes Container
        val attributesContainer = view.findViewById<android.widget.LinearLayout>(R.id.container_attributes)
        
        
        // Bind UI
        nameEdit = view.findViewById(R.id.edit_char_name)
        rollResultCard = view.findViewById(R.id.card_roll_result)
        rollTotalText = view.findViewById(R.id.text_roll_total)
        rollDetailText = view.findViewById(R.id.text_roll_detail)
        rollNameText = view.findViewById(R.id.text_roll_name)
        
        btnSelectUA = view.findViewById(R.id.btn_select_ua)
        uaCard = view.findViewById(R.id.card_unique_advantage)
        btnAddCustomRoll = view.findViewById(R.id.btn_add_custom_roll)
        customRollsRecycler = view.findViewById(R.id.recycler_custom_rolls)

        val avatarImage = view.findViewById<ImageView>(R.id.img_character_avatar)
        val editAvatarIcon = view.findViewById<ImageView>(R.id.img_edit_avatar_icon)

        val btnSystemOptions = view.findViewById<View>(R.id.btn_system_options)
        if (!tableId.isNullOrEmpty()) {
            btnSystemOptions.visibility = View.GONE
        } else {
            btnSystemOptions.visibility = View.VISIBLE
            btnSystemOptions.setOnClickListener {
                Toast.makeText(context, "Botão Opções Clicado", Toast.LENGTH_SHORT).show()
                val dialog = DialogSystemOptions(
                    onSaveAsClick = {
                         DialogSaveSystem { newName ->
                             ruleSystemViewModel.saveSystemAs(newName) { success, newSystem ->
                                 if (success && newSystem != null) {
                                     viewModel.updateRuleSystem(newSystem)
                                     Toast.makeText(context, "Sistema salvo como '$newName'!", Toast.LENGTH_SHORT).show()
                                 } else {
                                     Toast.makeText(context, "Erro ao salvar sistema.", Toast.LENGTH_SHORT).show()
                                 }
                             }
                         }.show(parentFragmentManager, "SaveSystem")
                    },
                    onExportClick = {
                        val sysName = ruleSystemViewModel.ruleSystem.value?.name ?: "sistema"
                        val safeName = sysName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                        systemExportLauncher.launch("system_${safeName}.json")
                    },
                    onImportClick = {
                        systemImportLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                    },
                    onResetClick = {
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Restaurar Sistema Padrão?")
                            .setMessage("Isso irá reverter o sistema '3DeT Alpha' para as regras originais. \n\nCUIDADO: Se você editou o sistema padrão sem salvar como cópia, suas alterações serão perdidas.")
                            .setPositiveButton("Restaurar") { _, _ ->
                                ruleSystemViewModel.resetBaseSystem { success ->
                                    if (success) Toast.makeText(context, "Sistema restaurado!", Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(context, "Erro ao restaurar.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                )
                dialog.show(parentFragmentManager, "SystemOptions")
            }
        }

        val btnBack = view.findViewById<android.widget.ImageButton>(R.id.btn_reset)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        nameEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (nameEdit.hasFocus()) {
                    viewModel.updateName(s.toString())
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })



        setupFlowCollectors(view)
        setupButtonListeners(view)
    }

    /**
     * Creates or removes attribute views based on the current rule system.
     * MUST be called from the ruleSystemViewModel.ruleSystem observer ONLY.
     * This separates view creation (expensive) from value updates (cheap).
     */
    private fun renderAttributeStructure(ruleSystem: com.galeria.defensores.models.RuleSystem) {
        val attributesContainer = view?.findViewById<android.widget.LinearLayout>(R.id.container_attributes) ?: return
        val inflater = android.view.LayoutInflater.from(context)
        
        val currentKeys = (0 until attributesContainer.childCount).mapNotNull { attributesContainer.getChildAt(it).tag as? String }
        val targetKeys = ruleSystem.attributes.map { it.key }
        
        if (currentKeys == targetKeys) return // Nothing to do structurally
        
        // Rebuild views to ensure exact alignment with target rules
        attributesContainer.removeAllViews()
        
        ruleSystem.attributes.forEach { attr ->
            val itemView = inflater.inflate(R.layout.view_attribute_input, attributesContainer, false)
            itemView.tag = attr.key
            
            itemView.findViewById<Button>(R.id.btn_minus).setOnClickListener {
                val input = itemView.findViewById<EditText>(R.id.attribute_input)
                input.clearFocus()
                val currentVal = input.text.toString().toIntOrNull() ?: 0
                viewModel.updateAttribute(attr.key, currentVal - 1)
            }
            
            itemView.findViewById<Button>(R.id.btn_plus).setOnClickListener {
                val input = itemView.findViewById<EditText>(R.id.attribute_input)
                input.clearFocus()
                val currentVal = input.text.toString().toIntOrNull() ?: 0
                viewModel.updateAttribute(attr.key, currentVal + 1)
            }
            
            itemView.findViewById<EditText>(R.id.attribute_input).setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    val input = v as EditText
                    val quantity = input.text.toString().toIntOrNull() ?: 0
                    val currentVal = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)
                        ?.character?.attributeValues?.get(attr.key) ?: 0
                    if (quantity != currentVal) {
                        viewModel.updateAttribute(attr.key, quantity)
                    }
                }
            }
            
            itemView.setOnLongClickListener {
                DialogEditAttributeDefinition(attr, 
                    onSave = { updated -> ruleSystemViewModel.updateAttributeDefinition(updated) },
                    onDelete = { deleted -> ruleSystemViewModel.removeAttributeDefinition(deleted) }
                ).show(parentFragmentManager, "EditAttribute")
                true
            }
            
            val label = itemView.findViewById<TextView>(R.id.attribute_label)
            val icon = itemView.findViewById<ImageView>(R.id.attribute_icon)
            label.text = (attr.name ?: "UNNAMED").uppercase()
            try {
                val parsedColor = Color.parseColor(if (!attr.color.isNullOrEmpty()) attr.color else "#000000")
                label.setTextColor(parsedColor)
                icon.setColorFilter(parsedColor)
            } catch (e: Exception) {
                label.setTextColor(Color.BLACK)
                icon.setColorFilter(Color.BLACK)
            }
            
            attributesContainer.addView(itemView)
        }
    }

    /**
     * Updates attribute values in existing views. Cheap and safe to call on every character state emission.
     * Does NOT create or remove views.
     */
    private fun updateAttributeValues(char: com.galeria.defensores.models.Character) {
        val attributesContainer = view?.findViewById<android.widget.LinearLayout>(R.id.container_attributes) ?: return
        val sys = ruleSystemViewModel.ruleSystem.value
        
        // Iterate over current views and correctly associate tags instead of mixing indices
        for (i in 0 until attributesContainer.childCount) {
            val itemView = attributesContainer.getChildAt(i)
            val key = itemView.tag as? String ?: continue
            val input = itemView.findViewById<EditText>(R.id.attribute_input) ?: continue
            
            val value = char.attributeValues[key] ?: 0
            if (input.text.toString() != value.toString() && !input.hasFocus()) {
                input.setText(value.toString())
            }
        }
    }

    private fun setupButtonListeners(view: View) {
        view.findViewById<Button>(R.id.btn_add_advantage).setOnClickListener {
            val dialog = SelectAdvantageDialogFragment { selectedAdvantage ->
                viewModel.addAdvantage(selectedAdvantage)
            }
            dialog.show(parentFragmentManager, "SelectAdvantageDialog")
        }

        view.findViewById<Button>(R.id.btn_add_disadvantage).setOnClickListener {
            val dialog = SelectDisadvantageDialogFragment { selectedDisadvantage ->
                viewModel.addDisadvantage(selectedDisadvantage)
            }
            dialog.show(parentFragmentManager, "SelectDisadvantageDialog")
        }

        view.findViewById<Button>(R.id.btn_add_skill).setOnClickListener {
            val dialog = SelectSkillDialogFragment { selectedSkill ->
                viewModel.addSkill(selectedSkill)
            }
            dialog.show(parentFragmentManager, "SelectSkillDialog")
        }

        view.findViewById<Button>(R.id.btn_add_specialization).setOnClickListener {
             val dialog = MultiSelectSpecializationDialogFragment { selectedSpecs ->
                 viewModel.addSpecializations(selectedSpecs)
             }
             dialog.show(parentFragmentManager, "MultiSelectSpecDialog")
        }

        view.findViewById<Button>(R.id.btn_add_spell).setOnClickListener {
             val dialog = EditSpellDialogFragment(null, { newSpell ->
                 viewModel.addSpell(newSpell)
             })
             dialog.show(parentFragmentManager, "AddSpellDialog")
        }

        view.findViewById<Button>(R.id.btn_add_inventory).setOnClickListener {
             val dialog = EditInventoryItemDialogFragment(null, { newItem ->
                 viewModel.addInventoryItem(newItem)
             })
             dialog.show(parentFragmentManager, "AddInvDialog")
        }

        view.findViewById<Button>(R.id.btn_add_attribute).setOnClickListener {
             DialogEditAttributeDefinition(null, { newAttr ->
                 ruleSystemViewModel.addAttributeDefinition(newAttr)
             }).show(parentFragmentManager, "AddAttribute")
        }
        
        view.findViewById<Button>(R.id.btn_add_resource).setOnClickListener {
             DialogEditResourceDefinition(null, { newRes ->
                 ruleSystemViewModel.addResourceDefinition(newRes)
             }).show(parentFragmentManager, "AddResource")
        }

        // Virtual Roll Result Listener
        parentFragmentManager.setFragmentResultListener(
            com.galeria.defensores.ui.VirtualDiceFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val diceValues = bundle.getIntegerArrayList("diceValues")?.toList() ?: emptyList()
            rollViewModel.finalizeVirtualRoll(diceValues)
        }

        // Notes Saving Logic & Rich Text
        val notesEdit = view.findViewById<EditText>(R.id.edit_notes)
        notesEdit.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null && notesEdit.hasFocus()) {
                    TextFormatUtils.applyParagraphSpacingToEditable(s)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        notesEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                 val html = androidx.core.text.HtmlCompat.toHtml(notesEdit.text as android.text.Spanned, androidx.core.text.HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                 viewModel.updateNotes(html)
            }
        }
        


        // Roll Listeners
        view.findViewById<Button>(R.id.btn_attack_f).setOnClickListener {
            checkPermissionAndRoll(RollType.ATTACK_F)
        }
        view.findViewById<Button>(R.id.btn_attack_pdf).setOnClickListener {
            checkPermissionAndRoll(RollType.ATTACK_PDF)
        }
        view.findViewById<Button>(R.id.btn_defense).setOnClickListener {
            checkPermissionAndRoll(RollType.DEFENSE)
        }
        view.findViewById<Button>(R.id.btn_initiative).setOnClickListener {
            checkPermissionAndRoll(RollType.INITIATIVE)
        }


        // Score Buttons (Saved & XP)
        view.findViewById<Button>(R.id.btn_minus_saved).setOnClickListener {
            val current = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character?.savedPoints ?: 0
            if (current > 0) viewModel.updateSavedPoints(current - 1)
        }
        view.findViewById<Button>(R.id.btn_plus_saved).setOnClickListener {
            val current = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character?.savedPoints ?: 0
            viewModel.updateSavedPoints(current + 1)
        }
        view.findViewById<Button>(R.id.btn_minus_xp).setOnClickListener {
            val current = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character?.experience ?: 0
            if (current > 0) viewModel.updateExperience(current - 1)
        }
        view.findViewById<Button>(R.id.btn_plus_xp).setOnClickListener {
            val current = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character?.experience ?: 0
            viewModel.updateExperience(current + 1)
        }
        
        val scaleText = view.findViewById<TextView>(R.id.text_scale)
        scaleText.setOnClickListener {
            val scales = arrayOf("Ningen (x1)", "Sugoi (x10)", "Kiodai (x100)", "Kami (x1000)")
            val char = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character ?: return@setOnClickListener
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Alterar Escala de Poder")
                .setSingleChoiceItems(scales, char.scale) { dialog, which ->
                    viewModel.updateScale(which)
                    dialog.dismiss()
                }
                .show()
        }

        val savedPointsText = view.findViewById<TextView>(R.id.text_saved_points)
        savedPointsText.setOnClickListener {
            val char = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character ?: return@setOnClickListener
            val context = view.context
            val input = EditText(context)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            input.setText(char.savedPoints.toString())
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Pontos Guardados")
                .setView(input)
                .setPositiveButton("Salvar") { _, _ ->
                    val newVal = input.text.toString().toIntOrNull()
                    if (newVal != null) viewModel.updateSavedPoints(newVal)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Damage Type Management
        view.findViewById<View>(R.id.btn_manage_damage_types).setOnClickListener {
            ManageDamageTypesDialogFragment(
                availableTypesFlow = ruleSystemViewModel.availableDamageTypes,
                onAdd = { ruleSystemViewModel.addCustomDamageType(it) },
                onRemove = { ruleSystemViewModel.removeCustomDamageType(it) }
            ).show(parentFragmentManager, "ManageDamageTypes")
        }

        // Spinners for Damage Selection
        val spinnerForca = view.findViewById<android.widget.Spinner>(R.id.spinner_damage_forca)
        val spinnerPdf = view.findViewById<android.widget.Spinner>(R.id.spinner_damage_pdf)
        
        // Listeners for spinners (Setup in observer)

        // Reset/Back Button
        val btnBack = view.findViewById<android.widget.ImageButton>(R.id.btn_reset)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Export Button
        view.findViewById<Button>(R.id.btn_export_character).setOnClickListener {
            val char = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character
            if (char != null) {
                val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                val safeName = char.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                exportLauncher.launch("char_${safeName}_$dateStr.json")
            }
        }

        // Delete Button
        val btnDelete = view.findViewById<Button>(R.id.btn_delete_character)
        btnDelete.setOnClickListener {
            val char = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character
            if (char != null) {
                 androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Excluir Personagem")
                    .setMessage("Tem certeza que deseja excluir ${char.name}? Essa ação não pode ser desfeita.")
                    .setPositiveButton("Excluir") { _, _ ->
                         viewModel.deleteCharacter(
                             onSuccess = {
                                 Toast.makeText(context, "Personagem excluído.", Toast.LENGTH_SHORT).show()
                                 parentFragmentManager.popBackStack()
                             },
                             onError = { errorMsg ->
                                 Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                             }
                         )
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        btnDelete.visibility = View.GONE // Initial state

        // --- ATTRIBUTES & RESOURCES BUTTONS (OFFLINE MODE) ---
        // Ensuring these are visible and functional
        val btnAddAttr = view.findViewById<Button>(R.id.btn_add_attribute)
        val btnAddRes = view.findViewById<Button>(R.id.btn_add_resource)
        


        btnAddAttr.setOnClickListener {
             val sys = ruleSystemViewModel.ruleSystem.value
             if (sys != null && (sys.id == "3det_alpha_base" || sys.isBaseSystem)) {
                 Toast.makeText(context, "Não é possível editar o Sistema Base. Use 'Salvar como' primeiro.", Toast.LENGTH_LONG).show()
                 return@setOnClickListener
             }
             DialogEditAttributeDefinition(null, { newAttr ->
                 ruleSystemViewModel.addAttributeDefinition(newAttr)
             }).show(parentFragmentManager, "AddAttribute")
        }

        btnAddRes.setOnClickListener {
             val sys = ruleSystemViewModel.ruleSystem.value
             if (sys != null && (sys.id == "3det_alpha_base" || sys.isBaseSystem)) {
                 Toast.makeText(context, "Não é possível editar o Sistema Base. Use 'Salvar como' primeiro.", Toast.LENGTH_LONG).show()
                 return@setOnClickListener
             }
             DialogEditResourceDefinition(null, { newRes ->
                 ruleSystemViewModel.addResourceDefinition(newRes)
             }).show(parentFragmentManager, "AddResource")
        }


        // Custom Rolls Setup
        // customRollsRecycler and btnAddCustomRoll initialized in onViewCreated
        
        // Use Flexbox or GridLayout? For now LinearLayout vertical or horizontal?
        // Layout manager was not set in XML. Let's use GridLayoutManager for buttons grid
        customRollsRecycler.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)
        
        val customRollsAdapter = CustomRollsAdapter(
            items = mutableListOf(),
            onRollClick = { roll -> rollViewModel.rollCustom(roll) },
            onEditClick = { roll ->
                // Show Edit Dialog
                 val dialog = EditCustomRollDialogFragment(
                     existingRoll = roll,
                     onSave = { updatedRoll ->
                         viewModel.updateCustomRoll(updatedRoll)
                     },
                     onDelete = { rollToDelete ->
                         viewModel.removeCustomRoll(rollToDelete)
                     }
                 )
                 dialog.show(parentFragmentManager, "EditCustomRoll")
            },
            canEdit = false // Initial state, update in observer
        )
        customRollsRecycler.adapter = customRollsAdapter

        btnAddCustomRoll.setOnClickListener {
             val dialog = EditCustomRollDialogFragment(
                 existingRoll = null, 
                 onSave = { newRoll ->
                     viewModel.addCustomRoll(newRoll)
                 },
                 onDelete = null
             )
             dialog.show(parentFragmentManager, "NewCustomRoll")
        }

        // Redundant character observer removed - moved to setupFlowCollectors
    }
 
    private fun setupFlowCollectors(view: View) {
        val avatarImage = view.findViewById<ImageView>(R.id.img_character_avatar)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Centralized UiState Collector (MVI)
                launch {
                    viewModel.uiState.collect { state ->
                        if (state !is com.galeria.defensores.viewmodels.CharacterUiState.Success) return@collect
                        
                        val char = state.character
                        val sys = state.ruleSystem
                        
                        // Keep RuleSystemViewModel in sync for its own editing functions
                        val effectiveTableId = tableId ?: char.tableId
                        val effectiveTableIdOrNull = effectiveTableId.takeIf { it.isNotEmpty() }
                        val sysChanged = ruleSystemViewModel.ruleSystem.value.id != sys.id
                        if (sysChanged) {
                            ruleSystemViewModel.loadRuleSystem(sys)
                        }
                        // Load damage types on first init OR when system changes
                        if (!adaptersInitialized || sysChanged) {
                            ruleSystemViewModel.loadDamageTypes(effectiveTableIdOrNull)
                            ruleSystemViewModel.loadUniqueAdvantages(effectiveTableIdOrNull)
                        }

                        // 1. Basic Info
                        view.findViewById<TextView>(R.id.text_system_name).text = sys.name.uppercase()
                        if (!nameEdit.hasFocus()) { nameEdit.setText(char.name) }
                        nameEdit.isEnabled = true 

                        // 2. Avatar
                        if (char.imageUrl.isNotEmpty()) {
                            Glide.with(this@CharacterSheetFragment).load(char.imageUrl).apply(RequestOptions.circleCropTransform()).into(avatarImage)
                        } else {
                            avatarImage.setImageResource(android.R.drawable.sym_def_app_icon)
                        }
                        avatarImage.setOnClickListener { pickImage.launch("image/*") }

                        // 3. Stats
                        val scaleName = when(char.scale) {
                            1 -> "Sugoi (x10)"; 2 -> "Kiodai (x100)"; 3 -> "Kami (x1000)"; else -> "Ningen (x1)"
                        }
                        view.findViewById<TextView>(R.id.text_scale).text = "Escala: $scaleName"
                        view.findViewById<TextView>(R.id.text_score_value).text = char.calculateScore().toString()
                        view.findViewById<TextView>(R.id.text_saved_points).text = char.savedPoints.toString()
                        view.findViewById<TextView>(R.id.text_experience).text = char.experience.toString()
                        
                        // 4. Unique Advantage
                        // 4. Unique Advantage
                        val showSelect = char.uniqueAdvantage == null
                        btnSelectUA.text = if (showSelect) "Adicionar Vantagem Única" else "Trocar Vantagem"
                        uaCard.visibility = if (showSelect) View.GONE else View.VISIBLE
                        
                        val onUAClick = {
                            val availableUAs = ruleSystemViewModel.availableUniqueAdvantages.value
                            SelectUniqueAdvantageDialogFragment(
                                availableUAs = availableUAs,
                                canManage = true,
                                onSelect = { selected -> viewModel.setUniqueAdvantage(selected) },
                                onAddCustom = { ruleSystemViewModel.addCustomUniqueAdvantage(it) },
                                onEditCustom = { old, new -> ruleSystemViewModel.updateCustomUniqueAdvantage(old, new) },
                                onDeleteCustom = { ruleSystemViewModel.removeCustomUniqueAdvantage(it) }
                            ).show(parentFragmentManager, "SelectUA")
                        }
                        
                        btnSelectUA.setOnClickListener { onUAClick() }
                        if (char.uniqueAdvantage != null) {
                            val ua = char.uniqueAdvantage!!
                            uaCard.findViewById<TextView>(R.id.text_ua_name).text = ua.name
                            uaCard.findViewById<TextView>(R.id.text_ua_cost).text = if (ua.cost < 0) ua.cost.toString() else "+${ua.cost}"
                            uaCard.findViewById<TextView>(R.id.text_ua_description).text = "Benefícios: ${ua.benefits}\nFraquezas: ${ua.weaknesses}"
                            uaCard.setOnClickListener {
                                EditUniqueAdvantageDialogFragment(
                                    ua = ua,
                                    onSave = { updatedUA -> viewModel.setUniqueAdvantage(updatedUA) },
                                    onDelete = { viewModel.setUniqueAdvantage(null) }
                                ).show(parentFragmentManager, "EditCharacterUA")
                            }
                        }

                        // 5. Spinners Selection Update
                        val spinnerForca = view.findViewById<android.widget.Spinner>(R.id.spinner_damage_forca)
                        val spinnerPdf = view.findViewById<android.widget.Spinner>(R.id.spinner_damage_pdf)
                        val damageTypes = ruleSystemViewModel.availableDamageTypes.value
                        if (damageTypes.isNotEmpty()) {
                            val indexF = damageTypes.indexOf(char.damageTypeForca).coerceAtLeast(0)
                            if (spinnerForca.selectedItemPosition != indexF) {
                                spinnerForca.onItemSelectedListener = null
                                spinnerForca.setSelection(indexF)
                                spinnerForca.post {
                                    spinnerForca.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                            if (char.damageTypeForca != damageTypes[pos]) viewModel.updateDamageType(damageTypes[pos], false)
                                        }
                                        override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
                                    }
                                }
                            }
                            val indexP = damageTypes.indexOf(char.damageTypePdf).coerceAtLeast(0)
                            if (spinnerPdf.selectedItemPosition != indexP) {
                                spinnerPdf.onItemSelectedListener = null
                                spinnerPdf.setSelection(indexP)
                                spinnerPdf.post {
                                    spinnerPdf.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                            if (char.damageTypePdf != damageTypes[pos]) viewModel.updateDamageType(damageTypes[pos], true)
                                        }
                                        override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
                                    }
                                }
                            }
                        }
                        // 6. Attributes and Adapters
                        updateAttributeValues(char)
                        val currentUser = com.galeria.defensores.data.SessionManager.currentUser
                        val isOwner = currentUser != null && char.ownerId == currentUser.id
                        val canEdit = isOwner || isCurrentMaster
                        
                        val canEditSystem = canEdit
                        view.findViewById<Button>(R.id.btn_add_attribute).visibility = if (canEditSystem) View.VISIBLE else View.GONE
                        view.findViewById<Button>(R.id.btn_add_resource).visibility = if (canEditSystem) View.VISIBLE else View.GONE
                        
                        view.findViewById<Button>(R.id.btn_delete_character).visibility = 
                            if (currentUser != null && (isOwner || currentUser.id == "admin")) View.VISIBLE else View.GONE
                            
                        // Trait Lists Add Buttons Visibility
                        view.findViewById<Button>(R.id.btn_select_ua).visibility = if (canEdit) View.VISIBLE else View.GONE
                        view.findViewById<Button>(R.id.btn_add_advantage).visibility = if (canEdit) View.VISIBLE else View.GONE
                        view.findViewById<Button>(R.id.btn_add_disadvantage).visibility = if (canEdit) View.VISIBLE else View.GONE
                        view.findViewById<Button>(R.id.btn_add_skill).visibility = if (canEdit) View.VISIBLE else View.GONE
                        view.findViewById<Button>(R.id.btn_add_specialization).visibility = if (canEdit) View.VISIBLE else View.GONE
                        view.findViewById<Button>(R.id.btn_add_inventory).visibility = if (canEdit) View.VISIBLE else View.GONE
                        view.findViewById<Button>(R.id.btn_add_spell).visibility = if (canEdit) View.VISIBLE else View.GONE

                        // Update Notes Text without interrupting typing
                        val notesEdit = view.findViewById<EditText>(R.id.edit_notes)
                        if (!notesEdit.hasFocus()) {
                            notesEdit.setText(androidx.core.text.HtmlCompat.fromHtml(char.anotacoes, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY))
                        }

                        if (!adaptersInitialized) {
                            setupRecyclerViewsInternal(view, char, true, true)
                            adaptersInitialized = true
                        }
                        updateAdaptersInternal(char, ruleSystemViewModel.ruleSystem.value)
                    }
                }

                // Damage Types Reactive Update
                launch {
                    ruleSystemViewModel.availableDamageTypes.collect { damageTypes ->
                        if (damageTypes.isEmpty()) return@collect
                        val char = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character ?: return@collect
                        val spinnerForca = view.findViewById<android.widget.Spinner>(R.id.spinner_damage_forca)
                        val spinnerPdf = view.findViewById<android.widget.Spinner>(R.id.spinner_damage_pdf)
                        
                        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, damageTypes)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        
                        // Temporarily detach listeners to avoid recursion during setup
                        spinnerForca.onItemSelectedListener = null
                        spinnerPdf.onItemSelectedListener = null
                        
                        spinnerForca.adapter = adapter
                        spinnerPdf.adapter = adapter
                        
                        spinnerForca.setSelection(damageTypes.indexOf(char.damageTypeForca).coerceAtLeast(0))
                        spinnerPdf.setSelection(damageTypes.indexOf(char.damageTypePdf).coerceAtLeast(0))
                        
                        // Re-attach listeners after layout is executed
                        spinnerForca.post {
                            spinnerForca.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                    if (char.damageTypeForca != damageTypes[pos]) viewModel.updateDamageType(damageTypes[pos], false)
                                }
                                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
                            }
                        }
                        
                        spinnerPdf.post {
                            spinnerPdf.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                                    if (char.damageTypePdf != damageTypes[pos]) viewModel.updateDamageType(damageTypes[pos], true)
                                }
                                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
                            }
                        }
                    }
                }

                // Rule System Structure Observer: drives attribute view creation/removal AND resource list.
                // Separated from uiState to prevent view flashing on character mutations (e.g., +/- presses).
                launch {
                    ruleSystemViewModel.ruleSystem.collect { sys ->
                        renderAttributeStructure(sys)
                        // Also update values and adapters immediately after structural changes.
                        // This is critical for showing newly added/removed resources and attributes
                        // without requiring a full character state reload (which won't happen since
                        // LoadCharacterUseCase does a one-shot fetch).
                        val char = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character
                        if (char != null) {
                            updateAttributeValues(char)
                            if (adaptersInitialized) {
                                updateAdaptersInternal(char, sys)
                            }
                        }
                    }
                }

                // Roll Logic Collectors
                launch {
                    rollViewModel.isRolling.collect { isRolling ->
                        val buttons = listOf(R.id.btn_attack_f, R.id.btn_attack_pdf, R.id.btn_defense)
                        buttons.forEach { view.findViewById<View>(it).isEnabled = !isRolling }
                        if (isRolling) {
                            rollResultCard.visibility = View.VISIBLE
                            rollTotalText.setTextColor(Color.YELLOW)
                            rollTotalText.text = "..."
                        }
                    }
                }
                launch {
                    rollViewModel.lastRoll.collect { result ->
                        if (result != null) {
                            rollResultCard.visibility = View.VISIBLE
                            rollNameText.text = result.name
                            rollTotalText.text = result.total.toString()
                            rollDetailText.text = result.details.ifEmpty { "${result.attributeUsed}(${result.attributeValue}) + ${result.die}" }
                            rollTotalText.setTextColor(if (result.isCritical) Color.parseColor("#D97706") else Color.WHITE)
                        } else {
                            rollResultCard.visibility = View.GONE
                        }
                    }
                }
                launch {
                    rollViewModel.virtualRollRequest.collect { request ->
                        com.galeria.defensores.ui.VirtualDiceFragment.newInstance(
                            diceCount = request.diceCount,
                            bonus = request.bonus,
                            attrVal = request.attributeValue,
                            skillVal = request.skillValue,
                            attrName = request.attributeName,
                            charId = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character?.id ?: "",
                            expectedResults = request.diceOverride,
                            canCrit = request.canCrit,
                            isNegative = request.isNegative,
                            critRangeStart = request.critRangeStart,
                            diceProperties = request.diceProperties
                        ).show(parentFragmentManager, "virtual_dice")
                    }
                }
            }
        }
    }

    private fun updateAdaptersInternal(char: com.galeria.defensores.models.Character, ruleSystem: com.galeria.defensores.models.RuleSystem) {
        val maxValues = mutableMapOf<String, Int>()
        val currentResourceValues = mutableMapOf<String, Int>()
        ruleSystem.resources.forEach { res ->
            val max = viewModel.calculateResourceMax(res, char, ruleSystem)
            maxValues[res.key] = max
            currentResourceValues[res.key] = if (res.key == "pv") char.currentPv else if (res.key == "pm") char.currentPm else char.resourceValues[res.key] ?: max
        }
        resourcesAdapter.updateData(ruleSystem.resources, currentResourceValues, maxValues)
        advantagesAdapter.updateItems(char.vantagens)
        disadvantagesAdapter.updateItems(char.desvantagens)
        skillsAdapter.updateItems(char.pericias)
        specializationsAdapter.updateItems(char.especializacoes)
        inventoryAdapter.updateData(char.inventario)
        spellsAdapter.updateData(char.magias)
        customRollsAdapter.updateData(char.customRolls)
    }

    private fun setupRecyclerViewsInternal(view: View, char: com.galeria.defensores.models.Character, canEdit: Boolean, isMaster: Boolean) {
        // Advantages
        advantagesAdapter = AdvantagesAdapter(char.vantagens, onItemClick = { item ->
             EditAdvantageDialogFragment(item, { viewModel.updateAdvantage(it) }, { viewModel.removeAdvantage(it) }).show(parentFragmentManager, "EditAdvantage")
        })
        view.findViewById<RecyclerView>(R.id.recycler_advantages).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = advantagesAdapter
        }
        
        // Disadvantages
        disadvantagesAdapter = AdvantagesAdapter(char.desvantagens, onItemClick = { item ->
             EditAdvantageDialogFragment(item, { viewModel.updateDisadvantage(it) }, { viewModel.removeDisadvantage(it) }).show(parentFragmentManager, "EditDisadvantage")
        })
        view.findViewById<RecyclerView>(R.id.recycler_disadvantages).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = disadvantagesAdapter
        }
        
        // Skills
        skillsAdapter = AdvantagesAdapter(char.pericias, onItemClick = { item ->
             EditSkillDialogFragment(item, { viewModel.updateSkill(it) }, { viewModel.removeSkill(item) }).show(parentFragmentManager, "EditSkill")
        })
        view.findViewById<RecyclerView>(R.id.recycler_skills).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = skillsAdapter
        }
        
        // Specializations
        specializationsAdapter = AdvantagesAdapter(char.especializacoes, onItemClick = { item ->
             EditSpecializationDialogFragment(item, { viewModel.updateSpecialization(it) }, { viewModel.removeSpecialization(item) }).show(parentFragmentManager, "EditSpec")
        })
        view.findViewById<RecyclerView>(R.id.recycler_specializations).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = specializationsAdapter
        }
        
        // Inventory
        inventoryAdapter = InventoryAdapter(char.inventario, canEdit, 
            onItemClick = { item ->
                EditInventoryItemDialogFragment(item, { viewModel.updateInventoryItem(it) }, { viewModel.removeInventoryItem(it) }).show(parentFragmentManager, "EditInv")
            },
            onQuantityChange = { item, delta -> viewModel.adjustInventoryQuantity(item, delta) }
        )
        view.findViewById<RecyclerView>(R.id.recycler_inventory).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = inventoryAdapter
        }
        
        // Spells
        spellsAdapter = SpellsAdapter(char.magias) { item ->
             EditSpellDialogFragment(item, { viewModel.updateSpell(it) }, { viewModel.removeSpell(it) }).show(parentFragmentManager, "EditSpell")
        }
        view.findViewById<RecyclerView>(R.id.recycler_spells).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = spellsAdapter
        }
        
        // Custom Rolls
        customRollsAdapter = CustomRollsAdapter(
            items = char.customRolls.toMutableList(),
            onRollClick = { roll -> rollViewModel.rollCustom(roll) },
            onEditClick = { roll ->
                EditCustomRollDialogFragment(roll, { viewModel.updateCustomRoll(it) }, { viewModel.removeCustomRoll(it) }).show(parentFragmentManager, "EditCustomRoll")
            },
            canEdit = isMaster
        )
        view.findViewById<RecyclerView>(R.id.recycler_custom_rolls).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            adapter = customRollsAdapter
        }

        // Resources
        resourcesAdapter = ResourcesAdapter(emptyList(), emptyMap(), emptyMap(), { key, delta ->
            viewModel.updateResource(key, delta)
        }, { res ->
            DialogEditResourceDefinition(res, 
                onSave = { updated -> ruleSystemViewModel.updateResourceDefinition(updated) },
                onDelete = { deleted -> ruleSystemViewModel.removeResourceDefinition(deleted) }
            ).show(parentFragmentManager, "EditResource")
        })
        view.findViewById<RecyclerView>(R.id.recycler_resources).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            (itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
            adapter = resourcesAdapter
        }
    }
        


    private fun checkPermissionAndRoll(type: RollType) {
        // Re-check permission on click to be safe, or store it in a member variable
        // For simplicity and safety, let's fetch current state
        val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id
        val char = (viewModel.uiState.value as? com.galeria.defensores.viewmodels.CharacterUiState.Success)?.character
        
        if (char != null && currentUserId != null) {
            // We need to fetch table to know if isMaster. 
            // Since this is async, we might want to store 'isMaster' in ViewModel or Fragment scope.
            // However, we already did this check in onViewCreated. Let's promote 'canEdit' to a class property?
            // Or better: just check ownerId for now, and if not owner, check table master async or assume false if not loaded.
            
            // A safer quick fix: rely on the UI state. If buttons are enabled/visible, user can click.
            // But we added a Toast for disabled state.
            
            // Let's implement a proper check
            val isOwner = char.ownerId == currentUserId
            
            if (isOwner || isCurrentMaster) {
                rollViewModel.rollDice(type)
            } else {
                Toast.makeText(context, "Apenas o dono ou mestre pode rolar dados.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
