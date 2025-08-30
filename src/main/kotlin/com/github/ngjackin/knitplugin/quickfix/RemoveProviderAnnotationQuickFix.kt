package com.github.ngjackin.knitplugin.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * Quick fix to remove only the @Provides annotation
 */
class RemoveProviderAnnotationQuickFix(private val annotation: KtAnnotationEntry) : LocalQuickFix {
    
    override fun getName(): String = "Remove @Provides annotation"
    
    override fun getFamilyName(): String = "Knit Dependency Injection"
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        try {
            // Remove the annotation from the code
            annotation.delete()
        } catch (e: Exception) {
            // Silent fail for safety
        }
    }
}
