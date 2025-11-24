package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.Character

class CharacterListFragment : Fragment() {

    private lateinit var characterRecyclerView: RecyclerView
    private var characterList: List<Character> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_character_list, container, false)
        characterRecyclerView = view.findViewById(R.id.character_recycler_view)
        characterRecyclerView.layoutManager = LinearLayoutManager(context)
        characterRecyclerView.adapter = CharacterAdapter(characterList)
        return view
    }

    private inner class CharacterAdapter(private val characters: List<Character>) :
        RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_character, parent, false)
            return CharacterViewHolder(view)
        }

        override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
            val character = characters[position]
            holder.bind(character)
        }

        override fun getItemCount(): Int = characters.size

        inner class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(character: Character) {
                // Bind character data to the view
            }
        }
    }
}