package com.github.ngjackin.knitplugin.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KnitDependencyInspection : LocalInspectionTool() {
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                checkByDiUsage(property, holder)
            }
            
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                checkProvidesAnnotation(klass, holder)
            }
        }
    }
    
    private fun checkByDiUsage(property: KtProperty, holder: ProblemsHolder) {
        property.delegate?.let { delegate ->
            val delegateExpression = delegate.expression
            if (delegateExpression is KtNameReferenceExpression && delegateExpression.getReferencedName() == "di") {
                val propertyType = property.typeReference?.text
                if (propertyType != null) {
                    // Check if there's a corresponding provider in the same class or constructor
                    val containingClass = property.containingClass()
                    if (containingClass != null) {
                        val hasProvider = hasProviderForType(containingClass, propertyType)
                        if (!hasProvider) {
                            holder.registerProblem(
                                delegateExpression,
                                "No provider found for type '$propertyType'. Consider adding @Provides annotation to a constructor parameter or method."
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun checkProvidesAnnotation(klass: KtClass, holder: ProblemsHolder) {
        // Check if class with @Provides has a proper constructor
        val hasProvides = klass.annotationEntries.any { it.shortName?.asString() == "Provides" }
        if (hasProvides) {
            val primaryConstructor = klass.primaryConstructor
            if (primaryConstructor == null || primaryConstructor.valueParameters.isEmpty()) {
                val nameIdentifier = klass.nameIdentifier
                if (nameIdentifier != null) {
                    holder.registerProblem(
                        nameIdentifier,
                        "Class with @Provides annotation should have a primary constructor with parameters for dependency injection."
                    )
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
        // This is a simplified check - in a real implementation, you'd want more sophisticated type matching
        if (containingClass.annotationEntries.any { it.shortName?.asString() == "Provides" } &&
            containingClass.name == targetType) {
            return true
        }
        
        return false
    }
}
