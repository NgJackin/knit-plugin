package com.github.ngjackin.knitplugin.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Text-based annotator for Knit DI annotations that's K2-compatible.
 * Uses simple text matching instead of complex PSI navigation.
 */
class KnitTextBasedAnnotator : Annotator {
    
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        try {
            val text = element.text
            
            // Highlight @Provides annotations
            if (text.contains("@Provides")) {
                val startOffset = text.indexOf("@Provides")
                if (startOffset >= 0) {
                    val range = TextRange(
                        element.textRange.startOffset + startOffset,
                        element.textRange.startOffset + startOffset + "@Provides".length
                    )
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(range)
                        .textAttributes(DefaultLanguageHighlighterColors.KEYWORD)
                        .create()
                }
            }
            
            // Highlight 'by di' delegations
            if (text.contains("by di")) {
                val startOffset = text.indexOf("by di")
                if (startOffset >= 0) {
                    val range = TextRange(
                        element.textRange.startOffset + startOffset,
                        element.textRange.startOffset + startOffset + "by di".length
                    )
                    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(range)
                        .textAttributes(DefaultLanguageHighlighterColors.INSTANCE_METHOD)
                        .create()
                }
            }
            
        } catch (e: Exception) {
            // Silent fail for K2 compatibility
        }
    }
}
