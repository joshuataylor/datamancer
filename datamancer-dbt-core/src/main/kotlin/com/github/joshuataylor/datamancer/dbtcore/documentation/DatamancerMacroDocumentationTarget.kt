package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.github.joshuataylor.datamancer.core.documentation.DatamancerDocumentationHtmlUtils
import com.github.joshuataylor.datamancer.core.services.DbtMacroIndexService
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer

/**
 * Documentation target for dbt macro hover popups.
 *
 * Renders a PyCharm-style documentation popup showing the macro signature
 * with syntax-highlighted keyword, function name, and parameters, along
 * with the source file path.
 */
class DatamancerMacroDocumentationTarget(
    private val element: PsiElement,
    private val project: Project,
    private val macroName: String,
    private val parameters: List<String>,
    private val macroFilePath: String,
    private val targetElement: PsiElement? = null
) : DocumentationTarget {

    override val navigatable: Navigatable?
        get() = targetElement as? Navigatable

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val targetElementPtr = targetElement?.createSmartPointer()
        val name = macroName
        val params = parameters
        val filePath = macroFilePath

        return Pointer {
            val restoredElement = elementPtr.dereference() ?: return@Pointer null
            val restoredProject = restoredElement.project

            // Re-query macro definition in case files have changed
            val macroIndexService = DbtMacroIndexService.getInstance(restoredProject)
            val macroDef = macroIndexService.findMacro(name)
            val restoredTarget = if (macroDef != null) {
                val psiManager = com.intellij.psi.PsiManager.getInstance(restoredProject)
                psiManager.findFile(macroDef.file)?.findElementAt(macroDef.textOffset)
            } else {
                targetElementPtr?.dereference()
            }

            DatamancerMacroDocumentationTarget(
                element = restoredElement,
                project = restoredProject,
                macroName = name,
                parameters = macroDef?.parameters ?: params,
                macroFilePath = filePath,
                targetElement = restoredTarget
            )
        }
    }

    override fun computePresentation(): TargetPresentation {
        val paramsText = if (parameters.isNotEmpty()) {
            "(${parameters.joinToString(", ")})"
        } else {
            "()"
        }
        return TargetPresentation.builder("$macroName$paramsText").presentation()
    }

    override fun computeDocumentation(): DocumentationResult {
        val builder = HtmlBuilder()

        // Definition section: syntax-highlighted macro signature
        val signatureBuilder = HtmlBuilder()
        signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan("macro ", DefaultLanguageHighlighterColors.KEYWORD))
        signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan(macroName, DefaultLanguageHighlighterColors.FUNCTION_DECLARATION))
        signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan("(", DefaultLanguageHighlighterColors.PARENTHESES))

        parameters.forEachIndexed { index, param ->
            if (index > 0) {
                signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan(", ", DefaultLanguageHighlighterColors.COMMA))
            }
            signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan(param, DefaultLanguageHighlighterColors.PARAMETER))
        }

        signatureBuilder.appendRaw(DatamancerDocumentationHtmlUtils.styledSpan(")", DefaultLanguageHighlighterColors.PARENTHESES))

        builder.append(
            signatureBuilder.toFragment()
                .wrapWith(DocumentationMarkup.PRE_ELEMENT)
                .wrapWith(DocumentationMarkup.DEFINITION_ELEMENT)
        )

        // Content section: source file path
        val contentBuilder = HtmlBuilder()
        contentBuilder.append(
            HtmlChunk.text("Defined in $macroFilePath").wrapWith(DocumentationMarkup.GRAYED_ELEMENT)
        )
        builder.append(contentBuilder.toFragment().wrapWith(DocumentationMarkup.CONTENT_ELEMENT))

        return DocumentationResult.documentation(builder.wrapWithHtmlBody().toString())
    }
}
