package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.CharacterRepository
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.models.Character
import kotlinx.coroutines.launch

class MyCharactersFragment : Fragment() {

    private lateinit var recyclerCharacters: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var adapter: MyCharactersAdapter

    private var pendingExportCharacter: Character? = null

    private val exportCharacterLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && pendingExportCharacter != null) {
            val character = pendingExportCharacter!!
            lifecycleScope.launch {
                val success = com.galeria.defensores.data.BackupRepository.exportCharacter(requireContext(), character.id, uri)
                if (success) {
                    android.widget.Toast.makeText(context, "Personagem exportado com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Erro ao exportar personagem.", android.widget.Toast.LENGTH_SHORT).show()
                }
                pendingExportCharacter = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_characters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerCharacters = view.findViewById(R.id.recycler_characters)
        textEmpty = view.findViewById(R.id.text_empty)
        
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        loadCharacters()
    }

    private fun loadCharacters() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUser = SessionManager.currentUser
            if (currentUser != null) {
                val characters = CharacterRepository.getCharactersForUser(currentUser.id)
                if (characters.isEmpty()) {
                    textEmpty.visibility = View.VISIBLE
                    recyclerCharacters.visibility = View.GONE
                } else {
                    textEmpty.visibility = View.GONE
                    recyclerCharacters.visibility = View.VISIBLE
                    
                    val charactersWithTableNames = characters.map { character ->
                        val tableName = if (character.tableId.isNotEmpty()) {
                             val table = TableRepository.getTable(character.tableId)
                             table?.name ?: "Mesa Desconhecida"
                        } else {
                            "Nenhuma"
                        }
                        character to tableName
                    }

                    adapter = MyCharactersAdapter(charactersWithTableNames, 
                        onClick = { character ->
                            // Open Sheet
                            val fragment = CharacterSheetFragment.newInstance(character.id)
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit()
                        },
                        onExport = { character ->
                            pendingExportCharacter = character
                            val safeName = character.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                            exportCharacterLauncher.launch("char_${safeName}_${character.id.take(4)}.json")
                        },
                        onDelete = { character ->
                             androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Excluir Personagem")
                                .setMessage("Tem certeza que deseja excluir ${character.name}?")
                                .setPositiveButton("Excluir") { _, _ ->
                                    lifecycleScope.launch {
                                        CharacterRepository.deleteCharacter(character.id)
                                        loadCharacters() 
                                    }
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    )
                    recyclerCharacters.adapter = adapter
                }
            }
        }
    }

    class MyCharactersAdapter(
        private val data: List<Pair<Character, String>>,
        private val onClick: (Character) -> Unit,
        private val onExport: (Character) -> Unit,
        private val onDelete: (Character) -> Unit
    ) : RecyclerView.Adapter<MyCharactersAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_character_name)
            val tableInfo: TextView = view.findViewById(R.id.tv_table_name)
            val stats: TextView = view.findViewById(R.id.tv_character_stats)
            val btnMore: android.widget.ImageButton = view.findViewById(R.id.btn_more)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_my_character, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (character, tableName) = data[position]
            holder.name.text = character.name
            holder.tableInfo.text = "Mesa: $tableName"
            holder.stats.text = "F:${character.forca} H:${character.habilidade} R:${character.resistencia} A:${character.armadura} PdF:${character.poderFogo}"
            
            holder.itemView.setOnClickListener { onClick(character) }

            holder.btnMore.setOnClickListener { view ->
                val popup = android.widget.PopupMenu(view.context, view)
                popup.menu.add("Abrir Ficha")
                popup.menu.add("Exportar JSON")
                popup.menu.add("Excluir")
                
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Abrir Ficha" -> onClick(character)
                        "Exportar JSON" -> onExport(character)
                        "Excluir" -> onDelete(character)
                    }
                    true
                }
                popup.show()
            }
        }

        override fun getItemCount() = data.size
    }
}
