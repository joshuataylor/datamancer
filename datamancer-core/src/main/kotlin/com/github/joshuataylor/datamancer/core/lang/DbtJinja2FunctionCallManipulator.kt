package com.github.joshuataylor.datamancer.core.lang

import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.Jinja2TemplateElementGenerator
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

/**
 * Element manipulator for Jinja2 function calls.
 *
 * Enables rename refactoring for dbt model references. When a model file is renamed,
 * this manipulator updates the string argument in ref() calls to match the new name.
 *
 * Example: When renaming `customers.sql` to `customer_data.sql`, all `ref('customers')`
 * calls are automatically updated to `ref('customer_data')`.
 */
class DbtJinja2FunctionCallManipulator : AbstractElementManipulator<Jinja2FunctionCall>() {

    /**
     * Handles content change during rename refactoring.
     *
     * @param element The Jinja2FunctionCall element being modified
     * @param range The text range being changed (unused, we replace the whole string literal)
     * @param newContent The new content (typically the new filename with extension)
     * @return The modified function call element
     * @throws IncorrectOperationException If the element cannot be modified
     */
    @Throws(IncorrectOperationException::class)
    override fun handleContentChange(
        element: Jinja2FunctionCall,
        range: TextRange,
        newContent: String
    ): Jinja2FunctionCall {
        // Find the string literal argument in the function call
        val stringLiteral = PsiTreeUtil.findChildOfType(element, Jinja2StringLiteral::class.java)
            ?: return element

        // Strip file extension and wrap in quotes, preserving the original quote style
        val quoteChar = stringLiteral.text.firstOrNull() ?: '"'
        val newArgumentValue = "$quoteChar${FileUtilRt.getNameWithoutExtension(newContent)}$quoteChar"

        // Create new string literal and replace the old one
        val generator = Jinja2TemplateElementGenerator.getInstance(element.project)
        stringLiteral.replace(generator.createStringLiteral(newArgumentValue))

        return element
    }
}
