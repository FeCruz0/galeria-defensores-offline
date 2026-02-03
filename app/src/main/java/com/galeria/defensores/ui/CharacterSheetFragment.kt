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
import com.galeria.defensores.models.Buff
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_character_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind UI
        nameEdit = view.findViewById(R.id.edit_char_name)
        rollResultCard = view.findViewById(R.id.card_roll_result)
        rollTotalText = view.findViewById(R.id.text_roll_total)
        rollDetailText = view.findViewById(R.id.text_roll_detail)
        rollNameText = view.findViewById(R.id.text_roll_name)
        
        val hiddenCheck = view.findViewById<android.widget.CheckBox>(R.id.check_hidden)
        
        val avatarImage = view.findViewById<ImageView>(R.id.img_character_avatar)
        val editAvatarIcon = view.findViewById<ImageView>(R.id.img_edit_avatar_icon)

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

        // Setup Attribute Labels and Inputs
        setupAttributeInput(view.findViewById(R.id.attr_forca), "Força", 0, "#EF4444", "forca") // Red
        setupAttributeInput(view.findViewById(R.id.attr_habilidade), "Habilidade", 0, "#3B82F6", "habilidade") // Blue
        setupAttributeInput(view.findViewById(R.id.attr_resistencia), "Resistência", 0, "#10B981", "resistencia") // Green
        setupAttributeInput(view.findViewById(R.id.attr_armadura), "Armadura", 0, "#6B7280", "armadura") // Gray
        setupAttributeInput(view.findViewById(R.id.attr_pdf), "Poder de Fogo", 0, "#8B5CF6", "poderFogo") // Purple

        // Setup Status Labels
        setupStatusManager(view.findViewById(R.id.status_pv), "Pontos de Vida", 0, "#EF4444", "pv")
        setupStatusManager(view.findViewById(R.id.status_pm), "Pontos de Magia", 0, "#3B82F6", "pm")

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

        viewModel.character.observe(viewLifecycleOwner) { char ->
            if (char == null) return@observe

            
            if (!nameEdit.hasFocus()) {
                nameEdit.setText(char.name)
            }

            // Restore lost Data Binding
            updateAttributeValue(view.findViewById(R.id.attr_forca), char.forca)
            updateAttributeValue(view.findViewById(R.id.attr_habilidade), char.habilidade)
            updateAttributeValue(view.findViewById(R.id.attr_resistencia), char.resistencia)
            updateAttributeValue(view.findViewById(R.id.attr_armadura), char.armadura)
            updateAttributeValue(view.findViewById(R.id.attr_pdf), char.poderFogo)

            updateStatusValue(view.findViewById(R.id.status_pv), char.currentPv, char.getMaxPv())
            updateStatusValue(view.findViewById(R.id.status_pm), char.currentPm, char.getMaxPm())

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
                val table = if (effectiveTableId.isNotEmpty()) com.galeria.defensores.data.TableRepository.getTable(effectiveTableId) else null
                
                val isMaster = table?.masterId == currentUserId || table?.masterId == "mock-master-id"
                val isOwner = char.ownerId == currentUserId
                canEdit = isMaster || isOwner
                val canEditScale = isMaster // Strictly Master unless not in table? Requirement: "apenas o mestre".
                // If not in a table, isOwner could edit? Let's assume yes if tableId is empty.
                val effectivelyCanEditScale = if (char.tableId.isEmpty()) isOwner else isMaster
                
                // Load Damage Types if not already associated (or just refresh)
                if (char.tableId.isNotEmpty()) {
                    viewModel.loadDamageTypes(char.tableId)
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

                // Enable/Disable Status Editing
                updateStatusPermissions(view.findViewById(R.id.status_pv), canEdit)
                updateStatusPermissions(view.findViewById(R.id.status_pm), canEdit)

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

                btnSelectUA.setOnClickListener {
                    viewModel.availableUniqueAdvantages.observe(viewLifecycleOwner) { uas ->
                        // Prevent multiple dialogs if called rapidly or multiple updates
                        // Ideally we check if dialog is added.
                         if (parentFragmentManager.findFragmentByTag("SelectUADialog") == null) {
                             val dialog = SelectUniqueAdvantageDialogFragment(
                                 availableUAs = uas,
                                 canManage = isMaster, // Only master can manage custom UAs
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
                if (isMaster) {
                    hiddenCheck.visibility = View.VISIBLE
                    hiddenCheck.isChecked = char.isHidden
                    hiddenCheck.setOnCheckedChangeListener { _, isChecked ->
                        if (char.isHidden != isChecked) {
                            viewModel.updateHidden(isChecked)
                        }
                    }
                } else {
                    hiddenCheck.visibility = View.GONE
                }
                // Enable/Disable Attribute Inputs
                val attributeContainers = listOf(
                    view.findViewById<View>(R.id.attr_forca),
                    view.findViewById<View>(R.id.attr_habilidade),
                    view.findViewById<View>(R.id.attr_resistencia),
                    view.findViewById<View>(R.id.attr_armadura),
                    view.findViewById<View>(R.id.attr_pdf)
                )
                
                attributeContainers.forEach { container ->
                    container.findViewById<EditText>(R.id.attribute_input).isEnabled = canEdit
                    container.findViewById<Button>(R.id.btn_minus).isEnabled = canEdit
                    container.findViewById<Button>(R.id.btn_plus).isEnabled = canEdit
                }
                
                // Delete Button Visibility
                val canDelete = currentUserId != null && (isOwner || isMaster)
                android.util.Log.d("SheetDebug", "Delete Visibility: User=$currentUserId, Owner=${char.ownerId}, Master=${table?.masterId} -> canDelete=$canDelete")
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
    
                // Update Spells List
                val spellsRecycler = view.findViewById<RecyclerView>(R.id.recycler_spells)
                spellsRecycler.layoutManager = LinearLayoutManager(context)
                val spellsAdapter = SpellsAdapter(spells = char.magias, onSpellClick = { selectedSpell ->
                     if (canEdit) {
                         val editDialog = EditSpellDialogFragment(
                             spell = selectedSpell,
                             onSave = { updatedSpell ->
                                 viewModel.updateSpell(updatedSpell)
                             },
                             onDelete = { spellToDelete ->
                                 viewModel.removeSpell(spellToDelete)
                             }
                         )
                         editDialog.show(parentFragmentManager, "EditSpellDialog")
                     }
                })
                spellsRecycler.adapter = spellsAdapter
    
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
    
                // Update Buffs List
                val buffsRecycler = view.findViewById<RecyclerView>(R.id.recycler_buffs)
                buffsRecycler.layoutManager = LinearLayoutManager(context)
                val buffsAdapter = BuffsAdapter(char.buffs, isMaster) { buffToDelete ->
                     viewModel.removeBuff(buffToDelete)
                }
                buffsRecycler.adapter = buffsAdapter
                
                view.findViewById<Button>(R.id.btn_add_buff).visibility = if (isMaster) View.VISIBLE else View.GONE
                view.findViewById<Button>(R.id.btn_add_buff).setOnClickListener {
                     val dialog = EditBuffDialogFragment(null) { newBuff ->
                         viewModel.addBuff(newBuff)
                     }
                     dialog.show(parentFragmentManager, "AddBuffDialog")
                }
    
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

    private fun setupAttributeInput(view: View, label: String, iconRes: Int, colorHex: String, attrKey: String) {
        val labelView = view.findViewById<TextView>(R.id.attribute_label)
        val iconView = view.findViewById<ImageView>(R.id.attribute_icon)
        val inputView = view.findViewById<EditText>(R.id.attribute_input)

        labelView.text = label
        // iconView.setImageResource(iconRes) // Need to add icons or use defaults
        iconView.setColorFilter(Color.parseColor(colorHex))
        labelView.setTextColor(Color.parseColor(colorHex))

        inputView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (inputView.hasFocus()) {
                    val value = s.toString().toIntOrNull() ?: 0
                    viewModel.updateAttribute(attrKey, value)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        view.findViewById<Button>(R.id.btn_minus).setOnClickListener {
            val currentValue = inputView.text.toString().toIntOrNull() ?: 0
            if (currentValue > 0) {
                viewModel.updateAttribute(attrKey, currentValue - 1)
                inputView.setText((currentValue - 1).toString())
            }
        }

        view.findViewById<Button>(R.id.btn_plus).setOnClickListener {
            val currentValue = inputView.text.toString().toIntOrNull() ?: 0
            if (currentValue < 99) {
                viewModel.updateAttribute(attrKey, currentValue + 1)
                inputView.setText((currentValue + 1).toString())
            }
        }
    }
    
    private fun updateAttributeValue(view: View, value: Int) {
        val inputView = view.findViewById<EditText>(R.id.attribute_input)
        if (!inputView.hasFocus()) {
            inputView.setText(value.toString())
        }
    }

    private fun setupStatusManager(view: View, label: String, iconRes: Int, colorHex: String, typeKey: String) {
        val labelView = view.findViewById<TextView>(R.id.status_label)
        val iconView = view.findViewById<ImageView>(R.id.status_icon)
        val barView = view.findViewById<ProgressBar>(R.id.status_bar)
        val valueView = view.findViewById<TextView>(R.id.status_value)
        
        labelView.text = label
        // iconView.setImageResource(iconRes)
        iconView.setColorFilter(Color.parseColor(colorHex))
        labelView.setTextColor(Color.parseColor(colorHex))
        
        barView.progressTintList = ColorStateList.valueOf(Color.parseColor(colorHex))

        view.findViewById<Button>(R.id.btn_minus_5).setOnClickListener { viewModel.updateStatus(typeKey, -5) }
        view.findViewById<Button>(R.id.btn_minus_1).setOnClickListener { viewModel.updateStatus(typeKey, -1) }
        view.findViewById<Button>(R.id.btn_plus_1).setOnClickListener { viewModel.updateStatus(typeKey, 1) }
        view.findViewById<Button>(R.id.btn_plus_5).setOnClickListener { viewModel.updateStatus(typeKey, 5) }

        // Direct Edit Listener
        valueView.setOnClickListener {
            val context = view.context
            val input = EditText(context)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            input.setText(valueView.text.toString().split(" / ")[0]) // Get current value

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Editar $label")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val newValue = input.text.toString().toIntOrNull()
                    if (newValue != null) {
                        viewModel.setStatus(typeKey, newValue)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun updateStatusPermissions(view: View, canEdit: Boolean) {
        val buttons = listOf<Button>(
            view.findViewById(R.id.btn_minus_5),
            view.findViewById(R.id.btn_minus_1),
            view.findViewById(R.id.btn_plus_1),
            view.findViewById(R.id.btn_plus_5)
        )
        buttons.forEach { it.isEnabled = canEdit }
        
        val valueView = view.findViewById<TextView>(R.id.status_value)
        valueView.isEnabled = canEdit
    }

    private fun updateStatusValue(view: View, current: Int, max: Int) {
        val valueView = view.findViewById<TextView>(R.id.status_value)
        val barView = view.findViewById<ProgressBar>(R.id.status_bar)
        
        valueView.text = "$current / $max"
        barView.max = max
        barView.progress = current
    }
    private inner class BuffsAdapter(
        private val items: List<Buff>,
        private val canEdit: Boolean,
        private val onDelete: (Buff) -> Unit
    ) : RecyclerView.Adapter<BuffsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.text_advantage_name)
            val descText: TextView = view.findViewById(R.id.text_advantage_description)
            val deleteBtn: View = view.findViewById(R.id.btn_delete_item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_advantage, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.nameText.text = "${item.name} (${if(item.value >= 0) "+"+item.value else item.value})"
            holder.descText.text = "Alvo: ${item.targetType}"
            
            if (canEdit) {
                holder.deleteBtn.visibility = View.VISIBLE
                holder.deleteBtn.setOnClickListener { onDelete(item) }
            } else {
                holder.deleteBtn.visibility = View.GONE
            }
        }

        override fun getItemCount() = items.size
    }
}
