package com.github.ngjackin.knitplugin.services

import com.github.ngjackin.knitplugin.analysis.CircularDependencyAnalyzer
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing circular dependency detection with caching
 */
@Service(Service.Level.PROJECT)
class CircularDependencyService(private val project: Project) {
    
    private val cache = ConcurrentHashMap<String, List<CircularDependencyAnalyzer.CircularDependency>>()
    private val elementCache = ConcurrentHashMap<PsiElement, Boolean>()
    
    /**
     * Get all circular dependencies in the project
     */
    fun getAllCircularDependencies(): List<CircularDependencyAnalyzer.CircularDependency> {
        return cache.getOrPut("all") {
            CircularDependencyAnalyzer.findCircularDependencies(project)
        }
    }
    
    /**
     * Check if an element is part of any circular dependency
     */
    fun isElementInCircularDependency(element: PsiElement): Boolean {
        return elementCache.getOrPut(element) {
            CircularDependencyAnalyzer.isPartOfCircularDependency(element)
        }
    }
    
    /**
     * Get circular dependencies for a specific type
     */
    fun getCircularDependenciesForType(type: String): List<CircularDependencyAnalyzer.CircularDependency> {
        return cache.getOrPut("type:$type") {
            CircularDependencyAnalyzer.getCircularDependenciesForType(type, project)
        }
    }
    
    /**
     * Clear the cache when files change
     */
    fun clearCache() {
        cache.clear()
        elementCache.clear()
    }
    
    /**
     * Get summary statistics about circular dependencies
     */
    fun getCircularDependencyStats(): CircularDependencyStats {
        val allDependencies = getAllCircularDependencies()
        val affectedTypes = allDependencies.flatMap { it.cycle }.toSet()
        val maxCycleLength = allDependencies.maxOfOrNull { it.cycle.size } ?: 0
        
        return CircularDependencyStats(
            totalCycles = allDependencies.size,
            affectedTypes = affectedTypes.size,
            maxCycleLength = maxCycleLength,
            cycleDetails = allDependencies.map { cycle ->
                CycleDetail(
                    cycle = cycle.cycle,
                    length = cycle.cycle.size
                )
            }
        )
    }
    
    data class CircularDependencyStats(
        val totalCycles: Int,
        val affectedTypes: Int,
        val maxCycleLength: Int,
        val cycleDetails: List<CycleDetail>
    )
    
    data class CycleDetail(
        val cycle: List<String>,
        val length: Int
    )
}
