package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.data.UserRepository
import com.galeria.defensores.models.User
import kotlinx.coroutines.launch

class TablePlayersDialogFragment : DialogFragment() {

    private var tableId: String? = null
    private lateinit var recyclerPlayers: RecyclerView
    private lateinit var adapter: PlayersAdapter

    companion object {
        private const val ARG_TABLE_ID = "table_id"

        fun newInstance(tableId: String): TablePlayersDialogFragment {
            val fragment = TablePlayersDialogFragment()
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_table_players, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerPlayers = view.findViewById(R.id.recycler_players)
        recyclerPlayers.layoutManager = LinearLayoutManager(context)
        
        view.findViewById<Button>(R.id.btn_close).setOnClickListener {
            dismiss()
        }

        loadPlayers()
    }

    private fun loadPlayers() {
        if (tableId == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            val table = TableRepository.getTable(tableId!!)
            if (table != null) {
                val playerIds = table.players.toMutableList()
                
                // Also add Master if not in table (optional logic? usually master is owner)
                // For now, let's just list 'players' array. 
                // But wait, owner might not be in players list explicitly depending on implementation.
                // Table doesn't have explicit owner in players usually.
                // Let's add master to display if checks out.
                if (!playerIds.contains(table.masterId)) {
                    playerIds.add(0, table.masterId) // Add master at top
                }

                val users = mutableListOf<User>()
                for (id in playerIds) {
                    val user = UserRepository.getUser(id)
                    if (user != null) {
                        users.add(user)
                    }
                }

                adapter = PlayersAdapter(users) { user ->
                    // Open User Profile in Read-Only Mode
                    val fragment = UserProfileFragment.newInstance(user.id)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                    dismiss()
                }
                recyclerPlayers.adapter = adapter
            } else {
                Toast.makeText(context, "Erro ao carregar mesa.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class PlayersAdapter(
        private val players: List<User>,
        private val onClick: (User) -> Unit
    ) : RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_player_list, parent, false)
            return PlayerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            val player = players[position]
            holder.bind(player)
            holder.itemView.setOnClickListener { onClick(player) }
        }

        override fun getItemCount(): Int = players.size

        class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.player_name)
            
            fun bind(user: User) {
                nameText.text = user.name
            }
        }
    }
}
