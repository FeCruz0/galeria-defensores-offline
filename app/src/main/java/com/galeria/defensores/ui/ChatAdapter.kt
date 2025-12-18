package com.galeria.defensores.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.models.ChatMessage
import com.galeria.defensores.models.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_ROLL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).type) {
            MessageType.ROLL -> TYPE_ROLL
            else -> TYPE_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ROLL -> {
                val view = inflater.inflate(R.layout.message_item_roll, parent, false)
                RollMessageViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.message_item_text, parent, false)
                TextMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is TextMessageViewHolder -> holder.bind(message)
            is RollMessageViewHolder -> holder.bind(message)
        }
    }

    class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_sender_name)
        private val contentText: TextView = itemView.findViewById(R.id.text_message_content)

        fun bind(message: ChatMessage) {
            nameText.text = message.senderName
            contentText.text = message.content
        }
    }

    class RollMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_sender_name)
        private val titleText: TextView = itemView.findViewById(R.id.text_roll_title)
        private val totalText: TextView = itemView.findViewById(R.id.text_roll_total)
        private val detailText: TextView = itemView.findViewById(R.id.text_roll_detail)

        fun bind(message: ChatMessage) {
            nameText.text = message.senderName
            
            val roll = message.rollResult
            if (roll != null) {
                titleText.text = "ROLOGEM: ${roll.name}"
                totalText.text = roll.total.toString()
                
                // Reconstruct or use a stored string for details if complicated
                // Ideally detail string comes from RollResult or we construct it:
                val detail = if (roll.bonus != 0) {
                   "${roll.attributeUsed}(${roll.attributeValue}) + ${roll.die} + ${roll.bonus}"
                } else {
                   "${roll.attributeUsed}(${roll.attributeValue}) + ${roll.die}"
                }
                detailText.text = detail
            } else {
                titleText.text = "ROLOGEM INVÁLIDA"
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
