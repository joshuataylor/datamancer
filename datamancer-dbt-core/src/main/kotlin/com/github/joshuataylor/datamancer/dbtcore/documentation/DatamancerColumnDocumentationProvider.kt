package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.services.DbtColumnMetadataService
import com.github.joshuataylor.datamancer.dbtcore.manifest.DatamancerManifestService
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.sql.psi.SqlLanguage

/**
 * Documentation provider for dbt column descriptions from YAML schema files.
 *
 * Implements both [DocumentationTargetProvider] (for editor hover) and
 * [PsiDocumentationTargetProvider] (for autocomplete popup documentation).
 *
 * When hovering over a column identifier in a dbt SQL model file, this provider
 * looks up the column description from the corresponding YAML schema file and
 * displays it as quick documentation.
 */
class DatamancerColumnDocumentationProvider : DocumentationTargetProvider, PsiDocumentationTargetProvider {

    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val target = createTargetFromOffset(file, offset)
            ?: createTargetFromOffset(file, offset - 1)
            ?: return emptyList()
        return listOf(target)
    }

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        val sourceElement = originalElement ?: element
        val file = sourceElement.containingFile ?: return null
        return createTargetFromElement(file, sourceElement)
    }

    private fun createTargetFromOffset(file: PsiFile, offset: Int): DatamancerColumnDocumentationTarget? {
        if (offset < 0) return null

        val viewProvider = file.viewProvider
        if (viewProvider !is TemplateLanguageFileViewProvider) return null

        // Find the SQL language layer
        val sqlLanguage = viewProvider.languages.firstOrNull { it.isKindOf(SqlLanguage.INSTANCE) }
            ?: return null

        val sqlPsi = viewProvider.getPsi(sqlLanguage) ?: return null
        val element = sqlPsi.findElementAt(offset) ?: return null

        return createTargetFromElement(file, element)
    }

    private fun createTargetFromElement(file: PsiFile, element: PsiElement): DatamancerColumnDocumentationTarget? {
        val project = file.project

        // Check file is in a dbt project
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        if (indexService.getAllDbtConfigsSync().isEmpty()) return null

        // Extract identifier text from the leaf element
        val identifierText = element.text?.takeIf { it.isNotBlank() } ?: return null

        // Skip non-identifier tokens (keywords, operators, punctuation, whitespace)
        if (!isLikelyColumnIdentifier(identifierText)) return null

        // Derive model name from filename
        val virtualFile = file.virtualFile ?: return null
        val modelName = virtualFile.nameWithoutExtension

        // Look up column metadata from YAML schema files
        val metadataService = DbtColumnMetadataService.getInstance(project)
        val columnMetadata = metadataService.findColumnMetadata(modelName, identifierText)

        // Look up manifest column metadata to enrich with data types and compiled SQL
        val manifestService = DatamancerManifestService.getInstance(project)
        val manifestColumn = manifestService.findColumnMetadata(modelName, identifierText)

        // At least one source must have column info for this to be meaningful
        if (columnMetadata == null && manifestColumn == null) return null

        // Get model metadata for model description
        val modelMetadata = metadataService.findModel(modelName)

        // Prefer YAML description (hand-authored), fall back to manifest description
        val columnDescription = columnMetadata?.description?.takeIf { it.isNotBlank() }
            ?: manifestColumn?.description

        // Prefer manifest data type (dbt-inferred) over YAML data type
        val columnDataType = manifestColumn?.dataType?.takeIf { it.isNotBlank() }
            ?: columnMetadata?.dataType

        // Compiled SQL from manifest for the model (shown in documentation when available)
        val compiledSql = manifestService.findCompiledSql(modelName)

        // Compute a relative schema file path for display
        val schemaFilePath = columnMetadata?.file?.let { schemaFile ->
            val projectBasePath = project.basePath
            if (projectBasePath != null && schemaFile.path.startsWith(projectBasePath)) {
                schemaFile.path.removePrefix(projectBasePath).removePrefix("/")
            } else {
                schemaFile.name
            }
        } ?: "manifest.json"

        return DatamancerColumnDocumentationTarget(
            element = element,
            project = project,
            columnName = identifierText,
            columnDescription = columnDescription,
            columnDataType = columnDataType,
            modelName = modelName,
            modelDescription = modelMetadata?.description,
            schemaFilePath = schemaFilePath,
            yamlElement = columnMetadata?.psiElement,
            compiledSql = compiledSql
        )
    }

    /**
     * Checks if the given text looks like it could be a column identifier.
     *
     * Filters out SQL keywords, operators, punctuation, and whitespace-only tokens
     * that would never be column names.
     */
    private fun isLikelyColumnIdentifier(text: String): Boolean {
        if (text.length > 128) return false
        // Must start with a letter or underscore (standard SQL identifier rules)
        val first = text[0]
        if (!first.isLetter() && first != '_') return false
        // Must contain only identifier characters
        return text.all { it.isLetterOrDigit() || it == '_' }
    }
}
