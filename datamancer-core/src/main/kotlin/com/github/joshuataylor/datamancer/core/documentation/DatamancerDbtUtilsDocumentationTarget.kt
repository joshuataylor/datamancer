package com.github.joshuataylor.datamancer.core.documentation

import com.github.joshuataylor.datamancer.core.services.DbtBuiltinDocumentationService.DbtBuiltinFunctionDoc
import com.github.joshuataylor.datamancer.core.services.DbtUtilsDocumentationService
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
 * Documentation target for dbt_utils package function hover popups.
 *
 * Renders a documentation popup with a syntax-highlighted signature showing
 * the fully qualified name (e.g. `dbt_utils.generate_surrogate_key(field_list)`),
 * the full documentation body, and a link to the GitHub source.
 */
class DatamancerDbtUtilsDocumentationTarget(
    private val element: PsiElement,
    private val project: Project,
    private val packageName: String,
    private val functionName: String,
    private val doc: DbtBuiltinFunctionDoc
) : DocumentationTarget {

    override val navigatable: Navigatable? = null

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val pkg = packageName
        val name = functionName

        return Pointer {
            val restoredElement = elementPtr.dereference() ?: return@Pointer null
            val restoredProject = restoredElement.project
            val docService = DbtUtilsDocumentationService.getInstance(restoredProject)
            val restoredDoc = docService.getDocumentation(name) ?: return@Pointer null
            DatamancerDbtUtilsDocumentationTarget(restoredElement, restoredProject, pkg, name, restoredDoc)
        }
    }

    override fun computePresentation(): TargetPresentation {
        val params = doc.parameterNames.joinToString(", ")
        val label = if (params.isNotEmpty()) {
            "$packageName.$functionName($params)"
        } else {
            "$packageName.$functionName()"
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

        // Package prefix: dbt_utils.
        signatureBuilder.appendRaw(
            DatamancerDocumentationHtmlUtils.styledSpan(packageName, DefaultLanguageHighlighterColors.GLOBAL_VARIABLE)
        )
        signatureBuilder.appendRaw(
            DatamancerDocumentationHtmlUtils.styledSpan(".", DefaultLanguageHighlighterColors.DOT)
        )

        // Function name
        signatureBuilder.appendRaw(
            DatamancerDocumentationHtmlUtils.styledSpan(functionName, DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
        )

        // Parameters
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

        // Wrap the signature in a link to the external docs if available
        val signatureFragment = signatureBuilder.toFragment()
        val linkedSignature = if (doc.externalDocUrl != null) {
            HtmlChunk.link(doc.externalDocUrl, signatureFragment)
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
        if (doc.externalDocUrl != null) {
            contentBuilder.append(
                HtmlChunk.link(doc.externalDocUrl, "View on GitHub")
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
            HtmlChunk.text("dbt_utils package").wrapWith(DocumentationMarkup.GRAYED_ELEMENT)
        )

        return contentBuilder.toFragment().wrapWith(DocumentationMarkup.CONTENT_ELEMENT)
    }

}
