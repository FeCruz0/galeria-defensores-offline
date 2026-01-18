package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.graphics.Color
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.R
import com.galeria.defensores.ui.views.DiceBoardView
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context

class VirtualDiceFragment : DialogFragment(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private val SHAKE_THRESHOLD = 15.0f // Adjust sensitivity
    private val SHAKE_WAIT_TIME_MS = 1000 // Cooldown between rolls
    
    private lateinit var diceBoard: DiceBoardView

    companion object {
        const val REQUEST_KEY = "VIRTUAL_ROLL_RESULT"
        
        fun newInstance(diceCount: Int, bonus: Int, attrVal: Int, skillVal: Int, attrName: String, charId: String, expectedResults: List<Int>? = null, canCrit: Boolean = false, isNegative: Boolean = false, critRangeStart: Int = 6, diceProperties: List<com.galeria.defensores.models.DieProperty>? = null): VirtualDiceFragment {
            val f = VirtualDiceFragment()
            val args = Bundle()
            args.putInt("diceCount", diceCount)
            args.putInt("bonus", bonus)
            args.putInt("attrVal", attrVal)
            args.putInt("skillVal", skillVal)
            args.putString("attrName", attrName)
            args.putString("charId", charId)
            args.putBoolean("isPassive", false)
            args.putBoolean("canCrit", canCrit)
            args.putBoolean("isNegative", isNegative)
            args.putInt("critRangeStart", critRangeStart)
            if (expectedResults != null) {
                args.putIntegerArrayList("expectedResults", ArrayList(expectedResults))
            }
            if (diceProperties != null) {
                args.putParcelableArrayList("diceProperties", ArrayList(diceProperties))
            }
            f.arguments = args
            return f
        }

        fun newPassiveInstance(diceCount: Int, expectedResults: List<Int>, canCrit: Boolean = false, isNegative: Boolean = false, critRangeStart: Int = 6, diceProperties: List<com.galeria.defensores.models.DieProperty>? = null): VirtualDiceFragment {
            val f = VirtualDiceFragment()
            val args = Bundle()
            args.putInt("diceCount", diceCount)
            args.putBoolean("isPassive", true)
            args.putBoolean("canCrit", canCrit)
            args.putBoolean("isNegative", isNegative)
            args.putInt("critRangeStart", critRangeStart)
            args.putIntegerArrayList("expectedResults", ArrayList(expectedResults))
            if (diceProperties != null) {
                args.putParcelableArrayList("diceProperties", ArrayList(diceProperties))
            }
            f.arguments = args
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        
        // Initialize sensor manager
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
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

        val isPassive = arguments?.getBoolean("isPassive") ?: false
        val canCrit = arguments?.getBoolean("canCrit") ?: false
        val isNegative = arguments?.getBoolean("isNegative") ?: false
        val critRangeStart = arguments?.getInt("critRangeStart") ?: 6
        val expectedResults = arguments?.getIntegerArrayList("expectedResults")
        
        val diceProperties = arguments?.getParcelableArrayList<com.galeria.defensores.models.DieProperty>("diceProperties")
        
        diceBoard = view.findViewById(R.id.dice_board_view)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_roll)
        
        if (isPassive) {
            btnCancel.visibility = View.GONE
            view.findViewById<View>(R.id.text_roll_instruction).visibility = View.GONE
            view.findViewById<View>(R.id.layout_root_dice).setBackgroundColor(Color.TRANSPARENT)
        }
        // Always apply expected results if provided
        if (expectedResults != null) {
            diceBoard.expectedResults = expectedResults
        }
        
        diceBoard.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                if (right - left > 0 && bottom - top > 0) {
                    diceBoard.removeOnLayoutChangeListener(this)
                    diceBoard.initializeDice(diceCount, canCrit, isNegative, critRangeStart, diceProperties)
                    if (isPassive) {
                        diceBoard.postDelayed({
                            diceBoard.autoFlick()
                        }, 200) // Much shorter delay for more immediate action
                    }
                }
            }
        })
        
        diceBoard.onRollFinished = { diceValues ->
            // Increase delay by 1s (total 2500ms) as requested
            val displayDelay = 2500L
            
            if (isPassive) {
                diceBoard.postDelayed({
                    dismiss()
                }, displayDelay)
            } else {
                // Pass raw dice values to ViewModel for calculation
                val resultBundle = Bundle().apply {
                    putIntegerArrayList("diceValues", ArrayList(diceValues))
                }
                parentFragmentManager.setFragmentResult(REQUEST_KEY, resultBundle)
                
                // Also delay dismissal for active player
                diceBoard.postDelayed({
                    dismiss()
                }, displayDelay)
            }
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val isPassive = arguments?.getBoolean("isPassive") ?: false
        if (isPassive) {
            dialog?.window?.apply {
                // Remove the dim background
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                // Make it non-touchable so users can interact with the app behind
                addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                // Also ensure it's not focusable so keyboard stays where it is
                addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || arguments?.getBoolean("isPassive") == true) return
        
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            // Calculate total acceleration magnitude
            val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            
            // Check if acceleration exceeds threshold + gravity (~9.8)
            // 15.0f means it needs a decent shake (about 1.5g)
            if (acceleration > SHAKE_THRESHOLD) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > SHAKE_WAIT_TIME_MS) {
                    lastShakeTime = now
                    diceBoard.autoFlick()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
