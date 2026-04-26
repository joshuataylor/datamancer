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
 * Reference provider for dbt `var()` function calls.
 *
 * This provider creates references for Jinja2 function calls that are `var()` calls,
 * enabling go-to-definition and code completion for dbt variable references.
 *
 * Variables are defined in dbt_project.yml under the `vars` key.
 */
class DbtVarRefReferenceProvider : PsiReferenceProvider() {

    companion object {
        private const val VAR_FUNCTION_NAME = "var"

        /**
         * Pattern condition that checks if a Jinja2FunctionCall is a var() call.
         */
        val isVarFunction: PatternCondition<Jinja2FunctionCall> =
            object : PatternCondition<Jinja2FunctionCall>(VAR_FUNCTION_NAME) {
                override fun accepts(call: Jinja2FunctionCall, context: ProcessingContext?): Boolean {
                    val callee = call.callee
                    return (callee as? Jinja2VariableReferenceImpl)?.name == VAR_FUNCTION_NAME
                }
            }

        /**
         * Pattern that matches Jinja2FunctionCall elements that are var() calls.
         */
        val VAR_FUNCTION_PATTERN = PlatformPatterns.psiElement(Jinja2FunctionCall::class.java)
            .with(isVarFunction)
    }

    /**
     * Creates references for Jinja2FunctionCall elements.
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

        // Get string literal arguments
        val stringLiterals = DatamancerUtils.getStringLiteralArguments(functionCall)
        if (stringLiterals.isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }

        // var() takes one argument: the variable name
        val varNameLiteral = stringLiterals[0]
        val varName = varNameLiteral.value

        // Create reference for variable name
        val varNameOffset = varNameLiteral.startOffsetInParent
        val varNameRange = TextRange(varNameOffset + 1, varNameOffset + 1 + varName.length)

        return arrayOf(DbtVarReference(functionCall, varNameRange, varName, varNameLiteral))
    }
}
