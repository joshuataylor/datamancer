package com.github.joshuataylor.datamancer.core.services

import com.intellij.openapi.util.text.StringUtil

/**
 * Converts dbt documentation markdown (with MDX extensions) to HTML
 * suitable for IntelliJ documentation popups.
 *
 * Handles stripping of MDX-specific elements (`<File>`, `<Term>`, `<Constant>`,
 * `:::` admonitions, `import` statements) and converting standard markdown to HTML.
 */
object DbtMarkdownConverter {

    /**
     * Parsed sections extracted from a dbt documentation markdown file.
     */
    data class ParsedDoc(
        val title: String,
        val sidebarLabel: String,
        val id: String,
        val description: String,
        val bodyHtml: String,
        val firstExample: String?,
        val argsHtml: String?,
        val parameterNames: List<String>
    )

    /**
     * Parses a raw markdown file into structured sections.
     */
    fun parse(rawMarkdown: String): ParsedDoc {
        val (frontmatter, body) = splitFrontmatter(rawMarkdown)
        var title = extractFrontmatterField(frontmatter, "title") ?: ""
        var sidebarLabel = extractFrontmatterField(frontmatter, "sidebar_label") ?: ""
        var id = extractFrontmatterField(frontmatter, "id") ?: ""
        var description = extractFrontmatterField(frontmatter, "description") ?: ""

        // When frontmatter fields are blank, extract fallback metadata from the body
        if (title.isBlank() || sidebarLabel.isBlank() || id.isBlank() || description.isBlank()) {
            val fallback = extractFallbackMetadata(body)
            if (title.isBlank()) title = fallback.title
            if (sidebarLabel.isBlank()) sidebarLabel = fallback.sidebarLabel
            if (id.isBlank()) id = fallback.id
            if (description.isBlank()) description = fallback.description
        }

        val cleaned = stripMdxElements(body)
        val firstExample = extractFirstCodeBlock(cleaned)
        val argsResult = extractArgsSection(cleaned)
        val bodyHtml = markdownToHtml(cleaned)

        return ParsedDoc(
            title = title,
            sidebarLabel = sidebarLabel,
            id = id,
            description = description,
            bodyHtml = bodyHtml,
            firstExample = firstExample,
            argsHtml = argsResult?.html,
            parameterNames = argsResult?.parameterNames ?: emptyList()
        )
    }

    /**
     * Fallback metadata extracted from the markdown body when YAML frontmatter is absent.
     */
    internal data class FallbackMetadata(
        val title: String,
        val sidebarLabel: String,
        val id: String,
        val description: String
    )

    /**
     * Extracts metadata from the body when no YAML frontmatter is present.
     * Derives title from the first `# Heading`, function name from patterns like
     * "About ref function", and description from the first paragraph after
     * any heading and code block.
     */
    internal fun extractFallbackMetadata(body: String): FallbackMetadata {
        // Extract title from first # heading
        val headingRegex = Regex("""^#\s+(.+)$""", RegexOption.MULTILINE)
        val headingMatch = headingRegex.find(body)
        val title = headingMatch?.groupValues?.get(1)?.trim() ?: ""

        // Extract function/object name from heading pattern like "About ref function"
        // or "About run_query macro" or "About adapter object"
        val nameRegex = Regex("""^#\s+About\s+(\S+)""", RegexOption.MULTILINE)
        val nameMatch = nameRegex.find(body)
        val name = nameMatch?.groupValues?.get(1)?.trim() ?: ""

        // Extract description: first non-blank paragraph text that is not a heading,
        // code block, or bullet list item, after the initial heading
        var description = ""
        val lines = body.lines()
        var pastHeading = false
        var inCodeBlock = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# ") && !pastHeading) {
                pastHeading = true
                continue
            }
            if (!pastHeading) continue
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                continue
            }
            if (inCodeBlock) continue
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("#")) continue
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ")) continue
            description = trimmed
            break
        }

        return FallbackMetadata(
            title = title,
            sidebarLabel = name,
            id = name,
            description = description
        )
    }

    // -- Frontmatter parsing --

    internal data class FrontmatterSplit(val frontmatter: String, val body: String)

    internal fun splitFrontmatter(raw: String): FrontmatterSplit {
        val trimmed = raw.trimStart()
        if (!trimmed.startsWith("---")) {
            return FrontmatterSplit("", trimmed)
        }
        val endIndex = trimmed.indexOf("\n---", 3)
        if (endIndex < 0) {
            return FrontmatterSplit("", trimmed)
        }
        val frontmatter = trimmed.substring(3, endIndex).trim()
        val body = trimmed.substring(endIndex + 4).trimStart()
        return FrontmatterSplit(frontmatter, body)
    }

    internal fun extractFrontmatterField(frontmatter: String, field: String): String? {
        // Try quoted value first: field: "some text"
        val quotedRegex = Regex("""^$field:\s*"(.+)"\s*$""", RegexOption.MULTILINE)
        quotedRegex.find(frontmatter)?.let {
            return it.groupValues[1].trimEnd('"').trim()
        }
        // Fall back to unquoted value: field: some text
        val unquotedRegex = Regex("""^$field:\s*(.+?)\s*$""", RegexOption.MULTILINE)
        return unquotedRegex.find(frontmatter)?.groupValues?.get(1)?.trim()
    }

    // -- MDX stripping --

    internal fun stripMdxElements(markdown: String): String {
        var result = markdown

        // Remove import statements
        result = result.replace(Regex("""^import\s+.*$""", RegexOption.MULTILINE), "")

        // Replace <Constant name="..."/> with the name value
        result = result.replace(Regex("""<Constant\s+name="([^"]+)"\s*/?>""")) { match ->
            match.groupValues[1]
        }

        // Replace <Term id="...">text</Term> with the inner text
        result = result.replace(Regex("""<Term\s+[^>]*>([^<]*)</Term>""")) { match ->
            match.groupValues[1]
        }
        // Self-closing <Term id="..." />
        result = result.replace(Regex("""<Term\s+id="([^"]+)"\s*/>""")) { match ->
            match.groupValues[1]
        }

        // Remove <File name='...'> and </File> tags (keep content between them)
        result = result.replace(Regex("""<File\s+name='[^']*'>\s*"""), "")
        result = result.replace(Regex("""<File\s+name="[^"]*">\s*"""), "")
        result = result.replace(Regex("""\s*</File>"""), "")

        // Convert ::: admonitions to bold labels
        result = result.replace(
            Regex("""^:::(tip|warning|danger|info|note|caution)[ \t]*(.*?)$""", RegexOption.MULTILINE)
        ) { match ->
            val type = match.groupValues[1].replaceFirstChar { it.uppercase() }
            val title = match.groupValues[2].ifBlank { "" }
            if (title.isNotBlank()) "**$type: $title**" else "**$type**"
        }
        result = result.replace(Regex("""^:::$""", RegexOption.MULTILINE), "")

        // Replace HTML entities
        result = result.replace("&mdash;", " -- ")
        result = result.replace("&amp;", "&")

        // Remove escaped braces used for literal display in MDX
        result = result.replace("\\{", "{")
        result = result.replace("\\}", "}")

        return result.trim()
    }

    // -- Args extraction --

    internal data class ArgsResult(val html: String, val parameterNames: List<String>)

    internal fun extractArgsSection(markdown: String): ArgsResult? {
        // Look for __Args__: pattern
        val argsPattern = Regex("""__Args__:\s*\n((?:\s*\*\s+.+\n?)+)""")
        // Look for **Args**: pattern (bold markdown)
        val boldPattern = Regex("""\*\*Args\*\*:\s*\n((?:\s*\*\s+.+\n?)+)""")
        // Look for ## Arguments or ## Args pattern (with optional anchor suffix and blank lines)
        val altPattern = Regex("""##\s*(?:Arguments|Args)[^\n]*\n+((?:\s*\*\s+.+\n?)+)""")

        val argsBlock = argsPattern.find(markdown)?.groupValues?.get(1)
            ?: boldPattern.find(markdown)?.groupValues?.get(1)
            ?: altPattern.find(markdown)?.groupValues?.get(1)
            ?: return null
        return parseArgsList(argsBlock)
    }

    private fun parseArgsList(argsBlock: String): ArgsResult {
        val paramNames = mutableListOf<String>()
        val html = StringBuilder("<ul>")
        val paramRegex = Regex("""\*\s+`(\w+)`[^:]*:\s*(.+)""")

        for (line in argsBlock.lines()) {
            val paramMatch = paramRegex.find(line.trim())
            if (paramMatch != null) {
                val name = paramMatch.groupValues[1]
                val desc = paramMatch.groupValues[2].trim()
                paramNames.add(name)
                html.append("<li><code>")
                html.append(StringUtil.escapeXmlEntities(name))
                html.append("</code>: ")
                html.append(StringUtil.escapeXmlEntities(desc))
                html.append("</li>")
            }
        }
        html.append("</ul>")
        return ArgsResult(html.toString(), paramNames)
    }

    // -- Code block extraction --

    internal fun extractFirstCodeBlock(markdown: String): String? {
        val codeBlockRegex = Regex("""```\w*\s*\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
        val match = codeBlockRegex.find(markdown) ?: return null
        return match.groupValues[1].trim()
    }

    // -- Markdown to HTML conversion --

    internal fun markdownToHtml(markdown: String): String {
        val lines = markdown.lines()
        val html = StringBuilder()
        var inCodeBlock = false
        var inList = false
        var listType = ""

        for (line in lines) {
            val trimmed = line.trim()

            // Code block fences
            if (trimmed.startsWith("```") && !inCodeBlock) {
                if (inList) {
                    html.append(closeList(listType))
                    inList = false
                }
                inCodeBlock = true
                val lang = trimmed.removePrefix("```").trim()
                if (lang.isNotEmpty()) {
                    html.append("<pre><code data-lang=\"${StringUtil.escapeXmlEntities(lang)}\">")
                } else {
                    html.append("<pre><code>")
                }
                continue
            }
            if (trimmed.startsWith("```") && inCodeBlock) {
                inCodeBlock = false
                html.append("</code></pre>")
                continue
            }
            if (inCodeBlock) {
                html.append(StringUtil.escapeXmlEntities(line)).append("\n")
                continue
            }

            // Skip empty lines
            if (trimmed.isEmpty()) {
                if (inList) {
                    html.append(closeList(listType))
                    inList = false
                }
                continue
            }

            // Headers
            if (trimmed.startsWith("####")) {
                if (inList) { html.append(closeList(listType)); inList = false }
                html.append("<h4>").append(convertInlineMarkdown(trimmed.removePrefix("####").trim())).append("</h4>")
                continue
            }
            if (trimmed.startsWith("###")) {
                if (inList) { html.append(closeList(listType)); inList = false }
                html.append("<h3>").append(convertInlineMarkdown(trimmed.removePrefix("###").trim())).append("</h3>")
                continue
            }
            if (trimmed.startsWith("##")) {
                if (inList) { html.append(closeList(listType)); inList = false }
                html.append("<h2>").append(convertInlineMarkdown(trimmed.removePrefix("##").trim())).append("</h2>")
                continue
            }

            // Bullet list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList || listType != "ul") {
                    if (inList) html.append(closeList(listType))
                    html.append("<ul>")
                    inList = true
                    listType = "ul"
                }
                html.append("<li>").append(convertInlineMarkdown(trimmed.removePrefix("- ").removePrefix("* ").trim())).append("</li>")
                continue
            }

            // Numbered list
            val numberedMatch = Regex("""^\d+\.\s+(.+)""").find(trimmed)
            if (numberedMatch != null) {
                if (!inList || listType != "ol") {
                    if (inList) html.append(closeList(listType))
                    html.append("<ol>")
                    inList = true
                    listType = "ol"
                }
                html.append("<li>").append(convertInlineMarkdown(numberedMatch.groupValues[1])).append("</li>")
                continue
            }

            // Regular paragraph
            if (inList) {
                html.append(closeList(listType))
                inList = false
            }
            html.append("<p>").append(convertInlineMarkdown(trimmed)).append("</p>")
        }

        if (inList) {
            html.append(closeList(listType))
        }

        return html.toString()
    }

    private fun closeList(type: String): String = "</$type>"

    /**
     * Converts inline markdown elements (bold, italic, inline code, links) to HTML.
     */
    internal fun convertInlineMarkdown(text: String): String {
        var result = StringUtil.escapeXmlEntities(text)

        // Inline code: `text`
        result = result.replace(Regex("""`([^`]+)`""")) { match ->
            "<code>${match.groupValues[1]}</code>"
        }

        // Bold: **text** or __text__
        result = result.replace(Regex("""\*\*(.+?)\*\*""")) { match ->
            "<b>${match.groupValues[1]}</b>"
        }
        result = result.replace(Regex("""__(.+?)__""")) { match ->
            "<b>${match.groupValues[1]}</b>"
        }

        // Italic: *text*
        result = result.replace(Regex("""\*(.+?)\*""")) { match ->
            "<i>${match.groupValues[1]}</i>"
        }

        // Links: [text](url) -> just the text
        result = result.replace(Regex("""\[([^\]]+)]\([^)]+\)""")) { match ->
            match.groupValues[1]
        }

        return result
    }
}
