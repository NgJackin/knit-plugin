package com.github.ngjackin.knitplugin.annotator

import com.github.ngjackin.knitplugin.analysis.CircularDependencyAnalyzer
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import java.awt.Color
import java.awt.Font

class KnitAnnotator : Annotator {
    
    companion object {
        // Custom text attributes for circular dependency highlighting
        private val CIRCULAR_DEPENDENCY_ATTRIBUTES = TextAttributesKey.createTextAttributesKey(
            "KNIT_CIRCULAR_DEPENDENCY"
        ).apply {
            fallbackAttributeKey = DefaultLanguageHighlighterColors.BLOCK_COMMENT
        }
    }
    
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
            
            // Check if this element is part of a circular dependency
            val isCircular = CircularDependencyAnalyzer.isPartOfCircularDependency(annotationEntry)
            
            if (isCircular) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Knit Producer: This element provides dependencies but is part of a circular dependency!")
                    .range(textRange)
                    .textAttributes(CIRCULAR_DEPENDENCY_ATTRIBUTES)
                    .create()
            } else {
                holder.newAnnotation(HighlightSeverity.INFORMATION, "Knit Producer: This element provides dependencies")
                    .range(textRange)
                    .textAttributes(DefaultLanguageHighlighterColors.METADATA)
                    .create()
            }
        }
    }
    
    private fun annotateByDi(property: KtProperty, holder: AnnotationHolder) {
        property.delegate?.let { delegate ->
            val delegateExpression = delegate.expression
            if (delegateExpression is KtNameReferenceExpression && delegateExpression.getReferencedName() == "di") {
                val textRange = delegateExpression.textRange
                
                // Check if this property is part of a circular dependency
                val isCircular = CircularDependencyAnalyzer.isPartOfCircularDependency(property)
                
                if (isCircular) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "Knit Consumer: This property will be injected but is part of a circular dependency!")
                        .range(textRange)
                        .textAttributes(CIRCULAR_DEPENDENCY_ATTRIBUTES)
                        .create()
                } else {
                    holder.newAnnotation(HighlightSeverity.INFORMATION, "Knit Consumer: This property will be injected")
                        .range(textRange)
                        .textAttributes(DefaultLanguageHighlighterColors.INSTANCE_FIELD)
                        .create()
                }
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
                // Check if this class is part of a circular dependency
                val isCircular = CircularDependencyAnalyzer.isPartOfCircularDependency(ktClass)
                
                if (isCircular) {
                    holder.newAnnotation(HighlightSeverity.WARNING, "Knit Producer Class: This class constructor can be injected but is part of a circular dependency!")
                        .range(nameIdentifier.textRange)
                        .textAttributes(CIRCULAR_DEPENDENCY_ATTRIBUTES)
                        .create()
                } else {
                    holder.newAnnotation(HighlightSeverity.INFORMATION, "Knit Producer Class: This class constructor can be injected")
                        .range(nameIdentifier.textRange)
                        .textAttributes(DefaultLanguageHighlighterColors.CLASS_NAME)
                        .create()
                }
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
                    // Check if this parameter is part of a circular dependency
                    val isCircular = CircularDependencyAnalyzer.isPartOfCircularDependency(parameter)
                    
                    if (isCircular) {
                        holder.newAnnotation(HighlightSeverity.WARNING, "Knit Producer Parameter: This parameter provides a dependency but is part of a circular dependency!")
                            .range(nameIdentifier.textRange)
                            .textAttributes(CIRCULAR_DEPENDENCY_ATTRIBUTES)
                            .create()
                    } else {
                        holder.newAnnotation(HighlightSeverity.INFORMATION, "Knit Producer Parameter: This parameter provides a dependency")
                            .range(nameIdentifier.textRange)
                            .textAttributes(DefaultLanguageHighlighterColors.PARAMETER)
                            .create()
                    }
                }
            }
        }
    }
}
