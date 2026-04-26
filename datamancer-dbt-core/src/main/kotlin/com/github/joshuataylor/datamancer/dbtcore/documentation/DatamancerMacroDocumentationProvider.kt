package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.github.joshuataylor.datamancer.core.lang.DbtMacroRefReferenceProvider
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.services.DbtMacroIndexService
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.util.PsiTreeUtil

/**
 * Documentation provider for dbt macro signatures.
 *
 * Implements both [DocumentationTargetProvider] (for editor hover) and
 * [PsiDocumentationTargetProvider] (for autocomplete popup documentation).
 *
 * When hovering over a macro call like `{{ testf2('x') }}` in a dbt SQL model file,
 * this provider shows the macro signature with syntax-highlighted parameters
 * and the source file location.
 */
class DatamancerMacroDocumentationProvider : DocumentationTargetProvider, PsiDocumentationTargetProvider {

    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val target = createTargetFromOffset(file, offset)
            ?: createTargetFromOffset(file, offset - 1)
            ?: return emptyList()
        return listOf(target)
    }

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        val sourceElement = originalElement ?: element
        val file = sourceElement.containingFile ?: return null
        return createTargetFromElement(file, sourceElement)
    }

    private fun createTargetFromOffset(file: PsiFile, offset: Int): DatamancerMacroDocumentationTarget? {
        if (offset < 0) return null

        val viewProvider = file.viewProvider
        if (viewProvider !is TemplateLanguageFileViewProvider) return null

        // Find the element in the Jinja2 (base) language layer
        val basePsi = viewProvider.getPsi(viewProvider.baseLanguage) ?: return null
        val element = basePsi.findElementAt(offset) ?: return null

        return createTargetFromElement(file, element)
    }

    private fun createTargetFromElement(file: PsiFile, element: PsiElement): DatamancerMacroDocumentationTarget? {
        val project = file.project

        // Check file is in a dbt project
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        if (indexService.getAllDbtConfigsSync().isEmpty()) return null

        // Walk up to find a Jinja2FunctionCall ancestor
        val functionCall = PsiTreeUtil.getParentOfType(element, Jinja2FunctionCall::class.java, false)
            ?: return null

        // Get the callee name
        val callee = functionCall.callee
        val macroName = (callee as? Jinja2VariableReferenceImpl)?.name ?: return null

        // Only show documentation for custom macros (not built-in functions)
        if (macroName in DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS) return null

        // Check the hover is on the function name itself, not on arguments
        val calleeRange = callee.textRange
        val elementRange = element.textRange
        if (elementRange.startOffset < calleeRange.startOffset || elementRange.endOffset > calleeRange.endOffset) {
            return null
        }

        // Look up the macro definition
        val macroIndexService = DbtMacroIndexService.getInstance(project)
        val macroDef = macroIndexService.findMacro(macroName) ?: return null

        // Compute a relative file path for display
        val macroFilePath = macroDef.file.let { macroFile ->
            val projectBasePath = project.basePath
            if (projectBasePath != null && macroFile.path.startsWith(projectBasePath)) {
                macroFile.path.removePrefix(projectBasePath).removePrefix("/")
            } else {
                macroFile.name
            }
        }

        // Resolve the target PSI element in the macro definition file
        val psiManager = com.intellij.psi.PsiManager.getInstance(project)
        val targetPsiFile = psiManager.findFile(macroDef.file)
        val targetElement = targetPsiFile?.findElementAt(macroDef.textOffset)

        return DatamancerMacroDocumentationTarget(
            element = element,
            project = project,
            macroName = macroName,
            parameters = macroDef.parameters,
            macroFilePath = macroFilePath,
            targetElement = targetElement
        )
    }
}
