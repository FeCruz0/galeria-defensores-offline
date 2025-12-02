package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.data.UserRepository
import com.galeria.defensores.models.Table
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TableListFragment : Fragment() {

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
                // Safeguard: Ensure user is loaded
                if (SessionManager.currentUser == null) {
                    SessionManager.refreshUser()
                }
                
                val currentUser = SessionManager.currentUser
                val adapter = TablesAdapter(
                    tables = TableRepository.getTables(),
                    currentUserId = currentUser?.id,
                    scope = viewLifecycleOwner.lifecycleScope,
                    onTableClick = { table ->
                        val fragment = CharacterListFragment.newInstance(table.id)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    },
                    onInviteClick = { table ->
                        showInviteDialog(table)
                    },
                    onEditClick = { table ->
                        showEditTableDialog(table) { loadTables() }
                    },
                    onDeleteClick = { table ->
                        showDeleteTableDialog(table) { loadTables() }
                    }
                )
                recyclerView.adapter = adapter
            }
        }

        loadTables()

        // FAB Menu Logic
        val fabMenu = view.findViewById<FloatingActionButton>(R.id.fab_menu)
        val layoutFabSettings = view.findViewById<View>(R.id.layout_fab_settings)
        val layoutFabCreateTable = view.findViewById<View>(R.id.layout_fab_create_table)
        val fabSettings = view.findViewById<FloatingActionButton>(R.id.fab_settings)
        val fabCreateTable = view.findViewById<FloatingActionButton>(R.id.fab_create_table)
        
        var isMenuOpen = false

        fabMenu.setOnClickListener {
            isMenuOpen = !isMenuOpen
            if (isMenuOpen) {
                layoutFabSettings.visibility = View.VISIBLE
                layoutFabCreateTable.visibility = View.VISIBLE
                fabMenu.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                layoutFabSettings.visibility = View.GONE
                layoutFabCreateTable.visibility = View.GONE
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
                fabMenu.setImageResource(R.drawable.ic_more_vert)
            }
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
            fabMenu.setImageResource(R.drawable.ic_more_vert)
        }
    }

    private fun showInviteDialog(table: Table) {
        val input = EditText(context)
        input.hint = "Telefone do Convidado"
        input.inputType = android.text.InputType.TYPE_CLASS_PHONE
        
        AlertDialog.Builder(requireContext())
            .setTitle("Convidar Jogador")
            .setView(input)
            .setPositiveButton("Convidar") { _, _ ->
                val phone = input.text.toString()
                if (phone.isNotBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val user = UserRepository.findUserByPhone(phone)
                        if (user != null) {
                            // User exists, add to table
                            TableRepository.addPlayerToTable(table.id, user.id)
                            android.widget.Toast.makeText(context, "${user.name} adicionado à mesa!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            // User not found, generate code
                            val inviteCode = java.util.UUID.randomUUID().toString().substring(0, 6).uppercase()
                            android.widget.Toast.makeText(context, "Usuário não encontrado. Código de convite: $inviteCode", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

        val checkPrivate = android.widget.CheckBox(context)
        checkPrivate.text = "Mesa Privada"
        layout.addView(checkPrivate)

        val inputPassword = EditText(context)
        inputPassword.hint = "Senha da Mesa"
        inputPassword.visibility = View.GONE
        layout.addView(inputPassword)

        checkPrivate.setOnCheckedChangeListener { _, isChecked ->
            inputPassword.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Nova Mesa")
            .setView(layout)
            .setPositiveButton("Criar") { _, _ ->
                val name = inputName.text.toString()
                val description = inputDescription.text.toString()
                val isPrivate = checkPrivate.isChecked
                val password = inputPassword.text.toString()

                if (name.isNotBlank()) {
                    if (isPrivate && password.isBlank()) {
                        android.widget.Toast.makeText(context, "Senha é obrigatória para mesas privadas", android.widget.Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
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
                                isPrivate = isPrivate,
                                password = if (isPrivate) password else null
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

        val checkPrivate = android.widget.CheckBox(context)
        checkPrivate.text = "Mesa Privada"
        checkPrivate.isChecked = table.isPrivate
        layout.addView(checkPrivate)

        val inputPassword = EditText(context)
        inputPassword.hint = "Senha da Mesa"
        inputPassword.setText(table.password)
        inputPassword.visibility = if (table.isPrivate) View.VISIBLE else View.GONE
        layout.addView(inputPassword)

        checkPrivate.setOnCheckedChangeListener { _, isChecked ->
            inputPassword.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Editar Mesa")
            .setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val name = inputName.text.toString()
                val description = inputDescription.text.toString()
                val isPrivate = checkPrivate.isChecked
                val password = inputPassword.text.toString()

                if (name.isNotBlank()) {
                     if (isPrivate && password.isBlank()) {
                        android.widget.Toast.makeText(context, "Senha é obrigatória para mesas privadas", android.widget.Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val updatedTable = table.copy(
                        name = name, 
                        description = description,
                        isPrivate = isPrivate,
                        password = if (isPrivate) password else null
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
