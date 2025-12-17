package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galeria.defensores.R
import com.galeria.defensores.data.SkillsRepository
import com.galeria.defensores.models.AdvantageItem

class SelectSkillDialogFragment(
    private val onSkillSelected: (AdvantageItem) -> Unit
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
        return inflater.inflate(R.layout.dialog_select_skill, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_skills)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        fun loadSkills() {
            val adapter = AdvantagesAdapter(
                items = SkillsRepository.getAllSkills(),
                onItemClick = { selectedItem ->
                    onSkillSelected(selectedItem)
                    dismiss()
                },
                onLongClick = { itemToDelete ->
                     androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Excluir Perícia")
                        .setMessage("Deseja excluir '${itemToDelete.name}'?")
                        .setPositiveButton("Sim") { _, _ ->
                            SkillsRepository.removeSkill(itemToDelete)
                            loadSkills()
                        }
                        .setNegativeButton("Não", null)
                        .show()
                }
            )
            recyclerView.adapter = adapter
        }
        
        loadSkills()

        view.findViewById<View>(R.id.btn_cancel_selection).setOnClickListener {
            dismiss()
        }
        
        view.findViewById<View>(R.id.btn_new_skill).setOnClickListener {
            val editDialog = EditSkillDialogFragment(null, { newSkill ->
                SkillsRepository.addSkill(newSkill)
                loadSkills() // Refresh list
            })
            editDialog.show(parentFragmentManager, "EditSkillDialog")
        }
    }
}
