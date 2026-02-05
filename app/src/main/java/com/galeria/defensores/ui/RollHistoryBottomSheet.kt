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
import androidx.lifecycle.lifecycleScope


class RollHistoryBottomSheet(private val tableId: String) : BottomSheetDialogFragment() {

    private var allRollsCache: List<RollResult> = emptyList()

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


        // Offline Load
        viewLifecycleOwner.lifecycleScope.launch {
            val table = withContext(Dispatchers.IO) {
                TableRepository.getTable(tableId)
            }
            
            if (table != null) {
                allRollsCache = table.rollHistory.sortedByDescending { it.timestamp }
                
                updateUI(view, recycler)
            } else {
                android.widget.Toast.makeText(context, "Mesa não encontrada.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(view: View, recycler: RecyclerView) {
        val displayedHistory = allRollsCache

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
