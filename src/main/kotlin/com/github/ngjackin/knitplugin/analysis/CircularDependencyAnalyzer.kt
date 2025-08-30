package com.github.ngjackin.knitplugin.analysis

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass

/**
 * Analyzes dependency injection patterns to detect circular dependencies
 */
object CircularDependencyAnalyzer {

    /**
     * Represents a dependency node in the dependency graph
     */
    data class DependencyNode(
        val type: String,
        val element: PsiElement,
        val dependencies: MutableSet<String> = mutableSetOf()
    )

    /**
     * Represents a circular dependency path
     */
    data class CircularDependency(
        val cycle: List<String>,
        val elements: List<PsiElement>
    ) {
        fun containsType(type: String): Boolean = cycle.contains(type)
        
        fun getElementForType(type: String): PsiElement? {
            val index = cycle.indexOf(type)
            return if (index >= 0 && index < elements.size) elements[index] else null
        }
    }

    /**
     * Find all circular dependencies in the project
     */
    fun findCircularDependencies(project: Project): List<CircularDependency> {
        val dependencyGraph = buildDependencyGraph(project)
        return detectCycles(dependencyGraph)
    }

    /**
     * Check if a specific element is part of any circular dependency
     */
    fun isPartOfCircularDependency(element: PsiElement): Boolean {
        val project = element.project
        
        // Get the type of the element we're checking
        val elementType = when (element) {
            is KtClass -> element.name
            is KtProperty -> element.typeReference?.text
            is KtParameter -> element.typeReference?.text
            else -> {
                // Try to find the containing class or property
                val containingClass = element.parent as? KtClass ?: 
                                    PsiTreeUtil.getParentOfType(element, KtClass::class.java)
                val containingProperty = element.parent as? KtProperty ?: 
                                       PsiTreeUtil.getParentOfType(element, KtProperty::class.java)
                
                when {
                    containingClass != null -> containingClass.name
                    containingProperty != null -> containingProperty.typeReference?.text
                    else -> null
                }
            }
        } ?: return false
        
        val normalizedType = normalizeType(elementType)
        val circularDependencies = findCircularDependencies(project)
        
        return circularDependencies.any { cycle ->
            cycle.containsType(normalizedType)
        }
    }

    /**
     * Get circular dependencies that affect a specific type
     */
    fun getCircularDependenciesForType(type: String, project: Project): List<CircularDependency> {
        val allCircularDependencies = findCircularDependencies(project)
        return allCircularDependencies.filter { it.containsType(normalizeType(type)) }
    }

    /**
     * Build the complete dependency graph for the project
     */
    private fun buildDependencyGraph(project: Project): Map<String, DependencyNode> {
        val nodes = mutableMapOf<String, DependencyNode>()
        
        try {
            val scope = GlobalSearchScope.projectScope(project)
            val kotlinFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, scope)
            
            for (virtualFile in kotlinFiles) {
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? KtFile
                psiFile?.let { file ->
                    analyzeFileForDependencies(file, nodes)
                }
            }
        } catch (e: Exception) {
            // Log error but continue with partial analysis
        }
        
        return nodes
    }

    /**
     * Analyze a single file for dependency relationships
     */
    private fun analyzeFileForDependencies(file: KtFile, nodes: MutableMap<String, DependencyNode>) {
        // Find all classes and their dependencies
        PsiTreeUtil.findChildrenOfType(file, KtClass::class.java).forEach { ktClass ->
            analyzeClassDependencies(ktClass, nodes)
        }
    }

    /**
     * Analyze dependencies within a class
     */
    private fun analyzeClassDependencies(ktClass: KtClass, nodes: MutableMap<String, DependencyNode>) {
        val className = ktClass.name ?: return
        val normalizedClassName = normalizeType(className)
        
        // Check if this class is a provider (has @Provides annotation)
        val isProvider = ktClass.annotationEntries.any { it.shortName?.asString() == "Provides" }
        
        if (isProvider) {
            // Create or get the node for this class
            val classNode = nodes.getOrPut(normalizedClassName) {
                DependencyNode(normalizedClassName, ktClass)
            }
            
            // ONLY add dependencies from injected properties (by di)
            // Constructor parameters are NOT dependencies - they're just constructor parameters
            PsiTreeUtil.findChildrenOfType(ktClass, KtProperty::class.java).forEach { property ->
                if (isInjectedProperty(property)) {
                    val propertyType = property.typeReference?.text
                    if (propertyType != null) {
                        val normalizedPropertyType = normalizeType(propertyType)
                        classNode.dependencies.add(normalizedPropertyType)
                        
                        // Ensure the dependency type has a node (even if it doesn't have dependencies yet)
                        if (!nodes.containsKey(normalizedPropertyType)) {
                            nodes[normalizedPropertyType] = DependencyNode(normalizedPropertyType, property)
                        }
                    }
                }
            }
        }
        
        // Register constructor parameters with @Provides as providers, but they don't have dependencies
        // This allows them to be PROVIDED TO other classes, but they don't DEPEND ON anything
        ktClass.primaryConstructor?.valueParameters?.forEach { param ->
            val hasProvides = param.annotationEntries.any { it.shortName?.asString() == "Provides" }
            if (hasProvides) {
                val paramType = param.typeReference?.text
                if (paramType != null) {
                    val normalizedParamType = normalizeType(paramType)
                    // Create a node for this parameter provider (but no dependencies)
                    nodes.getOrPut(normalizedParamType) {
                        DependencyNode(normalizedParamType, param)
                    }
                    // Note: Constructor parameters don't add dependencies to their containing class
                    // They are just providers of their type, nothing more
                }
            }
        }
    }

    /**
     * Check if a property is injected using 'by di'
     */
    private fun isInjectedProperty(property: KtProperty): Boolean {
        return property.delegate?.expression?.text?.contains("di") == true
    }

    /**
     * Detect cycles in the dependency graph using DFS
     */
    private fun detectCycles(graph: Map<String, DependencyNode>): List<CircularDependency> {
        val visited = mutableSetOf<String>()
        val cycles = mutableListOf<CircularDependency>()
        
        // Try DFS from each unvisited node
        for (startNode in graph.keys) {
            if (startNode !in visited) {
                val recursionStack = mutableSetOf<String>()
                val path = mutableListOf<String>()
                val elements = mutableListOf<PsiElement>()
                findCyclesFromNode(startNode, graph, visited, recursionStack, path, elements, cycles)
            }
        }
        
        // Remove duplicate cycles (same cycle detected from different starting points)
        return cycles.distinctBy { cycle ->
            val sortedCycle = cycle.cycle.sorted()
            sortedCycle.joinToString("->")
        }
    }

    /**
     * DFS to find cycles starting from a specific node
     */
    private fun findCyclesFromNode(
        current: String,
        graph: Map<String, DependencyNode>,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        path: MutableList<String>,
        elements: MutableList<PsiElement>,
        cycles: MutableList<CircularDependency>
    ) {
        visited.add(current)
        recursionStack.add(current)
        path.add(current)
        
        val currentNode = graph[current]
        if (currentNode != null) {
            elements.add(currentNode.element)
            
            for (dependency in currentNode.dependencies) {
                if (dependency == current) {
                    // Self-dependency - create a simple cycle
                    cycles.add(CircularDependency(listOf(current, current), listOf(currentNode.element)))
                } else if (dependency in recursionStack) {
                    // Found a cycle - trace back to where the cycle starts
                    val cycleStartIndex = path.indexOf(dependency)
                    if (cycleStartIndex >= 0) {
                        val cyclePath = path.subList(cycleStartIndex, path.size) + dependency
                        val cycleElements = elements.subList(cycleStartIndex, elements.size)
                        
                        cycles.add(CircularDependency(cyclePath, cycleElements.toList()))
                    }
                } else if (dependency !in visited && graph.containsKey(dependency)) {
                    findCyclesFromNode(dependency, graph, visited, recursionStack, path, elements, cycles)
                }
            }
        }
        
        recursionStack.remove(current)
        if (path.isNotEmpty() && path.last() == current) {
            path.removeAt(path.size - 1)
        }
        if (elements.isNotEmpty()) {
            elements.removeAt(elements.size - 1)
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
}
