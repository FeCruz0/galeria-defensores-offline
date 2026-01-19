package com.galeria.defensores.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment

class ColorPickerDialogFragment : DialogFragment() {

    private var onColorSelected: ((Int) -> Unit)? = null
    private var title: String = "Select Color"

    // Material 500 equivalent colors + Black/White
    private val colors = listOf(
        Color.WHITE, 
        Color.BLACK,
        Color.parseColor("#F44336"), // Red
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#673AB7"), // Deep Purple
        Color.parseColor("#3F51B5"), // Indigo
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#03A9F4"), // Light Blue
        Color.parseColor("#00BCD4"), // Cyan
        Color.parseColor("#009688"), // Teal
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#8BC34A"), // Light Green
        Color.parseColor("#CDDC39"), // Lime
        Color.parseColor("#FFEB3B"), // Yellow
        Color.parseColor("#FFC107"), // Amber
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#FF5722"), // Deep Orange
        Color.parseColor("#795548"), // Brown
        Color.parseColor("#9E9E9E"), // Grey
        Color.parseColor("#607D8B")  // Blue Grey
    )

    companion object {
        fun newInstance(title: String): ColorPickerDialogFragment {
            val f = ColorPickerDialogFragment()
            f.title = title
            return f
        }
    }

    fun setOnColorSelectedListener(listener: (Int) -> Unit) {
        onColorSelected = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val grid = GridLayout(context).apply {
            columnCount = 5
            useDefaultMargins = true
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }

        val padding = (16 * resources.displayMetrics.density).toInt()
        grid.setPadding(padding, padding, padding, padding)

        colors.forEach { color ->
            val view = View(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = (48 * resources.displayMetrics.density).toInt()
                    height = (48 * resources.displayMetrics.density).toInt()
                    setMargins(8, 8, 8, 8)
                }
                background = GradientDrawable().apply {
                    setColor(color)
                    shape = GradientDrawable.OVAL
                    setStroke(2, Color.LTGRAY)
                }
                setOnClickListener {
                    onColorSelected?.invoke(color)
                    dismiss()
                }
            }
            grid.addView(view)
        }
        
        // Wrap in a container to ensure centering logic if needed, though Alert builder handles it well
        val container = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            addView(grid)
        }

        return AlertDialog.Builder(context)
            .setTitle(title)
            .setView(container)
            .setNegativeButton("Cancel") { _, _ -> dismiss() }
            .create()
    }
}
