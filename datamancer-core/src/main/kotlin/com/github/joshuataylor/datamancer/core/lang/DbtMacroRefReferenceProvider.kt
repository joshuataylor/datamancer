package com.github.joshuataylor.datamancer.core.lang

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
 * Reference provider for dbt custom macro function calls.
 *
 * This provider creates references for Jinja2 function calls that are custom macro calls
 * (not built-in dbt or Jinja2 functions), enabling go-to-definition, code completion,
 * and find-usages for dbt macros defined in macros/ directories.
 *
 * Macro calls look like `{{ my_macro(arg1, arg2) }}` and resolve to definitions
 * like `{percent} macro my_macro(arg1, arg2) {percent}` in .sql files.
 */
class DbtMacroRefReferenceProvider : PsiReferenceProvider() {

    companion object {
        /**
         * Standard Jinja2 built-in functions and globals that should not be treated
         * as custom macro calls.
         */
        private val JINJA2_BUILT_IN_FUNCTIONS: Set<String> = setOf(
            "range", "lipsum", "dict", "cycler", "joiner", "namespace",
            "caller", "super"
        )

        /**
         * Combined set of all built-in dbt and Jinja2 function names that should
         * NOT be treated as custom macro calls.
         */
        val BUILT_IN_FUNCTIONS: Set<String> = DbtTagLibrary.DBT_PARAMETERIZED_TAGS +
            DbtTagLibrary.DBT_UNPARAMETERIZED_TAGS +
            JINJA2_BUILT_IN_FUNCTIONS

        /**
         * Pattern condition that accepts any Jinja2FunctionCall whose callee name
         * is NOT in the built-in function set.
         *
         * Returns false if the callee is null or not a simple variable reference
         * (excludes qualified calls like `package.macro_name()`).
         */
        val isMacroFunction: PatternCondition<Jinja2FunctionCall> =
            object : PatternCondition<Jinja2FunctionCall>("dbtMacroCall") {
                override fun accepts(
                    call: Jinja2FunctionCall,
                    context: ProcessingContext?
                ): Boolean {
                    val callee = call.callee
                    val name = (callee as? Jinja2VariableReferenceImpl)?.name
                        ?: return false
                    return name !in BUILT_IN_FUNCTIONS
                }
            }

        /**
         * Pattern that matches Jinja2FunctionCall elements that are custom macro calls.
         *
         * Use this pattern when registering the reference provider.
         */
        val MACRO_FUNCTION_PATTERN = PlatformPatterns.psiElement(Jinja2FunctionCall::class.java)
            .with(isMacroFunction)
    }

    /**
     * Creates references for custom macro function calls.
     *
     * The reference text range covers the function name (callee), not a string argument,
     * since the macro name itself is what the user navigates from.
     *
     * @param element The Jinja2FunctionCall element
     * @param context Processing context (unused)
     * @return Array containing a DbtMacroReference, or empty array if not applicable
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

        // Get the callee name
        val callee = functionCall.callee
        val macroName = (callee as? Jinja2VariableReferenceImpl)?.name
            ?: return PsiReference.EMPTY_ARRAY

        if (macroName.isBlank()) {
            return PsiReference.EMPTY_ARRAY
        }

        // Calculate text range covering the function name (callee element)
        val calleeOffset = callee.startOffsetInParent
        val textRange = TextRange(calleeOffset, calleeOffset + macroName.length)

        return arrayOf(DbtMacroReference(functionCall, textRange, macroName))
    }
}
