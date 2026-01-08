package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.TableRepository
import com.galeria.defensores.models.RollResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.google.android.material.tabs.TabLayout

class RollHistoryBottomSheet(private val tableId: String) : BottomSheetDialogFragment() {

    private var allRollsCache: List<RollResult> = emptyList()
    private var hiddenIdsCache: Set<String> = emptySet()
    private var isMasterCache: Boolean = false
    private var currentTab: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_roll_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val recycler = view.findViewById<RecyclerView>(R.id.recycler_roll_history)
        recycler.layoutManager = LinearLayoutManager(context)

        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout_history)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateUI(view, recycler)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Use SnapshotListener for real-time updates
        val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("tables").document(tableId)

        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                android.widget.Toast.makeText(context, "Erro ao carregar histórico.", android.widget.Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val table = snapshot.toObject(com.galeria.defensores.models.Table::class.java)
                allRollsCache = table?.rollHistory?.sortedByDescending { it.timestamp } ?: emptyList()
                
                val currentUserId = com.galeria.defensores.data.SessionManager.currentUser?.id
                isMasterCache = table?.masterId == currentUserId || table?.masterId == "mock-master-id"
                
                // Fetch characters to check current hidden status (Retroactive hiding)
                CoroutineScope(Dispatchers.Main).launch {
                    val characters = withContext(Dispatchers.IO) {
                        try {
                            com.galeria.defensores.data.CharacterRepository.getCharacters(tableId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    
                    hiddenIdsCache = characters.filter { it.isHidden }.map { it.id }.toSet()
                    updateUI(view, recycler) // Update UI with new data
                }
            }
        }
    }

    private fun updateUI(view: View, recycler: RecyclerView) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout_history)
        
        if (isMasterCache) {
            tabLayout.visibility = View.VISIBLE
        } else {
            tabLayout.visibility = View.GONE
        }

        val displayedHistory = if (isMasterCache) {
            // Master Logic
            if (currentTab == 1) {
                // Tab 1: Hidden Rolls Only
                 allRollsCache.filter { roll ->
                    val isHiddenSnapshot = roll.isHidden
                    val isHiddenNow = hiddenIdsCache.contains(roll.characterId)
                    (isHiddenSnapshot || (roll.characterId.isNotEmpty() && isHiddenNow))
                }
            } else {
                // Tab 0: General (All Visible Rolls)
                // Mutually exclusive: Hide hidden rolls here too
                allRollsCache.filter { roll ->
                    val isHiddenSnapshot = roll.isHidden
                    val isHiddenNow = hiddenIdsCache.contains(roll.characterId)
                    !(isHiddenSnapshot || (roll.characterId.isNotEmpty() && isHiddenNow))
                }
            }
        } else {
            // Player Logic: Filter out hidden
            allRollsCache.filter { roll ->
                val isHiddenSnapshot = roll.isHidden
                val isHiddenNow = hiddenIdsCache.contains(roll.characterId)
                !(isHiddenSnapshot || (roll.characterId.isNotEmpty() && isHiddenNow))
            }
        }

        if (displayedHistory.isEmpty()) {
            view.findViewById<TextView>(R.id.text_no_history).visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            view.findViewById<TextView>(R.id.text_no_history).visibility = View.GONE
            recycler.visibility = View.VISIBLE
            recycler.adapter = RollHistoryAdapter(displayedHistory)
            if (displayedHistory.isNotEmpty()) {
                recycler.scrollToPosition(0)
            }
        }
    }

    private inner class RollHistoryAdapter(private val rolls: List<RollResult>) :
        RecyclerView.Adapter<RollHistoryAdapter.RollViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RollViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_roll_history, parent, false)
            return RollViewHolder(view)
        }

        override fun onBindViewHolder(holder: RollViewHolder, position: Int) {
            holder.bind(rolls[position])
        }

        override fun getItemCount(): Int = rolls.size

        inner class RollViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.text_roll_player)
            private val timeText: TextView = itemView.findViewById(R.id.text_roll_time)
            private val descText: TextView = itemView.findViewById(R.id.text_roll_desc)
            private val resultText: TextView = itemView.findViewById(R.id.text_roll_result)

            fun bind(roll: RollResult) {
                nameText.text = roll.name
                
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                timeText.text = sdf.format(Date(roll.timestamp))

                val critText = if (roll.isCritical) " (CRÍTICO!)" else ""
                val bonusText = if (roll.bonus > 0) " + ${roll.bonus}" else ""
                if (roll.die == 0 && roll.attributeValue == 0) {
                     descText.text = roll.attributeUsed
                } else {
                     descText.text = "H(${roll.skillValue}) + ${roll.attributeUsed}(${roll.attributeValue}${if(roll.isCritical) "x2" else ""}) + 1d6(${roll.die})$bonusText$critText"
                }
                
                resultText.text = roll.total.toString()
                
                if (roll.isCritical) {
                    resultText.setTextColor(android.graphics.Color.parseColor("#D97706")) // Gold
                } else {
                    resultText.setTextColor(android.graphics.Color.parseColor("#FBBF24")) // Yellow
                }
            }
        }
    }
}
