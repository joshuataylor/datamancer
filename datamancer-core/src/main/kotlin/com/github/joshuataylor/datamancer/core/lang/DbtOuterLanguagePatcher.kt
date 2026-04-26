package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.template.psi.Jinja2TemplateElementTypes
import com.intellij.psi.templateLanguages.TemplateDataElementType
import com.intellij.psi.templateLanguages.TemplateDataElementType.OuterLanguageRangePatcher

/**
 * Outer language range patcher for dbt SQL files with Jinja2 templates.
 *
 * This class provides placeholder identifiers for template data elements when Jinja2
 * templates are injected into SQL files. This allows the SQL language to properly parse
 * the outer (SQL) language parts of the file while Jinja2 handles the template sections.
 *
 * For example, in a dbt model file:
 * ```sql
 * SELECT * FROM {{ ref('customers') }}
 * ```
 *
 * The SQL parser needs a placeholder for the `{{ ref('customers') }}` part to maintain
 * valid SQL syntax during parsing. This patcher provides that placeholder.
 *
 * ## Non-output expression handling
 *
 * Some dbt macros like `config()` and `generate_indexes()` use expression syntax `{{ }}`
 * but produce no SQL output. For these, we return `null` to avoid inserting a placeholder
 * that would create invalid SQL like `datamancer_placeholder SELECT ...`.
 *
 * The heuristic used to detect non-output expressions:
 * - **Setup/config macros** use keyword arguments only: `{{ config(materialized='table') }}`
 * - **Output macros** use positional string arguments: `{{ ref('model') }}`
 *
 * If a function call's first argument is a keyword argument (contains `=`), it's likely
 * a setup/config macro that doesn't produce SQL output.
 */
class DbtOuterLanguagePatcher : OuterLanguageRangePatcher {
    companion object {
        /**
         * Placeholder identifier used for Jinja2 template data in SQL context.
         * This identifier is used by the SQL parser when it encounters a Jinja2 template
         * section, allowing the SQL to remain syntactically valid during parsing.
         */
        const val PLACEHOLDER_IDENTIFIER = "datamancer_placeholder"

        /**
         * Regex to detect function calls with keyword arguments as the first argument.
         * These are typically setup/config macros that don't produce SQL output.
         *
         * Matches patterns like:
         * - `{{ config(materialized='table') }}`
         * - `{{ generate_indexes(primary_key='id') }}`
         * - `{{- my_setup_macro(option=True) -}}`
         *
         * Does NOT match (these have positional args first):
         * - `{{ ref('model') }}` - string as first arg
         * - `{{ source('schema', 'table') }}` - string as first arg
         * - `{{ my_macro('value', option=True) }}` - string before keyword
         */
        private val KEYWORD_ARG_FIRST_REGEX = Regex(
            """^\s*\{\{-?\s*\w+\s*\(\s*[a-zA-Z_]\w*\s*=""",
            RegexOption.MULTILINE
        )

        /**
         * Regex to detect known output-producing dbt functions.
         * These always produce SQL output and need placeholders.
         */
        private val KNOWN_OUTPUT_FUNCTIONS_REGEX = Regex(
            """^\s*\{\{-?\s*(ref|source|var|this|env_var)\s*[\(\s]""",
            RegexOption.IGNORE_CASE
        )
    }

    /**
     * Provides the text to be used for outer language insertion range for template data elements.
     *
     * @param templateDataElementType The type of template data element
     * @param outerElementText The original text of the outer element (e.g., `{{ ref('model') }}`)
     * @return A placeholder identifier for Jinja2 expressions that produce SQL output,
     *         null for setup/config expressions that don't produce output
     */
    override fun getTextForOuterLanguageInsertionRange(
        templateDataElementType: TemplateDataElementType,
        outerElementText: CharSequence
    ): String? {
        // Only patch Jinja2 template data elements
        if (templateDataElementType != Jinja2TemplateElementTypes.TEMPLATE_DATA) {
            return null
        }

        // Known output functions always need placeholders
        if (isKnownOutputFunction(outerElementText)) {
            return PLACEHOLDER_IDENTIFIER
        }

        // Function calls with keyword arguments first are likely setup/config macros
        // that don't produce SQL output (e.g., config(), generate_indexes())
        if (hasKeywordArgFirst(outerElementText)) {
            return null
        }

        // All other expressions (custom macros with positional args, variables, etc.)
        return PLACEHOLDER_IDENTIFIER
    }

    /**
     * Checks if the expression is a known output-producing dbt function.
     */
    private fun isKnownOutputFunction(text: CharSequence): Boolean {
        return KNOWN_OUTPUT_FUNCTIONS_REGEX.containsMatchIn(text)
    }

    /**
     * Checks if the function call has a keyword argument as its first argument.
     * This indicates a setup/config macro that doesn't produce SQL output.
     */
    private fun hasKeywordArgFirst(text: CharSequence): Boolean {
        return KEYWORD_ARG_FIRST_REGEX.containsMatchIn(text)
    }
}
