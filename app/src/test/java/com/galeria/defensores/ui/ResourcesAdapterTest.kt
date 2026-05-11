package com.galeria.defensores.ui

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.galeria.defensores.R
import com.galeria.defensores.models.ResourceDefinition
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ResourcesAdapterTest {

    @Test
    fun `test bind sets progress and max correctly`() {
        val view = mockk<View>(relaxed = true)
        val label = mockk<TextView>(relaxed = true)
        val valueText = mockk<TextView>(relaxed = true)
        val progressBar = mockk<ProgressBar>(relaxed = true)
        
        every { view.findViewById<TextView>(R.id.status_label) } returns label
        every { view.findViewById<TextView>(R.id.status_value) } returns valueText
        every { view.findViewById<ProgressBar>(R.id.status_bar) } returns progressBar
        
        val adapter = ResourcesAdapter(emptyList(), emptyMap(), emptyMap(), { _, _ -> }, {})
        val holder = adapter.ViewHolder(view)
        
        val res = ResourceDefinition(key = "pv", name = "PV", color = "#FF0000")
        val current = 10
        val max = 20
        
        holder.bind(res, current, max)
        
        verify { label.text = "PV" }
        verify { valueText.text = "10 / 20" }
        verify { progressBar.max = 20 }
        verify { progressBar.setProgress(10, false) }
    }

    @Test
    fun `test bind tints only progress layer`() {
        val view = mockk<View>(relaxed = true)
        val progressBar = mockk<ProgressBar>(relaxed = true)
        val progressDrawable = mockk<LayerDrawable>(relaxed = true)
        val progressLayer = mockk<Drawable>(relaxed = true)
        
        every { view.findViewById<ProgressBar>(R.id.status_bar) } returns progressBar
        every { progressBar.progressDrawable } returns progressDrawable
        every { progressDrawable.findDrawableByLayerId(android.R.id.progress) } returns progressLayer
        
        val adapter = ResourcesAdapter(emptyList(), emptyMap(), emptyMap(), { _, _ -> }, {})
        val holder = adapter.ViewHolder(view)
        
        val res = ResourceDefinition(key = "pv", name = "PV", color = "#FF0000")
        
        holder.bind(res, 10, 20)
        
        // This should pass now that we use progressTintList
        verify { progressBar.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF0000")) }
    }
}
