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

class RefactorProviderTypeAction : AnAction("Change Provider Type") {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return
        
        val element = psiFile.findElementAt(editor.caretModel.offset) ?: return
        val providerInfo = findProviderElement(element) ?: run {
            Messages.showInfoMessage(
                "Place cursor on a @Provides element (class, function, property, or parameter) to change what it provides",
                "Provider Type Refactoring"
            )
            return
        }
        
        val currentType = providerInfo.currentType
        val newType = Messages.showInputDialog(
            project,
            "Change provider type from '$currentType' to:",
            "Refactor Provider Type",
            null,
            currentType,
            null
        ) ?: return
        
        if (newType.isBlank() || newType == currentType) return
        
        performProviderTypeRefactoring(project, providerInfo, newType)
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val project = e.project
        
        var isEnabled = false
        
        if (editor != null && psiFile is KtFile && project != null) {
            val element = psiFile.findElementAt(editor.caretModel.offset)
            if (element != null) {
                isEnabled = findProviderElement(element) != null
            }
        }
        
        e.presentation.isEnabledAndVisible = isEnabled
    }
    
    private fun findProviderElement(element: PsiElement): ProviderInfo? {
        // Look for @Provides annotation on various elements
        var current: PsiElement? = element
        
        // Traverse up the tree looking for @Provides elements
        while (current != null && current !is KtFile) {
            if (current is KtAnnotated && hasProvides(current)) {
                val providerType = when (current) {
                    is KtClass -> current.name ?: return null
                    is KtNamedFunction -> current.typeReference?.text ?: return null  
                    is KtProperty -> current.typeReference?.text ?: return null
                    is KtParameter -> current.typeReference?.text ?: return null
                    else -> return null
                }
                
                return ProviderInfo(current as KtAnnotated, providerType)
            }
            
            current = current.parent
        }
        
        // Also try using PsiTreeUtil as a fallback
        val annotated = PsiTreeUtil.getParentOfType(element, KtAnnotated::class.java)
        if (annotated != null && hasProvides(annotated)) {
            val providerType = when (annotated) {
                is KtClass -> annotated.name ?: return null
                is KtNamedFunction -> annotated.typeReference?.text ?: return null  
                is KtProperty -> annotated.typeReference?.text ?: return null
                is KtParameter -> annotated.typeReference?.text ?: return null
                else -> return null
            }
            
            return ProviderInfo(annotated, providerType)
        }
        
        return null
    }
    
    private fun performProviderTypeRefactoring(project: Project, providerInfo: ProviderInfo, newType: String) {
        CommandProcessor.getInstance().executeCommand(project, {
            WriteAction.run<Exception> {
                val factory = KtPsiFactory(project)
                val oldType = providerInfo.currentType
                
                // Update the provider itself
                updateProviderElement(providerInfo.element, factory, newType)
                
                // Update all consumers that depend on this type
                updateAllConsumersOfType(project, oldType, newType)
            }
        }, "Refactor Provider Type: ${providerInfo.currentType} → $newType", null)
    }
    
    private fun updateProviderElement(element: KtAnnotated, factory: KtPsiFactory, newType: String) {
        when (element) {
            is KtClass -> {
                // For class providers, changing the type means renaming the class
                // This is complex and would ideally use IntelliJ's built-in rename refactoring
                // For now, we'll focus on type references rather than class names
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
    }
    
    private fun updateAllConsumersOfType(project: Project, oldType: String, newType: String) {
        val scope = GlobalSearchScope.allScope(project)
        val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
        val factory = KtPsiFactory(project)
        
        for (virtualFile in kotlinFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile ?: continue
            
            // Update all 'by di' consumers of this type
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
            
            // Update function parameters that use this type
            updateFunctionParameters(psiFile, factory, oldType, newType)
            
            // Update other providers that might reference this type
            updateOtherProviders(psiFile, factory, oldType, newType)
        }
    }
    
    private fun updateFunctionParameters(psiFile: KtFile, factory: KtPsiFactory, oldType: String, newType: String) {
        val functions = PsiTreeUtil.findChildrenOfType(psiFile, KtNamedFunction::class.java)
        for (function in functions) {
            function.valueParameters.forEach { parameter ->
                val typeRef = parameter.typeReference
                if (typeRef != null && normalizeType(typeRef.text) == normalizeType(oldType)) {
                    val newTypeRef = factory.createType(newType)
                    typeRef.replace(newTypeRef)
                }
            }
        }
    }
    
    private fun updateOtherProviders(psiFile: KtFile, factory: KtPsiFactory, oldType: String, newType: String) {
        // Update providers that might use this type as a dependency
        val classes = PsiTreeUtil.findChildrenOfType(psiFile, KtClass::class.java)
        for (ktClass in classes) {
            ktClass.primaryConstructor?.valueParameters?.forEach { parameter ->
                val typeRef = parameter.typeReference
                if (typeRef != null && normalizeType(typeRef.text) == normalizeType(oldType)) {
                    val newTypeRef = factory.createType(newType)
                    typeRef.replace(newTypeRef)
                }
            }
        }
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
    
    private fun normalizeType(type: String): String {
        return type.substringBefore('<').trim()
    }
    
    data class ProviderInfo(
        val element: KtAnnotated,
        val currentType: String
    )
}
