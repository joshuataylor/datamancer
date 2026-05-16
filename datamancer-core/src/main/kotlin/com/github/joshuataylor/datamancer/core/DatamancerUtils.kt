package com.github.joshuataylor.datamancer.core

import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.parsing.DjangoTemplateTokenTypes
import com.intellij.jinja.template.psi.impl.DjangoTagElementImpl
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.sql.dialects.SqlLanguageDialect
import com.intellij.sql.psi.SqlLanguage
import com.intellij.sql.psi.SqlQueryExpression
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Utility methods for Jinja2 and SQL PSI operations.
 *
 * This is a minimal utility object focused on PSI utilities needed for
 * code insight features. File discovery operations are in DbtDirectories.
 */
object DatamancerUtils {

    /**
     * Checks if the given Jinja2 function call is a ref() call.
     *
     * @param call The Jinja2 function call to check
     * @return true if this is a ref() call, false otherwise
     */
    @RequiresReadLock
    @JvmStatic
    fun isRefCall(call: Jinja2FunctionCall): Boolean {
        val callee = call.callee
        return (callee as? Jinja2VariableReferenceImpl)?.name == "ref"
    }

    /**
     * Checks if the given Jinja2 function call is a source() call.
     *
     * @param call The Jinja2 function call to check
     * @return true if this is a source() call, false otherwise
     */
    @RequiresReadLock
    @JvmStatic
    fun isSourceCall(call: Jinja2FunctionCall): Boolean {
        val callee = call.callee
        return (callee as? Jinja2VariableReferenceImpl)?.name == "source"
    }

    /**
     * Extracts the Jinja2 function call from a SQL PSI element.
     *
     * This method navigates from a SQL element (e.g., table reference) to the
     * corresponding Jinja2 function call in the template language layer.
     *
     * @param element The SQL PSI element (typically a table reference placeholder)
     * @return The Jinja2FunctionCall if found, null otherwise
     */
    @RequiresReadLock
    @JvmStatic
    fun getJinjaCall(element: PsiElement): Jinja2FunctionCall? {
        val viewProvider = element.containingFile.viewProvider
        val jinjaElement = viewProvider.getPsi(viewProvider.baseLanguage)
            ?.findElementAt(element.textOffset)

        // Check if we're at a Django/Jinja2 expression start ({{ or {%)
        if (jinjaElement?.elementType != DjangoTemplateTokenTypes.DJANGO_EXPRESSION_START) {
            return null
        }

        // Navigate up to find the containing tag element
        val djangoTag = PsiTreeUtil.getParentOfType(jinjaElement, DjangoTagElementImpl::class.java)
            ?: return null

        // Find the function call within the tag
        return PsiTreeUtil.findChildOfType(djangoTag, Jinja2FunctionCall::class.java)
    }

    /**
     * Extracts the referenced model/seed name from a ref() function call.
     *
     * @param refCall The ref() function call
     * @return The model name (string literal value), or null if not found
     */
    @RequiresReadLock
    @JvmStatic
    fun getReferencedName(refCall: Jinja2FunctionCall): String? {
        val stringLiteral = PsiTreeUtil.findChildOfType(refCall, Jinja2StringLiteral::class.java)
        return stringLiteral?.value
    }

    /**
     * Extracts all string literal arguments from a function call.
     *
     * Used for source() calls which have two arguments: source('source_name', 'table_name')
     *
     * @param functionCall The Jinja2 function call
     * @return List of string literal values in order, empty if none found
     */
    @RequiresReadLock
    @JvmStatic
    fun getStringArguments(functionCall: Jinja2FunctionCall): List<String> {
        return PsiTreeUtil.findChildrenOfType(functionCall, Jinja2StringLiteral::class.java)
            .mapNotNull { it.value }
    }

    /**
     * Extracts source name and table name from a source() function call.
     *
     * @param sourceCall The source() function call
     * @return Pair of (sourceName, tableName), or null if arguments are invalid
     */
    @RequiresReadLock
    @JvmStatic
    fun getSourceArguments(sourceCall: Jinja2FunctionCall): Pair<String, String>? {
        val args = getStringArguments(sourceCall)
        return if (args.size >= 2) {
            Pair(args[0], args[1])
        } else {
            null
        }
    }

    /**
     * Gets all string literals from a function call as PSI elements.
     *
     * @param functionCall The Jinja2 function call
     * @return List of Jinja2StringLiteral elements in order
     */
    @RequiresReadLock
    @JvmStatic
    fun getStringLiteralArguments(functionCall: Jinja2FunctionCall): List<Jinja2StringLiteral> {
        return PsiTreeUtil.findChildrenOfType(functionCall, Jinja2StringLiteral::class.java).toList()
    }

    /**
     * Finds the last SELECT query expression in a PSI file.
     *
     * This traverses the entire file and returns the last SqlQueryExpression found,
     * which is typically the main query in a dbt model.
     *
     * @param psiFile The PSI file to search
     * @return The last SqlQueryExpression, or null if none found
     */
    @RequiresReadLock
    @JvmStatic
    fun findLastSelectQuery(psiFile: PsiFile): SqlQueryExpression? {
        var result: SqlQueryExpression? = null

        psiFile.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (element is SqlQueryExpression) {
                    result = element
                }
                element.children.forEach { visitElement(it) }
            }
        })

        return result
    }

    /**
     * Checks if the file uses a SQL dialect.
     *
     * For template language files, checks the template data language.
     * For regular files, checks the file's language directly.
     *
     * @param file The PSI file to check
     * @return true if the file is SQL or a SQL dialect, false otherwise
     */
    @RequiresReadLock
    @JvmStatic
    fun isSqlDialect(file: PsiFile): Boolean {
        val viewProvider = file.viewProvider
        val language: Language = if (viewProvider is TemplateLanguageFileViewProvider) {
            viewProvider.templateDataLanguage
        } else {
            file.language
        }
        return language is SqlLanguageDialect || language is SqlLanguage
    }
}
