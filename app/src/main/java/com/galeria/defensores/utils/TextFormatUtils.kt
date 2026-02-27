package com.galeria.defensores.utils

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.widget.TextView

object TextFormatUtils {

    fun formatParagraphSpacing(text: String): SpannableStringBuilder {
        val normalized = text.replace("\u200B", "").replace(Regex("\\n+"), "\n\n").trim()
        val ssb = SpannableStringBuilder(normalized)
        var start = 0
        while (start < ssb.length) {
            val idx = ssb.indexOf("\n\n", start)
            if (idx == -1) break
            // Insert zero-width space between newlines for EditText to render the span properly
            ssb.replace(idx, idx + 2, "\n\u200B\n")
            ssb.setSpan(AbsoluteSizeSpan(8, true), idx + 1, idx + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = idx + 3
        }
        return ssb
    }
    
    fun applyParagraphSpacingToEditable(editable: android.text.Editable) {
        // Find existing \n\n without zero-width space and format them
        var start = 0
        while (start < editable.length) {
            val idx = editable.indexOf("\n\n", start)
            if (idx == -1) break
            editable.replace(idx, idx + 2, "\n\u200B\n")
            editable.setSpan(AbsoluteSizeSpan(8, true), idx + 1, idx + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = idx + 3
        }
    }
    
    fun cleanParagraphSpacing(text: String): String {
        return text.replace("\u200B", "")
    }
}
