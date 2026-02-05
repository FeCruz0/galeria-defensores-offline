package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.data.UserRepository
import com.galeria.defensores.models.Table
import com.galeria.defensores.ui.MyCharactersFragment // Added
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TableListFragment : Fragment() {

    private var pendingExportTable: Table? = null

    private val exportTableLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && pendingExportTable != null) {
            val table = pendingExportTable!!
            lifecycleScope.launch {
                val success = com.galeria.defensores.data.BackupRepository.exportTable(requireContext(), table.id, uri)
                if (success) {
                    Toast.makeText(context, "Mesa exportada com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Erro ao exportar mesa.", Toast.LENGTH_SHORT).show()
                }
                pendingExportTable = null
            }
        }
        }


    private val importTableLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                val success = com.galeria.defensores.data.BackupRepository.importTable(requireContext(), uri)
                if (success) {
                    Toast.makeText(context, "Mesa importada com sucesso!", Toast.LENGTH_SHORT).show()
                    // We need to refresh the table list. The loadTables is defined inside onViewCreated...
                    // We can move loadTables to a class member or make this trigger a refresh indirectly.
                    // Or check if we can access loadTables.
                    // Actually, re-triggering the fragment setup or notify adapter is needed.
                    // Let's rely on onResume? No, onResume calls loadTables if we move it there?
                    // Currently loadTables is local. We should refactor or just copy logic?
                    // Refactoring is better.
                    parentFragmentManager.beginTransaction().detach(this@TableListFragment).attach(this@TableListFragment).commit()
                } else {
                    Toast.makeText(context, "Erro ao importar mesa.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_table_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_tables)
        recyclerView.layoutManager = LinearLayoutManager(context)

        fun loadTables() {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Safeguard: Ensure user is loaded
                    if (SessionManager.currentUser == null) {
                        SessionManager.refreshUser()
                    }
                    
                    val currentUser = SessionManager.currentUser
                    val tables = TableRepository.getTables()
                    // Simple sort by name
                    val sortedTables = tables.sortedBy { it.name }

                    val adapter = TablesAdapter(
                        tables = sortedTables,
                        onTableClick = { table ->
                            // Access Granted directly
                            val fragment = TableContainerFragment.newInstance(table.id)
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit()
                        },
                        onEditClick = { table ->
                            showEditTableDialog(table) { loadTables() }
                        },
                        onDeleteClick = { table ->
                            showDeleteTableDialog(table) { loadTables() }
                        },
                        onExportClick = { table ->
                            pendingExportTable = table
                            val dateStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
                            val safeName = table.name.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
                            exportTableLauncher.launch("table_${safeName}_$dateStr.zip")
                        }
                    )
                    recyclerView.adapter = adapter
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Erro ao carregar mesas: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        loadTables()

        // FAB Menu Logic
        val fabMenu = view.findViewById<FloatingActionButton>(R.id.fab_menu)
        val layoutFabSettings = view.findViewById<View>(R.id.layout_fab_settings)
        val layoutFabCreateTable = view.findViewById<View>(R.id.layout_fab_create_table)

        val layoutFabMyCharacters = view.findViewById<View>(R.id.layout_fab_my_characters) // Added
        val fabSettings = view.findViewById<FloatingActionButton>(R.id.fab_settings)
        val fabCreateTable = view.findViewById<FloatingActionButton>(R.id.fab_create_table)
        val fabProfile = view.findViewById<FloatingActionButton>(R.id.fab_profile)
        val fabMyCharacters = view.findViewById<FloatingActionButton>(R.id.fab_my_characters) // Added
        
        var isMenuOpen = false

        // Import Table Logic
        val layoutFabImportTable = view.findViewById<View>(R.id.layout_fab_import_table)
        val fabImportTable = view.findViewById<FloatingActionButton>(R.id.fab_import_table)

        fabMenu.setOnClickListener {
            isMenuOpen = !isMenuOpen
            if (isMenuOpen) {
                layoutFabSettings.visibility = View.VISIBLE
                layoutFabCreateTable.visibility = View.VISIBLE
                layoutFabMyCharacters.visibility = View.VISIBLE
                layoutFabImportTable.visibility = View.VISIBLE // Added
                fabMenu.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                layoutFabSettings.visibility = View.GONE
                layoutFabCreateTable.visibility = View.GONE
                layoutFabMyCharacters.visibility = View.GONE
                layoutFabImportTable.visibility = View.GONE // Added
                fabMenu.setImageResource(R.drawable.ic_more_vert)
            }
        }

        fabCreateTable.setOnClickListener {
            showAddTableDialog {
                loadTables()
                // Close menu after action
                isMenuOpen = false
                layoutFabSettings.visibility = View.GONE
                layoutFabCreateTable.visibility = View.GONE
                layoutFabMyCharacters.visibility = View.GONE
                layoutFabImportTable.visibility = View.GONE // Added
                fabMenu.setImageResource(R.drawable.ic_more_vert)
            }
        }

        fabImportTable.setOnClickListener {
            importTableLauncher.launch(arrayOf("application/zip", "application/octet-stream")) // Zip mainly
             // Close menu after action
            isMenuOpen = false
            layoutFabSettings.visibility = View.GONE
            layoutFabCreateTable.visibility = View.GONE
            layoutFabMyCharacters.visibility = View.GONE
            layoutFabImportTable.visibility = View.GONE
            fabMenu.setImageResource(R.drawable.ic_more_vert)
        }

        fabSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
            
            // Close menu after action
            isMenuOpen = false
            layoutFabSettings.visibility = View.GONE
            layoutFabCreateTable.visibility = View.GONE
            layoutFabMyCharacters.visibility = View.GONE // Added
            fabMenu.setImageResource(R.drawable.ic_more_vert)
        }



        // New Listener
        fabMyCharacters.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MyCharactersFragment())
                .addToBackStack(null)
                .commit()

            // Close menu
            isMenuOpen = false
            layoutFabSettings.visibility = View.GONE
            layoutFabCreateTable.visibility = View.GONE
            layoutFabMyCharacters.visibility = View.GONE
            fabMenu.setImageResource(R.drawable.ic_more_vert)
        }
    }



    private fun showAddTableDialog(onTableAdded: () -> Unit) {
        val layout = android.widget.LinearLayout(context)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputName = EditText(context)
        inputName.hint = "Nome da Mesa"
        layout.addView(inputName)


        val inputDescription = EditText(context)
        inputDescription.hint = "Descrição da Mesa"
        layout.addView(inputDescription)

        // Rule System Selection
        val spinnerLabel = android.widget.TextView(context)
        spinnerLabel.text = "Sistema de Regras"
        spinnerLabel.setPadding(0, 20, 0, 5)
        layout.addView(spinnerLabel)

        val systemSpinner = android.widget.Spinner(context)
        layout.addView(systemSpinner)

        val loadingSystems = viewLifecycleOwner.lifecycleScope.async {
            com.galeria.defensores.data.RuleSystemRepository.getSystems()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val systems = loadingSystems.await()
            val systemNames = systems.map { it.name }
            val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, systemNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            systemSpinner.adapter = adapter
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Nova Mesa")
            .setView(layout)
            .setPositiveButton("Criar") { _, _ ->
                val name = inputName.text.toString()
                val description = inputDescription.text.toString()
                
                var ruleSystemId = "3det_alpha_base" // Default
                if (systemSpinner.selectedItemPosition >= 0) {
                     // We need to fetch the ID again or map indices to IDs. 
                     // Since we loaded systems async, we should probably store them in a fast accessible way or re-read
                     // But simpler: just blocking wait/check inside the click or store in a local var from the launch above?
                     // Let's store in a var.
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    // Re-fetch systems safely or rely on index if list didn't change
                    val systems = com.galeria.defensores.data.RuleSystemRepository.getSystems()
                    if (systemSpinner.selectedItemPosition >= 0 && systemSpinner.selectedItemPosition < systems.size) {
                        ruleSystemId = systems[systemSpinner.selectedItemPosition].id
                    }

                    var currentUser = SessionManager.currentUser
                    if (currentUser == null) {
                        SessionManager.refreshUser()
                        currentUser = SessionManager.currentUser
                    }

                    if (currentUser != null) {
                        val newTable = Table(
                            name = name, 
                            description = description,
                            masterId = currentUser.id,
                            isPrivate = false,
                            password = null,
                            ruleSystemId = ruleSystemId
                        )
                        val success = TableRepository.addTable(newTable)
                        if (success) {
                            android.widget.Toast.makeText(context, "Mesa criada com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                            onTableAdded()
                        } else {
                            android.widget.Toast.makeText(context, "Erro ao criar mesa. Tente novamente.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Erro: Usuário não identificado. Faça login novamente.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditTableDialog(table: Table, onTableUpdated: () -> Unit) {
        val layout = android.widget.LinearLayout(context)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputName = EditText(context)
        inputName.setText(table.name)
        inputName.hint = "Nome da Mesa"
        layout.addView(inputName)

        val inputDescription = EditText(context)
        inputDescription.setText(table.description)
        inputDescription.hint = "Descrição da Mesa"
        layout.addView(inputDescription)


        
        AlertDialog.Builder(requireContext())
            .setTitle("Editar Mesa")
            .setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val name = inputName.text.toString()
                val description = inputDescription.text.toString()
                
                if (name.isNotBlank()) {
                    val updatedTable = table.copy(
                        name = name, 
                        description = description,
                        isPrivate = false,
                        password = null
                    )
                    viewLifecycleOwner.lifecycleScope.launch {
                        TableRepository.updateTable(updatedTable)
                        onTableUpdated()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteTableDialog(table: Table, onTableDeleted: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Mesa")
            .setMessage("Tem certeza que deseja excluir a mesa '${table.name}'?")
            .setPositiveButton("Excluir") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    TableRepository.deleteTable(table.id)
                    onTableDeleted()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
