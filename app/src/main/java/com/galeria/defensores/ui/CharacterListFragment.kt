package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.CharacterRepository
import com.galeria.defensores.models.Character
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.galeria.defensores.ui.CharacterSheetFragment
import androidx.appcompat.app.AlertDialog
import android.widget.ImageButton

import com.galeria.defensores.data.NotificationRepository
import com.galeria.defensores.models.Notification
import com.galeria.defensores.models.NotificationStatus

class CharacterListFragment : Fragment() {

    private lateinit var characterRecyclerView: RecyclerView
    private lateinit var adapter: CharacterAdapter
    private var tableId: String? = null
    
    // Join Request Views
    private lateinit var layoutJoinRequest: View
    private lateinit var textJoinStatus: TextView
    private lateinit var btnRequestJoin: android.widget.Button

    companion object {
        private const val ARG_TABLE_ID = "table_id"

        fun newInstance(tableId: String): CharacterListFragment {
            val fragment = CharacterListFragment()
            val args = Bundle()
            args.putString(ARG_TABLE_ID, tableId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tableId = it.getString(ARG_TABLE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_character_list, container, false)
        characterRecyclerView = view.findViewById(R.id.character_recycler_view)
        characterRecyclerView.layoutManager = LinearLayoutManager(context)
        
        // Initialize Join Request Views
        layoutJoinRequest = view.findViewById(R.id.layout_join_request)
        textJoinStatus = view.findViewById(R.id.text_join_status)
        btnRequestJoin = view.findViewById(R.id.btn_request_join)
        
        // FAB Menu Logic
        val fabMenu = view.findViewById<FloatingActionButton>(R.id.fab_menu)
        val layoutFabNewSheet = view.findViewById<View>(R.id.layout_fab_new_sheet)
        val layoutFabViewPlayers = view.findViewById<View>(R.id.layout_fab_view_players)
        val fabNewSheet = view.findViewById<FloatingActionButton>(R.id.fab_new_sheet)
        val fabViewPlayers = view.findViewById<FloatingActionButton>(R.id.fab_view_players)
        
        var isMenuOpen = false

        fabMenu.setOnClickListener {
            isMenuOpen = !isMenuOpen
            if (isMenuOpen) {
                layoutFabNewSheet.visibility = View.VISIBLE
                layoutFabViewPlayers.visibility = View.VISIBLE
                fabMenu.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                layoutFabNewSheet.visibility = View.GONE
                layoutFabViewPlayers.visibility = View.GONE
                fabMenu.setImageResource(R.drawable.ic_more_vert)
            }
        }

        fabNewSheet.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val currentUser = com.galeria.defensores.data.SessionManager.currentUser
                if (currentUser != null && tableId != null) {
                    val newCharacter = Character(
                        tableId = tableId!!,
                        ownerId = currentUser.id,
                        ownerName = currentUser.name,
                        name = "Novo Defensor",
                        forca = 0,
                        habilidade = 0,
                        resistencia = 0,
                        armadura = 0,
                        poderFogo = 0
                    )
                    CharacterRepository.saveCharacter(newCharacter)
                    openCharacterSheet(newCharacter.id)
                    
                    // Close menu on action
                    isMenuOpen = false
                    layoutFabNewSheet.visibility = View.GONE
                    layoutFabViewPlayers.visibility = View.GONE
                    fabMenu.setImageResource(R.drawable.ic_more_vert)
                } else {
                    Toast.makeText(context, "Erro ao criar ficha. Verifique se está logado.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fabViewPlayers.setOnClickListener {
            if (tableId != null) {
                val dialog = TablePlayersDialogFragment.newInstance(tableId!!)
                dialog.show(parentFragmentManager, "TablePlayersDialog")
            }
             // Close menu
            isMenuOpen = false
            layoutFabNewSheet.visibility = View.GONE
            layoutFabViewPlayers.visibility = View.GONE
            fabMenu.setImageResource(R.drawable.ic_more_vert)
        }

        val btnLogs = view.findViewById<ImageButton>(R.id.btn_logs)
        btnLogs.setOnClickListener {
            if (tableId != null) {
                val bottomSheet = RollHistoryBottomSheet(tableId!!)
                bottomSheet.show(parentFragmentManager, "RollHistoryBottomSheet")
            } else {
                Toast.makeText(context, "Histórico disponível apenas dentro de uma mesa.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadCharacters()
    }

    private fun loadCharacters() {
        viewLifecycleOwner.lifecycleScope.launch {
            android.util.Log.d("CharacterListDebug", "Loading characters for tableId=$tableId")
            if (tableId != null) {
                val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id ?: return@launch
                val table = com.galeria.defensores.data.TableRepository.getTable(tableId!!)
                val isMaster = table?.masterId == currentUserId || table?.masterId == "mock-master-id"
                val isMember = table?.players?.contains(currentUserId) == true || isMaster

                val fabMenu = view?.findViewById<FloatingActionButton>(R.id.fab_menu)
                val btnLogs = view?.findViewById<ImageButton>(R.id.btn_logs)

                // Join Request Logic
                if (!isMember && table != null) {
                    layoutJoinRequest.visibility = View.VISIBLE
                    
                    // Hide Interaction Buttons for Visitors
                    fabMenu?.visibility = View.GONE
                    btnLogs?.visibility = View.GONE

                    // Check pending requests
                    val hasPending = NotificationRepository.hasPendingRequest(currentUserId, table.id)
                    if (hasPending) {
                        textJoinStatus.text = "Solicitação enviada. Aguardando aprovação."
                        btnRequestJoin.isEnabled = false
                        btnRequestJoin.text = "Aguardando"
                        btnRequestJoin.alpha = 0.5f
                    } else {
                        textJoinStatus.text = "Você está visitando esta mesa."
                        btnRequestJoin.isEnabled = true
                        btnRequestJoin.text = "Participar"
                        btnRequestJoin.alpha = 1.0f
                        
                        btnRequestJoin.setOnClickListener {
                            viewLifecycleOwner.lifecycleScope.launch {
                                val currentUser = com.galeria.defensores.data.SessionManager.currentUser
                                if (currentUser != null) {
                                    val notification = Notification(
                                        fromUserId = currentUser.id,
                                        fromUserName = currentUser.name,
                                        toUserId = table.masterId,
                                        tableId = table.id,
                                        tableName = table.name,
                                        status = NotificationStatus.PENDING
                                    )
                                    NotificationRepository.sendNotification(notification)
                                    Toast.makeText(context, "Solicitação enviada!", Toast.LENGTH_SHORT).show()
                                    // Refresh UI state
                                    loadCharacters()
                                }
                            }
                        }
                    }
                } else {
                    layoutJoinRequest.visibility = View.GONE
                    // Show Interaction Buttons for Members
                    fabMenu?.visibility = View.VISIBLE
                    btnLogs?.visibility = View.VISIBLE
                }
                
                val allCharacters = CharacterRepository.getCharacters(tableId)
                android.util.Log.d("CharacterListDebug", "Fetched ${allCharacters.size} characters. CurrentUser=$currentUserId, isMaster=$isMaster")
                
                val filteredCharacters = if (isMaster) {
                    allCharacters
                } else {
                    allCharacters.filter { !it.isHidden || it.ownerId == currentUserId }
                }

                val sortedCharacters = filteredCharacters.sortedWith(
                    compareByDescending<Character> { it.ownerId == currentUserId }
                        .thenBy { it.name }
                )
                android.util.Log.d("CharacterListDebug", "Showing ${sortedCharacters.size} characters after filter and sort")
                
                adapter = CharacterAdapter(sortedCharacters, isMaster, currentUserId) { character ->
                    openCharacterSheet(character.id)
                }
                characterRecyclerView.adapter = adapter
            } else {
                android.util.Log.d("CharacterListDebug", "Loading global character list (no tableId)")
                val characters = CharacterRepository.getCharacters(null)
                val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id
                adapter = CharacterAdapter(characters, true, currentUserId) { character ->
                    openCharacterSheet(character.id)
                }
                characterRecyclerView.adapter = adapter
            }
        }
    }

    private fun openCharacterSheet(characterId: String?) {
        val fragment = CharacterSheetFragment.newInstance(characterId, tableId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showDeleteConfirmation(character: Character) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Excluir Personagem")
            .setMessage("Tem certeza que deseja excluir '${character.name}'?")
            .setPositiveButton("Excluir") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    CharacterRepository.deleteCharacter(character.id)
                    Toast.makeText(context, "Personagem excluído.", Toast.LENGTH_SHORT).show()
                    loadCharacters() // Refresh list
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private inner class CharacterAdapter(
        private val characters: List<Character>,
        private val isMaster: Boolean,
        private val currentUserId: String?,
        private val onItemClick: (Character) -> Unit
    ) : RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_character, parent, false)
            return CharacterViewHolder(view)
        }

        override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
            val character = characters[position]
            holder.bind(character, isMaster, currentUserId)
        }

        override fun getItemCount(): Int = characters.size

        inner class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.character_name)
            private val descText: TextView = itemView.findViewById(R.id.character_description)
            private val creatorText: TextView = itemView.findViewById(R.id.character_creator)
            private val deleteBtn: android.widget.ImageButton = itemView.findViewById(R.id.btn_delete_character)

            fun bind(character: Character, isMaster: Boolean, currentUserId: String?) {
                nameText.text = character.name
                descText.text = "F:${character.forca} H:${character.habilidade} R:${character.resistencia} A:${character.armadura} PdF:${character.poderFogo}"
                
                if (character.ownerName.isNotEmpty()) {
                    creatorText.text = "Criado por: ${character.ownerName}"
                } else if (character.ownerId.isNotEmpty()) {
                    creatorText.text = "Criado por: ..."
                    // Async fetch for existing characters
                    itemView.post {
                        val lifecycleOwner = itemView.context as? androidx.lifecycle.LifecycleOwner
                        lifecycleOwner?.lifecycleScope?.launch {
                            val user = com.galeria.defensores.data.UserRepository.getUser(character.ownerId)
                            if (user != null) {
                                character.ownerName = user.name
                                // Update text only if this ViewHolder is still bound to the same character (simple check)
                                if (nameText.text == character.name) {
                                    creatorText.text = "Criado por: ${user.name}"
                                }
                            }
                        }
                    }
                } else {
                    creatorText.text = "Criado por: Desconhecido"
                }

                if (character.isHidden) {
                    itemView.alpha = 0.5f
                    nameText.text = "${character.name} (Oculto)"
                } else {
                    itemView.alpha = 1.0f
                }
                
                // Delete Logic
                val isOwner = character.ownerId == currentUserId
                if (isMaster || isOwner) {
                    deleteBtn.visibility = View.VISIBLE
                    deleteBtn.setOnClickListener { showDeleteConfirmation(character) }
                } else {
                    deleteBtn.visibility = View.GONE
                }
                
                itemView.setOnClickListener { onItemClick(character) }
            }
        }
    }
}