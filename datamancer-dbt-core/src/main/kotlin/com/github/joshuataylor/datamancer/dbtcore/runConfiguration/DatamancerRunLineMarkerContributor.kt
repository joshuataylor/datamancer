package com.github.joshuataylor.datamancer.dbtcore.runConfiguration

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfigStore
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement

/**
 * Adds a run gutter icon to dbt model SQL files.
 * The icon appears at the first element of the file and allows running
 * the dbt compile & execute configuration directly from the editor.
 */
class DatamancerRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        // Only show on the first element of a file to avoid multiple icons
        if (!isFirstElementInFile(element)) {
            return null
        }

        // Check if run gutter icons are enabled in settings
        val configStore = DatamancerProjectConfigStore.getInstance(element.project)
        if (!configStore.showRunGutterIcon) {
            return null
        }

        val containingFile = element.containingFile ?: return null
        val virtualFile = containingFile.virtualFile ?: return null

        // Only handle SQL files
        if (!virtualFile.name.endsWith(".sql")) {
            return null
        }

        // Check if this file is in a models directory
        val path = virtualFile.path
        if (!path.contains("/models/")) {
            return null
        }

        // Check if the module has a dbt project configuration
        val project = element.project
        val module = ModuleUtil.findModuleForFile(virtualFile, project) ?: return null
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        indexService.getDbtConfigForModuleName(module.name) ?: return null

        // Create the run actions
        val actions = ExecutorAction.getActions(0)

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            actions,
            { "Run dbt model '${virtualFile.nameWithoutExtension}'" }
        )
    }

    /**
     * Check if this element is the first meaningful element in the file.
     * We only want to show one gutter icon per file.
     */
    private fun isFirstElementInFile(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false

        // Get the first leaf element in the file
        var firstElement: PsiElement? = file.firstChild
        while (firstElement != null && firstElement.firstChild != null) {
            firstElement = firstElement.firstChild
        }

        // Skip whitespace and comments to find the first meaningful element
        while (firstElement != null && (firstElement.text.isBlank() || isComment(firstElement))) {
            firstElement = firstElement.nextSibling
            // Descend into children if needed
            while (firstElement != null && firstElement.firstChild != null) {
                firstElement = firstElement.firstChild
            }
        }

        return element == firstElement
    }

    private fun isComment(element: PsiElement): Boolean {
        val elementType = element.node?.elementType?.toString() ?: return false
        return elementType.contains("COMMENT", ignoreCase = true)
    }
}
