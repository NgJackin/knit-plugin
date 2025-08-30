package com.github.ngjackin.knitplugin.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.ngjackin.knitplugin.services.MyProjectService
import javax.swing.*
import java.awt.BorderLayout

class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = KnitToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), "Knit DI", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class KnitToolWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            
            val titleLabel = JLabel("Knit Dependency Injection", SwingConstants.CENTER)
            titleLabel.font = titleLabel.font.deriveFont(16f)
            add(titleLabel, BorderLayout.NORTH)
            
            val contentPanel = JPanel()
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
            
            // Add information about the plugin
            contentPanel.add(JLabel("This plugin helps identify:"))
            contentPanel.add(JLabel("• @Provides annotations (Producers)"))
            contentPanel.add(JLabel("• by di delegations (Consumers)"))
            contentPanel.add(Box.createVerticalStrut(10))
            
            contentPanel.add(JLabel("Legend:"))
            contentPanel.add(JLabel("🟢 P = Producer (provides dependencies)"))
            contentPanel.add(JLabel("🔵 C = Consumer (consumes dependencies)"))
            contentPanel.add(Box.createVerticalStrut(10))
            
            contentPanel.add(JLabel("Features:"))
            contentPanel.add(JLabel("• Syntax highlighting for DI patterns"))
            contentPanel.add(JLabel("• Gutter icons for quick identification"))
            contentPanel.add(JLabel("• Inspections for missing providers"))
            
            add(contentPanel, BorderLayout.CENTER)
        }
    }
}
