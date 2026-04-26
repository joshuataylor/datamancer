package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

/**
 * Reference provider for dbt `ref()` function calls.
 *
 * This provider creates references for Jinja2 function calls that are `ref()` calls,
 * enabling go-to-definition and code completion for dbt model references.
 *
 * Uses proper PSI patterns instead of regex heuristics for robust detection.
 */
class DbtModelRefReferenceProvider : PsiReferenceProvider() {

    companion object {
        private const val REF_FUNCTION_NAME = "ref"

        /**
         * Pattern condition that checks if a Jinja2FunctionCall is a ref() call.
         *
         * This checks the callee's name to ensure it's "ref".
         */
        val isRefFunction: PatternCondition<Jinja2FunctionCall> =
            object : PatternCondition<Jinja2FunctionCall>(REF_FUNCTION_NAME) {
                override fun accepts(call: Jinja2FunctionCall, context: ProcessingContext?): Boolean {
                    val callee = call.callee
                    return (callee as? Jinja2VariableReferenceImpl)?.name == REF_FUNCTION_NAME
                }
            }

        /**
         * Pattern that matches Jinja2FunctionCall elements that are ref() calls.
         *
         * Use this pattern when registering the reference provider.
         */
        val REF_FUNCTION_PATTERN = PlatformPatterns.psiElement(Jinja2FunctionCall::class.java)
            .with(isRefFunction)

        /**
         * Calculates the text range for the string literal value within the function call.
         *
         * @param textLength The total length of the string literal (including quotes)
         * @return TextRange for the value portion (excluding quotes)
         */
        private fun getValueRangeInStringLiteral(textLength: Int): TextRange {
            // Skip opening quote, adjust for closing quote
            // ref('model') -> range covers 'model' part
            return TextRange.from(minOf(5, textLength - 1), maxOf(0, textLength - 2))
        }
    }

    /**
     * Creates references for Jinja2FunctionCall elements.
     *
     * This method is called when the pattern matches a ref() function call.
     * It extracts the string literal argument and creates a reference for it.
     *
     * @param element The Jinja2FunctionCall element
     * @param context Processing context (unused)
     * @return Array of references, or empty array if no string literal found
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

        // Find the string literal argument
        val stringLiteral = PsiTreeUtil.findChildOfType(functionCall, Jinja2StringLiteral::class.java)
            ?: return PsiReference.EMPTY_ARRAY

        // Extract model name from string literal
        val modelName = stringLiteral.value
        if (modelName.isNullOrBlank()) {
            return PsiReference.EMPTY_ARRAY
        }

        // Create text range for the model name within the function call
        // Calculate offset relative to the function call element
        val literalOffset = stringLiteral.startOffsetInParent
        val textRange = TextRange(literalOffset + 1, literalOffset + 1 + modelName.length)

        return arrayOf(DbtModelRefReference(functionCall, textRange, modelName, stringLiteral))
    }
}
