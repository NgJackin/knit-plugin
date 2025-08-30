package com.github.ngjackin.knitplugin.inlayhints

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KnitInlayHintsCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        when (element) {
            is KtAnnotationEntry -> collectProvidesHints(element, sink)
            is KtProperty -> collectByDiHints(element, sink)
            is KtClass -> collectClassHints(element, sink)
        }
        return true
    }

    private fun collectProvidesHints(annotation: KtAnnotationEntry, sink: InlayHintsSink) {
        if (annotation.shortName?.asString() == "Provides") {
            val hint = factory.text(" 🟢 Producer")
            sink.addInlineElement(annotation.textRange.endOffset, false, hint, false)
        }
    }

    private fun collectByDiHints(property: KtProperty, sink: InlayHintsSink) {
        property.delegate?.let { delegate ->
            val delegateExpression = delegate.expression
            if (delegateExpression is KtNameReferenceExpression && delegateExpression.getReferencedName() == "di") {
                val propertyType = property.typeReference?.text
                val containingClass = property.containingClass()
                
                if (propertyType != null && containingClass != null) {
                    val hasProvider = hasProviderForType(containingClass, propertyType)
                    val hint = if (hasProvider) {
                        factory.text(" 🔵 Injected")
                    } else {
                        factory.text(" ❌ No provider")
                    }
                    sink.addInlineElement(delegateExpression.textRange.endOffset, false, hint, false)
                }
            }
        }
    }

    private fun collectClassHints(ktClass: KtClass, sink: InlayHintsSink) {
        val hasProvides = ktClass.annotationEntries.any { it.shortName?.asString() == "Provides" }
        if (hasProvides) {
            val nameIdentifier = ktClass.nameIdentifier
            if (nameIdentifier != null) {
                val hint = factory.text(" 🏭 Injectable")
                sink.addInlineElement(nameIdentifier.textRange.endOffset, false, hint, false)
            }
        }

        // Add hints for constructor parameters with @Provides
        ktClass.primaryConstructor?.valueParameters?.forEach { parameter ->
            val hasProvides = parameter.annotationEntries.any { it.shortName?.asString() == "Provides" }
            if (hasProvides) {
                val nameIdentifier = parameter.nameIdentifier
                if (nameIdentifier != null) {
                    val hint = factory.text(" 🔧 Provides")
                    sink.addInlineElement(nameIdentifier.textRange.endOffset, false, hint, false)
                }
            }
        }
    }

    private fun hasProviderForType(containingClass: KtClass, targetType: String): Boolean {
        // Check constructor parameters with @Provides
        containingClass.primaryConstructor?.valueParameters?.forEach { parameter ->
            val hasProvides = parameter.annotationEntries.any { it.shortName?.asString() == "Provides" }
            if (hasProvides && parameter.typeReference?.text == targetType) {
                return true
            }
        }

        // Check if there's a class with @Provides that matches the type
        if (containingClass.annotationEntries.any { it.shortName?.asString() == "Provides" } &&
            containingClass.name == targetType) {
            return true
        }

        return false
    }
}
