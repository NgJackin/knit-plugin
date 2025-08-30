package com.github.ngjackin.knitplugin.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Quick fix to remove the entire provider (class, property, etc.)
 */
class RemoveEntireProviderQuickFix(private val element: org.jetbrains.kotlin.psi.KtElement) : LocalQuickFix {
    
    override fun getName(): String = when (element) {
        is KtClass -> "Remove entire class '${element.name}'"
        is KtProperty -> "Remove entire property '${element.name}'"
        else -> "Remove entire provider"
    }
    
    override fun getFamilyName(): String = "Knit Dependency Injection"
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        try {
            // Remove the entire element from the code
            element.delete()
        } catch (e: Exception) {
            // Silent fail for safety
        }
    }
}
