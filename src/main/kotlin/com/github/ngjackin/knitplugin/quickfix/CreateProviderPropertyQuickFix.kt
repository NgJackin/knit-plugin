package com.github.ngjackin.knitplugin.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPsiFactory

class CreateProviderPropertyQuickFix(
    private val targetClass: KtClass,
    private val typeName: String
) : LocalQuickFix {
    
    override fun getFamilyName(): String = "Create missing providers"
    
    override fun getName(): String = "Add @Provides property for '$typeName' to ${targetClass.name}"
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val factory = KtPsiFactory(project)
        
        val propertyName = typeName.lowercase().let { name ->
            if (name.endsWith("service") || name.endsWith("repository") || name.endsWith("manager")) {
                name
            } else {
                "${name}Provider"
            }
        }
        
        // Create a property with @Provides annotation
        val propertyText = when (typeName) {
            "String" -> """    @Provides
    val $propertyName: $typeName = "default_value" // TODO: Set appropriate value"""
            "Int" -> """    @Provides
    val $propertyName: $typeName = 0 // TODO: Set appropriate value"""
            "Boolean" -> """    @Provides
    val $propertyName: $typeName = false // TODO: Set appropriate value"""
            "Double" -> """    @Provides
    val $propertyName: $typeName = 0.0 // TODO: Set appropriate value"""
            "Long" -> """    @Provides
    val $propertyName: $typeName = 0L // TODO: Set appropriate value"""
            else -> """    @Provides
    val $propertyName: $typeName = $typeName() // TODO: Provide appropriate implementation"""
        }
        
        val property = factory.createProperty(propertyText)
        
        val classBody = targetClass.body
        if (classBody != null) {
            // Add property to existing class body
            val lBrace = classBody.lBrace
            if (lBrace != null) {
                classBody.addAfter(factory.createNewLine(), lBrace)
                classBody.addAfter(property, lBrace)
                classBody.addAfter(factory.createNewLine(), lBrace)
            }
        } else {
            // Create class body and add property
            val newBody = factory.createBlock("{\n$propertyText\n}")
            targetClass.add(newBody)
        }
        
        // Commit the document changes
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }
}
