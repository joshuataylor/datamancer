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

/**
 * Reference for the source name argument in source() function calls.
 *
 * Resolves `source('source_name', 'table_name')` -> source definition in schema.yml
 */
class DbtSourceNameReference(
    element: Jinja2FunctionCall,
    textRange: TextRange,
    private val sourceName: String,
    private val stringLiteral: Jinja2StringLiteral
) : DbtModelReferenceBase<Jinja2FunctionCall>(element, textRange, sourceName) {

    override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val sourceIndexService = DbtSourceIndexService.getInstance(project)

        val sourceDefinition = sourceIndexService.findSource(sourceName)
        if (sourceDefinition != null) {
            return arrayOf(PsiElementResolveResult(sourceDefinition.psiElement))
        }

        return ResolveResult.EMPTY_ARRAY
    }

    /**
     * Provides code completion for source names.
     */
    override fun getVariants(): Array<Any> {
        val project = element.project
        val sourceIndexService = DbtSourceIndexService.getInstance(project)
        val variants = mutableListOf<LookupElement>()

        val sourceNames = sourceIndexService.getAllSourceNames()
        for (name in sourceNames) {
            val lookupElement = LookupElementBuilder
                .create(name)
                .withIcon(AllIcons.Nodes.DataSchema)
                .withTypeText("source")
                .withPresentableText(name)

            variants.add(lookupElement)
        }

        return variants.toTypedArray()
    }
}
