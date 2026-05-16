package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DbtVarIndexService
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
 * Reference for the variable name argument in var() function calls.
 *
 * Resolves `var('variable_name')` -> variable definition in dbt_project.yml
 */
class DbtVarReference(
    element: Jinja2FunctionCall,
    textRange: TextRange,
    private val varName: String,
    private val stringLiteral: Jinja2StringLiteral
) : DbtModelReferenceBase<Jinja2FunctionCall>(element, textRange, varName) {

    @RequiresReadLock
    override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val varIndexService = DbtVarIndexService.getInstance(project)

        val varDefinition = varIndexService.findVar(varName)
        if (varDefinition != null) {
            return arrayOf(PsiElementResolveResult(varDefinition.psiElement))
        }

        return ResolveResult.EMPTY_ARRAY
    }

    /**
     * Provides code completion for variable names.
     */
    @RequiresReadLock
    override fun getVariants(): Array<Any> {
        val project = element.project
        val varIndexService = DbtVarIndexService.getInstance(project)
        val variants = mutableListOf<LookupElement>()

        val allVars = varIndexService.getAllVars()
        for ((name, varDef) in allVars) {
            val typeText = if (varDef.scope != null) {
                "var (${varDef.scope})"
            } else {
                "var (global)"
            }

            val lookupElement = LookupElementBuilder
                .create(name)
                .withIcon(AllIcons.Nodes.Variable)
                .withTypeText(typeText)
                .withPresentableText(name)
                .withTailText(varDef.value?.let { " = $it" } ?: "", true)

            variants.add(lookupElement)
        }

        return variants.toTypedArray()
    }
}
