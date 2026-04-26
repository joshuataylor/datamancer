package com.github.joshuataylor.datamancer.core.services

import junit.framework.TestCase

/**
 * Tests for [DbtMarkdownConverter].
 * Tests MDX stripping, markdown-to-HTML conversion, and structured section extraction.
 */
class DbtMarkdownConverterTest : TestCase() {

    // -- Frontmatter parsing --

    fun testSplitFrontmatterExtractsBody() {
        val raw = """
            |---
            |title: "About ref"
            |---
            |Body content here.
        """.trimMargin()
        val (_, body) = DbtMarkdownConverter.splitFrontmatter(raw)
        assertEquals("Body content here.", body)
    }

    fun testSplitFrontmatterExtractsFrontmatter() {
        val raw = """
            |---
            |title: "About ref"
            |sidebar_label: "ref"
            |---
            |Body content here.
        """.trimMargin()
        val (frontmatter, _) = DbtMarkdownConverter.splitFrontmatter(raw)
        assertTrue(frontmatter.contains("title:"))
        assertTrue(frontmatter.contains("sidebar_label:"))
    }

    fun testSplitFrontmatterHandlesNoFrontmatter() {
        val raw = "Just plain content."
        val (frontmatter, body) = DbtMarkdownConverter.splitFrontmatter(raw)
        assertEquals("", frontmatter)
        assertEquals("Just plain content.", body)
    }

    fun testExtractFrontmatterFieldTitle() {
        val frontmatter = """title: "About ref function""""
        assertEquals("About ref function", DbtMarkdownConverter.extractFrontmatterField(frontmatter, "title"))
    }

    fun testExtractFrontmatterFieldSidebarLabel() {
        val frontmatter = """sidebar_label: "ref""""
        assertEquals("ref", DbtMarkdownConverter.extractFrontmatterField(frontmatter, "sidebar_label"))
    }

    fun testExtractFrontmatterFieldDescription() {
        val frontmatter = """description: "Read this guide to understand ref.""""""
        assertEquals(
            "Read this guide to understand ref.",
            DbtMarkdownConverter.extractFrontmatterField(frontmatter, "description")
        )
    }

    fun testExtractFrontmatterFieldId() {
        val frontmatter = """id: "ref""""
        assertEquals("ref", DbtMarkdownConverter.extractFrontmatterField(frontmatter, "id"))
    }

    fun testExtractFrontmatterFieldReturnsNullForMissingField() {
        val frontmatter = """title: "About ref""""
        assertNull(DbtMarkdownConverter.extractFrontmatterField(frontmatter, "description"))
    }

    // -- MDX element stripping --

    fun testStripImportStatements() {
        val input = """import Foo from '/snippets/_foo.md';

Some content."""
        val result = DbtMarkdownConverter.stripMdxElements(input)
        assertFalse(result.contains("import"))
        assertTrue(result.contains("Some content."))
    }

    fun testStripFileComponents() {
        val input = """<File name='models/my_model.sql'>

```sql
select 1
```

</File>"""
        val result = DbtMarkdownConverter.stripMdxElements(input)
        assertFalse(result.contains("<File"))
        assertFalse(result.contains("</File>"))
        assertTrue(result.contains("select 1"))
    }

    fun testReplaceConstantComponents() {
        val input = """Use <Constant name="cloud" /> for deployment."""
        val result = DbtMarkdownConverter.stripMdxElements(input)
        assertFalse(result.contains("<Constant"))
        assertTrue(result.contains("cloud"))
    }

    fun testReplaceTermComponents() {
        val input = """A <Term id="materialization">materialisation</Term> is a strategy."""
        val result = DbtMarkdownConverter.stripMdxElements(input)
        assertFalse(result.contains("<Term"))
        assertTrue(result.contains("materialisation"))
    }

    fun testConvertAdmonitions() {
        val input = """:::tip Using run_query
Check the guide.
:::"""
        val result = DbtMarkdownConverter.stripMdxElements(input)
        assertTrue(result.contains("**Tip: Using run_query**"))
        assertFalse(result.contains(":::"))
    }

    fun testConvertAdmonitionWithoutTitle() {
        val input = """:::warning
Be careful.
:::"""
        val result = DbtMarkdownConverter.stripMdxElements(input)
        assertTrue(result.contains("**Warning**"))
    }

    fun testReplaceMdash() {
        val input = "first &mdash; second"
        val result = DbtMarkdownConverter.stripMdxElements(input)
        assertEquals("first  --  second", result)
    }

    fun testRemoveEscapedBraces() {
        val input = """\{\{ this \}\}"""
        val result = DbtMarkdownConverter.stripMdxElements(input)
        assertEquals("{{ this }}", result)
    }

    // -- Code block extraction --

    fun testExtractFirstCodeBlock() {
        val markdown = """Some text.

```sql
select * from {{ ref("orders") }}
```

More text."""
        val result = DbtMarkdownConverter.extractFirstCodeBlock(markdown)
        assertNotNull(result)
        assertEquals("""select * from {{ ref("orders") }}""", result)
    }

    fun testExtractFirstCodeBlockReturnsNullWhenNone() {
        val markdown = "Just plain text with no code blocks."
        assertNull(DbtMarkdownConverter.extractFirstCodeBlock(markdown))
    }

    // -- Args extraction --

    fun testExtractArgsSectionWithUnderscorePattern() {
        val markdown = """__Args__:

 * `sql`: The SQL query to execute
 * `default`: Default value

Some other text."""
        val result = DbtMarkdownConverter.extractArgsSection(markdown)
        assertNotNull(result)
        assertEquals(listOf("sql", "default"), result!!.parameterNames)
        assertTrue(result.html.contains("<code>sql</code>"))
        assertTrue(result.html.contains("<code>default</code>"))
    }

    fun testExtractArgsSectionWithHashPattern() {
        val markdown = """## Arguments
* `source_name`: The name of the source
* `table_name`: The name of the table

## Example"""
        val result = DbtMarkdownConverter.extractArgsSection(markdown)
        assertNotNull(result)
        assertEquals(listOf("source_name", "table_name"), result!!.parameterNames)
    }

    fun testExtractArgsSectionReturnsNullWhenNone() {
        val markdown = """## Definition
This function does something.

## Example
```sql
select 1
```"""
        assertNull(DbtMarkdownConverter.extractArgsSection(markdown))
    }

    // -- Markdown to HTML conversion --

    fun testConvertCodeBlocksWithLanguage() {
        val markdown = """```sql
select 1
```"""
        val html = DbtMarkdownConverter.markdownToHtml(markdown)
        assertTrue(html.contains("""<pre><code data-lang="sql">"""))
        assertTrue(html.contains("select 1"))
        assertTrue(html.contains("</code></pre>"))
    }

    fun testConvertCodeBlocksWithoutLanguage() {
        val markdown = """```
select 1
```"""
        val html = DbtMarkdownConverter.markdownToHtml(markdown)
        assertTrue("Code block without language should have no data-lang", html.contains("<pre><code>"))
        assertFalse(html.contains("data-lang"))
    }

    fun testConvertHeaders() {
        val markdown = "## Definition"
        val html = DbtMarkdownConverter.markdownToHtml(markdown)
        assertTrue(html.contains("<h2>"))
        assertTrue(html.contains("Definition"))
    }

    fun testConvertBulletLists() {
        val markdown = """- Item one
- Item two"""
        val html = DbtMarkdownConverter.markdownToHtml(markdown)
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>"))
        assertTrue(html.contains("Item one"))
        assertTrue(html.contains("Item two"))
    }

    fun testConvertNumberedLists() {
        val markdown = """1. First
2. Second"""
        val html = DbtMarkdownConverter.markdownToHtml(markdown)
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>"))
    }

    fun testConvertInlineCode() {
        val result = DbtMarkdownConverter.convertInlineMarkdown("Use `ref()` function")
        assertTrue(result.contains("<code>ref()</code>"))
    }

    fun testConvertBoldText() {
        val result = DbtMarkdownConverter.convertInlineMarkdown("This is **important** text")
        assertTrue(result.contains("<b>important</b>"))
    }

    fun testConvertLinks() {
        val result = DbtMarkdownConverter.convertInlineMarkdown("See [Relation](/reference/dbt-classes#relation)")
        assertTrue(result.contains("Relation"))
        assertFalse(result.contains("/reference/"))
    }

    fun testHtmlEntitiesAreEscaped() {
        val result = DbtMarkdownConverter.convertInlineMarkdown("a < b & c > d")
        assertTrue(result.contains("&lt;"))
        assertTrue(result.contains("&amp;"))
        assertTrue(result.contains("&gt;"))
    }

    // -- Full parse --

    fun testParseExtractsFrontmatterAndBody() {
        val raw = """---
title: "About ref function"
sidebar_label: "ref"
id: "ref"
description: "Read this guide to understand ref."
---

```sql
select * from {{ ref("model") }}
```

## Definition

This function returns a Relation."""

        val parsed = DbtMarkdownConverter.parse(raw)
        assertEquals("About ref function", parsed.title)
        assertEquals("ref", parsed.sidebarLabel)
        assertEquals("ref", parsed.id)
        assertEquals("Read this guide to understand ref.", parsed.description)
        assertNotNull(parsed.firstExample)
        assertTrue(parsed.bodyHtml.contains("Definition"))
    }

    fun testParseExtractsArgsFromConfigDoc() {
        val raw = """---
title: "About config"
sidebar_label: "config"
id: "config"
description: "Config variable."
---

## config.get
__Args__:

 * `name`: The name of the config (required)
 * `default`: Default value (optional)

Some description."""

        val parsed = DbtMarkdownConverter.parse(raw)
        assertNotNull(parsed.argsHtml)
        assertEquals(listOf("name", "default"), parsed.parameterNames)
    }
}
