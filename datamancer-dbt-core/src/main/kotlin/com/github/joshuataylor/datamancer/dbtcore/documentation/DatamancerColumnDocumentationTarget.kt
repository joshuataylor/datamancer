package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.github.joshuataylor.datamancer.core.documentation.DatamancerDocumentationHtmlUtils
import com.github.joshuataylor.datamancer.core.services.DbtColumnMetadataService
import com.github.joshuataylor.datamancer.dbtcore.manifest.DatamancerManifestService
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer

/**
 * Documentation target for dbt column descriptions from YAML schema files.
 *
 * Renders hover documentation showing a PyCharm-style popup with a
 * syntax-highlighted column signature, prominent description text,
 * and grayed model/file context.
 */
class DatamancerColumnDocumentationTarget(
    private val element: PsiElement,
    private val project: Project,
    private val columnName: String,
    private val columnDescription: String?,
    private val columnDataType: String?,
    private val modelName: String,
    private val modelDescription: String?,
    private val schemaFilePath: String,
    private val yamlElement: PsiElement? = null,
    private val compiledSql: String? = null
) : DocumentationTarget {

    override val navigatable: Navigatable?
        get() = yamlElement as? Navigatable

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val yamlElementPtr = yamlElement?.createSmartPointer()
        val colName = columnName
        val colDesc = columnDescription
        val colDataType = columnDataType
        val modName = modelName
        val modDesc = modelDescription
        val schemaPath = schemaFilePath
        val sql = compiledSql

        return Pointer {
            val restoredElement = elementPtr.dereference() ?: return@Pointer null
            val restoredProject = restoredElement.project
            // Re-query metadata in case YAML or manifest has changed
            val metadataService = DbtColumnMetadataService.getInstance(restoredProject)
            val column = metadataService.findColumnMetadata(modName, colName)
            val manifestService = DatamancerManifestService.getInstance(restoredProject)
            val manifestColumn = manifestService.findColumnMetadata(modName, colName)
            DatamancerColumnDocumentationTarget(
                element = restoredElement,
                project = restoredProject,
                columnName = colName,
                columnDescription = column?.description?.takeIf { it.isNotBlank() }
                    ?: manifestColumn?.description ?: colDesc,
                columnDataType = manifestColumn?.dataType?.takeIf { it.isNotBlank() }
                    ?: column?.dataType ?: colDataType,
                modelName = modName,
                modelDescription = modDesc,
                schemaFilePath = schemaPath,
                yamlElement = column?.psiElement ?: yamlElementPtr?.dereference(),
                compiledSql = manifestService.findCompiledSql(modName) ?: sql
            )
        }
    }

    override fun computePresentation(): TargetPresentation {
        val text = if (columnDataType != null) {
            "$columnName: $columnDataType"
        } else {
            columnName
        }
        return TargetPresentation.builder(text).presentation()
    }

    override fun computeDocumentation(): DocumentationResult {
        val builder = HtmlBuilder()

        // Definition section: syntax-highlighted column signature
        val signatureBuilder = HtmlBuilder()
        signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan(columnName, DefaultLanguageHighlighterColors.IDENTIFIER))
        if (columnDataType != null) {
            signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan(": ", DefaultLanguageHighlighterColors.COMMA))
            signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan(columnDataType, DefaultLanguageHighlighterColors.CLASS_NAME))
        }

        builder.append(
            signatureBuilder.toFragment()
                .wrapWith(DocumentationMarkup.PRE_ELEMENT)
                .wrapWith(DocumentationMarkup.DEFINITION_ELEMENT)
        )

        // Content section: description as prominent text, then grayed metadata
        val contentBuilder = HtmlBuilder()

        if (!columnDescription.isNullOrBlank()) {
            contentBuilder.append(HtmlChunk.text(columnDescription))
            contentBuilder.append(HtmlChunk.br())
            contentBuilder.append(HtmlChunk.br())
        }

        contentBuilder.append(
            HtmlChunk.text("Model: $modelName").wrapWith(DocumentationMarkup.GRAYED_ELEMENT)
        )
        contentBuilder.append(HtmlChunk.br())
        contentBuilder.append(
            HtmlChunk.text("Defined in: $schemaFilePath").wrapWith(DocumentationMarkup.GRAYED_ELEMENT)
        )

        builder.append(contentBuilder.toFragment().wrapWith(DocumentationMarkup.CONTENT_ELEMENT))

        // Sections section: compiled SQL from manifest when available
        if (!compiledSql.isNullOrBlank()) {
            val sectionsBuilder = HtmlBuilder()
            sectionsBuilder.append(HtmlChunk.text("Compiled SQL").wrapWith("p"))
            // Truncate very long SQL to avoid flooding the popup
            val displaySql = if (compiledSql.length > 2000) {
                compiledSql.take(2000) + "\n-- (truncated)"
            } else {
                compiledSql
            }
            sectionsBuilder.append(HtmlChunk.text(displaySql).wrapWith("pre"))
            builder.append(sectionsBuilder.toFragment().wrapWith(DocumentationMarkup.SECTIONS_TABLE))
        }

        return DocumentationResult.documentation(builder.wrapWithHtmlBody().toString())
    }
}
