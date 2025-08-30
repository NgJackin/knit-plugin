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
            contentPanel.add(JLabel("<html>• <code>@Provides</code> annotations (Producers)</html>"))
            contentPanel.add(JLabel("<html>• <code>by di</code> delegations (Consumers)</html>"))
            contentPanel.add(Box.createVerticalStrut(10))


            val providerIcon = AllIcons.General.Add
            val consumerIcon = AllIcons.General.Locate

            contentPanel.add(JLabel("Legend:"))
            contentPanel.add(JLabel(" Producer (provides dependencies)", providerIcon, SwingConstants.LEADING))
            contentPanel.add(JLabel(" Consumer (consumes dependencies)", consumerIcon, SwingConstants.LEADING))
            contentPanel.add(Box.createVerticalStrut(10))


            contentPanel.add(JLabel("Features:"))
            contentPanel.add(JLabel("• Circular Dependency Detection"))
            contentPanel.add(JLabel("• Gutter Icons and Inline Warnings for quick identification"))
            contentPanel.add(JLabel("• Automatic Detection of New Files and Classes added to Project"))
            
            add(contentPanel, BorderLayout.CENTER)
        }
    }
}
