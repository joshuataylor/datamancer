package com.github.joshuataylor.datamancer.core.services

import com.github.joshuataylor.datamancer.core.workspace.DatamancerExcludedDirectories
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence

/**
 * Service for indexing and finding dbt source definitions from schema.yml files.
 *
 * Sources are defined in schema.yml files under the `sources` key:
 * ```yaml
 * sources:
 *   - name: raw_data
 *     tables:
 *       - name: customers
 *       - name: orders
 * ```
 */
@Service(Service.Level.PROJECT)
class DbtSourceIndexService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): DbtSourceIndexService = project.service()
    }

    /**
     * Represents a dbt source definition.
     */
    data class SourceDefinition(
        val name: String,
        val tables: List<TableDefinition>,
        val psiElement: PsiElement,
        val file: VirtualFile
    )

    /**
     * Represents a table within a source.
     */
    data class TableDefinition(
        val name: String,
        val psiElement: PsiElement
    )

    /**
     * Finds all source definitions in the project.
     *
     * @return Map of source name to SourceDefinition
     */
    @RequiresReadLock
    fun getAllSources(): Map<String, SourceDefinition> {
        val sources = mutableMapOf<String, SourceDefinition>()
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return sources
        }

        // Find all YAML files in the project
        val yamlFiles = FileTypeIndex.getFiles(
            YAMLFileType.YML,
            GlobalSearchScope.projectScope(project)
        )

        val psiManager = PsiManager.getInstance(project)

        for (virtualFile in yamlFiles) {
            ProgressManager.checkCanceled()
            // Only process files in dbt project directories
            if (!isInDbtProject(virtualFile, allConfigs)) {
                continue
            }

            val psiFile = psiManager.findFile(virtualFile) as? YAMLFile ?: continue
            val fileSources = parseSourcesFromYaml(psiFile, virtualFile)
            for (source in fileSources) {
                ProgressManager.checkCanceled()
                sources[source.name] = source
            }
        }

        return sources
    }

    /**
     * Finds a specific source by name.
     *
     * @param sourceName The name of the source to find
     * @return The SourceDefinition, or null if not found
     */
    @RequiresReadLock
    fun findSource(sourceName: String): SourceDefinition? {
        return getAllSources()[sourceName]
    }

    /**
     * Finds a specific table within a source.
     *
     * @param sourceName The name of the source
     * @param tableName The name of the table
     * @return The TableDefinition, or null if not found
     */
    @RequiresReadLock
    fun findSourceTable(sourceName: String, tableName: String): TableDefinition? {
        val source = findSource(sourceName) ?: return null
        return source.tables.find { it.name == tableName }
    }

    /**
     * Gets all available source names for code completion.
     */
    @RequiresReadLock
    fun getAllSourceNames(): List<String> {
        return getAllSources().keys.toList()
    }

    /**
     * Gets all table names for a given source.
     */
    @RequiresReadLock
    fun getTableNames(sourceName: String): List<String> {
        val source = findSource(sourceName) ?: return emptyList()
        return source.tables.map { it.name }
    }

    private fun isInDbtProject(file: VirtualFile, configs: Map<String, DatamancerProjectConfig>): Boolean {
        val filePath = file.path
        return configs.values.any { config ->
            filePath.startsWith(config.projectRoot)
                && !DatamancerExcludedDirectories.isInExcludedDirectory(filePath, config)
        }
    }

    private fun parseSourcesFromYaml(yamlFile: YAMLFile, virtualFile: VirtualFile): List<SourceDefinition> {
        val sources = mutableListOf<SourceDefinition>()

        val documents = yamlFile.documents
        if (documents.isEmpty()) {
            return sources
        }

        val rootMapping = documents[0].topLevelValue as? YAMLMapping ?: return sources

        // Look for 'sources' key
        val sourcesKeyValue = rootMapping.keyValues.find { it.keyText == "sources" } ?: return sources
        val sourcesSequence = sourcesKeyValue.value as? YAMLSequence ?: return sources

        for (sourceItem in sourcesSequence.items) {
            val sourceMapping = sourceItem.value as? YAMLMapping ?: continue
            val sourceDefinition = parseSourceDefinition(sourceMapping, virtualFile)
            if (sourceDefinition != null) {
                sources.add(sourceDefinition)
            }
        }

        return sources
    }

    private fun parseSourceDefinition(mapping: YAMLMapping, virtualFile: VirtualFile): SourceDefinition? {
        val nameKeyValue = mapping.keyValues.find { it.keyText == "name" } ?: return null
        val sourceName = nameKeyValue.valueText

        val tables = mutableListOf<TableDefinition>()

        // Parse tables
        val tablesKeyValue = mapping.keyValues.find { it.keyText == "tables" }
        if (tablesKeyValue != null) {
            val tablesSequence = tablesKeyValue.value as? YAMLSequence
            if (tablesSequence != null) {
                for (tableItem in tablesSequence.items) {
                    val tableMapping = tableItem.value as? YAMLMapping ?: continue
                    val tableDefinition = parseTableDefinition(tableMapping)
                    if (tableDefinition != null) {
                        tables.add(tableDefinition)
                    }
                }
            }
        }

        return SourceDefinition(
            name = sourceName,
            tables = tables,
            psiElement = nameKeyValue,
            file = virtualFile
        )
    }

    private fun parseTableDefinition(mapping: YAMLMapping): TableDefinition? {
        val nameKeyValue = mapping.keyValues.find { it.keyText == "name" } ?: return null
        val tableName = nameKeyValue.valueText

        return TableDefinition(
            name = tableName,
            psiElement = nameKeyValue
        )
    }
}
