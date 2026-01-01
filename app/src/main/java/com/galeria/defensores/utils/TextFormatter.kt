package com.galeria.defensores.utils

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import java.util.regex.Pattern

object TextFormatter {

    fun format(text: String): Spannable {
        val spannable = SpannableStringBuilder(text)

        // Bold: *text*
        processTag(spannable, "\\*(.*?)\\*", StyleSpan(Typeface.BOLD))

        // Italic: _text_
        processTag(spannable, "_(.*?)_", StyleSpan(Typeface.ITALIC))

        // Strikethrough: ~text~
        processTag(spannable, "~(.*?)~", StrikethroughSpan())

        return spannable
    }

    private fun processTag(spannable: SpannableStringBuilder, patternStr: String, span: Any) {
        val pattern = Pattern.compile(patternStr)
        var matcher = pattern.matcher(spannable)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val content = matcher.group(1) ?: ""

            // Replace the full match (*text*) with the content (text)
            spannable.replace(start, end, content)

            // Apply the span to the content range
            // Note: Since we replaced the text, the end index has shifted.
            // The length of the match was end - start. The length of content is content.length.
            // The new end is start + content.length.
            spannable.setSpan(span, start, start + content.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Since text changed, we need to reset the matcher to scan the rest of the string
            // from the point where we left off (or just start over if safer, but less efficient).
            // Starting over is safer for overlapping/moving indices.
            // To prevent infinite loops if something goes wrong, assume markers are removed so unique match will be gone.
            matcher = pattern.matcher(spannable)
        }
    }
}
