package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.galeria.defensores.R
import com.galeria.defensores.data.TableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TableContainerFragment : Fragment() {

    private var tableId: String? = null

    companion object {
        private const val ARG_TABLE_ID = "table_id"

        fun newInstance(tableId: String): TableContainerFragment {
            val fragment = TableContainerFragment()
            val args = Bundle()
            args.putString(ARG_TABLE_ID, tableId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tableId = arguments?.getString(ARG_TABLE_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_table_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_table)
        val titleView = view.findViewById<TextView>(R.id.toolbar_title)

        // Setup Toolbar
        toolbar.inflateMenu(R.menu.menu_table_chat) 
        // Menu is currently empty or cleaned up, but we keep inflation if we add options later.
        
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                // Add future option handlers here (e.g. Leave Table)
                else -> false
            }
        }
        
        // Load Table Data for Title
        tableId?.let { id ->
             CoroutineScope(Dispatchers.IO).launch {
                 val table = TableRepository.getTable(id)
                 withContext(Dispatchers.Main) {
                     titleView.text = table?.name ?: "Mesa"
                 }
             }

            // Initialize Character List Fragment directly
            if (childFragmentManager.findFragmentById(R.id.container_characters) == null) {
                val charactersFragment = CharacterListFragment.newInstance(id)
                childFragmentManager.beginTransaction()
                    .replace(R.id.container_characters, charactersFragment)
                    .commit()
            }
        }
    }
}
