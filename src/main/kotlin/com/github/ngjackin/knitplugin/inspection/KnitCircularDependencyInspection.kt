package com.github.ngjackin.knitplugin.inspection

import com.github.ngjackin.knitplugin.analysis.CircularDependencyAnalyzer
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * Inspection that detects circular dependencies in Knit DI
 */
class KnitCircularDependencyInspection : LocalInspectionTool() {
    
    override fun getDisplayName(): String = "Knit Circular Dependency Detection"
    
    override fun getShortName(): String = "KnitCircularDependency"
    
    override fun getGroupDisplayName(): String = "Knit Dependency Injection"
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(ktClass: KtClass) {
                super.visitClass(ktClass)
                checkClassForCircularDependencies(ktClass, holder)
            }
            
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                checkPropertyForCircularDependencies(property, holder)
            }
        }
    }
    
    private fun checkClassForCircularDependencies(ktClass: KtClass, holder: ProblemsHolder) {
        // Check if this class is a provider
        val hasProvides = ktClass.annotationEntries.any { it.shortName?.asString() == "Provides" }
        if (!hasProvides) return
        
        val className = ktClass.name ?: return
        val circularDependencies = CircularDependencyAnalyzer.getCircularDependenciesForType(className, ktClass.project)
        
        if (circularDependencies.isNotEmpty()) {
            val nameIdentifier = ktClass.nameIdentifier
            if (nameIdentifier != null) {
                for (cycle in circularDependencies) {
                    val cycleDescription = cycle.cycle.joinToString(" → ")
                    holder.registerProblem(
                        nameIdentifier,
                        "Circular dependency detected: $cycleDescription",
                        ProblemHighlightType.ERROR
                    )
                }
            }
        }
    }
    
    private fun checkPropertyForCircularDependencies(property: KtProperty, holder: ProblemsHolder) {
        // Check if this is an injected property
        val delegate = property.delegate
        if (delegate?.expression?.text?.contains("di") != true) return
        
        val propertyType = property.typeReference?.text ?: return
        val circularDependencies = CircularDependencyAnalyzer.getCircularDependenciesForType(propertyType, property.project)
        
        if (circularDependencies.isNotEmpty()) {
            val nameIdentifier = property.nameIdentifier
            if (nameIdentifier != null) {
                for (cycle in circularDependencies) {
                    val cycleDescription = cycle.cycle.joinToString(" → ")
                    holder.registerProblem(
                        nameIdentifier,
                        "Property participates in circular dependency: $cycleDescription",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
    }
}
