package com.github.ngjackin.knitplugin.debug

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*

class DebugLineMarkerProvider : LineMarkerProvider {
    
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Mark EVERY Kotlin class to test if the provider is working at all
        if (element is KtClass && element.nameIdentifier == element) {
            val icon = IconLoader.getIcon("/general/information.svg", DebugLineMarkerProvider::class.java)
            return LineMarkerInfo(
                element,
                element.textRange,
                icon,
                { "Debug: This is a Kotlin class" },
                null,
                GutterIconRenderer.Alignment.LEFT
            ) { "Debug marker" }
        }
        return null
    }
}
