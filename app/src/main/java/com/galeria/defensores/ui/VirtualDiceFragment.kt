package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.galeria.defensores.R
import com.galeria.defensores.models.RollRequest
import com.galeria.defensores.models.RollResult
import com.galeria.defensores.ui.views.DiceBoardView
import com.galeria.defensores.viewmodels.CharacterViewModel

class VirtualDiceFragment : DialogFragment() {

    companion object {
        const val REQUEST_KEY = "VIRTUAL_ROLL_RESULT"
        
        fun newInstance(diceCount: Int, bonus: Int, attrVal: Int, skillVal: Int, attrName: String, charId: String): VirtualDiceFragment {
            val f = VirtualDiceFragment()
            val args = Bundle()
            args.putInt("diceCount", diceCount)
            args.putInt("bonus", bonus)
            args.putInt("attrVal", attrVal)
            args.putInt("skillVal", skillVal)
            args.putString("attrName", attrName)
            args.putString("charId", charId)
            f.arguments = args
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_virtual_dice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val diceCount = arguments?.getInt("diceCount") ?: 1
        val bonus = arguments?.getInt("bonus") ?: 0
        val attrVal = arguments?.getInt("attrVal") ?: 0
        val skillVal = arguments?.getInt("skillVal") ?: 0
        val attrName = arguments?.getString("attrName") ?: ""
        val charId = arguments?.getString("charId") ?: ""

        val diceBoard = view.findViewById<DiceBoardView>(R.id.dice_board_view)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_roll)
        
        diceBoard.post {
            diceBoard.initializeDice(diceCount)
        }
        
        diceBoard.onRollFinished = { diceValues ->
            // Pass raw dice values to ViewModel for calculation
            val resultBundle = Bundle().apply {
                putIntegerArrayList("diceValues", ArrayList(diceValues))
                // Minimal context if needed, but VM has Request.
            }
            
            parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
