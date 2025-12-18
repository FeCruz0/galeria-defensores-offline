package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.viewmodels.ChatViewModel

class ChatFragment : Fragment() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var tableId: String? = null

    companion object {
        private const val ARG_TABLE_ID = "table_id"

        fun newInstance(tableId: String): ChatFragment {
            val fragment = ChatFragment()
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
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Recycler
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_chat)
        adapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(context)
        layoutManager.stackFromEnd = true // Start from bottom
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Helper to scroll down
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        })

        // Setup Inputs
        val editMessage = view.findViewById<EditText>(R.id.edit_chat_message)
        val btnSend = view.findViewById<ImageButton>(R.id.btn_send_chat)

        btnSend.setOnClickListener {
            val content = editMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.sendMessage(content)
                editMessage.setText("")
            }
        }

        // Init ViewModel
        tableId?.let { id ->
            viewModel.setTableId(id)
        }

        // Observe Messages
        // Observe Messages
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
        }

        // Join Request Logic
        val layoutJoinRequest = view.findViewById<View>(R.id.layout_join_request)
        val layoutChatInput = view.findViewById<View>(R.id.layout_chat_input)
        val textJoinStatus = view.findViewById<android.widget.TextView>(R.id.text_join_status)
        val btnRequestJoin = view.findViewById<android.widget.Button>(R.id.btn_request_join)

        viewModel.isMember.observe(viewLifecycleOwner) { isMember ->
            if (isMember) {
                layoutChatInput.visibility = View.VISIBLE
                layoutJoinRequest.visibility = View.GONE
            } else {
                layoutChatInput.visibility = View.GONE
                layoutJoinRequest.visibility = View.VISIBLE
            }
        }

        viewModel.joinRequestStatus.observe(viewLifecycleOwner) { status ->
             if (status == "PENDING") {
                 textJoinStatus.text = "Solicitação enviada. Aguardando aprovação."
                 btnRequestJoin.isEnabled = false
                 btnRequestJoin.text = "Aguardando"
                 btnRequestJoin.alpha = 0.5f
             } else {
                 textJoinStatus.text = "Você está visitando esta mesa."
                 btnRequestJoin.isEnabled = true
                 btnRequestJoin.text = "Participar"
                 btnRequestJoin.alpha = 1.0f
                 
                 btnRequestJoin.setOnClickListener {
                     viewModel.sendJoinRequest()
                     android.widget.Toast.makeText(context, "Solicitação enviada!", android.widget.Toast.LENGTH_SHORT).show()
                 }
             }
        }
    }
}
