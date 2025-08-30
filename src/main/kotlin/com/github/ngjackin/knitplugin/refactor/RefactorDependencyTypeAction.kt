package com.github.ngjackin.knitplugin.refactor

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*

class RefactorDependencyTypeAction : AnAction("Change Dependency Type") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return
        val property = PsiTreeUtil.getParentOfType(element, KtProperty::class.java) ?: return
        
        if (!isDependencyConsumer(property)) {
            Messages.showInfoMessage(
                "Place cursor on a 'by di' property to change its type",
                "Dependency Type Refactoring"
            )
            return
        }
        
        val currentType = property.typeReference?.text ?: return
        val newType = Messages.showInputDialog(
            project,
            "Change dependency type from '$currentType' to:",
            "Refactor Dependency Type",
            null,
            currentType,
            null
        ) ?: return
        
        if (newType.isBlank() || newType == currentType) return
        
        performDependencyTypeRefactoring(project, currentType, newType)
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val project = e.project
        
        var isEnabled = false
        
        if (editor != null && psiFile is KtFile && project != null) {
            val element = psiFile.findElementAt(editor.caretModel.offset)
            if (element != null) {
                // Look for 'by di' property more broadly
                var current: PsiElement? = element
                while (current != null && current !is KtFile) {
                    if (current is KtProperty && isDependencyConsumer(current)) {
                        isEnabled = true
                        break
                    }
                    current = current.parent
                }
                
                // Also check if we're within a property that has 'by di'
                if (!isEnabled) {
                    val property = PsiTreeUtil.getParentOfType(element, KtProperty::class.java)
                    isEnabled = property != null && isDependencyConsumer(property)
                }
            }
        }
        
        e.presentation.isEnabledAndVisible = isEnabled
    }
    
    private fun isDependencyConsumer(property: KtProperty): Boolean {
        property.delegate?.let { delegate ->
            val delegateExpression = delegate.expression
            return delegateExpression is KtNameReferenceExpression && 
                   delegateExpression.getReferencedName() == "di"
        }
        return false
    }
    
    private fun performDependencyTypeRefactoring(project: Project, oldType: String, newType: String) {
        CommandProcessor.getInstance().executeCommand(project, {
            WriteAction.run<Exception> {
                val scope = GlobalSearchScope.allScope(project)
                val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
                val factory = KtPsiFactory(project)
                
                for (virtualFile in kotlinFiles) {
                    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile ?: continue
                    
                    // Update all 'by di' consumers of this type
                    updateDependencyConsumers(psiFile, factory, oldType, newType)
                    
                    // Update all providers of this type
                    updateDependencyProviders(psiFile, factory, oldType, newType)
                }
            }
        }, "Refactor Dependency Type: $oldType → $newType", null)
    }
    
    private fun updateDependencyConsumers(psiFile: KtFile, factory: KtPsiFactory, oldType: String, newType: String) {
        val properties = PsiTreeUtil.findChildrenOfType(psiFile, KtProperty::class.java)
        for (property in properties) {
            if (isDependencyConsumer(property)) {
                val typeRef = property.typeReference
                if (typeRef != null && normalizeType(typeRef.text) == normalizeType(oldType)) {
                    val newTypeRef = factory.createType(newType)
                    typeRef.replace(newTypeRef)
                }
            }
        }
    }
    
    private fun updateDependencyProviders(psiFile: KtFile, factory: KtPsiFactory, oldType: String, newType: String) {
        // Update @Provides classes
        val classes = PsiTreeUtil.findChildrenOfType(psiFile, KtClass::class.java)
        for (ktClass in classes) {
            if (hasProvides(ktClass) && normalizeType(ktClass.name ?: "") == normalizeType(oldType)) {
                // Note: Full class renaming would require more sophisticated refactoring
                // For now, we focus on type references
            }
            
            // Update @Provides constructor parameters
            ktClass.primaryConstructor?.valueParameters?.forEach { parameter ->
                if (hasProvides(parameter)) {
                    updateTypeReference(parameter.typeReference, factory, oldType, newType)
                }
            }
            
            // Update @Provides properties within classes
            val properties = PsiTreeUtil.findChildrenOfType(ktClass, KtProperty::class.java)
            for (property in properties) {
                if (hasProvides(property)) {
                    updateTypeReference(property.typeReference, factory, oldType, newType)
                }
            }
        }
        
        // Update @Provides functions
        val functions = PsiTreeUtil.findChildrenOfType(psiFile, KtNamedFunction::class.java)
        for (function in functions) {
            if (hasProvides(function)) {
                updateTypeReference(function.typeReference, factory, oldType, newType)
            }
        }
    }
    
    private fun updateTypeReference(typeRef: KtTypeReference?, factory: KtPsiFactory, oldType: String, newType: String) {
        if (typeRef != null && normalizeType(typeRef.text) == normalizeType(oldType)) {
            val newTypeRef = factory.createType(newType)
            typeRef.replace(newTypeRef)
        }
    }
    
    private fun hasProvides(element: KtAnnotated): Boolean {
        return element.annotationEntries.any { it.shortName?.asString() == "Provides" }
    }
    
    private fun normalizeType(type: String): String {
        return type.substringBefore('<').trim()
    }
}
