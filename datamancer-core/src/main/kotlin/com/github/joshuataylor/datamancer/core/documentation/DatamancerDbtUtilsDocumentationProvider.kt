package com.github.joshuataylor.datamancer.core.documentation

import com.github.joshuataylor.datamancer.core.services.DatamancerDbtProjectIndexService
import com.github.joshuataylor.datamancer.core.services.DbtUtilsDocumentationService
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
 * Documentation provider for dbt package functions (e.g. `dbt_utils.generate_surrogate_key()`).
 *
 * Implements both [DocumentationTargetProvider] (for editor hover) and
 * [PsiDocumentationTargetProvider] (for autocomplete popup documentation).
 *
 * Handles qualified/dotted function calls where the callee text is `package.function_name`.
 * Currently supports `dbt_utils` as a known package prefix.
 */
class DatamancerDbtUtilsDocumentationProvider : DocumentationTargetProvider, PsiDocumentationTargetProvider {

    companion object {
        /**
         * Matches a qualified callee like `dbt_utils.generate_surrogate_key`.
         * Group 1: package name, Group 2: function name.
         */
        private val QUALIFIED_CALLEE_PATTERN = Regex("""^(\w+)\.(\w+)$""")
    }

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

    private fun createTargetFromOffset(file: PsiFile, offset: Int): DatamancerDbtUtilsDocumentationTarget? {
        if (offset < 0) return null

        val viewProvider = file.viewProvider
        if (viewProvider !is TemplateLanguageFileViewProvider) return null

        // Find the element in the Jinja2 (base) language layer
        val basePsi = viewProvider.getPsi(viewProvider.baseLanguage) ?: return null
        val element = basePsi.findElementAt(offset) ?: return null

        return createTargetFromElement(file, element)
    }

    private fun createTargetFromElement(file: PsiFile, element: PsiElement): DatamancerDbtUtilsDocumentationTarget? {
        val project = file.project

        // Check file is in a dbt project
        val indexService = DatamancerDbtProjectIndexService.getInstance(project)
        if (indexService.getAllDbtConfigsSync().isEmpty()) return null

        // Walk up to find a Jinja2FunctionCall ancestor
        val functionCall = PsiTreeUtil.getParentOfType(element, Jinja2FunctionCall::class.java, false)
            ?: return null

        val callee = functionCall.callee ?: return null

        // If the callee is a simple variable reference, this is a plain function call
        // (handled by the built-in or macro documentation providers)
        if (callee is Jinja2VariableReferenceImpl) return null

        // Parse the callee text for a qualified name like "dbt_utils.generate_surrogate_key"
        val calleeText = callee.text ?: return null
        val match = QUALIFIED_CALLEE_PATTERN.matchEntire(calleeText) ?: return null

        val packageName = match.groupValues[1]
        val functionName = match.groupValues[2]

        // Only handle known package prefixes
        if (packageName !in DbtUtilsDocumentationService.KNOWN_PACKAGE_PREFIXES) return null

        // Check the hover is within the callee text range (not on arguments)
        val calleeRange = callee.textRange
        val elementRange = element.textRange
        if (elementRange.startOffset < calleeRange.startOffset ||
            elementRange.endOffset > calleeRange.endOffset) return null

        // Look up documentation
        val docService = DbtUtilsDocumentationService.getInstance(project)
        val doc = docService.getDocumentation(functionName) ?: return null

        return DatamancerDbtUtilsDocumentationTarget(
            element = element,
            project = project,
            packageName = packageName,
            functionName = functionName,
            doc = doc
        )
    }
}
