package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.github.joshuataylor.datamancer.core.documentation.DatamancerDocumentationHtmlUtils
import com.github.joshuataylor.datamancer.core.services.DbtBuiltinDocumentationService.DbtBuiltinFunctionDoc
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
 * Documentation target for dbt built-in Jinja2 function hover popups.
 *
 * Renders a PyCharm-style documentation popup with a syntax-highlighted signature,
 * description, parameter documentation, and a usage example.
 */
class DatamancerBuiltinDocumentationTarget(
    private val element: PsiElement,
    private val project: Project,
    private val functionName: String,
    private val doc: DbtBuiltinFunctionDoc
) : DocumentationTarget {

    override val navigatable: Navigatable? = null

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val name = functionName
        val capturedDoc = doc

        return Pointer {
            val restoredElement = elementPtr.dereference() ?: return@Pointer null
            DatamancerBuiltinDocumentationTarget(restoredElement, restoredElement.project, name, capturedDoc)
        }
    }

    override fun computePresentation(): TargetPresentation {
        val label = if (doc.isParameterised) {
            val params = doc.parameterNames.joinToString(", ")
            "$functionName($params)"
        } else {
            functionName
        }
        return TargetPresentation.builder(label).presentation()
    }

    override fun computeDocumentation(): DocumentationResult {
        val builder = HtmlBuilder()

        // -- Definition section: syntax-highlighted signature --
        builder.append(buildSignatureSection())

        // -- Content section: description, args, example --
        builder.append(buildContentSection())

        return DocumentationResult.documentation(builder.wrapWithHtmlBody().toString())
    }

    private fun buildSignatureSection(): HtmlChunk {
        val signatureBuilder = HtmlBuilder()

        if (doc.isParameterised) {
            signatureBuilder.appendRaw(
                DatamancerDocumentationHtmlUtils.styledSpan(functionName, DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
            )
            signatureBuilder.appendRaw(
                DatamancerDocumentationHtmlUtils.styledSpan("(", DefaultLanguageHighlighterColors.PARENTHESES)
            )
            doc.parameterNames.forEachIndexed { index, param ->
                if (index > 0) {
                    signatureBuilder.appendRaw(
                        DatamancerDocumentationHtmlUtils.styledSpan(", ", DefaultLanguageHighlighterColors.COMMA)
                    )
                }
                signatureBuilder.appendRaw(
                    DatamancerDocumentationHtmlUtils.styledSpan(param, DefaultLanguageHighlighterColors.PARAMETER)
                )
            }
            signatureBuilder.appendRaw(
                DatamancerDocumentationHtmlUtils.styledSpan(")", DefaultLanguageHighlighterColors.PARENTHESES)
            )
        } else {
            signatureBuilder.appendRaw(
                DatamancerDocumentationHtmlUtils.styledSpan(functionName, DefaultLanguageHighlighterColors.GLOBAL_VARIABLE)
            )
        }

        // Wrap the signature in a link to the external docs if available
        val signatureFragment = signatureBuilder.toFragment()
        val externalUrl = doc.externalDocUrl
        val linkedSignature = if (externalUrl != null) {
            HtmlChunk.link(externalUrl, signatureFragment)
        } else {
            signatureFragment
        }

        return linkedSignature
            .wrapWith(DocumentationMarkup.PRE_ELEMENT)
            .wrapWith(DocumentationMarkup.DEFINITION_ELEMENT)
    }

    private fun buildContentSection(): HtmlChunk {
        val contentBuilder = HtmlBuilder()

        // Link to external documentation at the top
        val externalUrl = doc.externalDocUrl
        if (externalUrl != null) {
            contentBuilder.append(
                HtmlChunk.link(externalUrl, "View on docs.getdbt.com")
            )
            contentBuilder.append(HtmlChunk.br())
            contentBuilder.append(HtmlChunk.br())
        }

        // Full rendered body from the markdown documentation, with syntax-highlighted code blocks
        if (doc.bodyHtml.isNotBlank()) {
            contentBuilder.appendRaw(DatamancerDocumentationHtmlUtils.highlightCodeBlocks(project, doc.bodyHtml))
        }

        // Source label
        contentBuilder.append(
            HtmlChunk.text("dbt built-in function").wrapWith(DocumentationMarkup.GRAYED_ELEMENT)
        )

        return contentBuilder.toFragment().wrapWith(DocumentationMarkup.CONTENT_ELEMENT)
    }
}
