package com.github.ngjackin.knitplugin.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory

class CreateProviderParameterQuickFix(
    private val targetClass: KtClass,
    private val typeName: String
) : LocalQuickFix {
    
    override fun getFamilyName(): String = "Create missing providers"
    
    override fun getName(): String = "Add @Provides parameter for '$typeName' to ${targetClass.name} constructor"
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val factory = KtPsiFactory(project)
        
        val constructor = targetClass.primaryConstructor
        if (constructor != null) {
            // Add parameter to existing constructor
            val parameterName = typeName.lowercase().let { name ->
                if (name.endsWith("service") || name.endsWith("repository") || name.endsWith("manager")) {
                    name
                } else {
                    "${name}Provider"
                }
            }
            
            val newParameter = factory.createParameter("@Provides val $parameterName: $typeName")
            
            val parameterList = constructor.valueParameterList
            if (parameterList != null) {
                if (parameterList.parameters.isNotEmpty()) {
                    // Add comma before the new parameter
                    parameterList.addBefore(factory.createComma(), parameterList.rightParenthesis)
                    parameterList.addBefore(factory.createWhiteSpace(" "), parameterList.rightParenthesis)
                }
                parameterList.addBefore(newParameter, parameterList.rightParenthesis)
            }
        } else {
            // Create a new primary constructor
            val parameterName = typeName.lowercase().let { name ->
                if (name.endsWith("service") || name.endsWith("repository") || name.endsWith("manager")) {
                    name
                } else {
                    "${name}Provider"
                }
            }
            
            val constructorText = "(@Provides val $parameterName: $typeName)"
            val newConstructor = factory.createPrimaryConstructor(constructorText)
            
            // Add constructor after class name
            val classBody = targetClass.body
            if (classBody != null) {
                targetClass.addBefore(newConstructor, classBody)
            } else {
                targetClass.add(newConstructor)
                // Add empty class body if it doesn't exist
                targetClass.add(factory.createBlock("{}"))
            }
        }
        
        // Commit the document changes
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }
}
