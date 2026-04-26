package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.DatamancerUtils
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

/**
 * Reference provider for dbt `source()` function calls.
 *
 * This provider creates references for Jinja2 function calls that are `source()` calls,
 * enabling go-to-definition and code completion for dbt source references.
 *
 * source() calls have two arguments: `source('source_name', 'table_name')`
 * - First argument: references a source defined in schema.yml
 * - Second argument: references a table within that source
 */
class DbtSourceRefReferenceProvider : PsiReferenceProvider() {

    companion object {
        private const val SOURCE_FUNCTION_NAME = "source"

        /**
         * Pattern condition that checks if a Jinja2FunctionCall is a source() call.
         */
        val isSourceFunction: PatternCondition<Jinja2FunctionCall> =
            object : PatternCondition<Jinja2FunctionCall>(SOURCE_FUNCTION_NAME) {
                override fun accepts(call: Jinja2FunctionCall, context: ProcessingContext?): Boolean {
                    val callee = call.callee
                    return (callee as? Jinja2VariableReferenceImpl)?.name == SOURCE_FUNCTION_NAME
                }
            }

        /**
         * Pattern that matches Jinja2FunctionCall elements that are source() calls.
         */
        val SOURCE_FUNCTION_PATTERN = PlatformPatterns.psiElement(Jinja2FunctionCall::class.java)
            .with(isSourceFunction)
    }

    /**
     * Creates references for Jinja2FunctionCall elements.
     *
     * For source() calls, creates two references:
     * 1. A reference for the source name (first argument)
     * 2. A reference for the table name (second argument)
     *
     * @param element The Jinja2FunctionCall element
     * @param context Processing context (unused)
     * @return Array of references for both arguments
     */
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        // Verify this is in a dbt project
        val project = element.project
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        if (indexService.getAllDbtConfigsSync().isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }

        // Element should be a Jinja2FunctionCall (ensured by pattern)
        val functionCall = element as? Jinja2FunctionCall
            ?: return PsiReference.EMPTY_ARRAY

        // Get string literal arguments
        val stringLiterals = DatamancerUtils.getStringLiteralArguments(functionCall)
        if (stringLiterals.size < 2) {
            return PsiReference.EMPTY_ARRAY
        }

        val sourceNameLiteral = stringLiterals[0]
        val tableNameLiteral = stringLiterals[1]

        val sourceName = sourceNameLiteral.value
        val tableName = tableNameLiteral.value

        val references = mutableListOf<PsiReference>()

        // Create reference for source name (first argument)
        val sourceNameOffset = sourceNameLiteral.startOffsetInParent
        val sourceNameRange = TextRange(sourceNameOffset + 1, sourceNameOffset + 1 + sourceName.length)
        references.add(DbtSourceNameReference(functionCall, sourceNameRange, sourceName, sourceNameLiteral))

        // Create reference for table name (second argument)
        val tableNameOffset = tableNameLiteral.startOffsetInParent
        val tableNameRange = TextRange(tableNameOffset + 1, tableNameOffset + 1 + tableName.length)
        references.add(
            DbtSourceTableReference(
                functionCall,
                tableNameRange,
                sourceName,
                tableName,
                tableNameLiteral
            )
        )

        return references.toTypedArray()
    }
}
