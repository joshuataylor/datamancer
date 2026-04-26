package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.github.joshuataylor.datamancer.core.lang.DbtMacroRefReferenceProvider
import com.github.joshuataylor.datamancer.core.lang.DbtTagLibrary
import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.services.DbtBuiltinDocumentationService
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
 * Documentation provider for dbt built-in Jinja2 functions and context variables.
 *
 * Implements both [DocumentationTargetProvider] (for editor hover) and
 * [PsiDocumentationTargetProvider] (for autocomplete popup documentation).
 *
 * Handles two PSI scenarios:
 * - **Parameterised functions**: `ref()`, `source()`, `var()`, etc. inside a [Jinja2FunctionCall]
 * - **Unparameterised variables**: `this`, `target`, `execute`, etc. as bare [Jinja2VariableReferenceImpl]
 */
class DatamancerBuiltinDocumentationProvider : DocumentationTargetProvider, PsiDocumentationTargetProvider {

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

    private fun createTargetFromOffset(file: PsiFile, offset: Int): DatamancerBuiltinDocumentationTarget? {
        if (offset < 0) return null

        val viewProvider = file.viewProvider
        if (viewProvider !is TemplateLanguageFileViewProvider) return null

        // Find the element in the Jinja2 (base) language layer
        val basePsi = viewProvider.getPsi(viewProvider.baseLanguage) ?: return null
        val element = basePsi.findElementAt(offset) ?: return null

        return createTargetFromElement(file, element)
    }

    private fun createTargetFromElement(file: PsiFile, element: PsiElement): DatamancerBuiltinDocumentationTarget? {
        val project = file.project

        // Check file is in a dbt project
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        if (indexService.getAllDbtConfigsSync().isEmpty()) return null

        // Attempt 1: Check for parameterised function call (e.g., ref(), source())
        val functionCall = PsiTreeUtil.getParentOfType(element, Jinja2FunctionCall::class.java, false)
        if (functionCall != null) {
            val callee = functionCall.callee
            val functionName = (callee as? Jinja2VariableReferenceImpl)?.name ?: return null

            // Only handle built-in dbt functions (inverse of DatamancerMacroDocumentationProvider)
            if (functionName !in DbtMacroRefReferenceProvider.BUILT_IN_FUNCTIONS) return null

            // Check hover is on the function name itself, not on arguments
            val calleeRange = callee.textRange
            val elementRange = element.textRange
            if (elementRange.startOffset < calleeRange.startOffset ||
                elementRange.endOffset > calleeRange.endOffset) return null

            return createBuiltinTarget(element, project, functionName)
        }

        // Attempt 2: Check for unparameterised variable reference (e.g., {{ this }}, {{ target }})
        val varRef = PsiTreeUtil.getParentOfType(element, Jinja2VariableReferenceImpl::class.java, false)
            ?: (element.parent as? Jinja2VariableReferenceImpl)
        if (varRef != null) {
            val name = varRef.name ?: return null

            // Only trigger for known dbt built-in names
            if (name !in DbtTagLibrary.DBT_UNPARAMETERIZED_TAGS &&
                name !in DbtTagLibrary.DBT_PARAMETERIZED_TAGS) return null

            // Ensure hover is on this specific reference, not a property access child
            val refRange = varRef.textRange
            val elemRange = element.textRange
            if (elemRange.startOffset < refRange.startOffset ||
                elemRange.endOffset > refRange.endOffset) return null

            return createBuiltinTarget(element, project, name)
        }

        return null
    }

    private fun createBuiltinTarget(
        element: PsiElement,
        project: com.intellij.openapi.project.Project,
        functionName: String
    ): DatamancerBuiltinDocumentationTarget? {
        val docService = DbtBuiltinDocumentationService.getInstance(project)
        val doc = docService.getDocumentation(functionName) ?: return null
        return DatamancerBuiltinDocumentationTarget(element, project, functionName, doc)
    }
}
