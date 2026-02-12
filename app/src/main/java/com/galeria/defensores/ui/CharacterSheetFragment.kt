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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.RollType
import com.galeria.defensores.models.Spell

import com.galeria.defensores.viewmodels.CharacterViewModel
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.launch
import kotlin.random.Random

class CharacterSheetFragment : Fragment() {

    private lateinit var viewModel: CharacterViewModel
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
            val char = viewModel.character.value
            if (char != null) {
                lifecycleScope.launch {
                    val success = com.galeria.defensores.data.BackupRepository.exportCharacter(requireContext(), char.id, uri)
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
            val json = viewModel.exportSystemJson()
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
                        if (viewModel.importSystemJson(json)) {
                            Toast.makeText(context, "Sistema importado com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Erro ao importar sistema. Conteúdo inválido.", Toast.LENGTH_SHORT).show()
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
        viewModel = ViewModelProvider(this).get(CharacterViewModel::class.java)

    }

    private lateinit var attributesAdapter: AttributesAdapter
    private lateinit var resourcesAdapter: ResourcesAdapter

    private var isCurrentMaster = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_character_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Attributes Container
        val attributesContainer = view.findViewById<android.widget.LinearLayout>(R.id.container_attributes)

        fun renderAttributes(ruleSystem: com.galeria.defensores.models.RuleSystem, char: com.galeria.defensores.models.Character) {
             val inflater = android.view.LayoutInflater.from(context)
             
             // Check if we can reuse the existing views
             // Assumption: The order of attributes in ruleSystem is stable.
             // If size differs, rebuild. If keys differ, rebuild.
             
             val needsRebuild = if (attributesContainer.childCount != ruleSystem.attributes.size) {
                 true
             } else {
                 // Check if keys match
                 var mismatch = false
                 ruleSystem.attributes.forEachIndexed { index, attr ->
                     val view = attributesContainer.getChildAt(index)
                     if (view.tag != attr.key) { // Use Key as Tag
                         mismatch = true
                     }
                 }
                 mismatch
             }

             if (needsRebuild) {
                 attributesContainer.removeAllViews()
                 ruleSystem.attributes.forEach { attr ->
                     val itemView = inflater.inflate(R.layout.view_attribute_input, attributesContainer, false)
                     itemView.tag = attr.key // Set Tag for reuse check
                     
                     // Initial Setup of Listeners (invariant parts)
                     val label = itemView.findViewById<TextView>(R.id.attribute_label)
                     val input = itemView.findViewById<EditText>(R.id.attribute_input)
                     val btnMinus = itemView.findViewById<Button>(R.id.btn_minus)
                     val btnPlus = itemView.findViewById<Button>(R.id.btn_plus)
                     val icon = itemView.findViewById<ImageView>(R.id.attribute_icon)
                     
                     // Listeners
                     btnMinus.setOnClickListener {
                         input.clearFocus() // Clear focus to prevent conflict
                         val currentVal = input.text.toString().toIntOrNull() ?: 0
                         val newValue = currentVal - 1
                         viewModel.updateAttribute(attr.key, newValue)
                     }
                     
                     btnPlus.setOnClickListener {
                         input.clearFocus()
                         val currentVal = input.text.toString().toIntOrNull() ?: 0
                         val newValue = currentVal + 1
                         viewModel.updateAttribute(attr.key, newValue)
                     }
                     
                     // Focus Listener to save on blur
                     input.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            val quantity = input.text.toString().toIntOrNull() ?: 0
                            val oldVal = char.attributeValues[attr.key] ?: 0
                            if (quantity != oldVal) {
                                viewModel.updateAttribute(attr.key, quantity)
                            }
                        }
                     }
                     
                     itemView.setOnLongClickListener {
                         DialogEditAttributeDefinition(attr, 
                              onSave = { updated -> viewModel.updateAttributeDefinition(updated) },
                              onDelete = { deleted -> viewModel.removeAttributeDefinition(deleted) }
                          ).show(parentFragmentManager, "EditAttribute")
                         true
                     }
                     
                     attributesContainer.addView(itemView)
                 }
             }
             
             // Update Values (Binding) - Runs for both new and reused views
             ruleSystem.attributes.forEachIndexed { index, attr ->
                 val itemView = attributesContainer.getChildAt(index)
                 val label = itemView.findViewById<TextView>(R.id.attribute_label)
                 val input = itemView.findViewById<EditText>(R.id.attribute_input)
                 val icon = itemView.findViewById<ImageView>(R.id.attribute_icon)
                 
                 val safeName = attr.name ?: "UNNAMED"
                 label.text = safeName.uppercase()
                 
                 // Color Logic
                 try {
                    val colorStr = if (!attr.color.isNullOrEmpty()) attr.color else "#000000"
                    val parsedColor = android.graphics.Color.parseColor(colorStr)
                    label.setTextColor(parsedColor)
                    icon.setColorFilter(parsedColor)
                    input.setTextColor(android.graphics.Color.BLACK)
                 } catch (e: Exception) {
                    label.setTextColor(android.graphics.Color.BLACK)
                    icon.setColorFilter(android.graphics.Color.BLACK)
                    input.setTextColor(android.graphics.Color.BLACK)
                 }
                 
                 val value = char.attributeValues[attr.key] ?: 0
                 
                 // Update text ONLY if content changed AND it doesn't have focus (or we want to force update?)
                 // If user is typing, we shouldn't overwrite unless it's a remote/external change.
                 // But for simplified local logic:
                 if (input.text.toString() != value.toString()) {
                    if (!input.hasFocus()) {
                        input.setText(value.toString())
                    }
                 }
             }
        }
        
        // Setup RecyclerViews
        val recyclerResources = view.findViewById<RecyclerView>(R.id.recycler_resources)
        
        recyclerResources.layoutManager = LinearLayoutManager(context)
        
        resourcesAdapter = ResourcesAdapter(emptyList(), emptyMap(), emptyMap(),
             onValueChange = { key, delta ->
                viewModel.updateResource(key, delta)
             },
             onResourceLongClick = { res ->
                 if (isCurrentMaster) {
                     DialogEditResourceDefinition(res, 
                         onSave = { updated -> viewModel.updateResourceDefinition(updated) },
                         onDelete = { deleted -> viewModel.removeResourceDefinition(deleted) }
                     ).show(parentFragmentManager, "EditResource")
                 }
             }
        )
        
        
        recyclerResources.adapter = resourcesAdapter

        // Bind UI
        nameEdit = view.findViewById(R.id.edit_char_name)
        rollResultCard = view.findViewById(R.id.card_roll_result)
        rollTotalText = view.findViewById(R.id.text_roll_total)
        rollDetailText = view.findViewById(R.id.text_roll_detail)
        rollNameText = view.findViewById(R.id.text_roll_name)
        

        val avatarImage = view.findViewById<ImageView>(R.id.img_character_avatar)
        val editAvatarIcon = view.findViewById<ImageView>(R.id.img_edit_avatar_icon)

        view.findViewById<View>(R.id.btn_system_options).setOnClickListener {
            Toast.makeText(context, "Botão Opções Clicado", Toast.LENGTH_SHORT).show()
            val dialog = DialogSystemOptions(
                onSaveAsClick = {
                     DialogSaveSystem { newName ->
                         viewModel.saveSystemAs(newName) { success ->
                             if (success) {
                                 Toast.makeText(context, "Sistema salvo como '$newName'!", Toast.LENGTH_SHORT).show()
                             } else {
                                 Toast.makeText(context, "Erro ao salvar sistema.", Toast.LENGTH_SHORT).show()
                             }
                         }
                     }.show(parentFragmentManager, "SaveSystem")
                },
                onExportClick = {
                    val sysName = viewModel.ruleSystem.value?.name ?: "sistema"
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
                            viewModel.resetBaseSystem { success ->
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



        // Observe Data
        viewModel.loadCharacter(characterId, tableId)
        


        // Virtual Roll Observer
        viewModel.virtualRollRequest.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { request ->
                val frag = com.galeria.defensores.ui.VirtualDiceFragment.newInstance(
                    diceCount = request.diceCount,
                    bonus = request.bonus,
                    attrVal = request.attributeValue,
                    skillVal = request.skillValue,
                    attrName = request.attributeName,
                    charId = viewModel.character.value?.id ?: "",
                    expectedResults = request.diceOverride,
                    canCrit = request.canCrit,
                    isNegative = request.isNegative,
                    critRangeStart = request.critRangeStart,
                    diceProperties = request.diceProperties
                )
                frag.show(parentFragmentManager, "virtual_dice")
            }
        }



        // Virtual Roll Result Listener
        parentFragmentManager.setFragmentResultListener(
            com.galeria.defensores.ui.VirtualDiceFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val diceValues = bundle.getIntegerArrayList("diceValues")?.toList() ?: emptyList()
            viewModel.finalizeVirtualRoll(diceValues)
        }

        fun updateAdapters(char: com.galeria.defensores.models.Character, ruleSystem: com.galeria.defensores.models.RuleSystem) {
             // Attributes - Manual Render
             renderAttributes(ruleSystem, char)
             
             // Resources - Dynamic Calculation
             val maxValues = mutableMapOf<String, Int>()
             val currentResourceValues = mutableMapOf<String, Int>()
             
             ruleSystem.resources.forEach { res ->
                 // Calculate Max
                 val max = viewModel.calculateResourceMax(res, char)
                 maxValues[res.key] = max
                 
                 // Get Current
                 val current = if (res.key == "pv") char.currentPv 
                               else if (res.key == "pm") char.currentPm
                               else char.resourceValues[res.key] ?: max // Default to full if not found
                 
                 currentResourceValues[res.key] = current
             }
             
             resourcesAdapter.updateData(ruleSystem.resources, currentResourceValues, maxValues)
        }

        viewModel.ruleSystem.observe(viewLifecycleOwner) { ruleSystem ->
            val char = viewModel.character.value ?: return@observe
            updateAdapters(char, ruleSystem)
        }

        viewModel.character.observe(viewLifecycleOwner) { char ->
            if (char == null) return@observe

            if (!nameEdit.hasFocus()) {
                nameEdit.setText(char.name)
            }

            // Restore lost Data Binding
            // Update Adapters
            val ruleSystem = viewModel.ruleSystem.value ?: com.galeria.defensores.models.RuleSystem()
            updateAdapters(char, ruleSystem)

            // Handle Permissions
            var canEdit = false
            viewLifecycleOwner.lifecycleScope.launch {
                // Ensure user is loaded
                if (com.galeria.defensores.data.SessionManager.currentUser == null) {
                    com.galeria.defensores.data.SessionManager.refreshUser()
                }
                val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id
                
                // Fallback to character's tableId if argument is null
                val effectiveTableId = tableId ?: char.tableId
                // Offline Mode: Single User has full permissions
                val isMaster = true
                isCurrentMaster = true 
                val isOwner = true
                canEdit = true
                
                // Scale Editing: Always allowed
                val effectivelyCanEditScale = true
                
                // Load Damage Types if not already associated (or just refresh)
                if (char.tableId.isNotEmpty()) {
                    viewModel.loadDamageTypes(char.tableId)
                }
                
                // Add Buttons Logic
                val btnAddAttribute = view.findViewById<Button>(R.id.btn_add_attribute)
                val btnAddResource = view.findViewById<Button>(R.id.btn_add_resource)
                
                btnAddAttribute.visibility = if (isMaster) View.VISIBLE else View.GONE
                btnAddResource.visibility = if (isMaster) View.VISIBLE else View.GONE
                
                btnAddAttribute.setOnClickListener {
                     DialogEditAttributeDefinition(null, { newAttr ->
                         viewModel.addAttributeDefinition(newAttr)
                     }).show(parentFragmentManager, "AddAttribute")
                }
                
                btnAddResource.setOnClickListener {
                     DialogEditResourceDefinition(null, { newRes ->
                         viewModel.addResourceDefinition(newRes)
                     }).show(parentFragmentManager, "AddResource")
                }

                // Enable/Disable Editing based on permissions
                nameEdit.isEnabled = canEdit
                view.findViewById<Button>(R.id.btn_add_advantage).visibility = if (canEdit) View.VISIBLE else View.GONE
                view.findViewById<Button>(R.id.btn_add_disadvantage).visibility = if (canEdit) View.VISIBLE else View.GONE
                view.findViewById<Button>(R.id.btn_add_skill).visibility = if (canEdit) View.VISIBLE else View.GONE
                view.findViewById<Button>(R.id.btn_add_specialization).visibility = if (canEdit) View.VISIBLE else View.GONE
                view.findViewById<Button>(R.id.btn_add_inventory).visibility = if (canEdit) View.VISIBLE else View.GONE
                view.findViewById<Button>(R.id.btn_add_spell).visibility = if (canEdit) View.VISIBLE else View.GONE
                
                // Avatar Logic
                if (char.imageUrl.isNotEmpty()) {
                    Glide.with(this@CharacterSheetFragment)
                        .load(char.imageUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(android.R.drawable.sym_def_app_icon) // Fallback
                        .into(avatarImage)
                } else {
                     // Reset to default if empty (handling updates)
                     avatarImage.setImageResource(android.R.drawable.sym_def_app_icon)
                     avatarImage.setBackgroundResource(R.drawable.bg_circle_button)
                }

                if (canEdit) {
                    editAvatarIcon.visibility = View.VISIBLE
                    avatarImage.setOnClickListener { pickImage.launch("image/*") }
                    editAvatarIcon.setOnClickListener { pickImage.launch("image/*") }
                } else {
                    editAvatarIcon.visibility = View.GONE
                    avatarImage.setOnClickListener(null)
                    editAvatarIcon.setOnClickListener(null)
                }
                
                // Damage Types UI
                val spinnerForca = view.findViewById<android.widget.Spinner>(R.id.spinner_damage_forca)
                val spinnerPdf = view.findViewById<android.widget.Spinner>(R.id.spinner_damage_pdf)
                val btnManageTypes = view.findViewById<android.view.View>(R.id.btn_manage_damage_types)

                btnManageTypes.visibility = if (isMaster) View.VISIBLE else View.GONE
                spinnerForca.isEnabled = canEdit
                spinnerPdf.isEnabled = canEdit



                // Scale Logic
                val scaleText = view.findViewById<TextView>(R.id.text_scale)
                val scaleName = when(char.scale) {
                    0 -> "Ningen (x1)"
                    1 -> "Sugoi (x10)"
                    2 -> "Kiodai (x100)"
                    3 -> "Kami (x1000)"
                    else -> "Ningen (x1)"
                }
                scaleText.text = "Escala: $scaleName"

                scaleText.setOnClickListener {
                    if (effectivelyCanEditScale) {
                         val scales = arrayOf("Ningen (x1)", "Sugoi (x10)", "Kiodai (x100)", "Kami (x1000)")
                         androidx.appcompat.app.AlertDialog.Builder(requireContext())
                             .setTitle("Alterar Escala de Poder")
                             .setSingleChoiceItems(scales, char.scale) { dialog, which ->
                                 viewModel.updateScale(which)
                                 dialog.dismiss()
                             }
                             .show()
                    } else if (canEdit) {
                        Toast.makeText(context, "Apenas o Mestre pode alterar a Escala.", Toast.LENGTH_SHORT).show()
                    }
                }

                // UNIQUE ADVANTAGE LOGIC
                viewModel.loadUniqueAdvantages(effectiveTableId)
                val uaCard = view.findViewById<View>(R.id.card_unique_advantage)
                val btnSelectUA = view.findViewById<Button>(R.id.btn_select_ua)

                btnSelectUA.visibility = if (canEdit) View.VISIBLE else View.GONE
                
                if (char.uniqueAdvantage != null) {
                    uaCard.visibility = View.VISIBLE
                    val ua = char.uniqueAdvantage!!
                    uaCard.findViewById<TextView>(R.id.text_ua_name).text = ua.name
                    uaCard.findViewById<TextView>(R.id.text_ua_group).text = ua.group
                    uaCard.findViewById<TextView>(R.id.text_ua_cost).text = "${ua.cost} pts"
                    val benefitsText = uaCard.findViewById<TextView>(R.id.text_ua_benefits)
                    benefitsText.visibility = View.VISIBLE
                    benefitsText.text = "Benefícios: ${ua.benefits}\nFraquezas: ${ua.weaknesses}"
                    
                    // Allow clicking current UA to change/view
                    uaCard.setOnClickListener {
                        if (canEdit) btnSelectUA.performClick()
                    }
                } else {
                    uaCard.visibility = View.GONE
                }

                // --- BIND NEW STATS (Score, Saved, XP) ---
                view.findViewById<TextView>(R.id.text_score_value).text = char.calculateScore().toString()
                
                val savedPointsText = view.findViewById<TextView>(R.id.text_saved_points)
                savedPointsText.text = char.savedPoints.toString()
                
                // Saved Points Logic
                val btnMinusSaved = view.findViewById<Button>(R.id.btn_minus_saved)
                val btnPlusSaved = view.findViewById<Button>(R.id.btn_plus_saved)

                btnMinusSaved.setOnClickListener {
                    if (canEdit && char.savedPoints > 0) {
                        viewModel.updateSavedPoints(char.savedPoints - 1)
                    }
                }
                btnPlusSaved.setOnClickListener {
                     if (canEdit) {
                        viewModel.updateSavedPoints(char.savedPoints + 1)
                     }
                }

                savedPointsText.setOnClickListener {
                    if (canEdit) {
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
                }

                // Experience Logic
                val experienceText = view.findViewById<TextView>(R.id.text_experience)
                experienceText.text = char.experience.toString()
                
                val btnMinusXp = view.findViewById<Button>(R.id.btn_minus_xp)
                val btnPlusXp = view.findViewById<Button>(R.id.btn_plus_xp)

                btnMinusXp.setOnClickListener {
                    if (canEdit && char.experience > 0) {
                         viewModel.updateExperience(char.experience - 1)
                    }
                }
                btnPlusXp.setOnClickListener {
                    if (canEdit) {
                        viewModel.updateExperience(char.experience + 1)
                    }
                }

                // Removed direct editing click listener for experience as requested
                
                // Visibility of buttons based on canEdit
                val controls = listOf(btnMinusSaved, btnPlusSaved, btnMinusXp, btnPlusXp)
                controls.forEach { it.visibility = if (canEdit) View.VISIBLE else View.INVISIBLE }

                // Observer for Unique Advantages to keep local list updated
                viewModel.availableUniqueAdvantages.observe(viewLifecycleOwner) { uas ->
                    // Just update a local reference or the adapter if we had one here (we don't, it's for the dialog)
                    // We can access viewModel.availableUniqueAdvantages.value directly in the click listener,
                    // but since LiveData value can be null, we rely on the ViewModel state.
                }

                btnSelectUA.setOnClickListener {
                    val uas = viewModel.availableUniqueAdvantages.value ?: emptyList()
                    if (uas.isNotEmpty()) {
                        if (parentFragmentManager.findFragmentByTag("SelectUADialog") == null) {
                             val dialog = SelectUniqueAdvantageDialogFragment(
                                 availableUAs = uas,
                                 canManage = isMaster, 
                                 onSelect = { selectedUA ->
                                     viewModel.setUniqueAdvantage(selectedUA)
                                 },
                                 onAddCustom = { newUA ->
                                     viewModel.addCustomUniqueAdvantage(newUA)
                                 },
                                 onEditCustom = { oldUA, newUA ->
                                     viewModel.updateCustomUniqueAdvantage(oldUA, newUA)
                                 },
                                 onDeleteCustom = { uaToDelete ->
                                     viewModel.removeCustomUniqueAdvantage(uaToDelete)
                                 }
                             )
                             dialog.show(parentFragmentManager, "SelectUADialog")
                        }
                    } else {
                        // Maybe trigger load if empty? But it should be loaded by character observer.
                        Toast.makeText(requireContext(), "Carregando vantagens...", Toast.LENGTH_SHORT).show()
                    }
                }

                btnManageTypes.setOnClickListener {
                    val types = viewModel.availableDamageTypes.value ?: emptyList()
                    // Filter out defaults usually? Or allow removing custom only.
                    // The dialog logic handles removing custom. We pass the custom ones?
                    // The ViewModel logic handles Add/Remove.
                    // Let's pass the current *custom* list? ViewModel knows it. 
                    // Actually, the dialog adapter shows ALL types? 
                    // The request said "edit the list to add/remove options".
                    // Usually you only edit the custom ones.
                    // Let's assume dialog shows list.
                    // For simplicity, let's just observe data in dialog or pass current list.
                    // Ideally we fetch custom types from Table object again or use VM.
                    // Let's use VM helper.
                    
                    val dialog = ManageDamageTypesDialogFragment(
                         currentTypes = viewModel.availableDamageTypes.value?.filter { 
                             // Filter out defaults if we want to show only customs?
                             // User said "add or remove options". 
                             // "Delete" default options might be bad.
                             // Let's pass all, but adapter should disable delete for defaults.
                             // How to know defaults? defined in VM.
                             // For now pass all.
                             true
                         } ?: emptyList(),
                         onAdd = { viewModel.addCustomDamageType(it) },
                         onRemove = { viewModel.removeCustomDamageType(it) }
                    )
                    dialog.show(parentFragmentManager, "ManageDamageTypes")
                }

                // Setup Spinners
                viewModel.availableDamageTypes.observe(viewLifecycleOwner) { types ->
                    val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerForca.adapter = adapter
                    spinnerPdf.adapter = adapter

                    // Set current selections
                    val indexF = types.indexOf(char.damageTypeForca)
                    if (indexF >= 0) spinnerForca.setSelection(indexF)

                    val indexP = types.indexOf(char.damageTypePdf)
                    if (indexP >= 0) spinnerPdf.setSelection(indexP)
                }

                spinnerForca.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                         val selected = parent?.getItemAtPosition(position) as? String
                         if (selected != null && selected != char.damageTypeForca && canEdit) { // check recursion
                             viewModel.updateDamageType(selected, false)
                         }
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }

                spinnerPdf.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                         val selected = parent?.getItemAtPosition(position) as? String
                         if (selected != null && selected != char.damageTypePdf && canEdit) {
                             viewModel.updateDamageType(selected, true)
                         }
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }
                
                // Notes EditText
                val notesEdit = view.findViewById<EditText>(R.id.edit_notes)
                notesEdit.isEnabled = canEdit
                
                // Hidden Checkbox (Master Only)


                
                // Delete Button Visibility
                // Delete Button Visibility
                val canDelete = true
                android.util.Log.d("SheetDebug", "Delete Visibility: Offline Mode -> canDelete=$canDelete")
                view.findViewById<Button>(R.id.btn_delete_character).visibility = if (canDelete) View.VISIBLE else View.GONE
                
                // --- MOVED ADAPTER LOGIC INSIDE LAUNCH SCOPE ---
                // Update Advantages List
                val advantagesRecycler = view.findViewById<RecyclerView>(R.id.recycler_advantages)
                advantagesRecycler.layoutManager = LinearLayoutManager(context)
                val adapter = AdvantagesAdapter(items = char.vantagens, onItemClick = { selectedItem ->
                    // Open Edit Dialog with Remove option
                    val editDialog = EditAdvantageDialogFragment(
                        advantage = selectedItem,
                        onSave = { updatedItem ->
                            viewModel.updateAdvantage(updatedItem)
                        },
                        onDelete = { itemToDelete ->
                            viewModel.removeAdvantage(itemToDelete)
                        }
                    )
                    editDialog.show(parentFragmentManager, "EditAdvantageDialog")
                })
                advantagesRecycler.adapter = adapter
    
                // Update Disadvantages List
                val disadvantagesRecycler = view.findViewById<RecyclerView>(R.id.recycler_disadvantages)
                disadvantagesRecycler.layoutManager = LinearLayoutManager(context)
                val disAdapter = AdvantagesAdapter(items = char.desvantagens, onItemClick = { selectedItem ->
                    // Open Edit Dialog with Remove option
                    val editDialog = EditAdvantageDialogFragment(
                        advantage = selectedItem,
                        onSave = { updatedItem ->
                            viewModel.updateDisadvantage(updatedItem)
                        },
                        onDelete = { itemToDelete ->
                            viewModel.removeDisadvantage(itemToDelete)
                        }
                    )
                    editDialog.show(parentFragmentManager, "EditDisadvantageDialog")
                })
                disadvantagesRecycler.adapter = disAdapter
    
                // Update Skills List
                val skillsRecycler = view.findViewById<RecyclerView>(R.id.recycler_skills)
                skillsRecycler.layoutManager = LinearLayoutManager(context)
                val skillsAdapter = AdvantagesAdapter(items = char.pericias, onItemClick = { selectedItem ->
                    // Open Edit Dialog with Remove option
                    val editDialog = EditSkillDialogFragment(
                        skill = selectedItem,
                        onSave = { updatedItem ->
                            viewModel.updateSkill(updatedItem)
                        },
                        onDelete = { itemToDelete ->
                            viewModel.removeSkill(itemToDelete)
                        }
                    )
                    editDialog.show(parentFragmentManager, "EditSkillDialog")
                })
                skillsRecycler.adapter = skillsAdapter
    
                // Update Specializations List
                val specsRecycler = view.findViewById<RecyclerView>(R.id.recycler_specializations)
                specsRecycler.layoutManager = LinearLayoutManager(context)
                val specsAdapter = AdvantagesAdapter(items = char.especializacoes, onItemClick = { selectedItem ->
                    // Open Edit Dialog
                    val editDialog = EditSpecializationDialogFragment(
                        specialization = selectedItem,
                        onSave = { updatedItem ->
                            viewModel.updateSpecialization(updatedItem)
                        },
                        onDelete = { itemToDelete ->
                            viewModel.removeSpecialization(itemToDelete)
                        }
                    )
                    editDialog.show(parentFragmentManager, "EditSpecDialog")
                })
                specsRecycler.adapter = specsAdapter
    
                // Update Inventory List
                val invRecycler = view.findViewById<RecyclerView>(R.id.recycler_inventory)
                invRecycler.layoutManager = LinearLayoutManager(context)
                val invAdapter = InventoryAdapter(char.inventario, canEdit, 
                    onItemClick = { selectedItem ->
                        if (!canEdit) return@InventoryAdapter
                        val editDialog = EditInventoryItemDialogFragment(
                            item = selectedItem,
                            onSave = { updatedItem -> viewModel.updateInventoryItem(updatedItem) },
                            onDelete = { itemToDelete -> viewModel.removeInventoryItem(itemToDelete) }
                        )
                        editDialog.show(parentFragmentManager, "EditInvDialog")
                    },
                    onQuantityChange = { item, delta ->
                        viewModel.adjustInventoryQuantity(item, delta)
                    }
                )
                invRecycler.adapter = invAdapter
    


    
                // Update Notes (Prevent overwriting if user is typing)
                if (!view.findViewById<EditText>(R.id.edit_notes).hasFocus()) {
                     view.findViewById<EditText>(R.id.edit_notes).setText(androidx.core.text.HtmlCompat.fromHtml(char.anotacoes, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY))
                }
            } // End of launch scope

        }
        
        viewModel.lastRoll.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                rollResultCard.visibility = View.VISIBLE
                rollNameText.text = result.name
                rollTotalText.text = result.total.toString()
                rollDetailText.text = if (result.details.isNotEmpty()) result.details else {
                    val bonusText = if (result.bonus > 0) " + ${result.bonus}" else ""
                    "${result.attributeUsed}(${result.attributeValue}) + ${result.die}$bonusText"
                }
                
                if (result.isCritical) {
                    rollTotalText.setTextColor(Color.parseColor("#D97706")) // Yellow/Gold
                } else {
                    rollTotalText.setTextColor(Color.WHITE)
                }
            } else {
                rollResultCard.visibility = View.GONE
            }
        }

        viewModel.rollEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { result ->
                // Send to Chat if in a table
                // if (tableId != null) {
                //      val avatarUrl = viewModel.character.value?.imageUrl
                //      chatViewModel.sendRollResult(result, avatarUrl)
                // }
            }
        }

        viewModel.isRolling.observe(viewLifecycleOwner) { isRolling ->
            val buttons = listOf<Button>(
                view.findViewById(R.id.btn_attack_f),
                view.findViewById(R.id.btn_attack_pdf),
                view.findViewById(R.id.btn_defense)
            )
            buttons.forEach { it.isEnabled = !isRolling }
            
            if (isRolling) {
                rollResultCard.visibility = View.VISIBLE
                rollTotalText.setTextColor(Color.YELLOW)
            }
        }

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

        // Notes Saving Logic & Rich Text
        val notesEdit = view.findViewById<EditText>(R.id.edit_notes)
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


        view.findViewById<Button>(R.id.btn_export_character).setOnClickListener {
            val char = viewModel.character.value
            if (char != null) {
                val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                val safeName = char.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                exportLauncher.launch("char_${safeName}_$dateStr.json")
            }
        }

        // Delete Button
        val btnDelete = view.findViewById<Button>(R.id.btn_delete_character)
        btnDelete.setOnClickListener {
            val char = viewModel.character.value
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
        
        // Initial visibility check for delete button
        // We do this in the observer, but let's set it GONE initially to avoid flicker
        btnDelete.visibility = View.GONE

        // --- ATTRIBUTES & RESOURCES BUTTONS (OFFLINE MODE) ---
        // Ensuring these are visible and functional
        val btnAddAttr = view.findViewById<Button>(R.id.btn_add_attribute)
        val btnAddRes = view.findViewById<Button>(R.id.btn_add_resource)
        
        // Protection: formatting visibility based on system
        viewModel.ruleSystem.observe(viewLifecycleOwner) { sys ->
            val isBase = sys.id == "3det_alpha_base" || sys.isBaseSystem
            val canEditSystem = !isBase
            
            if (canEditSystem) {
                btnAddAttr.visibility = View.VISIBLE
                btnAddRes.visibility = View.VISIBLE
            } else {
                btnAddAttr.visibility = View.GONE
                btnAddRes.visibility = View.GONE
            }
        }

        btnAddAttr.setOnClickListener {
             // Extra check
             val sys = viewModel.ruleSystem.value
             if (sys != null && (sys.id == "3det_alpha_base" || sys.isBaseSystem)) {
                 Toast.makeText(context, "Não é possível editar o Sistema Base. Use 'Salvar como' primeiro.", Toast.LENGTH_LONG).show()
                 return@setOnClickListener
             }

             DialogEditAttributeDefinition(null, { newAttr ->
                 viewModel.addAttributeDefinition(newAttr)
             }).show(parentFragmentManager, "AddAttribute")
        }

        btnAddRes.setOnClickListener {
             val sys = viewModel.ruleSystem.value
             if (sys != null && (sys.id == "3det_alpha_base" || sys.isBaseSystem)) {
                 Toast.makeText(context, "Não é possível editar o Sistema Base. Use 'Salvar como' primeiro.", Toast.LENGTH_LONG).show()
                 return@setOnClickListener
             }
             
             DialogEditResourceDefinition(null, { newRes ->
                 viewModel.addResourceDefinition(newRes)
             }).show(parentFragmentManager, "AddResource")
        }


        // Custom Rolls Setup
        val customRollsRecycler = view.findViewById<RecyclerView>(R.id.recycler_custom_rolls)
        val btnAddCustomRoll = view.findViewById<Button>(R.id.btn_add_custom_roll)
        
        // Use Flexbox or GridLayout? For now LinearLayout vertical or horizontal?
        // Layout manager was not set in XML. Let's use GridLayoutManager for buttons grid
        customRollsRecycler.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 2)
        
        val customRollsAdapter = CustomRollsAdapter(
            items = mutableListOf(),
            onRollClick = { roll -> viewModel.rollCustom(roll) },
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

        viewModel.character.observe(viewLifecycleOwner) { char ->
            if (char == null) return@observe
             val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id
             val effectiveTableId = tableId ?: char.tableId
             
             viewLifecycleOwner.lifecycleScope.launch {
                 val table = if (effectiveTableId.isNotEmpty()) com.galeria.defensores.data.TableRepository.getTable(effectiveTableId) else null
                 val isMaster = table?.masterId == currentUserId || table?.masterId == "mock-master-id"
                 
                 val canManageRolls = isMaster
                 
                 btnAddCustomRoll.visibility = if (canManageRolls) View.VISIBLE else View.GONE
                 
                 // update adapter permissions
                 val newAdapter = CustomRollsAdapter(
                    items = char.customRolls,
                    onRollClick = { roll -> viewModel.rollCustom(roll) },
                    onEditClick = { roll ->
                         if (canManageRolls) {
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
                         }
                    },
                    canEdit = canManageRolls
                 )
                 customRollsRecycler.adapter = newAdapter
             }
        }
        
    }

    private fun checkPermissionAndRoll(type: RollType) {
        // Re-check permission on click to be safe, or store it in a member variable
        // For simplicity and safety, let's fetch current state
        val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id
        val char = viewModel.character.value
        
        if (char != null && currentUserId != null) {
            // We need to fetch table to know if isMaster. 
            // Since this is async, we might want to store 'isMaster' in ViewModel or Fragment scope.
            // However, we already did this check in onViewCreated. Let's promote 'canEdit' to a class property?
            // Or better: just check ownerId for now, and if not owner, check table master async or assume false if not loaded.
            
            // A safer quick fix: rely on the UI state. If buttons are enabled/visible, user can click.
            // But we added a Toast for disabled state.
            
            // Let's implement a proper check
            viewLifecycleOwner.lifecycleScope.launch {
                val table = if (char.tableId.isNotEmpty()) com.galeria.defensores.data.TableRepository.getTable(char.tableId) else null
                val isMaster = table?.masterId == currentUserId || table?.masterId == "mock-master-id"
                val isOwner = char.ownerId == currentUserId
                
                if (isMaster || isOwner) {
                    viewModel.rollDice(type)
                } else {
                    Toast.makeText(context, "Apenas o dono ou mestre pode rolar dados.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



}
