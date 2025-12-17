package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.DisadvantagesRepository
import com.galeria.defensores.models.AdvantageItem

class SelectDisadvantageDialogFragment(
    private val onDisadvantageSelected: (AdvantageItem) -> Unit
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
        return inflater.inflate(R.layout.dialog_select_disadvantage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleView = view.findViewById<TextView>(R.id.dialog_title)
        titleView.text = "Selecionar Desvantagem"

        val btnNew = view.findViewById<Button>(R.id.btn_new_disadvantage)
        btnNew.text = "Criar Nova Desvantagem"

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_disadvantages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        fun loadDisadvantages() {
            val adapter = AdvantagesAdapter(
                items = DisadvantagesRepository.getAllDisadvantages(),
                onItemClick = { selectedItem ->
                    onDisadvantageSelected(selectedItem)
                    dismiss()
                },
                onLongClick = { itemToDelete ->
                     androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Excluir Desvantagem")
                        .setMessage("Deseja excluir '${itemToDelete.name}'?")
                        .setPositiveButton("Sim") { _, _ ->
                            DisadvantagesRepository.removeDisadvantage(itemToDelete)
                            loadDisadvantages()
                        }
                        .setNegativeButton("Não", null)
                        .show()
                }
            )
            recyclerView.adapter = adapter
        }
        
        loadDisadvantages()

        view.findViewById<Button>(R.id.btn_cancel_selection).setOnClickListener {
            dismiss()
        }
        
        btnNew.setOnClickListener {
            val editDialog = EditAdvantageDialogFragment(
                advantage = null,
                onSave = { newDisadvantage ->
                    DisadvantagesRepository.addDisadvantage(newDisadvantage)
                    loadDisadvantages()
                }
            )
            editDialog.show(parentFragmentManager, "EditDisadvantageDialog")
        }
    }
}
