package com.github.joshuataylor.datamancer.core.lang

import com.github.joshuataylor.datamancer.core.services.DbtGenericTestIndexService
import com.github.joshuataylor.datamancer.core.services.DbtMacroIndexService
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Factory that enables Find Usages for dbt macro and generic test definitions.
 *
 * When the cursor is on a name inside `{percent} macro name(...) {percent}` or
 * `{percent} test name(...) {percent}`, this factory wraps the leaf element in
 * a [DbtMacroDefinitionElement] or [DbtTestDefinitionElement] and returns a handler
 * that lets the platform search for all references to it.
 */
class DatamancerFindUsagesHandlerFactory : FindUsagesHandlerFactory() {

    override fun canFindUsages(element: PsiElement): Boolean {
        if (element is DbtMacroDefinitionElement) return true
        if (element is DbtTestDefinitionElement) return true
        return findDefinitionAtElement(element) != null
    }

    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        val target = when (element) {
            is DbtMacroDefinitionElement -> element
            is DbtTestDefinitionElement -> element
            else -> findDefinitionAtElement(element) ?: return null
        }
        return object : FindUsagesHandler(target) {}
    }

    private fun findDefinitionAtElement(element: PsiElement): PsiElement? {
        return findMacroAtElement(element) ?: findTestAtElement(element)
    }

    private fun findMacroAtElement(element: PsiElement): DbtMacroDefinitionElement? {
        val file = element.containingFile ?: return null
        val vFile = file.virtualFile ?: return null
        val offset = element.textOffset

        val service = DbtMacroIndexService.getInstance(element.project)
        for ((name, macroDef) in service.getAllMacros()) {
            if (macroDef.file == vFile && macroDef.textOffset == offset) {
                val nameRange = TextRange(macroDef.textOffset, macroDef.textOffset + macroDef.nameLength)
                return DbtMacroDefinitionElement(file, name, nameRange)
            }
        }
        return null
    }

    private fun findTestAtElement(element: PsiElement): DbtTestDefinitionElement? {
        val file = element.containingFile ?: return null
        val vFile = file.virtualFile ?: return null
        val offset = element.textOffset

        val service = DbtGenericTestIndexService.getInstance(element.project)
        for ((name, testDef) in service.getAllGenericTests()) {
            if (testDef.file == vFile && testDef.textOffset == offset) {
                val nameRange = TextRange(testDef.textOffset, testDef.textOffset + testDef.nameLength)
                return DbtTestDefinitionElement(file, name, nameRange)
            }
        }
        return null
    }
}
