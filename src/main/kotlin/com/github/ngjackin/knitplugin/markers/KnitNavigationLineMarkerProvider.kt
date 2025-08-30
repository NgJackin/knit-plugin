package com.github.ngjackin.knitplugin.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.github.ngjackin.knitplugin.navigation.KnitDependencyResolverFixed
import org.jetbrains.kotlin.psi.*
import javax.swing.Icon

class KnitNavigationLineMarkerProvider : LineMarkerProvider {
    
    private val resolver = KnitDependencyResolverFixed
    
    companion object {
        private val PROVIDER_ICON: Icon = IconLoader.getIcon("/general/add.svg", KnitNavigationLineMarkerProvider::class.java)
        private val CONSUMER_ICON: Icon = IconLoader.getIcon("/general/locate.svg", KnitNavigationLineMarkerProvider::class.java)
        private val NAVIGATION_ICON: Icon = IconLoader.getIcon("/general/autoscrollToSource.svg", KnitNavigationLineMarkerProvider::class.java)
    }
    
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        try {
            return when {
                isProducerElement(element) -> createProducerNavigationMarker(element)
                isConsumerElement(element) -> createConsumerNavigationMarker(element)
                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun isProducerElement(element: PsiElement): Boolean {
        return try {
            when (element) {
                is KtAnnotationEntry -> {
                    val name = element.shortName?.asString()
                    name == "Provides" || name == "provides"
                }
                is KtClass -> element.nameIdentifier == element && 
                    element.annotationEntries.any { 
                        val name = it.shortName?.asString()
                        name == "Provides" || name == "provides"
                    }
                is KtParameter -> element.nameIdentifier == element &&
                    element.annotationEntries.any { 
                        val name = it.shortName?.asString()
                        name == "Provides" || name == "provides"
                    }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isConsumerElement(element: PsiElement): Boolean {
        return try {
            if (element is KtNameReferenceExpression && element.getReferencedName() == "di") {
                return element.parent is KtPropertyDelegate
            }
            // Also check for call expressions like "by di()"
            if (element is KtCallExpression) {
                val calleeExpr = element.calleeExpression
                return calleeExpr is KtNameReferenceExpression && 
                       calleeExpr.getReferencedName() == "di" &&
                       element.parent is KtPropertyDelegate
            }
            false
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createProducerNavigationMarker(element: PsiElement): LineMarkerInfo<PsiElement>? {
        try {
            val type = getProducedType(element) ?: return null
            val consumers = resolver.findConsumersOf(type, element)
            
            return if (consumers.isNotEmpty()) {
                NavigationGutterIconBuilder.create(PROVIDER_ICON)
                    .setTargets(consumers.map { it.element })
                    .setTooltipText("Navigate to consumers (${consumers.size})")
                    .createLineMarkerInfo(element)
            } else {
                LineMarkerInfo(
                    element,
                    element.textRange,
                    PROVIDER_ICON,
                    { "Knit Producer: $type (no consumers found)" },
                    null,
                    GutterIconRenderer.Alignment.LEFT
                ) { "Producer: $type" }
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun createConsumerNavigationMarker(element: PsiElement): LineMarkerInfo<PsiElement>? {
        try {
            val type = getConsumedType(element) ?: return null
            val producers = resolver.findProducersFor(type, element)
            
            return if (producers.isNotEmpty()) {
                NavigationGutterIconBuilder.create(CONSUMER_ICON)
                    .setTargets(producers.map { it.element })
                    .setTooltipText("Navigate to producers (${producers.size})")
                    .createLineMarkerInfo(element)
            } else {
                LineMarkerInfo(
                    element,
                    element.textRange,
                    CONSUMER_ICON,
                    { "Knit Consumer: $type (no producers found)" },
                    null,
                    GutterIconRenderer.Alignment.LEFT
                ) { "Consumer: $type" }
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun getProducedType(element: PsiElement): String? {
        return try {
            when (element) {
                is KtClass -> element.name
                is KtParameter -> element.typeReference?.text
                is KtAnnotationEntry -> {
                    // Get the type from the annotated element
                    when (val annotatedElement = getAnnotatedElement(element)) {
                        is KtClass -> annotatedElement.name
                        is KtParameter -> annotatedElement.typeReference?.text
                        else -> null
                    }
                }
                else -> null
            }?.let { normalizeTypeName(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getConsumedType(element: PsiElement): String? {
        return try {
            val property = when {
                element is KtNameReferenceExpression && element.getReferencedName() == "di" -> {
                    element.parent?.parent as? KtProperty
                }
                element is KtCallExpression && 
                (element.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == "di" -> {
                    element.parent?.parent as? KtProperty
                }
                else -> null
            }
            property?.typeReference?.text?.let { normalizeTypeName(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getAnnotatedElement(annotation: KtAnnotationEntry): KtElement? {
        return annotation.parent?.parent as? KtElement
    }
    
    private fun normalizeTypeName(type: String): String {
        return type.trim()
            .removePrefix("@")
            .removeSuffix("?")  // Remove nullable marker
            .replace(Regex("\\s+"), "") // Remove whitespace
    }
}
