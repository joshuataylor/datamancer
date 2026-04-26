package com.github.joshuataylor.datamancer.core.documentation

import com.github.joshuataylor.datamancer.core.services.DbtBuiltinDocumentationService.DbtBuiltinFunctionDoc
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for [DatamancerDbtUtilsDocumentationTarget].
 * Tests HTML output, presentation, and pointer behaviour.
 */
class DatamancerDbtUtilsDocumentationTargetTest : BasePlatformTestCase() {

    // -- Interface conformance --

    fun testTargetImplementsDocumentationTarget() {
        val target = createTarget()
        assertTrue(target is DocumentationTarget)
    }

    // -- computePresentation --

    fun testComputePresentationContainsQualifiedName() {
        val target = createTarget(packageName = "dbt_utils", functionName = "star")
        val presentation = target.computePresentation()
        assertNotNull(presentation)
        assertTrue(
            "Presentation should contain qualified function name",
            presentation.presentableText.contains("dbt_utils.star")
        )
    }

    fun testPresentationWithParams() {
        val target = createTarget(
            functionName = "star",
            doc = createDoc(
                functionName = "star",
                parameterNames = listOf("from", "except")
            )
        )
        val presentation = target.computePresentation()
        assertEquals("dbt_utils.star(from, except)", presentation.presentableText)
    }

    fun testPresentationWithNoParams() {
        val target = createTarget(
            functionName = "pretty_time",
            doc = createDoc(functionName = "pretty_time", parameterNames = emptyList())
        )
        val presentation = target.computePresentation()
        assertEquals("dbt_utils.pretty_time()", presentation.presentableText)
    }

    // -- computeDocumentation: definition section --

    fun testDocumentationContainsFunctionName() {
        val target = createTarget(functionName = "star")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain function name", html!!.contains("star"))
    }

    fun testDocumentationContainsPackageName() {
        val target = createTarget(packageName = "dbt_utils")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain package name", html!!.contains("dbt_utils"))
    }

    fun testDocumentationContainsDefinitionSection() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain definition section", html!!.contains("class=\"definition\""))
    }

    fun testDocumentationContainsContentSection() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain content section", html!!.contains("class=\"content\""))
    }

    // -- computeDocumentation: content section --

    fun testDocumentationContainsBodyHtml() {
        val target = createTarget(
            doc = createDoc(bodyHtml = "<p>This macro generates a surrogate key.</p>")
        )
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain body HTML", html!!.contains("This macro generates a surrogate key."))
    }

    fun testDocumentationContainsDbtUtilsLabel() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain 'dbt_utils package' label", html!!.contains("dbt_utils package"))
    }

    fun testDocumentationDbtUtilsLabelIsGrayed() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should use grayed styling", html!!.contains("grayed"))
    }

    fun testDocumentationIsWrappedInHtmlBody() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should be wrapped in html body", html!!.contains("<html>") || html.contains("<body>"))
    }

    // -- Signature links to external docs --

    fun testSignatureLinksToExternalDocs() {
        val target = createTarget(
            doc = createDoc(externalDocUrl = "https://github.com/dbt-labs/dbt-utils#star-source")
        )
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue(
            "Definition section should contain link to GitHub",
            html!!.contains("href=\"https://github.com/dbt-labs/dbt-utils#star-source\"")
        )
    }

    fun testSignatureHasNoLinkWhenUrlIsNull() {
        val target = createTarget(doc = createDoc(externalDocUrl = null))
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertFalse("Should not contain GitHub link when null", html!!.contains("github.com/dbt-labs"))
    }

    fun testContentSectionContainsGitHubLinkAtTop() {
        val target = createTarget(
            doc = createDoc(externalDocUrl = "https://github.com/dbt-labs/dbt-utils#star-source")
        )
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Content should contain visible link text", html!!.contains("View on GitHub"))
    }

    fun testContentSectionOmitsGitHubLinkWhenUrlIsNull() {
        val target = createTarget(doc = createDoc(externalDocUrl = null))
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertFalse("Should not contain link text when null", html!!.contains("View on GitHub"))
    }

    // -- navigatable --

    fun testNavigatableIsNull() {
        val target = createTarget()
        assertNull("dbt_utils functions should have null navigatable", target.navigatable)
    }

    // -- createPointer --

    fun testCreatePointerReturnsNonNull() {
        val target = createTarget()
        val pointer = target.createPointer()
        assertNotNull(pointer)
    }

    fun testPointerDereferenceReturnsTarget() {
        val target = createTarget()
        val pointer = target.createPointer()
        val dereferenced = pointer.dereference()
        assertNotNull("Dereferenced pointer should return a target", dereferenced)
    }

    fun testPointerDereferencedTargetHasSamePresentation() {
        val target = createTarget(
            functionName = "star",
            doc = createDoc(
                functionName = "star",
                parameterNames = listOf("from", "except", "relation_alias", "prefix", "suffix", "quote_identifiers")
            )
        )
        val pointer = target.createPointer()
        val dereferenced = pointer.dereference()
        assertNotNull(dereferenced)
        assertEquals(
            "dbt_utils.star(from, except, relation_alias, prefix, suffix, quote_identifiers)",
            dereferenced!!.computePresentation().presentableText
        )
    }

    // -- Helpers --

    private fun createDoc(
        functionName: String = "generate_surrogate_key",
        description: String = "Test description.",
        argsHtml: String? = null,
        parameterNames: List<String> = emptyList(),
        firstExample: String? = null,
        externalDocUrl: String? = "https://github.com/dbt-labs/dbt-utils#generate_surrogate_key-source",
        bodyHtml: String = "<p>Body HTML</p>"
    ): DbtBuiltinFunctionDoc {
        return DbtBuiltinFunctionDoc(
            functionName = functionName,
            title = functionName,
            sidebarLabel = functionName,
            description = description,
            argsHtml = argsHtml,
            parameterNames = parameterNames,
            bodyHtml = bodyHtml,
            firstExample = firstExample,
            isParameterised = true,
            externalDocUrl = externalDocUrl
        )
    }

    private fun createTarget(
        packageName: String = "dbt_utils",
        functionName: String = "generate_surrogate_key",
        doc: DbtBuiltinFunctionDoc = createDoc()
    ): DatamancerDbtUtilsDocumentationTarget {
        val file = myFixture.configureByText("test.txt", "placeholder")
        val element = file.findElementAt(0)!!
        return DatamancerDbtUtilsDocumentationTarget(
            element = element,
            project = project,
            packageName = packageName,
            functionName = functionName,
            doc = doc
        )
    }

    private fun getDocumentationHtml(target: DatamancerDbtUtilsDocumentationTarget): String? {
        val result = target.computeDocumentation()
        return (result as? DocumentationData)?.html
    }
}
