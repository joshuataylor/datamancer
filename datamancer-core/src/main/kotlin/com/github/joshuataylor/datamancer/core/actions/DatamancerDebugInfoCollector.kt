package com.github.joshuataylor.datamancer.core.actions

import com.github.joshuataylor.datamancer.core.api.DatamancerDebugInfoContributor
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.services.DbtColumnMetadataService
import com.github.joshuataylor.datamancer.core.services.DbtMacroIndexService
import com.github.joshuataylor.datamancer.core.services.DbtSourceIndexService
import com.github.joshuataylor.datamancer.core.services.DbtVarIndexService
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfigStore
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Collects and formats debug information for the Datamancer plugin.
 *
 * This class gathers comprehensive information about:
 * - Environment (plugin version, IDE version, platform)
 * - General settings (appearance toggles)
 * - Workspace and dbt projects (including all per-project configuration)
 * - Current file information (if a file is open)
 * - Indexed sources, variables, macros, and column metadata models
 * - Extension-contributed sections (e.g. manifest data from backend plugins)
 */
class DatamancerDebugInfoCollector(private val project: Project) {

    companion object {
        private const val SEPARATOR = "================================================================================"
        private const val PLUGIN_ID = "com.github.joshuataylor.datamancer"
    }

    /**
     * Collects all debug information and returns it as a formatted string.
     */
    fun collectAll(event: AnActionEvent?): String {
        return buildString {
            appendHeader()
            appendSafe { appendEnvironmentInfo() }
            appendSafe { appendGeneralSettings() }
            appendSafe { appendWorkspaceInfo() }
            if (event != null) {
                appendSafe { appendCurrentFileInfo(event) }
            }
            appendSafe { appendIndexedSources() }
            appendSafe { appendIndexedVariables() }
            appendSafe { appendIndexedMacros() }
            appendSafe { appendColumnMetadataModels() }
            appendSafe { appendExtensionContributions() }
            appendFooter()
        }
    }

    /**
     * Wraps a section in a try-catch so a failure in one section
     * does not prevent the rest of the debug output from being collected.
     */
    private inline fun StringBuilder.appendSafe(block: StringBuilder.() -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            appendLine("ERROR collecting section: ${e.javaClass.simpleName}: ${e.message}")
            appendLine()
        }
    }

    private fun StringBuilder.appendHeader() {
        appendLine(SEPARATOR)
        appendLine("                         Datamancer Debug Information")
        appendLine(SEPARATOR)
    }

    private fun StringBuilder.appendEnvironmentInfo() {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val pluginVersion = getPluginVersion()
        val appInfo = ApplicationInfo.getInstance()
        val ideVersion = "${appInfo.fullApplicationName} (Build ${appInfo.build.asString()})"
        val platform = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"

        appendLine("Generated: $timestamp")
        appendLine("Plugin Version: $pluginVersion")
        appendLine("IDE: $ideVersion")
        appendLine("Platform: $platform")
        appendLine("Project: ${project.name}")
        appendLine("Project Base Path: ${project.basePath ?: "(unknown)"}")
        appendLine()
    }

    private fun StringBuilder.appendGeneralSettings() {
        appendLine(SEPARATOR)
        appendLine("                              GENERAL SETTINGS")
        appendLine(SEPARATOR)

        val configStore = DatamancerProjectConfigStore.getInstance(project)
        appendLine("Show dbt Project Icon: ${configStore.showDbtProjectIcon}")
        appendLine("Show Run Gutter Icon: ${configStore.showRunGutterIcon}")
        appendLine()
    }

    private fun StringBuilder.appendWorkspaceInfo() {
        appendLine(SEPARATOR)
        appendLine("                              WORKSPACE & PROJECTS")
        appendLine(SEPARATOR)

        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val configs = indexService.getAllDbtConfigsSync()

        if (configs.isEmpty()) {
            appendLine("No dbt projects discovered.")
            appendLine()
            return
        }

        appendLine("Discovered dbt projects: ${configs.size}")
        appendLine()

        var projectNum = 1
        for ((moduleName, config) in configs) {
            appendLine("Project $projectNum:")
            appendLine("  Module: $moduleName")
            appendLine("  Root: ${config.projectRoot}")
            appendLine("  Profile Directory: ${config.profileDirectory ?: "(not set)"}")
            appendLine("  Default Target: ${config.defaultTarget ?: "(not set)"}")
            appendLine("  Default Data Source: ${config.defaultDataSource ?: "(not set)"}")
            appendLine("  Query Limit: ${config.queryLimit}")
            appendLine("  Preferred Backend: ${config.preferredBackend ?: "(not set)"}")
            appendLine("  Use Mise Environment: ${config.useMiseEnvironment}")
            if (config.environmentVariables.isNotEmpty()) {
                appendLine("  Environment Variables:")
                for ((key, value) in config.environmentVariables) {
                    appendLine("    $key: $value")
                }
            }
            appendLine()
            projectNum++
        }
    }

    private fun StringBuilder.appendCurrentFileInfo(event: AnActionEvent) {
        appendLine(SEPARATOR)
        appendLine("                              CURRENT FILE INFO")
        appendLine(SEPARATOR)

        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        val editor = event.getData(CommonDataKeys.EDITOR)

        if (virtualFile == null) {
            appendLine("No file currently open.")
            appendLine()
            return
        }

        appendLine("File: ${virtualFile.path}")
        appendLine("File Name: ${virtualFile.name}")
        appendLine("File Type: ${virtualFile.fileType.name}")
        appendLine("File Size: ${virtualFile.length} bytes")
        appendLine("Is Writable: ${virtualFile.isWritable}")

        // Check if file is in a dbt project
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val configs = indexService.getAllDbtConfigsSync()
        val containingProject = configs.entries.find { (_, config) ->
            virtualFile.path.startsWith(config.projectRoot)
        }

        if (containingProject != null) {
            appendLine("In dbt project: Yes (${containingProject.key})")
        } else {
            appendLine("In dbt project: No")
        }

        if (psiFile != null) {
            appendLine()
            appendLine("PSI Information:")
            appendLine("  Language: ${psiFile.language.displayName}")
            appendLine("  File Class: ${psiFile.javaClass.simpleName}")

            // Get all languages in the file (for multi-language files)
            val languages = psiFile.viewProvider.languages
            if (languages.size > 1) {
                appendLine("  All Languages: ${languages.map { it.displayName }}")
            }
        }

        if (editor != null) {
            appendLine()
            appendLine("Editor Information:")
            val caretModel = editor.caretModel
            val logicalPosition = caretModel.logicalPosition
            appendLine("  Caret Position: line ${logicalPosition.line + 1}, column ${logicalPosition.column + 1}")
            appendLine("  Offset: ${caretModel.offset}")

            val selectionModel = editor.selectionModel
            if (selectionModel.hasSelection()) {
                appendLine("  Selection: ${selectionModel.selectionStart} to ${selectionModel.selectionEnd}")
                appendLine("  Selected Text Length: ${selectionModel.selectedText?.length ?: 0}")
            }

            // Get PSI element at caret
            if (psiFile != null) {
                val elementAtCaret = psiFile.findElementAt(caretModel.offset)
                if (elementAtCaret != null) {
                    appendLine()
                    appendLine("Element at Caret:")
                    appendPsiHierarchy(elementAtCaret, indent = "  ")
                }
            }
        }

        appendLine()
    }

    private fun StringBuilder.appendPsiHierarchy(element: PsiElement, indent: String, maxDepth: Int = 5) {
        var current: PsiElement? = element
        var depth = 0
        val elements = mutableListOf<String>()

        while (current != null && depth < maxDepth) {
            val className = current.javaClass.simpleName
            val text = current.text?.take(50)?.replace("\n", "\\n") ?: ""
            elements.add("$className: \"$text\"")
            current = current.parent
            depth++
        }

        for ((index, elementDesc) in elements.withIndex()) {
            val currentIndent = indent + "  ".repeat(index)
            if (index == 0) {
                appendLine("${currentIndent}Element: $elementDesc")
            } else {
                appendLine("${currentIndent}Parent: $elementDesc")
            }
        }

        if (depth >= maxDepth) {
            appendLine("$indent${"  ".repeat(maxDepth)}... (truncated)")
        }
    }

    private fun StringBuilder.appendIndexedSources() {
        appendLine(SEPARATOR)
        appendLine("                              INDEXED SOURCES")
        appendLine(SEPARATOR)

        val sourceService = DbtSourceIndexService.getInstance(project)
        val sources = sourceService.getAllSources()

        if (sources.isEmpty()) {
            appendLine("No sources indexed.")
            appendLine()
            return
        }

        appendLine("Total sources: ${sources.size}")
        appendLine()

        for ((sourceName, sourceDefinition) in sources) {
            appendLine("Source: $sourceName")
            appendLine("  File: ${sourceDefinition.file.name}")
            appendLine("  Tables (${sourceDefinition.tables.size}):")
            for (table in sourceDefinition.tables) {
                appendLine("    - ${table.name}")
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendIndexedVariables() {
        appendLine(SEPARATOR)
        appendLine("                              INDEXED VARIABLES")
        appendLine(SEPARATOR)

        val varService = DbtVarIndexService.getInstance(project)
        val vars = varService.getAllVars()

        if (vars.isEmpty()) {
            appendLine("No variables indexed.")
            appendLine()
            return
        }

        appendLine("Total variables: ${vars.size}")
        appendLine()

        // Group by scope
        val globalVars = vars.filter { (_, varDef) -> varDef.scope == null }
        val scopedVars = vars.entries.filter { it.value.scope != null }.groupBy { it.value.scope }

        if (globalVars.isNotEmpty()) {
            appendLine("Global Variables:")
            for ((varName, varDef) in globalVars) {
                val valueDisplay = varDef.value?.take(50) ?: "(no value)"
                appendLine("  $varName = $valueDisplay")
            }
            appendLine()
        }

        for ((scope, varsInScope) in scopedVars) {
            appendLine("Scoped Variables ($scope):")
            for (entry in varsInScope) {
                val valueDisplay = entry.value.value?.take(50) ?: "(no value)"
                appendLine("  ${entry.key} = $valueDisplay")
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendIndexedMacros() {
        appendLine(SEPARATOR)
        appendLine("                              INDEXED MACROS")
        appendLine(SEPARATOR)

        val macroService = DbtMacroIndexService.getInstance(project)
        val macros = macroService.getAllMacros()

        if (macros.isEmpty()) {
            appendLine("No macros indexed.")
            appendLine()
            return
        }

        appendLine("Total macros: ${macros.size}")
        appendLine()

        for ((macroName, macroDef) in macros) {
            val params = if (macroDef.parameters.isNotEmpty()) macroDef.parameters.joinToString(", ") else "(none)"
            appendLine("Macro: $macroName($params)")
            appendLine("  File: ${macroDef.file.name}")
            appendLine()
        }
    }

    private fun StringBuilder.appendColumnMetadataModels() {
        appendLine(SEPARATOR)
        appendLine("                           COLUMN METADATA MODELS")
        appendLine(SEPARATOR)

        val metadataService = DbtColumnMetadataService.getInstance(project)
        val models = metadataService.getAllModels()

        if (models.isEmpty()) {
            appendLine("No column metadata models indexed.")
            appendLine()
            return
        }

        appendLine("Total models with metadata: ${models.size}")
        appendLine()

        for ((modelName, modelMeta) in models) {
            appendLine("Model: $modelName")
            appendLine("  File: ${modelMeta.file.name}")
            if (modelMeta.description != null) {
                appendLine("  Description: ${modelMeta.description.take(100)}")
            }
            appendLine("  Columns (${modelMeta.columns.size}):")
            for ((colName, colMeta) in modelMeta.columns) {
                val parts = mutableListOf(colName)
                if (colMeta.dataType != null) parts.add("type=${colMeta.dataType}")
                if (colMeta.description != null) parts.add("\"${colMeta.description.take(60)}\"")
                appendLine("    - ${parts.joinToString(", ")}")
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendExtensionContributions() {
        val contributors = DatamancerDebugInfoContributor.EP_NAME.extensionList
        for (contributor in contributors) {
            appendLine(SEPARATOR)
            val title = contributor.getSectionTitle()
            val padding = (SEPARATOR.length - title.length) / 2
            appendLine(" ".repeat(padding.coerceAtLeast(0)) + title)
            appendLine(SEPARATOR)
            contributor.appendDebugInfo(project, this)
        }
    }

    private fun StringBuilder.appendFooter() {
        appendLine(SEPARATOR)
        appendLine("                                END OF DEBUG INFO")
        appendLine(SEPARATOR)
    }

    private fun getPluginVersion(): String {
        val pluginId = PluginId.getId(PLUGIN_ID)
        val plugin = PluginManagerCore.getPlugin(pluginId)
        return plugin?.version ?: "unknown"
    }
}
