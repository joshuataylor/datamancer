package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.github.joshuataylor.datamancer.core.services.DbtBuiltinDocumentationService.DbtBuiltinFunctionDoc
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for [DatamancerBuiltinDocumentationTarget].
 * Tests HTML output, presentation, and pointer behaviour.
 */
class DatamancerBuiltinDocumentationTargetTest : BasePlatformTestCase() {

    // -- Interface conformance --

    fun testTargetImplementsDocumentationTarget() {
        val target = createTarget()
        assertTrue(target is DocumentationTarget)
    }

    // -- computePresentation --

    fun testComputePresentationContainsFunctionName() {
        val target = createTarget(functionName = "ref")
        val presentation = target.computePresentation()
        assertNotNull(presentation)
        assertTrue(
            "Presentation should contain function name",
            presentation.presentableText.contains("ref")
        )
    }

    fun testParameterisedPresentationContainsParentheses() {
        val target = createTarget(
            functionName = "ref",
            doc = createDoc(functionName = "ref", isParameterised = true, parameterNames = listOf("model_name"))
        )
        val presentation = target.computePresentation()
        assertEquals("ref(model_name)", presentation.presentableText)
    }

    fun testParameterisedPresentationContainsMultipleParams() {
        val target = createTarget(
            functionName = "source",
            doc = createDoc(
                functionName = "source",
                isParameterised = true,
                parameterNames = listOf("source_name", "table_name")
            )
        )
        val presentation = target.computePresentation()
        assertEquals("source(source_name, table_name)", presentation.presentableText)
    }

    fun testUnparameterisedPresentationShowsNameOnly() {
        val target = createTarget(
            functionName = "this",
            doc = createDoc(functionName = "this", isParameterised = false)
        )
        val presentation = target.computePresentation()
        assertEquals("this", presentation.presentableText)
    }

    // -- computeDocumentation: definition section --

    fun testDocumentationContainsFunctionName() {
        val target = createTarget(functionName = "ref")
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain function name", html!!.contains("ref"))
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
            doc = createDoc(bodyHtml = "<p>This function returns a Relation.</p>")
        )
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain body HTML", html!!.contains("This function returns a Relation."))
    }

    fun testDocumentationContainsBuiltInLabel() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain 'dbt built-in function' label", html!!.contains("dbt built-in function"))
    }

    fun testDocumentationBuiltInLabelIsGrayed() {
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
            doc = createDoc(externalDocUrl = "https://docs.getdbt.com/reference/dbt-jinja-functions/ref")
        )
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue(
            "Definition section should contain link to docs",
            html!!.contains("href=\"https://docs.getdbt.com/reference/dbt-jinja-functions/ref\"")
        )
    }

    fun testSignatureHasNoLinkWhenUrlIsNull() {
        val target = createTarget(doc = createDoc(externalDocUrl = null))
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertFalse("Should not contain docs.getdbt.com link when null", html!!.contains("docs.getdbt.com"))
    }

    fun testContentSectionContainsDocLinkAtTop() {
        val target = createTarget(
            doc = createDoc(externalDocUrl = "https://docs.getdbt.com/reference/dbt-jinja-functions/ref")
        )
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Content should contain visible link text", html!!.contains("View on docs.getdbt.com"))
    }

    fun testContentSectionOmitsDocLinkWhenUrlIsNull() {
        val target = createTarget(doc = createDoc(externalDocUrl = null))
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertFalse("Should not contain link text when null", html!!.contains("View on docs.getdbt.com"))
    }

    // -- navigatable --

    fun testNavigatableIsNull() {
        val target = createTarget()
        assertNull("Built-in functions should have null navigatable", target.navigatable)
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
            functionName = "ref",
            doc = createDoc(functionName = "ref", isParameterised = true, parameterNames = listOf("model_name"))
        )
        val pointer = target.createPointer()
        val dereferenced = pointer.dereference()
        assertNotNull(dereferenced)
        assertEquals("ref(model_name)", dereferenced!!.computePresentation().presentableText)
    }

    // -- Helpers --

    private fun createDoc(
        functionName: String = "ref",
        description: String = "Test description.",
        argsHtml: String? = null,
        parameterNames: List<String> = emptyList(),
        firstExample: String? = null,
        isParameterised: Boolean = true,
        externalDocUrl: String? = "https://docs.getdbt.com/reference/dbt-jinja-functions/ref",
        bodyHtml: String = "<p>Body HTML</p>"
    ): DbtBuiltinFunctionDoc {
        return DbtBuiltinFunctionDoc(
            functionName = functionName,
            title = "About $functionName",
            sidebarLabel = functionName,
            description = description,
            argsHtml = argsHtml,
            parameterNames = parameterNames,
            bodyHtml = bodyHtml,
            firstExample = firstExample,
            isParameterised = isParameterised,
            externalDocUrl = externalDocUrl
        )
    }

    private fun createTarget(
        functionName: String = "ref",
        doc: DbtBuiltinFunctionDoc = createDoc()
    ): DatamancerBuiltinDocumentationTarget {
        val file = myFixture.configureByText("test.txt", "placeholder")
        val element = file.findElementAt(0)!!
        return DatamancerBuiltinDocumentationTarget(
            element = element,
            project = project,
            functionName = functionName,
            doc = doc
        )
    }

    private fun getDocumentationHtml(target: DatamancerBuiltinDocumentationTarget): String? {
        val result = target.computeDocumentation()
        return (result as? DocumentationData)?.html
    }
}
