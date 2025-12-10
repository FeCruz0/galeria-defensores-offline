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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Reusing fragment_character_list layout as it likely has recycler + empty text
        // Or we should create a new one. Let's create 'fragment_my_characters.xml' to be safe,
        // or just reuse 'fragment_notifications' structure which is generic list.
        // Let's assume we can reuse 'fragment_character_list' if it fits.
        // Actually, let's create a new layout to avoid confusion.
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
                    
                    // We need to fetch table names for each character to display "Mesa: Nome"
                    // This is N+1 query problem potential, but for a user list it's fine (rarely > 10 chars).
                    val charactersWithTableNames = characters.map { character ->
                        val tableName = if (character.tableId.isNotEmpty()) {
                             val table = TableRepository.getTable(character.tableId)
                             table?.name ?: "Mesa Desconhecida"
                        } else {
                            "Nenhuma"
                        }
                        character to tableName
                    }

                    adapter = MyCharactersAdapter(charactersWithTableNames) { character ->
                        // OnClick: Open Character Sheet
                        val fragment = CharacterSheetFragment.newInstance(character.id)
                        
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                    recyclerCharacters.adapter = adapter
                }
            }
        }
    }

    class MyCharactersAdapter(
        private val data: List<Pair<Character, String>>,
        private val onClick: (Character) -> Unit
    ) : RecyclerView.Adapter<MyCharactersAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_character_name)
            val tableInfo: TextView = view.findViewById(R.id.tv_table_name)
            val stats: TextView = view.findViewById(R.id.tv_character_stats)
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
        }

        override fun getItemCount() = data.size
    }
}
