package com.github.ngjackin.knitplugin.inspection

import com.github.ngjackin.knitplugin.quickfix.CreateProviderClassQuickFix
import com.github.ngjackin.knitplugin.quickfix.CreateProviderParameterQuickFix
import com.github.ngjackin.knitplugin.quickfix.CreateProviderPropertyQuickFix
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

class KnitMissingProviderInspection : LocalInspectionTool() {
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                checkMissingProvider(property, holder)
            }
        }
    }
    
    private fun checkMissingProvider(property: KtProperty, holder: ProblemsHolder) {
        // Check if this is a 'by di' property
        property.delegate?.let { delegate ->
            val delegateExpression = delegate.expression
            if (delegateExpression is KtNameReferenceExpression && delegateExpression.getReferencedName() == "di") {
                val propertyType = property.typeReference?.text
                if (propertyType != null) {
                    val normalizedType = normalizeType(propertyType)
                    
                    // Check if there's a provider for this type anywhere in the project
                    if (!hasProviderForType(property, normalizedType)) {
                        val containingClass = property.containingClass()
                        
                        // Create quick fixes
                        val quickFixes = mutableListOf<LocalQuickFix>()
                        
                        // Option 1: Create a new @Provides class
                        quickFixes.add(CreateProviderClassQuickFix(normalizedType))
                        
                        // Option 2: Add @Provides parameter to current class constructor (if applicable)
                        if (containingClass != null) {
                            quickFixes.add(CreateProviderParameterQuickFix(containingClass, normalizedType))
                        }
                        
                        // Option 3: Add @Provides property to current class
                        if (containingClass != null) {
                            quickFixes.add(CreateProviderPropertyQuickFix(containingClass, normalizedType))
                        }
                        
                        holder.registerProblem(
                            delegateExpression,
                            "No provider found for type '$normalizedType'",
                            *quickFixes.toTypedArray()
                        )
                    }
                }
            }
        }
    }
    
    private fun hasProviderForType(context: KtProperty, targetType: String): Boolean {
        val project = context.project
        val scope = GlobalSearchScope.allScope(project)
        
        // Search all Kotlin files in the project
        val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
        
        for (virtualFile in kotlinFiles) {
            val psiFile = context.manager.findFile(virtualFile) as? KtFile ?: continue
            
            // Check all classes with @Provides
            val classes = PsiTreeUtil.findChildrenOfType(psiFile, KtClass::class.java)
            for (ktClass in classes) {
                if (hasProvides(ktClass) && normalizeType(ktClass.name ?: "") == targetType) {
                    return true
                }
                
                // Check constructor parameters with @Provides
                ktClass.primaryConstructor?.valueParameters?.forEach { parameter ->
                    if (hasProvides(parameter)) {
                        val paramType = parameter.typeReference?.text
                        if (paramType != null && normalizeType(paramType) == targetType) {
                            return true
                        }
                    }
                }
                
                // Check properties with @Provides
                val properties = PsiTreeUtil.findChildrenOfType(ktClass, KtProperty::class.java)
                for (property in properties) {
                    if (hasProvides(property)) {
                        val propType = property.typeReference?.text
                        if (propType != null && normalizeType(propType) == targetType) {
                            return true
                        }
                    }
                }
            }
            
            // Check top-level functions with @Provides (if any)
            val functions = PsiTreeUtil.findChildrenOfType(psiFile, KtNamedFunction::class.java)
            for (function in functions) {
                if (hasProvides(function)) {
                    val returnType = function.typeReference?.text
                    if (returnType != null && normalizeType(returnType) == targetType) {
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    private fun hasProvides(element: KtAnnotated): Boolean {
        return element.annotationEntries.any { it.shortName?.asString() == "Provides" }
    }
    
    private fun normalizeType(type: String): String {
        // Remove generic parameters and whitespace for simpler matching
        return type.substringBefore('<').trim()
    }
}
