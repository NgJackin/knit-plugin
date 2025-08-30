package com.github.ngjackin.knitplugin.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class CreateProviderClassQuickFix(private val typeName: String) : LocalQuickFix {
    
    override fun getFamilyName(): String = "Create missing providers"
    
    override fun getName(): String = "Create @Provides class for '$typeName'"
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val containingFile = element.containingFile as? KtFile ?: return
        
        val factory = KtPsiFactory(project)
        
        // Generate a simple provider class
        val classCode = """
@Provides
class $typeName {
    // TODO: Add implementation
}
""".trimIndent()
        
        try {
            val ktClass = factory.createClass(classCode)
            
            // Add the class to the end of the current file
            containingFile.add(factory.createNewLine())
            containingFile.add(factory.createNewLine())
            containingFile.add(ktClass)
            
            // Commit the document changes
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            
        } catch (e: Exception) {
            // If adding to current file fails, create a new file
            createNewProviderFile(project, typeName)
        }
    }
    
    private fun createNewProviderFile(project: Project, typeName: String) {
        val fileContent = """@Provides
annotation class Provides

@Provides
class $typeName {
    // TODO: Add implementation
}
"""
        
        val fileName = "${typeName}Provider.kt"
        val psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, KotlinFileType.INSTANCE, fileContent)
        
        // Note: In a full implementation, you'd want to show a dialog to let the user
        // choose where to save the file and actually save it to the filesystem
        // For now, this creates the PSI structure which can be inspected
    }
}
