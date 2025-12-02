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
        
        val fabAddCharacter = view.findViewById<FloatingActionButton>(R.id.fab_add_character)
        fabAddCharacter.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val currentUser = com.galeria.defensores.data.SessionManager.currentUser
                if (currentUser != null && tableId != null) {
                    val newCharacter = Character(
                        tableId = tableId!!,
                        ownerId = currentUser.id,
                        name = "Novo Defensor",
                        forca = 0,
                        habilidade = 0,
                        resistencia = 0,
                        armadura = 0,
                        poderFogo = 0
                    )
                    CharacterRepository.saveCharacter(newCharacter)
                    openCharacterSheet(newCharacter.id)
                } else {
                    Toast.makeText(context, "Erro ao criar ficha. Verifique se está logado.", Toast.LENGTH_SHORT).show()
                }
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
                val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id
                val table = com.galeria.defensores.data.TableRepository.getTable(tableId!!)
                val isMaster = table?.masterId == currentUserId || table?.masterId == "mock-master-id"
                
                val allCharacters = CharacterRepository.getCharacters(tableId)
                android.util.Log.d("CharacterListDebug", "Fetched ${allCharacters.size} characters. CurrentUser=$currentUserId, isMaster=$isMaster")
                
                val filteredCharacters = if (isMaster) {
                    allCharacters
                } else {
                    allCharacters.filter { !it.isHidden || it.ownerId == currentUserId }
                }
                android.util.Log.d("CharacterListDebug", "Showing ${filteredCharacters.size} characters after filter")
                
                adapter = CharacterAdapter(filteredCharacters, isMaster) { character ->
                    openCharacterSheet(character.id)
                }
                characterRecyclerView.adapter = adapter
            } else {
                android.util.Log.d("CharacterListDebug", "Loading global character list (no tableId)")
                val characters = CharacterRepository.getCharacters(null)
                adapter = CharacterAdapter(characters, true) { character ->
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

    private inner class CharacterAdapter(
        private val characters: List<Character>,
        private val isMaster: Boolean,
        private val onItemClick: (Character) -> Unit
    ) : RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_character, parent, false)
            return CharacterViewHolder(view)
        }

        override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
            val character = characters[position]
            holder.bind(character, isMaster)
        }

        override fun getItemCount(): Int = characters.size

        inner class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.character_name)
            private val descText: TextView = itemView.findViewById(R.id.character_description)

            fun bind(character: Character, isMaster: Boolean) {
                nameText.text = character.name
                descText.text = "F:${character.forca} H:${character.habilidade} R:${character.resistencia} A:${character.armadura} PdF:${character.poderFogo}"
                
                if (character.isHidden) {
                    itemView.alpha = 0.5f
                    nameText.text = "${character.name} (Oculto)"
                } else {
                    itemView.alpha = 1.0f
                }
                
                itemView.setOnClickListener { onItemClick(character) }
            }
        }
    }
}