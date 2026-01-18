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
import com.galeria.defensores.models.ChatMessage
import androidx.activity.result.contract.ActivityResultContracts
import com.yalantis.ucrop.UCrop
import android.net.Uri
import java.io.File
import android.app.Activity
import android.content.Intent

class ChatFragment : Fragment() {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var tableId: String? = null

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                context?.let { ctx ->
                    viewModel.processAndSendImage(ctx, resultUri)
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            android.widget.Toast.makeText(context, "Erro no crop: ${cropError?.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            startCrop(uri)
        }
    }

    private fun startCrop(uri: Uri) {
        val context = context ?: return
        val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(context.cacheDir, destinationFileName))

        val uCrop = UCrop.of(uri, destinationUri)
        // Options for free crop or square? User asked for "crop possibility". 
        // Default uCrop provides a UI. We can set input to free style or ratio.
        // Let's allow free crop.
        val options = UCrop.Options()
        options.setFreeStyleCropEnabled(true)
        
        val intent = uCrop.withOptions(options).getIntent(context)
        cropImageLauncher.launch(intent)
    }

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
        adapter = ChatAdapter(
            onImageClick = { imageUrl ->
                val dialog = ImageDetailDialogFragment.newInstance(imageUrl)
                dialog.show(parentFragmentManager, "ImageDetail")
            },
            onMessageLongClick = { message, view ->
                // Check if user is owner
                    val popup = android.widget.PopupMenu(context, view)
                    // Add "Reply" for everyone
                    popup.menu.add("Responder")

                    val currentUser = viewModel.currentUser
                    if (currentUser != null && currentUser.uid == message.senderId) {
                        popup.menu.add("Editar")
                        popup.menu.add("Excluir")
                    } else {
                        // If not owner, still show Reply
                    }
                    
                    popup.setOnMenuItemClickListener { item ->
                        when (item.title) {
                            "Responder" -> {
                                viewModel.enterReplyMode(message)
                                true
                            }
                            "Editar" -> {
                                if (message.type == com.galeria.defensores.models.MessageType.TEXT) {
                                    showEditDialog(message)
                                } else {
                                    android.widget.Toast.makeText(context, "Apenas mensagens de texto podem ser editadas.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                true
                            }
                            "Excluir" -> {
                                showDeleteDialog(message)
                                true
                            }
                            else -> false
                        }
                    }
                    popup.show()

            },
            onSenderClick = { message ->
                if (message.type == com.galeria.defensores.models.MessageType.ROLL && 
                    !message.rollResult?.characterId.isNullOrEmpty()) {
                    // Navigate to Character Sheet
                    val fragment = CharacterSheetFragment.newInstance(
                        message.rollResult?.characterId,
                        message.tableId
                    )
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                } else {
                    // Navigate to User Profile
                    val fragment = UserProfileFragment.newInstance(message.senderId)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            },
            onReplyClick = { targetMessageId ->
                val currentList = adapter.currentList
                val index = currentList.indexOfFirst { 
                    it is com.galeria.defensores.ui.ChatListItem.MessageItem && it.message.id == targetMessageId 
                }
                
                if (index != -1) {
                    performScrollAndHighlight(index)
                } else {
                     android.widget.Toast.makeText(context, "Mensagem original não carregada na tela.", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        val layoutManager = LinearLayoutManager(context)
        layoutManager.stackFromEnd = true // Start from bottom
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Helper to scroll down
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                // Scroll to bottom only if we were already at bottom or it's a new message sent by us
                // Complex logic usually needed, but simple for now:
                // If positionStart is 0 (loaded older), assume user wanted to see history, don't scroll to bottom
                if (positionStart > 0) {
                     recyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        })
        
        // Listeners for Highlight Feature
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (pendingHighlightPosition != -1) {
                        highlightRow(pendingHighlightPosition)
                        pendingHighlightPosition = -1
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    // User started scrolling, fade out
                    fadeOutHighlight()
                }
            }
        })
        
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                if (e.action == android.view.MotionEvent.ACTION_DOWN && highlightedView != null) {
                    fadeOutHighlight()
                }
                return false
            }
        })
        
        val btnLoadMore = view.findViewById<android.widget.Button>(R.id.btn_load_more)
        
        btnLoadMore.setOnClickListener {
            viewModel.loadOlderMessages()
        }

        viewModel.paginationStatus.observe(viewLifecycleOwner) { status ->
             when (status) {
                 "LOADING" -> {
                     btnLoadMore.text = "Carregando..."
                     btnLoadMore.isEnabled = false
                 }
                 "SUCCESS" -> {
                     btnLoadMore.text = "Carregar mais mensagens"
                     btnLoadMore.isEnabled = true
                     android.widget.Toast.makeText(context, "Histórico carregado", android.widget.Toast.LENGTH_SHORT).show()
                 }
                 "EMPTY" -> {
                     btnLoadMore.text = "Início da conversa"
                     btnLoadMore.isEnabled = false
                     android.widget.Toast.makeText(context, "Não há mais mensagens antigas", android.widget.Toast.LENGTH_SHORT).show()
                 }
                 "ERROR" -> {
                     btnLoadMore.text = "Tentar novamente"
                     btnLoadMore.isEnabled = true
                     android.widget.Toast.makeText(context, "Erro ao carregar", android.widget.Toast.LENGTH_SHORT).show()
                 }
             }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                btnLoadMore.visibility = View.VISIBLE
            } else {
                btnLoadMore.visibility = View.GONE
            }
        }

        // Observer for broadcast rolls from other players
        viewModel.visualRollBroadcast.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { visualRoll ->
                // Only show if not already showing (optional but good)
                if (parentFragmentManager.findFragmentByTag("virtual_dice_broadcast") == null) {
                    val frag = com.galeria.defensores.ui.VirtualDiceFragment.newPassiveInstance(
                        diceCount = visualRoll.diceCount,
                        expectedResults = visualRoll.diceValues,
                        canCrit = visualRoll.canCrit,
                        isNegative = visualRoll.isNegative,
                        critRangeStart = visualRoll.critRangeStart,
                        diceProperties = visualRoll.diceProperties
                    )
                    frag.show(parentFragmentManager, "virtual_dice_broadcast")
                }
            }
        }

        // Setup Inputs
        val editMessage = view.findViewById<EditText>(R.id.edit_chat_message)
        val btnSend = view.findViewById<ImageButton>(R.id.btn_send_chat)
        val btnAddImage = view.findViewById<ImageButton>(R.id.btn_add_image)

        btnAddImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        view.findViewById<ImageButton>(R.id.btn_quick_roll).setOnClickListener {
             tableId?.let { tid ->
                 val bottomSheet = QuickRollBottomSheet(tid) { result, avatarUrl ->
                     viewModel.sendRollResult(result, avatarUrl)
                 }
                 bottomSheet.show(parentFragmentManager, "QuickRoll")
             } ?: android.widget.Toast.makeText(context, "Mesa não identificada", android.widget.Toast.LENGTH_SHORT).show()
        }

        val textTypingIndicator = view.findViewById<android.widget.TextView>(R.id.text_typing_indicator)

        viewModel.typingStatusText.observe(viewLifecycleOwner) { text ->
             if (text.isNotEmpty()) {
                 textTypingIndicator.text = text
                 textTypingIndicator.visibility = View.VISIBLE
             } else {
                 textTypingIndicator.visibility = View.GONE
             }
        }

        btnSend.setOnClickListener {
            val content = editMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.sendMessage(content)
                editMessage.setText("")
                viewModel.onTypingInput("") // Clear immediately on send
            }
        }
        
        // Reply UI Wiring
        val layoutReplyContext = view.findViewById<View>(R.id.layout_reply_context)
        val textReplyName = view.findViewById<android.widget.TextView>(R.id.text_reply_context_name)
        val textReplyContent = view.findViewById<android.widget.TextView>(R.id.text_reply_context_message)
        val btnCancelReply = view.findViewById<android.widget.ImageButton>(R.id.btn_cancel_reply)
        
        viewModel.replyingToMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                layoutReplyContext.visibility = View.VISIBLE
                textReplyName.text = "Respondendo a ${message.senderName}"
                textReplyContent.text = when(message.type) {
                    com.galeria.defensores.models.MessageType.IMAGE -> "[Imagem]"
                    com.galeria.defensores.models.MessageType.ROLL -> "[Rologem] ${message.rollResult?.name}"
                    else -> message.content
                }
                
                // Focus edit text?
                editMessage.requestFocus()
                // Show keyboard? (Optional)
            } else {
                layoutReplyContext.visibility = View.GONE
            }
        }
        
        btnCancelReply.setOnClickListener {
            viewModel.cancelReplyMode()
        }

        editMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onTypingInput(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Init ViewModel
        tableId?.let { id ->
            viewModel.setTableId(id)
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

    private fun showEditDialog(message: ChatMessage) {
        val input = EditText(context)
        input.setText(message.content)
        
        android.app.AlertDialog.Builder(context)
            .setTitle("Editar Mensagem")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val newContent = input.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    viewModel.editMessage(message, newContent)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteDialog(message: ChatMessage) {
        android.app.AlertDialog.Builder(context)
            .setTitle("Excluir Mensagem")
            .setMessage("Tem certeza que deseja excluir esta mensagem?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.deleteMessage(message)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Highlight Logic
    private var highlightedView: View? = null
    private var pendingHighlightPosition: Int = -1

    private fun performScrollAndHighlight(position: Int) {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_chat) ?: return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        
        // If item is already fully visible, just highlight
        val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
        
        if (position in firstVisible..lastVisible) {
            highlightRow(position)
        } else {
            pendingHighlightPosition = position
            recyclerView.smoothScrollToPosition(position)
        }
    }

    private fun highlightRow(position: Int) {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_chat) ?: return
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
        
        if (viewHolder != null) {
            // Remove previous highlight if any
            fadeOutHighlight()
            
            highlightedView = viewHolder.itemView
            // Set highlight color (e.g. Semi-transparent yellow/gold)
            // Using a raw color similar to Slack/Discord mention highlight
            viewHolder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#4DFFD700")) // ~30% Gold
        }
    }

    private fun fadeOutHighlight() {
        val view = highlightedView ?: return
        highlightedView = null
        
        val colorFrom = android.graphics.Color.parseColor("#4DFFD700")
        val colorTo = android.graphics.Color.TRANSPARENT
        
        val colorAnimation = android.animation.ValueAnimator.ofObject(android.animation.ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = 500 // 500ms fade out
        colorAnimation.addUpdateListener { animator ->
            view.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }
}
