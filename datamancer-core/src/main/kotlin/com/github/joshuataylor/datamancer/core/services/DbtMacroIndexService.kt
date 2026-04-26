package com.github.joshuataylor.datamancer.core.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

/**
 * Service for indexing and finding dbt macro definitions from .sql files in macros/ directories.
 *
 * Macros are defined in .sql files using Jinja2 block tag syntax:
 * ```
 * {percent} macro my_macro(arg1, arg2) {percent}
 *   SELECT {{ arg1 }}, {{ arg2 }};
 * {percent} endmacro {percent}
 * ```
 *
 * Macro calls in model files like `{{ my_macro('a', 'b') }}` can then be resolved
 * back to their definitions for go-to-definition, code completion, and find-usages.
 */
@Service(Service.Level.PROJECT)
class DbtMacroIndexService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): DbtMacroIndexService = project.service()

        private const val DEFAULT_MACRO_DIR = "macros"

        /**
         * Regex to match macro definition tags in .sql files.
         *
         * Matches patterns like:
         * - `{percent} macro name() {percent}`
         * - `{percent}- macro name(arg1, arg2) -{percent}`
         * - `{percent} macro name(arg1, arg2='default') {percent}`
         *
         * Group 1 captures the macro name.
         */
        val MACRO_DEFINITION_REGEX = Regex(
            """\{%-?\s*macro\s+(\w+)\s*\(""",
            RegexOption.MULTILINE
        )

        /**
         * Regex to extract the parameter list from a macro definition.
         * Applied to the text starting from the opening parenthesis.
         */
        private val PARAMS_REGEX = Regex("""\(([^)]*)\)""")
    }

    /**
     * Represents a dbt macro definition found in a .sql file.
     *
     * @param name The macro name
     * @param file The .sql file containing the definition
     * @param textOffset Character offset of the macro name within the file text
     * @param nameLength Length of the macro name string
     * @param parameters Parameter names extracted from the definition (for display)
     */
    data class MacroDefinition(
        val name: String,
        val file: VirtualFile,
        val textOffset: Int,
        val nameLength: Int,
        val parameters: List<String> = emptyList()
    )

    /**
     * Finds all macro definitions across all dbt projects in the workspace.
     *
     * Scans macros/ directories for .sql files and parses them for
     * `{percent} macro name(...) {percent}` definitions.
     *
     * @return Map of macro name to MacroDefinition
     */
    fun getAllMacros(): Map<String, MacroDefinition> {
        val macros = mutableMapOf<String, MacroDefinition>()
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return macros
        }

        val vfm = VirtualFileManager.getInstance()

        for ((_, config) in allConfigs) {
            val macrosPath = "${config.projectRoot}/$DEFAULT_MACRO_DIR"
            val macrosDir = vfm.findFileByUrl("file://$macrosPath") ?: continue

            val sqlFiles = mutableListOf<VirtualFile>()
            collectSqlFiles(macrosDir, sqlFiles)

            for (sqlFile in sqlFiles) {
                val fileMacros = parseMacrosFromFile(sqlFile)
                for (macroDef in fileMacros) {
                    macros[macroDef.name] = macroDef
                }
            }
        }

        return macros
    }

    /**
     * Finds a specific macro definition by name.
     *
     * @param macroName The name of the macro to find
     * @return The MacroDefinition, or null if not found
     */
    fun findMacro(macroName: String): MacroDefinition? {
        return getAllMacros()[macroName]
    }

    /**
     * Gets all available macro names for code completion.
     */
    fun getAllMacroNames(): List<String> {
        return getAllMacros().keys.toList()
    }

    /**
     * Parses macro definitions from a single .sql file.
     *
     * @param file The .sql file to parse
     * @return List of MacroDefinition instances found in the file
     */
    private fun parseMacrosFromFile(file: VirtualFile): List<MacroDefinition> {
        val macros = mutableListOf<MacroDefinition>()

        val content = try {
            String(file.contentsToByteArray(), file.charset)
        } catch (_: Exception) {
            return macros
        }

        val matches = MACRO_DEFINITION_REGEX.findAll(content)

        for (match in matches) {
            val macroName = match.groupValues[1]
            val nameStart = match.range.first + match.value.indexOf(macroName)

            // Extract parameters from the text following the macro name
            val afterName = content.substring(match.range.first)
            val params = extractParameters(afterName)

            macros.add(
                MacroDefinition(
                    name = macroName,
                    file = file,
                    textOffset = nameStart,
                    nameLength = macroName.length,
                    parameters = params
                )
            )
        }

        return macros
    }

    /**
     * Extracts parameter names from a macro definition tag text.
     *
     * Given text starting from `{percent} macro name(arg1, arg2='default') {percent}`,
     * returns `["arg1", "arg2"]`.
     */
    private fun extractParameters(tagText: String): List<String> {
        val paramsMatch = PARAMS_REGEX.find(tagText) ?: return emptyList()
        val paramsText = paramsMatch.groupValues[1].trim()

        if (paramsText.isEmpty()) {
            return emptyList()
        }

        return paramsText.split(",").map { param ->
            // Take only the name part (before any '=' default value)
            param.trim().split("=")[0].trim()
        }.filter { it.isNotEmpty() }
    }

    /**
     * Recursively collects all .sql files from a directory.
     */
    private fun collectSqlFiles(directory: VirtualFile, collector: MutableList<VirtualFile>) {
        if (!directory.isDirectory) {
            return
        }

        for (child in directory.children) {
            if (child.isDirectory) {
                collectSqlFiles(child, collector)
            } else if (child.extension == "sql") {
                collector.add(child)
            }
        }
    }
}
