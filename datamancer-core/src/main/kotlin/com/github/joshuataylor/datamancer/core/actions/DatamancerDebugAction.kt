package com.github.joshuataylor.datamancer.core.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * Action that writes comprehensive debug information to a file.
 *
 * This action collects debug information from all Datamancer services
 * and writes it to a file named 'datamancer-debug.txt' in the project root.
 *
 * Accessible via:
 * - Tools menu: "Datamancer Debug"
 * - Keyboard shortcut: Ctrl+Alt+Shift+D
 */
class DatamancerDebugAction : AnAction() {

    companion object {
        private const val DEBUG_FILE_NAME = "datamancer-debug.txt"
        private const val NOTIFICATION_GROUP_ID = "Datamancer"
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        val debugInfo = try {
            val collector = DatamancerDebugInfoCollector(project)
            collector.collectAll(event)
        } catch (e: Exception) {
            "Failed to collect debug info: ${e.message}\n${e.stackTraceToString()}"
        }

        val outputFile = writeDebugFile(project, debugInfo)

        if (outputFile != null) {
            showSuccessNotification(project, outputFile)
        } else {
            showErrorNotification(project)
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun writeDebugFile(project: Project, content: String): File? {
        val basePath = project.basePath ?: return null
        val outputFile = File(basePath, DEBUG_FILE_NAME)

        return try {
            outputFile.writeText(content)
            outputFile
        } catch (e: Exception) {
            null
        }
    }

    private fun showSuccessNotification(project: Project, outputFile: File) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                "Debug Info Written",
                "Debug information saved to: ${outputFile.name}",
                NotificationType.INFORMATION
            )

        notification.addAction(object : com.intellij.notification.NotificationAction("Open File") {
            override fun actionPerformed(e: AnActionEvent, notification: com.intellij.notification.Notification) {
                openDebugFile(project, outputFile)
                notification.expire()
            }
        })

        notification.notify(project)
    }

    private fun showErrorNotification(project: Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                "Debug Info Failed",
                "Failed to write debug information to file.",
                NotificationType.ERROR
            )
            .notify(project)
    }

    private fun openDebugFile(project: Project, file: File) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}
