package com.github.joshuataylor.datamancer.core.highlighting

import com.intellij.jinja.lexer.Jinja2TokenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/**
 * Brace matcher for Jinja2 templates in dbt models.
 *
 * Provides matching for:
 * - Parentheses `(` and `)` for function calls like ref(), source()
 * - Square brackets `[` and `]` for list access and filters
 *
 * Note: Jinja2 template delimiters (`{{`, `}}`, `{%`, `%}`, `{#`, `#}`)
 * are handled by the Jinja2 plugin itself, not by this brace matcher.
 */
class DbtJinjaBraceMatcher : PairedBraceMatcher {

    private val PAIRS: Array<BracePair> = arrayOf(
        // Parentheses for function calls: ref('model'), source('schema', 'table')
        BracePair(Jinja2TokenTypes.LPAR, Jinja2TokenTypes.RPAR, false),
        // Square brackets for list access and filters: items[0], data | selectattr('key')
        BracePair(Jinja2TokenTypes.LBRACKET, Jinja2TokenTypes.RBRACKET, false)
    )

    override fun getPairs(): Array<BracePair> = PAIRS

    /**
     * Always allow paired braces in Jinja2 templates.
     */
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    /**
     * Returns the opening brace offset as the code construct start.
     *
     * This is the default behaviour - the construct starts at the opening brace.
     */
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
