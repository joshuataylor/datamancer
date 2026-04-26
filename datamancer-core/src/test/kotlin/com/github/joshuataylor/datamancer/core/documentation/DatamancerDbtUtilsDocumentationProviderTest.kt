package com.github.joshuataylor.datamancer.core.documentation

import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for [DatamancerDbtUtilsDocumentationProvider].
 * Tests interface conformance and negative cases.
 */
class DatamancerDbtUtilsDocumentationProviderTest : BasePlatformTestCase() {

    // -- Interface conformance --

    fun testProviderCanBeInstantiated() {
        val provider = DatamancerDbtUtilsDocumentationProvider()
        assertNotNull(provider)
    }

    fun testProviderImplementsDocumentationTargetProvider() {
        val provider = DatamancerDbtUtilsDocumentationProvider()
        assertTrue(
            "Should implement DocumentationTargetProvider",
            provider is DocumentationTargetProvider
        )
    }

    fun testProviderImplementsPsiDocumentationTargetProvider() {
        val provider = DatamancerDbtUtilsDocumentationProvider()
        assertTrue(
            "Should implement PsiDocumentationTargetProvider",
            provider is PsiDocumentationTargetProvider
        )
    }

    // -- Negative cases: non-template files --

    fun testDocumentationTargetsReturnsEmptyForPlainTextFile() {
        val file = myFixture.configureByText("test.txt", "hello world")
        val provider = DatamancerDbtUtilsDocumentationProvider()
        val targets = provider.documentationTargets(file, 0)
        assertNotNull(targets)
        assertTrue("Should return empty list for plain text file", targets.isEmpty())
    }

    fun testDocumentationTargetsReturnsEmptyForJsonFile() {
        val file = myFixture.configureByText("test.json", """{"key": "value"}""")
        val provider = DatamancerDbtUtilsDocumentationProvider()
        val targets = provider.documentationTargets(file, 2)
        assertTrue("Should return empty list for JSON file", targets.isEmpty())
    }

    fun testDocumentationTargetsReturnsEmptyForYamlFile() {
        val file = myFixture.configureByText("schema.yml", "models:\n  - name: foo")
        val provider = DatamancerDbtUtilsDocumentationProvider()
        val targets = provider.documentationTargets(file, 0)
        assertTrue("Should return empty list for YAML file", targets.isEmpty())
    }

    // -- Negative cases: PSI element API --

    fun testDocumentationTargetReturnsNullForNonDbtElement() {
        val file = myFixture.configureByText("test.txt", "hello world")
        val element = file.findElementAt(0) ?: return
        val provider = DatamancerDbtUtilsDocumentationProvider()
        val target = provider.documentationTarget(element, element)
        assertNull("Should return null for non-dbt element", target)
    }

    // -- Negative cases: offset boundaries --

    fun testDocumentationTargetsReturnsEmptyForBeyondEndOffset() {
        val file = myFixture.configureByText("test.txt", "hi")
        val provider = DatamancerDbtUtilsDocumentationProvider()
        val targets = provider.documentationTargets(file, 999)
        assertTrue(targets.isEmpty())
    }
}
