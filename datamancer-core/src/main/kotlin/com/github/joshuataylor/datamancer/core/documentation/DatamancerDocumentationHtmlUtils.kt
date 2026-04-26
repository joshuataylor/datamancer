package com.github.joshuataylor.datamancer.core.documentation

import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

/**
 * Shared HTML rendering helpers for Datamancer documentation targets.
 *
 * Provides syntax-highlighted spans and code-block replacement
 * for use in [com.intellij.platform.backend.documentation.DocumentationTarget]
 * implementations across both core and backend subprojects.
 */
@ApiStatus.Internal
object DatamancerDocumentationHtmlUtils {

    /**
     * Creates a syntax-highlighted HTML span using the platform's theme-aware
     * highlighting, matching PyCharm's documentation popup style.
     */
    fun styledSpan(text: String, textAttributesKey: TextAttributesKey): String {
        return QuickDocHighlightingHelper.getStyledFragment(text, textAttributesKey)
    }

    /**
     * Replaces `<pre><code data-lang="...">` blocks in the HTML with
     * syntax-highlighted versions using the platform's lexer-based highlighting.
     *
     * Code blocks without a `data-lang` attribute are left as-is.
     */
    fun highlightCodeBlocks(project: Project, html: String): String {
        val pattern = Regex(
            """<pre><code data-lang="([^"]+)">(.*?)</code></pre>""",
            RegexOption.DOT_MATCHES_ALL
        )
        return pattern.replace(html) { match ->
            val langId = match.groupValues[1]
            val escapedCode = match.groupValues[2]
            // Unescape the HTML entities back to raw text for the highlighter
            val rawCode = escapedCode
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
            val language = QuickDocHighlightingHelper.guessLanguage(normaliseLangId(langId))
            if (language != null) {
                QuickDocHighlightingHelper.getStyledCodeBlock(project, language, rawCode)
            } else {
                match.value
            }
        }
    }

    /**
     * Normalises markdown fence language identifiers to names the platform recognises.
     */
    fun normaliseLangId(langId: String): String = when (langId) {
        "yml" -> "yaml"
        "jinja" -> "jinja2"
        "bash" -> "shell script"
        "shell" -> "shell script"
        else -> langId
    }
}
