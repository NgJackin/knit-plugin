package com.github.ngjackin.knitplugin.navigation

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*

/**
 * Resolves dependency relationships for Knit DI annotations
 */
object KnitDependencyResolverFixed {

    /**
     * Find all producers in the project that can provide the given type
     */
    fun findProducersFor(type: String, context: PsiElement): List<Producer> {
        val producers = mutableListOf<Producer>()
        val project = context.project
        val normalizedType = normalizeType(type)
        
        if (normalizedType.isEmpty()) return producers
        
        try {
            // Search through all Kotlin files in the project
            val scope = GlobalSearchScope.projectScope(project)
            val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
            
            for (virtualFile in kotlinFiles) {
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile
                psiFile?.let { file ->
                    findProducersInFile(file, normalizedType, producers)
                }
            }
        } catch (e: Exception) {
            // Fallback to single file search
            findProducersInFile(context.containingFile as? KtFile, normalizedType, producers)
        }
        
        return producers
    }

    /**
     * Find all consumers in the project that use the given type
     */
    fun findConsumersOf(type: String, context: PsiElement): List<Consumer> {
        val consumers = mutableListOf<Consumer>()
        val project = context.project
        val normalizedType = normalizeType(type)
        
        if (normalizedType.isEmpty()) return consumers
        
        try {
            // Search through all Kotlin files in the project
            val scope = GlobalSearchScope.projectScope(project)
            val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
            
            for (virtualFile in kotlinFiles) {
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile
                psiFile?.let { file ->
                    findConsumersInFile(file, normalizedType, consumers)
                }
            }
        } catch (e: Exception) {
            // Fallback to single file search
            findConsumersInFile(context.containingFile as? KtFile, normalizedType, consumers)
        }
        
        return consumers
    }

    /**
     * Find producers within a single file
     */
    private fun findProducersInFile(file: KtFile?, normalizedType: String, producers: MutableList<Producer>) {
        file ?: return
        
        try {
            // Find @Provides annotations
            PsiTreeUtil.findChildrenOfType(file, KtAnnotationEntry::class.java).forEach { annotation ->
                if (annotation.shortName?.asString() == "Provides") {
                    val annotatedElement = getAnnotatedElement(annotation)
                    when (annotatedElement) {
                        is KtNamedFunction -> {
                            val returnType = getReturnType(annotatedElement)
                            if (normalizeType(returnType) == normalizedType) {
                                producers.add(Producer(
                                    annotatedElement,
                                    returnType,
                                    ProducerKind.FACTORY_METHOD
                                ))
                            }
                        }
                        is KtClass -> {
                            val className = annotatedElement.name ?: ""
                            if (normalizeType(className) == normalizedType) {
                                producers.add(Producer(
                                    annotatedElement,
                                    className,
                                    ProducerKind.CLASS_ANNOTATION
                                ))
                            }
                        }
                        is KtProperty -> {
                            val propertyType = getPropertyType(annotatedElement)
                            if (normalizeType(propertyType) == normalizedType) {
                                producers.add(Producer(
                                    annotatedElement,
                                    propertyType,
                                    ProducerKind.PROPERTY
                                ))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silent fail for compatibility
        }
    }

    /**
     * Find consumers within a single file
     */
    private fun findConsumersInFile(file: KtFile?, normalizedType: String, consumers: MutableList<Consumer>) {
        file ?: return
        
        try {
            // Find 'by di' delegations
            PsiTreeUtil.findChildrenOfType(file, KtPropertyDelegate::class.java).forEach { delegate ->
                if (delegate.text.contains("by di")) {
                    val property = PsiTreeUtil.getParentOfType(delegate, KtProperty::class.java)
                    property?.let { prop ->
                        val propertyType = getPropertyType(prop)
                        if (normalizeType(propertyType) == normalizedType) {
                            consumers.add(Consumer(
                                prop,
                                propertyType,
                                ConsumerKind.PROPERTY_DELEGATION
                            ))
                        }
                    }
                }
            }
            
            // Find constructor parameters that might be injected
            PsiTreeUtil.findChildrenOfType(file, KtParameter::class.java).forEach { param ->
                val paramType = getParameterType(param)
                if (normalizeType(paramType) == normalizedType) {
                    // Check if this is in a constructor that might use DI
                    val constructor = PsiTreeUtil.getParentOfType(param, KtPrimaryConstructor::class.java)
                        ?: PsiTreeUtil.getParentOfType(param, KtSecondaryConstructor::class.java)
                    
                    if (constructor != null) {
                        consumers.add(Consumer(
                            param,
                            paramType,
                            ConsumerKind.CONSTRUCTOR_PARAMETER
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            // Silent fail for compatibility
        }
    }

    /**
     * Get the element that is annotated by the given annotation
     */
    private fun getAnnotatedElement(annotation: KtAnnotationEntry): KtElement? {
        return try {
            annotation.parent?.parent as? KtElement
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the return type of a function
     */
    private fun getReturnType(function: KtNamedFunction): String {
        return try {
            function.typeReference?.text ?: "Unit"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get the type of a property
     */
    private fun getPropertyType(property: KtProperty): String {
        return try {
            property.typeReference?.text ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get the type of a parameter
     */
    private fun getParameterType(parameter: KtParameter): String {
        return try {
            parameter.typeReference?.text ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
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

    /**
     * Get dependency graph for visualization
     */
    fun getDependencyGraph(project: Project): DependencyGraph {
        val graph = DependencyGraph()
        
        try {
            val scope = GlobalSearchScope.projectScope(project)
            val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
            
            for (virtualFile in kotlinFiles) {
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile
                psiFile?.let { file ->
                    analyzeDependenciesInFile(file, graph)
                }
            }
        } catch (e: Exception) {
            // Silent fail for compatibility
        }
        
        return graph
    }

    /**
     * Analyze dependencies within a single file
     */
    private fun analyzeDependenciesInFile(file: KtFile, graph: DependencyGraph) {
        try {
            // Find all producers
            PsiTreeUtil.findChildrenOfType(file, KtAnnotationEntry::class.java).forEach { annotation ->
                if (annotation.shortName?.asString() == "Provides") {
                    val annotatedElement = getAnnotatedElement(annotation)
                    when (annotatedElement) {
                        is KtNamedFunction -> {
                            val returnType = getReturnType(annotatedElement)
                            graph.addProducer(normalizeType(returnType), annotatedElement)
                        }
                        is KtClass -> {
                            val className = annotatedElement.name ?: ""
                            graph.addProducer(normalizeType(className), annotatedElement)
                        }
                        is KtProperty -> {
                            val propertyType = getPropertyType(annotatedElement)
                            graph.addProducer(normalizeType(propertyType), annotatedElement)
                        }
                    }
                }
            }
            
            // Find all consumers
            PsiTreeUtil.findChildrenOfType(file, KtPropertyDelegate::class.java).forEach { delegate ->
                if (delegate.text.contains("by di")) {
                    val property = PsiTreeUtil.getParentOfType(delegate, KtProperty::class.java)
                    property?.let { prop ->
                        val propertyType = getPropertyType(prop)
                        graph.addConsumer(normalizeType(propertyType), prop)
                    }
                }
            }
        } catch (e: Exception) {
            // Silent fail for compatibility
        }
    }
}

/**
 * Represents a dependency producer
 */
data class Producer(
    val element: KtElement,
    val type: String,
    val kind: ProducerKind
)

/**
 * Represents a dependency consumer
 */
data class Consumer(
    val element: KtElement,
    val type: String,
    val kind: ConsumerKind
)

/**
 * Types of producers
 */
enum class ProducerKind {
    FACTORY_METHOD,
    CLASS_ANNOTATION,
    PROPERTY,
    CONSTRUCTOR_PARAMETER
}

/**
 * Types of consumers
 */
enum class ConsumerKind {
    PROPERTY_DELEGATION,
    CONSTRUCTOR_PARAMETER,
    METHOD_PARAMETER
}

/**
 * Represents the dependency graph
 */
class DependencyGraph {
    private val producers = mutableMapOf<String, MutableList<KtElement>>()
    private val consumers = mutableMapOf<String, MutableList<KtElement>>()
    
    fun addProducer(type: String, element: KtElement) {
        producers.getOrPut(type) { mutableListOf() }.add(element)
    }
    
    fun addConsumer(type: String, element: KtElement) {
        consumers.getOrPut(type) { mutableListOf() }.add(element)
    }
    
    fun getProducers(type: String): List<KtElement> = producers[type] ?: emptyList()
    fun getConsumers(type: String): List<KtElement> = consumers[type] ?: emptyList()
    fun getAllTypes(): Set<String> = producers.keys + consumers.keys
}
