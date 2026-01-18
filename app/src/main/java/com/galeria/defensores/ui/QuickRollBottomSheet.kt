package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.CharacterRepository
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.models.CustomRoll
import com.galeria.defensores.models.RollResult
import com.galeria.defensores.models.RollType
import com.galeria.defensores.viewmodels.CharacterViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickRollBottomSheet(
    private val tableId: String, 
    private val onRollResult: (RollResult, String?) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var viewModel: CharacterViewModel
    private var myCharacterId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_quick_roll, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this).get(CharacterViewModel::class.java)

        val recycler = view.findViewById<RecyclerView>(R.id.recycler_quick_rolls)
        val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progress_quick_roll)
        val emptyText = view.findViewById<TextView>(R.id.text_no_custom_rolls)
        val spinner = view.findViewById<android.widget.Spinner>(R.id.spinner_quick_chars)
        
        recycler.layoutManager = GridLayoutManager(context, 2)

        // 1. Fetch User's Characters
        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = SessionManager.currentUser?.id
            if (userId == null) {
                Toast.makeText(context, "Erro: Usuário não identificado", Toast.LENGTH_SHORT).show()
                dismiss()
                return@launch
            }

            val table = withContext(Dispatchers.IO) {
                try { com.galeria.defensores.data.TableRepository.getTable(tableId) } catch (e: Exception) { null }
            }
            
            val isMaster = table?.masterId == userId || table?.masterId == "mock-master-id"

            val characters = withContext(Dispatchers.IO) {
                try {
                    CharacterRepository.getCharacters(tableId)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // If Master, show ALL characters (for controlling NPCs etc). If Player, show only OWNED.
            // User request implies "Select Player". Usually GM wants to control NPCs. 
            // Let's filter: if Master -> All (or maybe just NPCs + Own? For now All is safer).
            // If Player -> Own.
            val myChars = if (isMaster) characters else characters.filter { it.ownerId == userId }
            
            progressBar.visibility = View.GONE

            if (myChars.isNotEmpty()) {
                spinner.visibility = View.VISIBLE
                val adapter = android.widget.ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    myChars.map { it.name }
                )
                spinner.adapter = adapter
                
                spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedChar = myChars[position]
                        myCharacterId = selectedChar.id
                        viewModel.loadCharacter(selectedChar.id, tableId)
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }
                
                // Initial load handled by spinner selection (default pos 0)
                
            } else {
                spinner.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                emptyText.text = "Você não tem personagens nesta mesa."
                disableButtons(view)
            }
        }
        
        // 2. Observe Character to Update UI (Custom Rolls)
        viewModel.character.observe(viewLifecycleOwner) { char ->
             if (char != null) {
                 setupStandardButtons(view) // Enable buttons
                 
                 if (char.customRolls.isNotEmpty()) {
                    recycler.adapter = QuickRollAdapter(char.customRolls) { roll ->
                        viewModel.rollCustom(roll)
                    }
                    recycler.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                } else {
                    recycler.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "Nenhuma rolagem personalizada criada."
                }
             } else {
                 disableButtons(view)
             }
        }
        
        // 3. Observe Results
        viewModel.rollEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { result ->
                val avatarUrl = viewModel.character.value?.imageUrl
                onRollResult(result, avatarUrl)
                dismiss()
            }
        }

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
    }
    
    private fun disableButtons(view: View) {
        val ids = listOf(R.id.btn_quick_attack_f, R.id.btn_quick_attack_pdf, R.id.btn_quick_defense, R.id.btn_quick_initiative)
        ids.forEach { id -> view.findViewById<View>(id).isEnabled = false }
    }

    private fun setupStandardButtons(view: View) {
        view.findViewById<Button>(R.id.btn_quick_attack_f).setOnClickListener {
            viewModel.rollDice(RollType.ATTACK_F)
        }
        view.findViewById<Button>(R.id.btn_quick_attack_pdf).setOnClickListener {
             viewModel.rollDice(RollType.ATTACK_PDF)
        }
        view.findViewById<Button>(R.id.btn_quick_defense).setOnClickListener {
             viewModel.rollDice(RollType.DEFENSE)
        }
        view.findViewById<Button>(R.id.btn_quick_initiative).setOnClickListener {
             viewModel.rollDice(RollType.INITIATIVE)
        }
    }
    
    private inner class QuickRollAdapter(
        private val rolls: List<CustomRoll>,
        private val onClick: (CustomRoll) -> Unit
    ) : RecyclerView.Adapter<QuickRollAdapter.ViewHolder>() {
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.btn_custom_roll)
            
            fun bind(roll: CustomRoll) {
                title.text = roll.name
                itemView.setOnClickListener { onClick(roll) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_custom_roll, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(rolls[position])
        }

        override fun getItemCount() = rolls.size
    }
}
