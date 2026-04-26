package com.github.joshuataylor.datamancer.core.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * Service for indexing and finding dbt variable definitions from dbt_project.yml files.
 *
 * Variables are defined in dbt_project.yml under the `vars` key:
 * ```yaml
 * vars:
 *   # Global variables
 *   start_date: '2020-01-01'
 *   end_date: '2024-12-31'
 *
 *   # Project-scoped variables
 *   my_project:
 *     some_var: 'value'
 * ```
 *
 * Variables can be accessed in models using `{{ var('variable_name') }}`.
 */
@Service(Service.Level.PROJECT)
class DbtVarIndexService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): DbtVarIndexService = project.service()

        private const val DBT_PROJECT_FILE = "dbt_project.yml"
        private const val VARS_KEY = "vars"
    }

    /**
     * Represents a dbt variable definition.
     */
    data class VarDefinition(
        val name: String,
        val value: String?,
        val psiElement: PsiElement,
        val file: VirtualFile,
        val scope: String? = null  // null for global, project name for scoped
    )

    /**
     * Finds all variable definitions in the project.
     *
     * @return Map of variable name to VarDefinition
     */
    fun getAllVars(): Map<String, VarDefinition> {
        val vars = mutableMapOf<String, VarDefinition>()
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return vars
        }

        val psiManager = PsiManager.getInstance(project)
        val vfsManager = VirtualFileManager.getInstance()

        for ((_, config) in allConfigs) {
            val dbtProjectPath = "${config.projectRoot}/$DBT_PROJECT_FILE"
            val virtualFile = vfsManager.findFileByUrl("file://$dbtProjectPath") ?: continue
            val psiFile = psiManager.findFile(virtualFile) as? YAMLFile ?: continue

            val fileVars = parseVarsFromYaml(psiFile, virtualFile)
            for (varDef in fileVars) {
                vars[varDef.name] = varDef
            }
        }

        return vars
    }

    /**
     * Finds a specific variable by name.
     *
     * @param varName The name of the variable to find
     * @return The VarDefinition, or null if not found
     */
    fun findVar(varName: String): VarDefinition? {
        return getAllVars()[varName]
    }

    /**
     * Gets all available variable names for code completion.
     */
    fun getAllVarNames(): List<String> {
        return getAllVars().keys.toList()
    }

    private fun parseVarsFromYaml(yamlFile: YAMLFile, virtualFile: VirtualFile): List<VarDefinition> {
        val vars = mutableListOf<VarDefinition>()

        val documents = yamlFile.documents
        if (documents.isEmpty()) {
            return vars
        }

        val rootMapping = documents[0].topLevelValue as? YAMLMapping ?: return vars

        // Look for 'vars' key
        val varsKeyValue = rootMapping.keyValues.find { it.keyText == VARS_KEY } ?: return vars
        val varsMapping = varsKeyValue.value as? YAMLMapping ?: return vars

        for (keyValue in varsMapping.keyValues) {
            val keyText = keyValue.keyText

            // Check if this is a nested mapping (project-scoped vars) or a direct value
            val valueMapping = keyValue.value as? YAMLMapping
            if (valueMapping != null) {
                // Project-scoped variables
                for (scopedKeyValue in valueMapping.keyValues) {
                    val scopedName = scopedKeyValue.keyText
                    val varDef = VarDefinition(
                        name = scopedName,
                        value = scopedKeyValue.valueText,
                        psiElement = scopedKeyValue,
                        file = virtualFile,
                        scope = keyText
                    )
                    vars.add(varDef)
                }
            } else {
                // Global variable
                val varDef = VarDefinition(
                    name = keyText,
                    value = keyValue.valueText,
                    psiElement = keyValue,
                    file = virtualFile,
                    scope = null
                )
                vars.add(varDef)
            }
        }

        return vars
    }
}
