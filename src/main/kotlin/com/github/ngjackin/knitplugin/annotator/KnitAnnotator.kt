package com.github.ngjackin.knitplugin.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

class KnitAnnotator : Annotator {
    
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is KtAnnotationEntry -> annotateProvides(element, holder)
            is KtProperty -> annotateByDi(element, holder)
            is KtClass -> annotateProvidingClass(element, holder)
        }
    }
    
    private fun annotateProvides(annotationEntry: KtAnnotationEntry, holder: AnnotationHolder) {
        val shortName = annotationEntry.shortName?.asString()
        if (shortName == "Provides") {
            val textRange = annotationEntry.textRange
            holder.newAnnotation(HighlightSeverity.INFORMATION, "Knit Producer: This element provides dependencies")
                .range(textRange)
                .textAttributes(DefaultLanguageHighlighterColors.METADATA)
                .create()
        }
    }
    
    private fun annotateByDi(property: KtProperty, holder: AnnotationHolder) {
        property.delegate?.let { delegate ->
            val delegateExpression = delegate.expression
            if (delegateExpression is KtNameReferenceExpression && delegateExpression.getReferencedName() == "di") {
                val textRange = delegateExpression.textRange
                holder.newAnnotation(HighlightSeverity.INFORMATION, "Knit Consumer: This property will be injected")
                    .range(textRange)
                    .textAttributes(DefaultLanguageHighlighterColors.INSTANCE_FIELD)
                    .create()
            }
        }
    }
    
    private fun annotateProvidingClass(ktClass: KtClass, holder: AnnotationHolder) {
        // Check if class has @Provides annotation
        val hasProvides = ktClass.annotationEntries.any { 
            it.shortName?.asString() == "Provides" 
        }
        
        if (hasProvides) {
            val nameIdentifier = ktClass.nameIdentifier
            if (nameIdentifier != null) {
                holder.newAnnotation(HighlightSeverity.INFORMATION, "Knit Producer Class: This class constructor can be injected")
                    .range(nameIdentifier.textRange)
                    .textAttributes(DefaultLanguageHighlighterColors.CLASS_NAME)
                    .create()
            }
        }
        
        // Check constructor parameters with @Provides
        ktClass.primaryConstructor?.valueParameters?.forEach { parameter ->
            val hasProvides = parameter.annotationEntries.any { 
                it.shortName?.asString() == "Provides" 
            }
            
            if (hasProvides) {
                val nameIdentifier = parameter.nameIdentifier
                if (nameIdentifier != null) {
                    holder.newAnnotation(HighlightSeverity.INFORMATION, "Knit Producer Parameter: This parameter provides a dependency")
                        .range(nameIdentifier.textRange)
                        .textAttributes(DefaultLanguageHighlighterColors.PARAMETER)
                        .create()
                }
            }
        }
    }
}
