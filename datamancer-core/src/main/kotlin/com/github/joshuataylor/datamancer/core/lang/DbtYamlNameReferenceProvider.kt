package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

/**
 * Reference provider for dbt model and source names in YAML schema files.
 *
 * Detects the structural context of a YAML `name:` value to determine whether
 * it is a model name, source name, or source table name, and creates the
 * appropriate reference for go-to-definition navigation.
 */
class DbtYamlNameReferenceProvider : PsiReferenceProvider() {

    /**
     * Context types for a `name:` value in a dbt YAML schema file.
     */
    private enum class NameContext {
        MODEL,
        COLUMN,
        SOURCE,
        SOURCE_TABLE
    }

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val scalar = element as? YAMLScalar ?: return PsiReference.EMPTY_ARRAY

        // Must be in a dbt project
        val project = element.project
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        if (indexService.getAllDbtConfigsSync().isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }

        // Must be the value of a `name:` key
        val keyValue = scalar.parent as? YAMLKeyValue ?: return PsiReference.EMPTY_ARRAY
        if (keyValue.keyText != "name") return PsiReference.EMPTY_ARRAY

        val nameValue = keyValue.valueText
        if (nameValue.isBlank()) return PsiReference.EMPTY_ARRAY

        val nameContext = getNameContext(keyValue) ?: return PsiReference.EMPTY_ARRAY

        val textRange = ElementManipulators.getValueTextRange(scalar)

        return when (nameContext) {
            NameContext.MODEL -> arrayOf(
                DbtYamlModelNameReference(scalar, textRange, nameValue)
            )

            NameContext.COLUMN -> {
                val modelName = getModelNameForColumn(keyValue) ?: return PsiReference.EMPTY_ARRAY
                arrayOf(
                    DbtYamlColumnReference(scalar, textRange, modelName, nameValue)
                )
            }

            NameContext.SOURCE -> arrayOf(
                DbtYamlSourceUsageReference(scalar, textRange, nameValue, null)
            )

            NameContext.SOURCE_TABLE -> {
                val sourceName = getSourceNameForTable(keyValue) ?: return PsiReference.EMPTY_ARRAY
                arrayOf(
                    DbtYamlSourceUsageReference(scalar, textRange, sourceName, nameValue)
                )
            }
        }
    }

    /**
     * Determines the structural context of a `name:` key-value pair by
     * walking up the YAML PSI tree.
     *
     * Expected structures:
     * - Model:       name -> YAMLMapping -> YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue(models)
     * - Source:      name -> YAMLMapping -> YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue(sources)
     * - Table:       name -> YAMLMapping -> YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue(tables)
     *                  -> YAMLMapping -> YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue(sources)
     */
    private fun getNameContext(nameKeyValue: YAMLKeyValue): NameContext? {
        val parentSequenceKey = getParentSequenceKeyText(nameKeyValue) ?: return null

        return when (parentSequenceKey) {
            "models" -> NameContext.MODEL
            "sources" -> NameContext.SOURCE
            "columns" -> {
                // Verify that `columns:` is inside a `models:` block
                val columnsKeyValue = getContainingSequenceKeyValue(nameKeyValue) ?: return null
                val grandparentKey = getParentSequenceKeyText(columnsKeyValue) ?: return null
                if (grandparentKey == "models") NameContext.COLUMN else null
            }
            "tables" -> {
                // Verify that `tables:` is inside a `sources:` block
                val tablesKeyValue = getContainingSequenceKeyValue(nameKeyValue) ?: return null
                val grandparentKey = getParentSequenceKeyText(tablesKeyValue) ?: return null
                if (grandparentKey == "sources") NameContext.SOURCE_TABLE else null
            }
            else -> null
        }
    }

    /**
     * Gets the key text of the YAMLKeyValue that owns the sequence containing
     * the given key-value's parent mapping.
     *
     * Traversal: keyValue -> YAMLMapping (parent) -> YAMLSequenceItem -> YAMLSequence -> YAMLKeyValue
     */
    private fun getParentSequenceKeyText(keyValue: YAMLKeyValue): String? {
        val kv = getContainingSequenceKeyValue(keyValue) ?: return null
        return kv.keyText
    }

    /**
     * Returns the YAMLKeyValue that owns the sequence containing the given
     * key-value's parent mapping.
     */
    private fun getContainingSequenceKeyValue(keyValue: YAMLKeyValue): YAMLKeyValue? {
        val mapping = keyValue.parent as? YAMLMapping ?: return null
        val sequenceItem = mapping.parent as? YAMLSequenceItem ?: return null
        val sequence = sequenceItem.parent as? YAMLSequence ?: return null
        return sequence.parent as? YAMLKeyValue
    }

    /**
     * For a table `name:` key-value, walks up to the source-level mapping and
     * extracts the source name.
     *
     * Traversal: nameKV -> tableMapping -> seqItem -> tablesSeq -> tablesKV
     *            -> sourceMapping -> find name KV -> valueText
     */
    private fun getSourceNameForTable(tableNameKeyValue: YAMLKeyValue): String? {
        val tableMapping = tableNameKeyValue.parent as? YAMLMapping ?: return null
        val tableSeqItem = tableMapping.parent as? YAMLSequenceItem ?: return null
        val tablesSequence = tableSeqItem.parent as? YAMLSequence ?: return null
        val tablesKeyValue = tablesSequence.parent as? YAMLKeyValue ?: return null
        val sourceMapping = tablesKeyValue.parent as? YAMLMapping ?: return null
        val sourceNameKv = sourceMapping.keyValues.find { it.keyText == "name" } ?: return null
        return sourceNameKv.valueText
    }

    /**
     * For a column `name:` key-value, walks up to the model-level mapping and
     * extracts the model name.
     *
     * Traversal: nameKV -> columnMapping -> seqItem -> columnsSeq -> columnsKV
     *            -> modelMapping -> find name KV -> valueText
     */
    private fun getModelNameForColumn(columnNameKeyValue: YAMLKeyValue): String? {
        val columnMapping = columnNameKeyValue.parent as? YAMLMapping ?: return null
        val columnSeqItem = columnMapping.parent as? YAMLSequenceItem ?: return null
        val columnsSequence = columnSeqItem.parent as? YAMLSequence ?: return null
        val columnsKeyValue = columnsSequence.parent as? YAMLKeyValue ?: return null
        val modelMapping = columnsKeyValue.parent as? YAMLMapping ?: return null
        val modelNameKv = modelMapping.keyValues.find { it.keyText == "name" } ?: return null
        return modelNameKv.valueText
    }
}
