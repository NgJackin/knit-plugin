package com.github.ngjackin.knitplugin.inspection

import com.github.ngjackin.knitplugin.quickfix.RemoveProviderAnnotationQuickFix
import com.github.ngjackin.knitplugin.quickfix.RemoveEntireProviderQuickFix
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * Inspection that detects unused @Provides annotations and offers quick fixes
 */
class KnitUnusedProviderInspection : LocalInspectionTool() {
    
    override fun getDisplayName(): String = "Knit Unused Provider Detection"
    
    override fun getShortName(): String = "KnitUnusedProvider"
    
    override fun getGroupDisplayName(): String = "Knit Dependency Injection"
    
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClass(ktClass: KtClass) {
                super.visitClass(ktClass)
                checkClassProvider(ktClass, holder)
            }
            
            override fun visitParameter(parameter: KtParameter) {
                super.visitParameter(parameter)
                checkParameterProvider(parameter, holder)
            }
            
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                checkPropertyProvider(property, holder)
            }
        }
    }
    
    private fun checkClassProvider(ktClass: KtClass, holder: ProblemsHolder) {
        val providesAnnotation = ktClass.annotationEntries.find { it.shortName?.asString() == "Provides" }
        if (providesAnnotation == null) return
        
        val className = ktClass.name ?: return
        
        if (!isProviderUsed(className, ktClass)) {
            val nameIdentifier = ktClass.nameIdentifier
            if (nameIdentifier != null) {
                holder.registerProblem(
                    nameIdentifier,
                    "Unused provider: No injection found for '$className'",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveProviderAnnotationQuickFix(providesAnnotation),
                    RemoveEntireProviderQuickFix(ktClass)
                )
            }
        }
    }
    
    private fun checkParameterProvider(parameter: KtParameter, holder: ProblemsHolder) {
        val providesAnnotation = parameter.annotationEntries.find { it.shortName?.asString() == "Provides" }
        if (providesAnnotation == null) return
        
        val parameterType = parameter.typeReference?.text ?: return
        val normalizedType = normalizeType(parameterType)
        
        if (!isProviderUsed(normalizedType, parameter)) {
            val nameIdentifier = parameter.nameIdentifier
            if (nameIdentifier != null) {
                holder.registerProblem(
                    nameIdentifier,
                    "Unused provider: No injection found for parameter type '$parameterType'",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveProviderAnnotationQuickFix(providesAnnotation)
                )
            }
        }
    }
    
    private fun checkPropertyProvider(property: KtProperty, holder: ProblemsHolder) {
        val providesAnnotation = property.annotationEntries.find { it.shortName?.asString() == "Provides" }
        if (providesAnnotation == null) return
        
        val propertyType = property.typeReference?.text ?: return
        val normalizedType = normalizeType(propertyType)
        
        if (!isProviderUsed(normalizedType, property)) {
            val nameIdentifier = property.nameIdentifier
            if (nameIdentifier != null) {
                holder.registerProblem(
                    nameIdentifier,
                    "Unused provider: No injection found for property type '$propertyType'",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveProviderAnnotationQuickFix(providesAnnotation)
                )
            }
        }
    }
    
    /**
     * Check if a provider type is actually used (injected) anywhere in the project
     */
    private fun isProviderUsed(typeName: String, context: KtElement): Boolean {
        val project = context.project
        val normalizedTypeName = normalizeType(typeName)
        
        // Search for 'by di' usages of this type in the project
        return findInjectionUsages(normalizedTypeName, project).isNotEmpty()
    }
    
    /**
     * Find all 'by di' usages that would consume this provider type
     */
    private fun findInjectionUsages(typeName: String, project: com.intellij.openapi.project.Project): List<KtProperty> {
        val usages = mutableListOf<KtProperty>()
        
        try {
            val scope = GlobalSearchScope.projectScope(project)
            
            // Find all properties with 'by di' that have matching type
            com.intellij.psi.search.FileTypeIndex.getFiles(org.jetbrains.kotlin.idea.KotlinFileType.INSTANCE, scope).forEach { virtualFile ->
                val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile) as? KtFile
                psiFile?.let { file ->
                    com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(file, KtProperty::class.java).forEach { property ->
                        if (isInjectedProperty(property)) {
                            val propertyType = property.typeReference?.text
                            if (propertyType != null && normalizeType(propertyType) == typeName) {
                                usages.add(property)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silent fail for compatibility
        }
        
        return usages
    }
    
    /**
     * Check if a property uses 'by di' injection
     */
    private fun isInjectedProperty(property: KtProperty): Boolean {
        return property.delegate?.expression?.text?.contains("di") == true
    }
    
    /**
     * Normalize type names for comparison
     */
    private fun normalizeType(type: String): String {
        return type
            .replace("?", "") // Remove nullable markers
            .replace(" ", "") // Remove spaces
            .replace("\n", "") // Remove newlines
            .replace("\t", "") // Remove tabs
            .substringBefore("<") // Remove generic parameters
            .substringAfterLast(".") // Get simple name
            .trim()
    }
}
