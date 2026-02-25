package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.AdvantagesRepository
import com.galeria.defensores.models.AdvantageItem

class SelectAdvantageDialogFragment(
    private val onAdvantageSelected: (AdvantageItem) -> Unit
) : DialogFragment() {

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
        return inflater.inflate(R.layout.dialog_select_advantage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_advantages)
        recyclerView.layoutManager = LinearLayoutManager(context)

        fun loadAdvantages() {
            val adapter = AdvantagesAdapter(
                items = AdvantagesRepository.getAllAdvantages(),
                onItemClick = { selectedItem ->
                    // Itens não-modulares: seleciona e fecha imediatamente
                    onAdvantageSelected(selectedItem)
                    dismiss()
                },
                onLongClick = { itemToDelete ->
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Excluir Vantagem")
                        .setMessage("Deseja excluir '${itemToDelete.name}'?")
                        .setPositiveButton("Sim") { _, _ ->
                            AdvantagesRepository.removeAdvantage(itemToDelete)
                            loadAdvantages()
                        }
                        .setNegativeButton("Não", null)
                        .show()
                },
                showDescription = true,
                // Callback para itens modulares: recebe o item com selectedModifiers preenchidos
                onModularItemConfirmed = { confirmedItem ->
                    onAdvantageSelected(confirmedItem)
                    dismiss()
                }
            )
            recyclerView.adapter = adapter
        }

        loadAdvantages()

        view.findViewById<View>(R.id.btn_cancel_selection).setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.btn_new_advantage).setOnClickListener {
            val editDialog = EditAdvantageDialogFragment(null, { newAdvantage ->
                AdvantagesRepository.addAdvantage(newAdvantage)
                loadAdvantages()
            })
            editDialog.show(parentFragmentManager, "EditAdvantageDialog")
        }
    }
}
