package com.github.joshuataylor.datamancer.core.services

import com.github.joshuataylor.datamancer.core.workspace.DatamancerExcludedDirectories
import com.github.joshuataylor.datamancer.core.workspace.DatamancerProjectConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
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
 * Service for extracting column and model metadata from dbt YAML schema files.
 *
 * Scans schema YAML files for model definitions and their column descriptions:
 * ```yaml
 * models:
 *   - name: customers
 *     description: Customer table
 *     columns:
 *       - name: customer_id
 *         description: Unique customer identifier
 * ```
 */
@Service(Service.Level.PROJECT)
class DbtColumnMetadataService(private val project: Project) {

    companion object {
        fun getInstance(project: Project): DbtColumnMetadataService = project.service()
    }

    /**
     * Metadata for a single column defined in a YAML schema file.
     */
    data class ColumnMetadata(
        val name: String,
        val description: String?,
        val dataType: String?,
        val psiElement: PsiElement,
        val file: VirtualFile
    )

    /**
     * Metadata for a model defined in a YAML schema file.
     */
    data class ModelMetadata(
        val name: String,
        val description: String?,
        val columns: Map<String, ColumnMetadata>,
        val psiElement: PsiElement,
        val file: VirtualFile
    )

    /**
     * Finds model metadata by name.
     *
     * @param modelName The dbt model name (e.g. "stg_customers")
     * @return The model metadata, or null if not found
     */
    @RequiresReadLock
    fun findModel(modelName: String): ModelMetadata? {
        return getAllModels()[modelName]
    }

    /**
     * Finds column metadata for a specific column in a model.
     *
     * @param modelName The dbt model name
     * @param columnName The column name
     * @return The column metadata, or null if not found
     */
    @RequiresReadLock
    fun findColumnMetadata(modelName: String, columnName: String): ColumnMetadata? {
        val model = findModel(modelName) ?: return null
        return model.columns[columnName]
    }

    /**
     * Scans all YAML files in dbt project directories and extracts model metadata.
     *
     * @return Map of model name to ModelMetadata
     */
    @RequiresReadLock
    fun getAllModels(): Map<String, ModelMetadata> {
        val models = mutableMapOf<String, ModelMetadata>()
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        val allConfigs = indexService.getAllDbtConfigsSync()

        if (allConfigs.isEmpty()) {
            return models
        }

        val yamlFiles = FileTypeIndex.getFiles(
            YAMLFileType.YML,
            GlobalSearchScope.projectScope(project)
        )

        val psiManager = PsiManager.getInstance(project)

        for (virtualFile in yamlFiles) {
            if (!isInDbtProject(virtualFile, allConfigs)) {
                continue
            }

            val psiFile = psiManager.findFile(virtualFile) as? YAMLFile ?: continue
            val fileModels = parseModelsFromYaml(psiFile, virtualFile)
            for (model in fileModels) {
                models[model.name] = model
            }
        }

        return models
    }

    private fun isInDbtProject(file: VirtualFile, configs: Map<String, DatamancerProjectConfig>): Boolean {
        val filePath = file.path
        return configs.values.any { config ->
            filePath.startsWith(config.projectRoot)
                && !DatamancerExcludedDirectories.isInExcludedDirectory(filePath, config)
        }
    }

    @RequiresReadLock
    internal fun parseModelsFromYaml(yamlFile: YAMLFile, virtualFile: VirtualFile): List<ModelMetadata> {
        val models = mutableListOf<ModelMetadata>()

        val documents = yamlFile.documents
        if (documents.isEmpty()) {
            return models
        }

        val rootMapping = documents[0].topLevelValue as? YAMLMapping ?: return models

        val modelsKeyValue = rootMapping.keyValues.find { it.keyText == "models" } ?: return models
        val modelsSequence = modelsKeyValue.value as? YAMLSequence ?: return models

        for (modelItem in modelsSequence.items) {
            val modelMapping = modelItem.value as? YAMLMapping ?: continue
            val modelMetadata = parseModelMetadata(modelMapping, virtualFile)
            if (modelMetadata != null) {
                models.add(modelMetadata)
            }
        }

        return models
    }

    private fun parseModelMetadata(mapping: YAMLMapping, virtualFile: VirtualFile): ModelMetadata? {
        val nameKeyValue = mapping.keyValues.find { it.keyText == "name" } ?: return null
        val modelName = nameKeyValue.valueText
        if (modelName.isBlank()) return null

        val description = mapping.keyValues.find { it.keyText == "description" }?.valueText

        val columns = mutableMapOf<String, ColumnMetadata>()

        val columnsKeyValue = mapping.keyValues.find { it.keyText == "columns" }
        if (columnsKeyValue != null) {
            val columnsSequence = columnsKeyValue.value as? YAMLSequence
            if (columnsSequence != null) {
                for (columnItem in columnsSequence.items) {
                    val columnMapping = columnItem.value as? YAMLMapping ?: continue
                    val columnMetadata = parseColumnMetadata(columnMapping, virtualFile)
                    if (columnMetadata != null) {
                        columns[columnMetadata.name] = columnMetadata
                    }
                }
            }
        }

        return ModelMetadata(
            name = modelName,
            description = description,
            columns = columns,
            psiElement = nameKeyValue,
            file = virtualFile
        )
    }

    private fun parseColumnMetadata(mapping: YAMLMapping, virtualFile: VirtualFile): ColumnMetadata? {
        val nameKeyValue = mapping.keyValues.find { it.keyText == "name" } ?: return null
        val columnName = nameKeyValue.valueText
        if (columnName.isBlank()) return null

        val description = mapping.keyValues.find { it.keyText == "description" }?.valueText
        val dataType = mapping.keyValues.find { it.keyText == "data_type" }?.valueText

        return ColumnMetadata(
            name = columnName,
            description = description,
            dataType = dataType,
            psiElement = nameKeyValue,
            file = virtualFile
        )
    }
}
