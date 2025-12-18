package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.galeria.defensores.R
import com.galeria.defensores.data.TableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TableContainerFragment : Fragment() {

    private lateinit var drawerLayout: DrawerLayout
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

        drawerLayout = view.findViewById(R.id.drawer_layout)
        val toolbar = view.findViewById<Toolbar>(R.id.toolbar_table)
        val titleView = view.findViewById<TextView>(R.id.toolbar_title)

        // Setup Toolbar
        toolbar.inflateMenu(R.menu.menu_table_chat) // pending creation or reuse
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_characters -> {
                    if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                        drawerLayout.closeDrawer(GravityCompat.END)
                    } else {
                        drawerLayout.openDrawer(GravityCompat.END)
                    }
                    true
                }
                else -> false
            }
        }
        
        // Add "People" icon to toolbar to open drawer
        // Ideally we use setNavigationIcon but that's usually for "Back" or "Left Drawer"
        // Since we are adding actions, let's just use the menu item above.
        
        // Load Table Data for Title
        tableId?.let { id ->
             CoroutineScope(Dispatchers.IO).launch {
                 val table = TableRepository.getTable(id)
                 withContext(Dispatchers.Main) {
                     titleView.text = table?.name ?: "Chat da Mesa"
                 }
             }

            // Initialize Fragments if not added
            if (childFragmentManager.findFragmentById(R.id.container_chat) == null) {
                val chatFragment = ChatFragment.newInstance(id)
                childFragmentManager.beginTransaction()
                    .replace(R.id.container_chat, chatFragment)
                    .commit()
            }

            if (childFragmentManager.findFragmentById(R.id.container_characters) == null) {
                val charactersFragment = CharacterListFragment.newInstance(id)
                childFragmentManager.beginTransaction()
                    .replace(R.id.container_characters, charactersFragment)
                    .commit()
            }
        }
    }
}
