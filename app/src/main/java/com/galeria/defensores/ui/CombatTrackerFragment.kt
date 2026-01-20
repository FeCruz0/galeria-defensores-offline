package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.galeria.defensores.R
import com.galeria.defensores.models.Combatant
import com.galeria.defensores.viewmodels.CombatViewModel
import com.galeria.defensores.data.SessionManager

class CombatTrackerFragment : Fragment() {

    private lateinit var viewModel: CombatViewModel
    private var tableId: String? = null
    private lateinit var adapter: CombatantsAdapter
    
    // UI
    private lateinit var statusText: TextView
    private lateinit var btnAction: Button
    private lateinit var btnNextTurn: Button
    private lateinit var titleText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_combat_tracker, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tableId = arguments?.getString("table_id")
        viewModel = ViewModelProvider(this).get(CombatViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Bind UI
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_combatants)
        statusText = view.findViewById(R.id.text_action_status)
        btnAction = view.findViewById(R.id.btn_combat_action)
        btnNextTurn = view.findViewById(R.id.btn_next_turn)
        titleText = view.findViewById(R.id.text_combat_title)

        recycler.layoutManager = LinearLayoutManager(context)
        adapter = CombatantsAdapter()
        recycler.adapter = adapter

        if (tableId != null) {
            viewModel.setTableId(tableId!!)
        }

        // State Observer
        viewModel.combatState.observe(viewLifecycleOwner) { state ->
            if (state == null) {
                // No Active Combat
                titleText.text = "Combate Inativo"
                statusText.text = "Nenhum combate em andamento."
                btnAction.text = "Iniciar Combate"
                btnNextTurn.visibility = View.GONE
                
                btnAction.setOnClickListener {
                    // Logic to select participants and start
                    // For now, simple start with all table characters?
                    // We need a dialog to select. But for MVP, let's load all characters.
                    startCombatFlow()
                }
                adapter.submitList(emptyList(), -1)
                return@observe
            }

            // Master Check (Simplified for MVP - ideally check table.masterId)
            // For now, enable Start/Next for everyone to test, or check strict rule later.
            val isMaster = true 
            
            // Logic for Buttons
            val currentCombatant = state.combatants.getOrNull(state.currentTurnIndex)
            val currentUser = SessionManager.currentUser?.id
            
            if (state.pendingAction != null) {
                // Pending Action State
                val action = state.pendingAction!!
                if (action.type == "ATTACK") {
                     val targetName = state.combatants.find { it.characterId == action.targetId }?.name ?: "Unknown"
                     val attackerName = state.combatants.find { it.characterId == action.attackerId }?.name ?: "Unknown"
                     statusText.text = "$attackerName ataca $targetName! (Rolagem: ${action.attackRoll})\n$targetName deve defender!"
                     
                     // Check if I am the target
                     // For MVP, enable Defend button for everyone or check ID
                     // val amITarget = action.targetId == currentUser
                     val amITarget = true // Open for testing
                     
                     if (amITarget) {
                         btnAction.text = "DEFENDER"
                         btnAction.visibility = View.VISIBLE
                         btnAction.setOnClickListener {
                             // Roll Defense Dialog/Logic
                             showDefenseDialog(action)
                         }
                     } else {
                         btnAction.visibility = View.GONE
                     }
                     btnNextTurn.visibility = View.GONE 
                }
            } else {
                // Normal Turn State
                statusText.text = "Turno de: ${currentCombatant?.name}"
                val amIActive = true // Open for testing (currentCombatant?.characterId == currentUser)
                
                if (amIActive) {
                    btnAction.text = "ATACAR"
                    btnAction.visibility = View.VISIBLE
                    btnAction.setOnClickListener {
                        showAttackDialog(state.combatants, currentCombatant?.characterId ?: "")
                    }
                } else {
                    btnAction.visibility = View.GONE
                }
                
                if (isMaster) btnNextTurn.visibility = View.VISIBLE else btnNextTurn.visibility = View.GONE
            }
            
            btnNextTurn.setOnClickListener { viewModel.nextTurn() }
        }
    }
    
    private fun startCombatFlow() {
        viewModel.getAvailableCharacters { chars ->
            if (chars.isEmpty()) {
                Toast.makeText(context, "Nenhum personagem na mesa.", Toast.LENGTH_SHORT).show()
                return@getAvailableCharacters
            }
            
            // Show Multi-Select Dialog
            val charNames = chars.map { it.name }.toTypedArray()
            val selected = BooleanArray(chars.size) { true } // Default all selected
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Iniciar Combate: Selecionar Participantes")
                .setMultiChoiceItems(charNames, selected) { _, which, isChecked ->
                    selected[which] = isChecked
                }
                .setPositiveButton("Iniciar") { _, _ ->
                    val selectedChars = chars.filterIndexed { index, _ -> selected[index] }
                    if (selectedChars.isNotEmpty()) {
                        viewModel.startCombat(selectedChars)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun showAttackDialog(combatants: List<Combatant>, attackerId: String) {
        val targets = combatants.filter { it.characterId != attackerId && !it.isDefeated }
        val targetNames = targets.map { it.name }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Selecionar Alvo")
            .setItems(targetNames) { _, which ->
                val target = targets[which]
                // Roll Attack Logic
                // For MVP: Simple input or auto-roll?
                // Let's ask for Roll Value (simulating the player rolling dice physically or via chat)
                showRollInputDialog("Rolagem de Ataque") { rollVal ->
                     viewModel.attack(attackerId, target.characterId, rollVal, "Ataque Manual")
                }
            }
            .show()
    }

    private fun showDefenseDialog(action: com.galeria.defensores.models.CombatAction) {
        showRollInputDialog("Rolagem de Defesa") { rollVal ->
             viewModel.defend(action.targetId, rollVal, "Defesa Manual")
        }
    }

    private fun showRollInputDialog(title: String, onResult: (Int) -> Unit) {
        val input = android.widget.EditText(context)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage("Insira o valor total da rolagem:")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val stringVal = input.text.toString()
                val intVal = stringVal.toIntOrNull()
                if (intVal != null) onResult(intVal)
            }
            .show()
    }
}

class CombatantsAdapter : RecyclerView.Adapter<CombatantsAdapter.ViewHolder>() {
    private var list: List<Combatant> = emptyList()
    private var activeIndex: Int = -1

    fun submitList(newList: List<Combatant>, index: Int) {
        list = newList
        activeIndex = index
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(android.R.id.text1) // Placeholder layout
        // Custom layout needed for Combatant Item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Use simple list item for now or create item_combatant.xml
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.nameText.text = "${item.name} (PV: ${item.currentPv}/${item.maxPv}) Init: ${item.initiative}"
        
        if (position == activeIndex) {
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#1F2937")) // Highlight
        } else {
             holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    override fun getItemCount() = list.size
}
