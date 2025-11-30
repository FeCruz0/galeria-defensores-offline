package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.CharacterRepository
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.data.UserRepository
import com.galeria.defensores.models.Character
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class CharacterListFragment : Fragment() {

    private lateinit var characterRecyclerView: RecyclerView
    private lateinit var adapter: CharacterAdapter
    private var tableId: String? = null

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
        arguments?.let { tableId = it.getString(ARG_TABLE_ID) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_character_list, container, false)
        characterRecyclerView = view.findViewById(R.id.character_recycler_view)
        characterRecyclerView.layoutManager = LinearLayoutManager(context)
        
        view.findViewById<FloatingActionButton>(R.id.fab_add_character).setOnClickListener {
            openCharacterSheet(null)
        }
        view.findViewById<ImageButton>(R.id.btn_add_player).setOnClickListener {
            showAddPlayerDialog()
        }
        
        loadCharacters()
        return view
    }

    private fun loadCharacters() {
        if (tableId == null) return
        lifecycleScope.launch {
            val table = TableRepository.getTable(tableId!!)
            val characters = CharacterRepository.getCharacters(tableId)
            val currentUser = com.galeria.defensores.data.SessionManager.currentUser
            if (table != null && currentUser != null) {
                val filtered = characters.filter { char ->
                    !char.isPrivate || char.ownerId == currentUser.id || table.masterId == currentUser.id
                }.sortedWith(
                    compareByDescending<Character> { it.ownerId == currentUser.id }
                        .thenBy { it.name.lowercase() }
                )
                adapter = CharacterAdapter(filtered) { character ->
                    openCharacterSheet(character.id)
                }
                characterRecyclerView.adapter = adapter
                
                // Permission Check for Add Buttons
                val isMaster = table.masterId == currentUser.id
                val isPlayer = table.players.contains(currentUser.id)
                val canEdit = isMaster || isPlayer
                
                val fabAdd = view?.findViewById<FloatingActionButton>(R.id.fab_add_character)
                val btnInvite = view?.findViewById<ImageButton>(R.id.btn_add_player)
                
                if (canEdit) {
                    fabAdd?.visibility = View.VISIBLE
                    btnInvite?.visibility = View.VISIBLE
                } else {
                    fabAdd?.visibility = View.GONE
                    btnInvite?.visibility = View.GONE
                }
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

    private fun showAddPlayerDialog() {
        val input = EditText(requireContext())
        input.hint = "Telefone do Convidado"
        input.inputType = android.text.InputType.TYPE_CLASS_PHONE
        AlertDialog.Builder(requireContext())
            .setTitle("Convidar Jogador")
            .setView(input)
            .setPositiveButton("Convidar") { _, _ ->
                val phone = input.text.toString()
                if (phone.isNotBlank() && tableId != null) {
                    lifecycleScope.launch {
                        val user = UserRepository.findUserByPhone(phone)
                        if (user != null) {
                            TableRepository.addPlayerToTable(tableId!!, user.id)
                            Toast.makeText(requireContext(), "${user.name} adicionado à mesa!", Toast.LENGTH_SHORT).show()
                        } else {
                            val code = java.util.UUID.randomUUID().toString().substring(0, 6).uppercase()
                            Toast.makeText(requireContext(), "Usuário não encontrado. Código de convite: $code", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private inner class CharacterAdapter(
        private val characters: List<Character>,
        private val onItemClick: (Character) -> Unit
    ) : RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_character, parent, false)
            return CharacterViewHolder(view)
        }

        override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
            holder.bind(characters[position])
        }

        override fun getItemCount(): Int = characters.size

        inner class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.character_name)
            private val descText: TextView = itemView.findViewById(R.id.character_description)
            private val creatorText: TextView = itemView.findViewById(R.id.character_creator)
            private val privacyText: TextView = itemView.findViewById(R.id.character_privacy)
            private val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete_character)

            fun bind(character: Character) {
                nameText.text = character.name
                descText.text = "F:${character.forca} H:${character.habilidade} R:${character.resistencia} A:${character.armadura} PdF:${character.poderFogo}"
                if (character.isPrivate) {
                    privacyText.visibility = View.VISIBLE
                    privacyText.text = "Privativa"
                } else {
                    privacyText.visibility = View.GONE
                }
                if (!character.ownerId.isNullOrBlank()) {
                    lifecycleScope.launch {
                        val user = UserRepository.getUser(character.ownerId)
                        creatorText.text = "Criado por: ${user?.name ?: "Desconhecido"}"
                    }
                } else {
                    creatorText.text = "Criado por: Desconhecido"
                }
                lifecycleScope.launch {
                    val currentUser = com.galeria.defensores.data.SessionManager.currentUser
                    val table = if (tableId != null) TableRepository.getTable(tableId!!) else null
                    val isOwner = character.ownerId == currentUser?.id
                    val isMaster = table?.masterId == currentUser?.id
                    if (isOwner || isMaster) {
                        deleteButton.visibility = View.VISIBLE
                        deleteButton.setOnClickListener { showDeleteCharacterDialog(character) }
                    } else {
                        deleteButton.visibility = View.GONE
                    }
                }
                itemView.setOnClickListener { onItemClick(character) }
            }
        }
    }

    private fun showDeleteCharacterDialog(character: Character) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Ficha")
            .setMessage("Tem certeza que deseja excluir a ficha '${character.name}'? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    CharacterRepository.deleteCharacter(character.id)
                    loadCharacters()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}