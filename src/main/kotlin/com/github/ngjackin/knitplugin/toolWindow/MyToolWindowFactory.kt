package com.github.ngjackin.knitplugin.toolWindow

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.icons.AllIcons
import javax.swing.*
import java.awt.BorderLayout

class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = KnitToolWindow()
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), "Knit DI", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class KnitToolWindow {

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()

            val titleLabel = JLabel("Knit Dependency Injection", SwingConstants.CENTER)
            titleLabel.font = titleLabel.font.deriveFont(16f)
            titleLabel.border = BorderFactory.createEmptyBorder(8, 0, 8, 0) // top, left, bottom, right padding
            add(titleLabel, BorderLayout.NORTH)

            val contentPanel = JPanel()
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
            contentPanel.border = BorderFactory.createEmptyBorder(0, 12, 0, 0) // add left padding for the body content

            // Add information about the plugin
            contentPanel.add(JLabel("This plugin helps identify:"))
            contentPanel.add(JLabel("• @Provides annotations (Producers)"))
            contentPanel.add(JLabel("• by di delegations (Consumers)"))
            contentPanel.add(JLabel("• Circular dependencies (⚠️ Warnings)"))
            contentPanel.add(Box.createVerticalStrut(10))


            val providerIcon = AllIcons.General.Add
            val consumerIcon = AllIcons.General.Locate

            contentPanel.add(JLabel("Legend:"))
            contentPanel.add(JLabel("🟢 P = Producer (provides dependencies)"))
            contentPanel.add(JLabel("🔵 C = Consumer (consumes dependencies)"))
            contentPanel.add(JLabel("⚠️ = Circular dependency detected"))
            contentPanel.add(Box.createVerticalStrut(10))


            contentPanel.add(JLabel("Features:"))
            contentPanel.add(JLabel("• Syntax highlighting for DI patterns"))
            contentPanel.add(JLabel("• Gutter icons for quick identification"))
            contentPanel.add(JLabel("• Inspections for missing providers"))
            contentPanel.add(JLabel("• Circular dependency detection & warnings"))
            contentPanel.add(JLabel("• Unused provider detection with quick fixes"))
            
            add(contentPanel, BorderLayout.CENTER)
        }
    }
}
