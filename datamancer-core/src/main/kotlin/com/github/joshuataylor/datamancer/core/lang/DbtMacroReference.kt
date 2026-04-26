package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DbtMacroIndexService
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.ResolveResult

/**
 * Reference implementation for dbt custom macro calls.
 *
 * This reference resolves macro names in `{{ macro_name() }}` calls to their
 * definitions (`{percent} macro macro_name() {percent}`) in .sql files within
 * macros/ directories.
 *
 * Provides code completion for available macros with parameter information.
 *
 * @param element The Jinja2FunctionCall element containing the macro call
 * @param textRange The text range of the macro name within the element
 * @param macroName The macro name being referenced
 */
class DbtMacroReference(
    element: Jinja2FunctionCall,
    textRange: TextRange,
    macroName: String
) : DbtModelReferenceBase<Jinja2FunctionCall>(element, textRange, macroName) {

    /**
     * Resolves the reference to the macro definition.
     *
     * Looks up the macro in the macro index service and navigates to the
     * specific PSI element at the macro name position in the definition file.
     *
     * @param incompleteCode Whether to consider incomplete code during resolution
     * @return Array containing the resolved element, or an empty array if not found
     */
    override fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val macroIndexService = DbtMacroIndexService.getInstance(project)
        val macroDef = macroIndexService.findMacro(modelName)
            ?: return ResolveResult.EMPTY_ARRAY

        val psiManager = PsiManager.getInstance(project)
        val psiFile = psiManager.findFile(macroDef.file)
            ?: return ResolveResult.EMPTY_ARRAY

        // Wrap in DbtMacroDefinitionElement so the target is a PsiNamedElement,
        // enabling Find Usages from the definition site
        val nameRange = TextRange(macroDef.textOffset, macroDef.textOffset + macroDef.nameLength)
        val target = DbtMacroDefinitionElement(psiFile, macroDef.name, nameRange)

        return arrayOf(PsiElementResolveResult(target))
    }

    /**
     * Provides code completion variants for macro calls.
     *
     * Returns all available macros in the project with their parameter
     * information displayed as tail text.
     *
     * @return Array of lookup elements for code completion
     */
    override fun getVariants(): Array<Any> {
        val project = element.project
        val macroIndexService = DbtMacroIndexService.getInstance(project)
        val variants = mutableListOf<LookupElement>()

        val allMacros = macroIndexService.getAllMacros()
        for ((name, macroDef) in allMacros) {
            val paramsText = if (macroDef.parameters.isNotEmpty()) {
                "(${macroDef.parameters.joinToString(", ")})"
            } else {
                "()"
            }

            val lookupElement = LookupElementBuilder
                .create(name)
                .withIcon(AllIcons.Nodes.Method)
                .withTypeText("macro")
                .withTailText(paramsText, true)
                .withPresentableText(name)

            variants.add(lookupElement)
        }

        return variants.toTypedArray()
    }
}
