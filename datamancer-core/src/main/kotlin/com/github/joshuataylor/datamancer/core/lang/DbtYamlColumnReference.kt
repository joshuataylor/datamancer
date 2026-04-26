package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Reference from a column name in a YAML schema file to its occurrence in
 * the corresponding SQL model file.
 *
 * Resolves `name: customer_id` under `columns:` to the first word-boundary
 * match of `customer_id` in the model's SQL file.  Falls back to the file
 * itself when the column name does not appear in the SQL text.
 *
 * @param element    The YAMLScalar containing the column name
 * @param textRange  Text range of the column name within the element
 * @param modelName  The parent model name (used to locate the SQL file)
 * @param columnName The column name to search for in the SQL file
 */
class DbtYamlColumnReference(
    element: YAMLScalar,
    textRange: TextRange,
    private val modelName: String,
    private val columnName: String
) : PsiReferenceBase<YAMLScalar>(element, textRange, true) {

    override fun resolve(): PsiElement? {
        val project = element.project

        val modelFile = DbtDirectories.findModel(project, modelName) ?: return null

        val text = modelFile.text
        val pattern = Regex("\\b${Regex.escape(columnName)}\\b")
        val match = pattern.find(text)

        if (match != null) {
            val psiElement = modelFile.findElementAt(match.range.first)
            if (psiElement != null) return psiElement
        }

        // Fall back to the file itself so navigation still opens the SQL file
        return modelFile
    }
}
