package com.github.joshuataylor.datamancer.core.actions

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.services.DbtColumnMetadataService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil

/**
 * Action to navigate from a dbt SQL model file to its YAML schema definition.
 *
 * Accessible via:
 * - Navigate menu: "Go to YAML Definition"
 * - Keyboard shortcut: Ctrl+Alt+Y / Cmd+Alt+Y
 */
class DatamancerGoToYamlDefinitionAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val modelName = virtualFile.nameWithoutExtension
        val metadata = DbtColumnMetadataService.getInstance(project).findModel(modelName) ?: return

        val yamlElement = metadata.psiElement
        val offset = yamlElement.textOffset

        FileEditorManager.getInstance(project)
            .openTextEditor(OpenFileDescriptor(project, metadata.file, offset), true)
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project == null || virtualFile == null) {
            event.presentation.isEnabledAndVisible = false
            return
        }

        if (!virtualFile.name.endsWith(".sql") || !virtualFile.path.contains("/models/")) {
            event.presentation.isEnabledAndVisible = false
            return
        }

        val module = ReadAction.nonBlocking<Module?> {
            ModuleUtil.findModuleForFile(virtualFile, project)
        }.executeSynchronously()
        if (module == null) {
            event.presentation.isEnabledAndVisible = false
            return
        }

        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        event.presentation.isEnabledAndVisible = indexService.getDbtConfigForModuleName(module.name) != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
