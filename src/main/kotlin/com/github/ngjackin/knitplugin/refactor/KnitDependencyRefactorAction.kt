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
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KnitDependencyRefactorAction : AnAction("Refactor Knit Dependency") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return
        
        // Find the relevant Knit DI element
        val refactorTarget = findRefactorTarget(element)
        
        when (refactorTarget) {
            is DependencyConsumer -> handleConsumerRefactor(refactorTarget, project, editor)
            is DependencyProvider -> handleProviderRefactor(refactorTarget, project, editor)
            else -> {
                Messages.showInfoMessage(
                    "Place cursor on a 'by di' property or @Provides element to refactor",
                    "Knit Dependency Refactoring"
                )
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val project = e.project
        
        e.presentation.isEnabledAndVisible = editor != null && 
                                            psiFile is KtFile && 
                                            project != null &&
                                            isKnitDIContext(psiFile, editor)
    }
    
    private fun isKnitDIContext(psiFile: KtFile, editor: Editor): Boolean {
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return false
        return findRefactorTarget(element) != null
    }
    
    private fun findRefactorTarget(element: PsiElement): RefactorTarget? {
        // Look for 'by di' property
        val property = PsiTreeUtil.getParentOfType(element, KtProperty::class.java)
        if (property != null && isDependencyConsumer(property)) {
            val typeRef = property.typeReference
            if (typeRef != null) {
                return DependencyConsumer(property, typeRef.text)
            }
        }
        
        // Look for @Provides element
        val annotated = PsiTreeUtil.getParentOfType(element, KtAnnotated::class.java)
        if (annotated != null && hasProvides(annotated)) {
            val providerType = when (annotated) {
                is KtClass -> annotated.name
                is KtNamedFunction -> annotated.typeReference?.text
                is KtProperty -> annotated.typeReference?.text
                is KtParameter -> annotated.typeReference?.text
                else -> null
            }
            
            if (providerType != null) {
                return DependencyProvider(annotated, providerType)
            }
        }
        
        return null
    }
    
    private fun isDependencyConsumer(property: KtProperty): Boolean {
        property.delegate?.let { delegate ->
            val delegateExpression = delegate.expression
            return delegateExpression is KtNameReferenceExpression && 
                   delegateExpression.getReferencedName() == "di"
        }
        return false
    }
    
    private fun hasProvides(element: KtAnnotated): Boolean {
        return element.annotationEntries.any { it.shortName?.asString() == "Provides" }
    }
    
    private fun handleConsumerRefactor(consumer: DependencyConsumer, project: Project, editor: Editor) {
        val currentType = consumer.currentType
        val newType = Messages.showInputDialog(
            project,
            "Enter new dependency type:",
            "Refactor Dependency Type",
            null,
            currentType,
            null
        ) ?: return
        
        if (newType.isBlank() || newType == currentType) return
        
        // Perform the refactoring
        CommandProcessor.getInstance().executeCommand(project, {
            WriteAction.run<Exception> {
                refactorDependencyType(project, currentType, newType)
            }
        }, "Refactor Knit Dependency Type", null)
    }
    
    private fun handleProviderRefactor(provider: DependencyProvider, project: Project, editor: Editor) {
        val currentType = provider.currentType
        val newType = Messages.showInputDialog(
            project,
            "Enter new provider type:",
            "Refactor Provider Type", 
            null,
            currentType,
            null
        ) ?: return
        
        if (newType.isBlank() || newType == currentType) return
        
        // Perform the refactoring
        CommandProcessor.getInstance().executeCommand(project, {
            WriteAction.run<Exception> {
                refactorProviderType(project, provider, newType)
            }
        }, "Refactor Knit Provider Type", null)
    }
    
    private fun refactorDependencyType(project: Project, oldType: String, newType: String) {
        val scope = GlobalSearchScope.allScope(project)
        val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
        
        for (virtualFile in kotlinFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile ?: continue
            val factory = KtPsiFactory(project)
            
            // Update all consumers of this type
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
            
            // Update providers that return this type
            updateProvidersForType(psiFile, factory, oldType, newType)
        }
    }
    
    private fun refactorProviderType(project: Project, provider: DependencyProvider, newType: String) {
        val factory = KtPsiFactory(project)
        val oldType = provider.currentType
        
        // Update the provider itself
        when (val element = provider.element) {
            is KtClass -> {
                // For class providers, we might need to rename the class
                // This is more complex and might require IDE's built-in refactoring
                // element.name = newType // Cannot reassign val, would need more complex refactoring
            }
            is KtNamedFunction -> {
                element.typeReference?.let { typeRef ->
                    val newTypeRef = factory.createType(newType)
                    typeRef.replace(newTypeRef)
                }
            }
            is KtProperty -> {
                element.typeReference?.let { typeRef ->
                    val newTypeRef = factory.createType(newType)
                    typeRef.replace(newTypeRef)
                }
            }
            is KtParameter -> {
                element.typeReference?.let { typeRef ->
                    val newTypeRef = factory.createType(newType)
                    typeRef.replace(newTypeRef)
                }
            }
        }
        
        // Update all consumers that depend on this provider
        refactorDependencyType(project, oldType, newType)
    }
    
    private fun updateProvidersForType(psiFile: KtFile, factory: KtPsiFactory, oldType: String, newType: String) {
        // Update @Provides classes
        val classes = PsiTreeUtil.findChildrenOfType(psiFile, KtClass::class.java)
        for (ktClass in classes) {
            if (hasProvides(ktClass) && normalizeType(ktClass.name ?: "") == normalizeType(oldType)) {
                // This is more complex - might need to rename the class entirely
                // For now, we'll skip automatic class renaming as it requires more sophisticated refactoring
            }
            
            // Update @Provides constructor parameters
            ktClass.primaryConstructor?.valueParameters?.forEach { parameter ->
                if (hasProvides(parameter)) {
                    val paramType = parameter.typeReference?.text
                    if (paramType != null && normalizeType(paramType) == normalizeType(oldType)) {
                        val newTypeRef = factory.createType(newType)
                        parameter.typeReference?.replace(newTypeRef)
                    }
                }
            }
            
            // Update @Provides properties
            val properties = PsiTreeUtil.findChildrenOfType(ktClass, KtProperty::class.java)
            for (property in properties) {
                if (hasProvides(property)) {
                    val propType = property.typeReference?.text
                    if (propType != null && normalizeType(propType) == normalizeType(oldType)) {
                        val newTypeRef = factory.createType(newType)
                        property.typeReference?.replace(newTypeRef)
                    }
                }
            }
        }
        
        // Update @Provides functions
        val functions = PsiTreeUtil.findChildrenOfType(psiFile, KtNamedFunction::class.java)
        for (function in functions) {
            if (hasProvides(function)) {
                val returnType = function.typeReference?.text
                if (returnType != null && normalizeType(returnType) == normalizeType(oldType)) {
                    val newTypeRef = factory.createType(newType)
                    function.typeReference?.replace(newTypeRef)
                }
            }
        }
    }
    
    private fun normalizeType(type: String): String {
        return type.substringBefore('<').trim()
    }
    
    // Data classes for refactor targets
    sealed class RefactorTarget
    
    data class DependencyConsumer(
        val property: KtProperty,
        val currentType: String
    ) : RefactorTarget()
    
    data class DependencyProvider(
        val element: KtAnnotated,
        val currentType: String
    ) : RefactorTarget()
}
