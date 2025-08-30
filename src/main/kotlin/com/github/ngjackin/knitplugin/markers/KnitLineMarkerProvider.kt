package com.github.ngjackin.knitplugin.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import javax.swing.Icon

class KnitLineMarkerProvider : LineMarkerProvider {
    
    companion object {
        // Use built-in IntelliJ icons as fallback
        private val PROVIDER_ICON: Icon = IconLoader.getIcon("/general/add.svg", KnitLineMarkerProvider::class.java)
        private val CONSUMER_ICON: Icon = IconLoader.getIcon("/general/locate.svg", KnitLineMarkerProvider::class.java)
    }
    
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Simplified detection for debugging
        when (element) {
            is KtAnnotationEntry -> {
                if (element.shortName?.asString() == "Provides") {
                    return createProviderMarker(element)
                }
            }
            is KtNameReferenceExpression -> {
                if (element.getReferencedName() == "di" && element.parent is KtPropertyDelegate) {
                    return createConsumerMarker(element)
                }
            }
            is KtClass -> {
                // Show marker for any class with @Provides (for debugging)
                if (element.nameIdentifier == element && 
                    element.annotationEntries.any { it.shortName?.asString() == "Provides" }) {
                    return createProviderMarker(element)
                }
            }
        }
        return null
    }
    
    private fun createProviderMarker(element: PsiElement): LineMarkerInfo<PsiElement> {
        return LineMarkerInfo(
            element,
            element.textRange,
            PROVIDER_ICON,
            { "Knit Producer: Provides dependencies" },
            null,
            GutterIconRenderer.Alignment.LEFT
        ) { "Knit Producer" }
    }
    
    private fun createConsumerMarker(element: PsiElement): LineMarkerInfo<PsiElement> {
        return LineMarkerInfo(
            element,
            element.textRange,
            CONSUMER_ICON,
            { "Knit Consumer: Dependency will be injected" },
            null,
            GutterIconRenderer.Alignment.LEFT
        ) { "Knit Consumer" }
    }
}
