package com.github.ngjackin.knitplugin.inlayhints

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage

/**
 * Provider for Knit DI inlay hints with circular dependency detection
 */
@Suppress("UnstableApiUsage")
class KnitInlayHintsProvider : InlayHintsProvider<NoSettings> {
    
    override val key: SettingsKey<NoSettings> = SettingsKey("knit.hints")
    
    override val name: String = "Knit Dependency Injection"
    
    override val previewText: String = """
        @Provides
        class UserService {
            val user: User by di
        }
    """.trimIndent()
    
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return KnitInlayHintsCollector(editor)
    }
    
    override fun createSettings(): NoSettings = NoSettings()
    
    override fun isLanguageSupported(language: Language): Boolean {
        return language is KotlinLanguage
    }
    
    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener) = 
                javax.swing.JPanel() // Return a simple JPanel as required by the interface
        }
    }
}
