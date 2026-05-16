package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DbtSourceIndexService
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.ResolveResult
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Reference for the table name argument in source() function calls.
 *
 * Resolves `source('source_name', 'table_name')` -> table definition in schema.yml
 */
class DbtSourceTableReference(
    element: Jinja2FunctionCall,
    textRange: TextRange,
    private val sourceName: String,
    private val tableName: String,
    private val stringLiteral: Jinja2StringLiteral
) : DbtModelReferenceBase<Jinja2FunctionCall>(element, textRange, tableName) {

    @RequiresReadLock
    override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val sourceIndexService = DbtSourceIndexService.getInstance(project)

        val tableDefinition = sourceIndexService.findSourceTable(sourceName, tableName)
        if (tableDefinition != null) {
            return arrayOf(PsiElementResolveResult(tableDefinition.psiElement))
        }

        return ResolveResult.EMPTY_ARRAY
    }

    /**
     * Provides code completion for table names within the current source.
     */
    @RequiresReadLock
    override fun getVariants(): Array<Any> {
        val project = element.project
        val sourceIndexService = DbtSourceIndexService.getInstance(project)
        val variants = mutableListOf<LookupElement>()

        val tableNames = sourceIndexService.getTableNames(sourceName)
        for (name in tableNames) {
            val lookupElement = LookupElementBuilder
                .create(name)
                .withIcon(AllIcons.Nodes.DataTables)
                .withTypeText("table")
                .withPresentableText(name)

            variants.add(lookupElement)
        }

        return variants.toTypedArray()
    }
}
