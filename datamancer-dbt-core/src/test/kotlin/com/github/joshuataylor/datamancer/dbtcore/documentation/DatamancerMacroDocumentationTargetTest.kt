package com.github.joshuataylor.datamancer.dbtcore.documentation

import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Tests for DatamancerMacroDocumentationTarget.
 * Tests HTML output, presentation, and pointer behaviour.
 */
class DatamancerMacroDocumentationTargetTest : BasePlatformTestCase() {

    // -- Interface conformance --

    fun testTargetImplementsDocumentationTarget() {
        val target = createTarget()
        assertTrue(target is DocumentationTarget)
    }

    // -- computePresentation --

    fun testComputePresentationContainsMacroName() {
        val target = createTarget()
        val presentation = target.computePresentation()
        assertNotNull(presentation)
        assertTrue(
            "Presentation should contain macro name",
            presentation.presentableText.contains("testf")
        )
    }

    fun testComputePresentationContainsEmptyParens() {
        val target = createTarget(parameters = emptyList())
        val presentation = target.computePresentation()
        assertEquals("testf()", presentation.presentableText)
    }

    fun testComputePresentationContainsParameters() {
        val target = createTarget(macroName = "testf2", parameters = listOf("hello"))
        val presentation = target.computePresentation()
        assertEquals("testf2(hello)", presentation.presentableText)
    }

    fun testComputePresentationContainsMultipleParameters() {
        val target = createTarget(
            macroName = "my_macro",
            parameters = listOf("arg1", "arg2", "arg3")
        )
        val presentation = target.computePresentation()
        assertEquals("my_macro(arg1, arg2, arg3)", presentation.presentableText)
    }

    // -- computeDocumentation: definition section --

    fun testDocumentationContainsMacroName() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain macro name", html!!.contains("testf"))
    }

    fun testDocumentationContainsMacroKeyword() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain 'macro' keyword", html!!.contains("macro"))
    }

    fun testDocumentationContainsParameterNames() {
        val target = createTarget(parameters = listOf("hello", "world"))
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain parameter 'hello'", html!!.contains("hello"))
        assertTrue("Should contain parameter 'world'", html.contains("world"))
    }

    fun testDocumentationContainsParentheses() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain opening parenthesis", html!!.contains("("))
        assertTrue("Should contain closing parenthesis", html.contains(")"))
    }

    // -- computeDocumentation: content section --

    fun testDocumentationContainsFilePath() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain file path", html!!.contains("macros/testf.sql"))
    }

    fun testDocumentationFilePathIsGrayed() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should use grayed styling for file path", html!!.contains("grayed"))
    }

    fun testDocumentationContainsDefinedInPrefix() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should contain 'Defined in' text", html!!.contains("Defined in"))
    }

    // -- computeDocumentation: structure --

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

    fun testDocumentationIsWrappedInHtmlBody() {
        val target = createTarget()
        val html = getDocumentationHtml(target)
        assertNotNull(html)
        assertTrue("Should be wrapped in html body", html!!.contains("<html>") || html.contains("<body>"))
    }

    // -- navigatable --

    fun testNavigatableIsNullWithoutTargetElement() {
        val target = createTarget()
        assertNull("Should return null navigatable without target element", target.navigatable)
    }

    // -- createPointer --

    fun testCreatePointerReturnsNonNull() {
        val target = createTarget()
        val pointer = target.createPointer()
        assertNotNull(pointer)
    }

    @RequiresReadLock
    fun testPointerDereferenceReturnsTarget() {
        val target = createTarget()
        val pointer = target.createPointer()
        val dereferenced = pointer.dereference()
        assertNotNull("Dereferenced pointer should return a target", dereferenced)
    }

    @RequiresReadLock
    fun testPointerDereferencedTargetHasSamePresentation() {
        val target = createTarget(macroName = "testf2", parameters = listOf("hello"))
        val pointer = target.createPointer()
        val dereferenced = pointer.dereference()
        assertNotNull(dereferenced)
        assertEquals(
            "testf2(hello)",
            dereferenced!!.computePresentation().presentableText
        )
    }

    // -- Helpers --

    private fun createTarget(
        macroName: String = "testf",
        parameters: List<String> = emptyList(),
        macroFilePath: String = "macros/testf.sql",
        targetElement: com.intellij.psi.PsiElement? = null
    ): DatamancerMacroDocumentationTarget {
        val file = myFixture.configureByText("test.txt", "placeholder")
        val element = file.findElementAt(0)!!
        return DatamancerMacroDocumentationTarget(
            element = element,
            project = project,
            macroName = macroName,
            parameters = parameters,
            macroFilePath = macroFilePath,
            targetElement = targetElement
        )
    }

    private fun getDocumentationHtml(target: DatamancerMacroDocumentationTarget): String? {
        val result = target.computeDocumentation()
        return (result as? DocumentationData)?.html
    }
}
